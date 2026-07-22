package ru.badmintonlab.worker.snapshot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import ru.badmintonlab.core.domain.Discipline;
import ru.badmintonlab.core.domain.TournamentStatus;
import ru.badmintonlab.core.entity.SnapshotMeta;
import ru.badmintonlab.core.repository.SnapshotMetaRepository;
import ru.badmintonlab.parser.PlayerProfileParser;
import ru.badmintonlab.parser.TournamentGamesParser;
import ru.badmintonlab.parser.TournamentListParser;
import ru.badmintonlab.parser.TournamentResultsParser;
import ru.badmintonlab.parser.model.PairMatch;
import ru.badmintonlab.parser.model.PlayerProfile;
import ru.badmintonlab.parser.model.TournamentListEntry;
import ru.badmintonlab.parser.model.TournamentResults;
import ru.badmintonlab.worker.config.ParserProperties;
import ru.badmintonlab.worker.config.SnapshotProperties;
import ru.badmintonlab.worker.http.Badminton4uClient;
import ru.badmintonlab.worker.http.HttpFetcher;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Оркестратор слепка региона: список завершённых турниров по дисциплинам → парсинг итогов и
 * матчей → upsert игроков, участия, матчей → пересборка rival_summary → snapshot_meta.
 * Многопоточно и rate-limited (ограничение на уровне {@link HttpFetcher}).
 */
@Service
public class SnapshotService {

    private static final Logger log = LoggerFactory.getLogger(SnapshotService.class);

    private final Badminton4uClient client;
    private final SnapshotProperties snapshotProperties;
    private final ParserProperties parserProperties;
    private final PlayerUpsertService playerUpsertService;
    private final TournamentUpsertService tournamentUpsertService;
    private final ResultUpsertService resultUpsertService;
    private final MatchUpsertService matchUpsertService;
    private final RivalSummaryRebuildService rivalSummaryRebuildService;
    private final SnapshotMetaRepository snapshotMetaRepository;

    private final TournamentListParser listParser = new TournamentListParser();
    private final TournamentResultsParser resultsParser = new TournamentResultsParser();
    private final TournamentGamesParser gamesParser = new TournamentGamesParser();
    private final PlayerProfileParser profileParser = new PlayerProfileParser();

    private final AtomicBoolean running = new AtomicBoolean(false);

    public SnapshotService(Badminton4uClient client,
                           SnapshotProperties snapshotProperties,
                           ParserProperties parserProperties,
                           PlayerUpsertService playerUpsertService,
                           TournamentUpsertService tournamentUpsertService,
                           ResultUpsertService resultUpsertService,
                           MatchUpsertService matchUpsertService,
                           RivalSummaryRebuildService rivalSummaryRebuildService,
                           SnapshotMetaRepository snapshotMetaRepository) {
        this.client = client;
        this.snapshotProperties = snapshotProperties;
        this.parserProperties = parserProperties;
        this.playerUpsertService = playerUpsertService;
        this.tournamentUpsertService = tournamentUpsertService;
        this.resultUpsertService = resultUpsertService;
        this.matchUpsertService = matchUpsertService;
        this.rivalSummaryRebuildService = rivalSummaryRebuildService;
        this.snapshotMetaRepository = snapshotMetaRepository;
    }

    /**
     * Запускает полный слепок. Повторный запуск при уже идущем слепке игнорируется.
     *
     * @return метрики прогона, либо {@code null}, если слепок уже выполняется.
     */
    public SnapshotMetrics runSnapshot() {
        if (!running.compareAndSet(false, true)) {
            log.warn("Слепок уже выполняется — повторный запуск пропущен");
            return null;
        }
        SnapshotMetrics metrics = new SnapshotMetrics();
        LocalDate to = LocalDate.now(SnapshotSupport.SOURCE_ZONE);
        LocalDate from = to.minusYears(snapshotProperties.yearsBack());
        String region = snapshotProperties.regionCode();
        ExecutorService pool = Executors.newFixedThreadPool(parserProperties.threads());
        try {
            log.info("Старт слепка: регион={}, окно {}..{}, дисциплины={}",
                    region, from, to, snapshotProperties.disciplines());

            Map<Long, TournamentTask> tasks = discoverTournaments(region, from, to, metrics);
            for (TournamentTask task : tasks.values()) {
                tournamentUpsertService.upsert(task.entry(), region, TournamentStatus.COMPLETED);
            }

            List<ParsedTournament> parsed = parseTournaments(tasks.values(), pool, metrics);
            upsertPlayers(parsed, pool, metrics);
            persistResultsAndMatches(parsed, pool, metrics);

            metrics.setRivalRows(rivalSummaryRebuildService.rebuild());
            updateSnapshotMeta(region, from, to);

            log.info("Слепок завершён: {}", metrics);
        } catch (RuntimeException e) {
            log.error("Слепок прерван ошибкой: {}", e.toString(), e);
            throw e;
        } finally {
            pool.shutdownNow();
            running.set(false);
        }
        return metrics;
    }

    /**
     * Точечный импорт завершённых турниров по ID (без полного обхода списка).
     * Идемпотентен: повторный прогон дополняет participation/match, не дублирует матчи.
     */
    public SnapshotMetrics runTournamentIds(List<Long> tournamentIds) {
        if (tournamentIds == null || tournamentIds.isEmpty()) {
            throw new IllegalArgumentException("tournamentIds must not be empty");
        }
        if (!running.compareAndSet(false, true)) {
            log.warn("Слепок уже выполняется — точечный импорт пропущен");
            return null;
        }
        SnapshotMetrics metrics = new SnapshotMetrics();
        String region = snapshotProperties.regionCode();
        ExecutorService pool = Executors.newFixedThreadPool(parserProperties.threads());
        try {
            log.info("Точечный импорт: {} турниров, регион={}", tournamentIds.size(), region);
            List<ParsedTournament> parsed = new ArrayList<>();
            for (Long id : tournamentIds) {
                ParsedTournament pt = parseSingleTournament(id, metrics);
                if (pt != null) {
                    parsed.add(pt);
                }
            }
            upsertPlayers(parsed, pool, metrics);
            persistResultsAndMatches(parsed, pool, metrics);
            metrics.setRivalRows(rivalSummaryRebuildService.rebuild());
            touchSnapshotMeta(region);
            log.info("Точечный импорт завершён: {}", metrics);
        } catch (RuntimeException e) {
            log.error("Точечный импорт прерван ошибкой: {}", e.toString(), e);
            throw e;
        } finally {
            pool.shutdownNow();
            running.set(false);
        }
        return metrics;
    }

    private ParsedTournament parseSingleTournament(long id, SnapshotMetrics metrics) {
        try {
            var page = client.tournamentPage(id);
            Discipline discipline = SnapshotSupport.inferDisciplineFromPage(page);
            TournamentResults results = resultsParser.parse(page);
            List<PairMatch> matches = gamesParser.parse(client.tournamentGames(id));

            Set<Long> playerIds = new java.util.HashSet<>();
            results.pairs().forEach(p -> {
                playerIds.add(p.player1Id());
                playerIds.add(p.player2Id());
            });
            matches.forEach(m -> {
                m.sideA().forEach(mp -> playerIds.add(mp.playerId()));
                m.sideB().forEach(mp -> playerIds.add(mp.playerId()));
            });
            metrics.incTournament();
            return new ParsedTournament(id, discipline, results, matches, playerIds);
        } catch (RuntimeException e) {
            metrics.incError();
            log.warn("Ошибка обработки турнира {}: {}", id, e.toString());
            return null;
        }
    }

    private void touchSnapshotMeta(String region) {
        snapshotMetaRepository.findById(region).ifPresent(meta -> {
            meta.setLastSyncAt(Instant.now());
            snapshotMetaRepository.save(meta);
        });
    }

    private Map<Long, TournamentTask> discoverTournaments(String region, LocalDate from, LocalDate to,
                                                          SnapshotMetrics metrics) {
        Map<Long, TournamentTask> tasks = new LinkedHashMap<>();
        for (Discipline discipline : snapshotProperties.disciplines()) {
            try {
                List<TournamentListEntry> entries = listParser.parse(
                        client.completedTournaments(region, discipline, from, to));
                for (TournamentListEntry entry : entries) {
                    tasks.putIfAbsent(entry.id(), new TournamentTask(entry.id(), discipline, entry));
                }
                log.info("Дисциплина {}: найдено {} турниров", discipline, entries.size());
            } catch (RuntimeException e) {
                metrics.incError();
                log.warn("Не удалось получить список турниров для {}: {}", discipline, e.toString());
            }
        }
        metrics.addDiscovered(tasks.size());

        if (!snapshotProperties.isUnlimited() && tasks.size() > snapshotProperties.maxTournaments()) {
            Map<Long, TournamentTask> limited = new LinkedHashMap<>();
            for (TournamentTask task : tasks.values()) {
                if (limited.size() >= snapshotProperties.maxTournaments()) {
                    break;
                }
                limited.put(task.id(), task);
            }
            log.info("Дымовой режим: ограничение {} турниров (из {})",
                    snapshotProperties.maxTournaments(), tasks.size());
            return limited;
        }
        return tasks;
    }

    private List<ParsedTournament> parseTournaments(Iterable<TournamentTask> tasks, ExecutorService pool,
                                                    SnapshotMetrics metrics) {
        List<Callable<ParsedTournament>> jobs = new ArrayList<>();
        for (TournamentTask task : tasks) {
            jobs.add(() -> parseTournament(task, metrics));
        }
        List<ParsedTournament> result = new ArrayList<>();
        for (ParsedTournament parsed : invokeAll(pool, jobs)) {
            if (parsed != null) {
                result.add(parsed);
            }
        }
        return result;
    }

    private ParsedTournament parseTournament(TournamentTask task, SnapshotMetrics metrics) {
        try {
            TournamentResults results = resultsParser.parse(client.tournamentPage(task.id()));
            List<PairMatch> matches = gamesParser.parse(client.tournamentGames(task.id()));

            Set<Long> playerIds = new java.util.HashSet<>();
            results.pairs().forEach(p -> {
                playerIds.add(p.player1Id());
                playerIds.add(p.player2Id());
            });
            matches.forEach(m -> {
                m.sideA().forEach(mp -> playerIds.add(mp.playerId()));
                m.sideB().forEach(mp -> playerIds.add(mp.playerId()));
            });
            metrics.incTournament();
            return new ParsedTournament(task.id(), task.discipline(), results, matches, playerIds);
        } catch (RuntimeException e) {
            metrics.incError();
            log.warn("Ошибка обработки турнира {}: {}", task.id(), e.toString());
            return null;
        }
    }

    private void upsertPlayers(List<ParsedTournament> parsed, ExecutorService pool, SnapshotMetrics metrics) {
        Set<Long> uniquePlayers = ConcurrentHashMap.newKeySet();
        parsed.forEach(pt -> uniquePlayers.addAll(pt.playerIds()));

        List<Callable<Void>> jobs = new ArrayList<>();
        for (Long playerId : uniquePlayers) {
            jobs.add(() -> {
                upsertPlayer(playerId, metrics);
                return null;
            });
        }
        invokeAll(pool, jobs);
    }

    private void upsertPlayer(long playerId, SnapshotMetrics metrics) {
        try {
            PlayerProfile profile = profileParser.parse(client.playerProfile(playerId));
            playerUpsertService.upsert(profile);
            metrics.incPlayer();
        } catch (HttpFetcher.FetchException e) {
            metrics.incError();
            log.warn("Профиль игрока {} недоступен: {} — сохраняю заглушку", playerId, e.toString());
            playerUpsertService.ensurePlayer(playerId, null);
        } catch (RuntimeException e) {
            metrics.incError();
            log.warn("Ошибка профиля игрока {}: {} — сохраняю заглушку", playerId, e.toString());
            playerUpsertService.ensurePlayer(playerId, null);
        }
    }

    private void persistResultsAndMatches(List<ParsedTournament> parsed, ExecutorService pool,
                                          SnapshotMetrics metrics) {
        List<Callable<Void>> jobs = new ArrayList<>();
        for (ParsedTournament pt : parsed) {
            jobs.add(() -> {
                try {
                    resultUpsertService.upsert(pt.id(), pt.results(), pt.discipline());
                    int inserted = matchUpsertService.upsert(pt.matches(), pt.discipline(), pt.id());
                    metrics.addMatches(inserted);
                } catch (RuntimeException e) {
                    metrics.incError();
                    log.warn("Ошибка записи итогов/матчей турнира {}: {}", pt.id(), e.toString());
                }
                return null;
            });
        }
        invokeAll(pool, jobs);
    }

    private void updateSnapshotMeta(String region, LocalDate from, LocalDate to) {
        SnapshotMeta meta = snapshotMetaRepository.findById(region)
                .orElseGet(() -> new SnapshotMeta(region, Instant.now(), from, to));
        meta.setLastSyncAt(Instant.now());
        meta.setTournamentsFrom(from);
        meta.setTournamentsTo(to);
        snapshotMetaRepository.save(meta);
    }

    private <T> List<T> invokeAll(ExecutorService pool, List<Callable<T>> jobs) {
        List<T> results = new ArrayList<>(jobs.size());
        List<Future<T>> futures = new ArrayList<>(jobs.size());
        for (Callable<T> job : jobs) {
            futures.add(pool.submit(job));
        }
        for (Future<T> future : futures) {
            try {
                results.add(future.get());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Слепок прерван", e);
            } catch (ExecutionException e) {
                throw new IllegalStateException("Ошибка задачи слепка", e.getCause());
            }
        }
        return results;
    }

    private record TournamentTask(long id, Discipline discipline, TournamentListEntry entry) {
    }

    private record ParsedTournament(long id, Discipline discipline, TournamentResults results,
                                    List<PairMatch> matches, Set<Long> playerIds) {
    }
}
