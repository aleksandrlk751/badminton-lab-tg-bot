package ru.badmintonlab.worker.snapshot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.badmintonlab.core.domain.TournamentStatus;
import ru.badmintonlab.core.entity.Tournament;
import ru.badmintonlab.core.entity.TournamentRegistration;
import ru.badmintonlab.core.entity.TournamentRegistrationId;
import ru.badmintonlab.core.repository.TournamentRegistrationRepository;
import ru.badmintonlab.core.repository.TournamentRepository;
import ru.badmintonlab.parser.TournamentListParser;
import ru.badmintonlab.parser.TournamentPageParser;
import ru.badmintonlab.parser.TournamentRatingLimitsParser;
import ru.badmintonlab.parser.TournamentRegistrationParser;
import ru.badmintonlab.parser.model.TournamentListEntry;
import ru.badmintonlab.parser.model.TournamentPageMeta;
import ru.badmintonlab.worker.config.SnapshotProperties;
import ru.badmintonlab.worker.http.Badminton4uClient;

import java.time.Instant;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Синхронизация ближайших парных турниров и регистраций (этап 6 — подбор партнёра).
 */
@Service
public class UpcomingTournamentsSyncService {

    private static final Logger log = LoggerFactory.getLogger(UpcomingTournamentsSyncService.class);
    private static final int DEFAULT_HORIZON_DAYS = 90;
    private static final int MAX_TOURNAMENTS = 40;

    private final Badminton4uClient client;
    private final SnapshotProperties snapshotProperties;
    private final TournamentRepository tournamentRepository;
    private final TournamentRegistrationRepository registrationRepository;
    private final PairService pairService;
    private final PlayerUpsertService playerUpsertService;

    private final TournamentListParser listParser = new TournamentListParser();
    private final TournamentPageParser pageParser = new TournamentPageParser();
    private final TournamentRatingLimitsParser ratingLimitsParser = new TournamentRatingLimitsParser();
    private final TournamentRegistrationParser registrationParser = new TournamentRegistrationParser();

    public UpcomingTournamentsSyncService(Badminton4uClient client,
                                          SnapshotProperties snapshotProperties,
                                          TournamentRepository tournamentRepository,
                                          TournamentRegistrationRepository registrationRepository,
                                          PairService pairService,
                                          PlayerUpsertService playerUpsertService) {
        this.client = client;
        this.snapshotProperties = snapshotProperties;
        this.tournamentRepository = tournamentRepository;
        this.registrationRepository = registrationRepository;
        this.pairService = pairService;
        this.playerUpsertService = playerUpsertService;
    }

    @Transactional
    public int syncRegion() {
        String region = snapshotProperties.regionCode();
        LocalDate from = LocalDate.now(SnapshotSupport.SOURCE_ZONE);
        LocalDate to = from.plusDays(DEFAULT_HORIZON_DAYS);
        var doc = client.upcomingPairTournaments(region, from, to);
        List<TournamentListEntry> entries = listParser.parse(doc).stream()
                .filter(TournamentListEntry::doubles)
                .filter(e -> !e.date().isBefore(from))
                .limit(MAX_TOURNAMENTS)
                .toList();

        int processed = 0;
        for (TournamentListEntry entry : entries) {
            try {
                syncTournament(entry, region);
                processed++;
            } catch (RuntimeException e) {
                log.warn("Не удалось синхронизировать турнир {}: {}", entry.id(), e.toString());
            }
        }
        log.info("Синхронизация будущих турниров: {} из {}", processed, entries.size());
        return processed;
    }

    /**
     * Точечная синхронизация страницы турнира (регистрация + метаданные). Для бота: вставка ссылки.
     *
     * @return id турнира после upsert
     * @throws IllegalArgumentException если страница не парный турнир
     */
    @Transactional
    public long syncById(long tournamentId) {
        var page = client.tournamentPage(tournamentId);
        TournamentPageMeta meta = pageParser.parse(page);
        if (!meta.doubles()) {
            throw new IllegalArgumentException("Tournament is not doubles: " + tournamentId);
        }
        String region = snapshotProperties.regionCode();
        upsertTournamentFromMeta(meta, region, page);
        ru.badmintonlab.parser.model.TournamentRegistration parsed = registrationParser.parse(page);
        upsertRegistration(tournamentId, parsed, meta);
        return tournamentId;
    }

    private void syncTournament(TournamentListEntry entry, String region) {
        var page = client.tournamentPage(entry.id());
        TournamentPageMeta meta = pageParser.parse(page);
        upsertTournament(entry, meta, region, page);
        ru.badmintonlab.parser.model.TournamentRegistration parsed = registrationParser.parse(page);
        upsertRegistration(entry.id(), parsed, meta);
    }

    private void upsertTournament(TournamentListEntry entry,
                                  TournamentPageMeta meta,
                                  String region,
                                  org.jsoup.nodes.Document page) {
        Tournament tournament = tournamentRepository.findById(entry.id())
                .orElseGet(() -> new Tournament(entry.id()));
        tournament.setName(meta.name() != null && !meta.name().isBlank() ? meta.name() : entry.name());
        tournament.setCategoryCode(meta.categoryCode());
        applyRatingLimits(tournament, page);
        tournament.setRegionCode(region);
        tournament.setStatus(TournamentStatus.UPCOMING);
        LocalDate date = entry.date();
        var time = entry.time().orElse(meta.startsAt().toLocalTime());
        tournament.setStartsAt(SnapshotSupport.toInstant(date.atTime(time)));
        tournamentRepository.save(tournament);
    }

    private void upsertTournamentFromMeta(TournamentPageMeta meta, String region, org.jsoup.nodes.Document page) {
        Tournament tournament = tournamentRepository.findById(meta.id())
                .orElseGet(() -> new Tournament(meta.id()));
        tournament.setName(meta.name());
        tournament.setCategoryCode(meta.categoryCode());
        applyRatingLimits(tournament, page);
        tournament.setRegionCode(region);
        tournament.setStatus(TournamentStatus.UPCOMING);
        tournament.setStartsAt(SnapshotSupport.toInstant(meta.startsAt()));
        tournamentRepository.save(tournament);
    }

    private void applyRatingLimits(Tournament tournament, org.jsoup.nodes.Document page) {
        var limits = ratingLimitsParser.parse(page, tournament.getCategoryCode(), tournament.getName());
        TournamentRatingLimitsBackfillService.applyLimits(tournament, limits);
    }

    private void upsertRegistration(long tournamentId,
                                      ru.badmintonlab.parser.model.TournamentRegistration parsed,
                                      TournamentPageMeta meta) {
        registrationRepository.deleteByTournamentId(tournamentId);
        Set<Long> playerIds = new HashSet<>();
        parsed.players().forEach(p -> playerIds.add(p.playerId()));
        parsed.pairs().forEach(p -> {
            playerIds.add(p.player1Id());
            playerIds.add(p.player2Id());
        });
        if (!playerIds.isEmpty()) {
            for (var player : parsed.players()) {
                playerUpsertService.ensurePlayer(player.playerId(), player.nick());
            }
            for (var pair : parsed.pairs()) {
                pair.player1Nick().ifPresent(n -> playerUpsertService.ensurePlayer(pair.player1Id(), n));
                pair.player2Nick().ifPresent(n -> playerUpsertService.ensurePlayer(pair.player2Id(), n));
                if (pair.player1Nick().isEmpty()) {
                    playerUpsertService.ensurePlayer(pair.player1Id(), null);
                }
                if (pair.player2Nick().isEmpty()) {
                    playerUpsertService.ensurePlayer(pair.player2Id(), null);
                }
            }
        }

        var pairDiscipline = ru.badmintonlab.core.domain.TournamentDisciplineSupport
                .pairDiscipline(meta.categoryCode(), meta.name());

        for (var pair : parsed.pairs()) {
            Long pairId = pairService.getOrCreate(pair.player1Id(), pair.player2Id(), pairDiscipline);
            saveRegistration(tournamentId, pair.player1Id(), pairId);
            saveRegistration(tournamentId, pair.player2Id(), pairId);
        }
        Set<Long> inPair = new HashSet<>();
        parsed.pairs().forEach(p -> {
            inPair.add(p.player1Id());
            inPair.add(p.player2Id());
        });
        for (var player : parsed.players()) {
            if (!inPair.contains(player.playerId())) {
                saveRegistration(tournamentId, player.playerId(), null);
            }
        }
    }

    private void saveRegistration(long tournamentId, long playerId, Long pairId) {
        registrationRepository.save(
                TournamentRegistration.create(tournamentId, playerId, pairId, Instant.now()));
    }
}
