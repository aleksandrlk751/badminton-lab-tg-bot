package ru.badmintonlab.bot.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.badmintonlab.core.domain.Discipline;
import ru.badmintonlab.core.repository.H2hRepository;
import ru.badmintonlab.core.repository.ParticipationRepository;
import ru.badmintonlab.parser.TournamentGamesParser;
import ru.badmintonlab.parser.model.PairMatch;
import ru.badmintonlab.worker.http.Badminton4uClient;
import ru.badmintonlab.worker.snapshot.MatchUpsertService;
import ru.badmintonlab.worker.snapshot.SnapshotSupport;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Lazy-fetch матчей для H2H: сначала {@code games/?user1&user2} (SSR-таблица),
 * затем {@code gamesd} по общим турнирам участия.
 */
@Service
public class H2hLazyFetchService {

    private static final Logger log = LoggerFactory.getLogger(H2hLazyFetchService.class);
    private static final int MAX_TOURNAMENT_FETCH = 10;

    private final Badminton4uClient client;
    private final MatchUpsertService matchUpsertService;
    private final ParticipationRepository participationRepository;
    private final H2hRepository h2hRepository;
    private final TournamentGamesParser gamesParser = new TournamentGamesParser();

    public H2hLazyFetchService(Badminton4uClient client,
                               MatchUpsertService matchUpsertService,
                               ParticipationRepository participationRepository,
                               H2hRepository h2hRepository) {
        this.client = client;
        this.matchUpsertService = matchUpsertService;
        this.participationRepository = participationRepository;
        this.h2hRepository = h2hRepository;
    }

    /**
     * @return число новых матчей, добавленных в БД
     */
    @Transactional
    public int fetchIfMissing(long playerA, long playerB) {
        if (!h2hRepository.findHeadToHead(playerA, playerB).isEmpty()) {
            return 0;
        }
        int inserted = fetchFromGamesPage(playerA, playerB);
        if (inserted > 0 || !h2hRepository.findHeadToHead(playerA, playerB).isEmpty()) {
            return inserted;
        }
        return fetchFromCommonTournaments(playerA, playerB);
    }

    private int fetchFromGamesPage(long playerA, long playerB) {
        try {
            List<PairMatch> parsed = gamesParser.parse(client.playerGames(playerA, playerB));
            if (parsed.isEmpty()) {
                return 0;
            }
            int inserted = 0;
            Set<Long> tournaments = new HashSet<>();
            for (PairMatch match : parsed) {
                if (!isHeadToHead(match, playerA, playerB)) {
                    continue;
                }
                Discipline discipline = disciplineForTournament(match.tournamentId(), tournaments);
                inserted += matchUpsertService.upsert(List.of(match), discipline, match.tournamentId());
            }
            if (inserted > 0) {
                log.info("H2H lazy-fetch games: {}–{}, добавлено {} матчей", playerA, playerB, inserted);
            }
            return inserted;
        } catch (RuntimeException e) {
            log.warn("H2H lazy-fetch games {}–{}: {}", playerA, playerB, e.toString());
            return 0;
        }
    }

    private int fetchFromCommonTournaments(long playerA, long playerB) {
        List<Long> tournamentIds = participationRepository.findCommonTournamentIds(playerA, playerB);
        int inserted = 0;
        int fetched = 0;
        for (Long tournamentId : tournamentIds) {
            if (fetched >= MAX_TOURNAMENT_FETCH) {
                break;
            }
            if (h2hRepository.hasHeadToHeadInTournament(playerA, playerB, tournamentId)) {
                continue;
            }
            try {
                Discipline discipline = SnapshotSupport.inferDisciplineFromPage(client.tournamentPage(tournamentId));
                List<PairMatch> matches = gamesParser.parse(client.tournamentGames(tournamentId));
                inserted += matchUpsertService.upsert(matches, discipline, tournamentId);
                fetched++;
            } catch (RuntimeException e) {
                log.warn("H2H lazy-fetch gamesd tour {}: {}", tournamentId, e.toString());
            }
        }
        if (inserted > 0) {
            log.info("H2H lazy-fetch gamesd: {}–{}, добавлено {} матчей", playerA, playerB, inserted);
        }
        return inserted;
    }

    private Discipline disciplineForTournament(long tournamentId, Set<Long> cache) {
        if (cache.add(tournamentId)) {
            try {
                return SnapshotSupport.inferDisciplineFromPage(client.tournamentPage(tournamentId));
            } catch (RuntimeException e) {
                log.warn("Не удалось определить дисциплину турнира {}: {}", tournamentId, e.toString());
            }
        }
        return Discipline.D;
    }

    private static boolean isHeadToHead(PairMatch match, long playerA, long playerB) {
        boolean aOnA = match.sideA().stream().anyMatch(p -> p.playerId() == playerA);
        boolean aOnB = match.sideB().stream().anyMatch(p -> p.playerId() == playerA);
        boolean bOnA = match.sideA().stream().anyMatch(p -> p.playerId() == playerB);
        boolean bOnB = match.sideB().stream().anyMatch(p -> p.playerId() == playerB);
        return (aOnA && bOnB) || (aOnB && bOnA);
    }
}
