package ru.badmintonlab.worker.snapshot;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.badmintonlab.core.domain.Discipline;
import ru.badmintonlab.core.entity.Pair;
import ru.badmintonlab.core.entity.Participation;
import ru.badmintonlab.core.repository.ParticipationRepository;
import ru.badmintonlab.parser.model.TournamentPairResult;
import ru.badmintonlab.parser.model.TournamentResults;

import java.math.BigDecimal;

/**
 * Идемпотентный upsert итогов турнира: пары ({@link Pair}) и участие игроков
 * ({@link Participation}) с местом, рейтингами до/дельта/после и балансом матчей/сетов.
 */
@Service
public class ResultUpsertService {

    private final PairService pairService;
    private final ParticipationRepository participationRepository;

    public ResultUpsertService(PairService pairService,
                               ParticipationRepository participationRepository) {
        this.pairService = pairService;
        this.participationRepository = participationRepository;
    }

    @Transactional
    public void upsert(TournamentResults results, Discipline discipline) {
        for (TournamentPairResult pair : results.pairs()) {
            Long pairId = pairService.getOrCreate(pair.player1Id(), pair.player2Id(), discipline);
            upsertParticipation(results.tournamentId(), pair, pair.player1Id(), pair.player1RatingBefore().orElse(null), pairId);
            upsertParticipation(results.tournamentId(), pair, pair.player2Id(), pair.player2RatingBefore().orElse(null), pairId);
        }
    }

    private void upsertParticipation(long tournamentId, TournamentPairResult result,
                                     long playerId, BigDecimal ratingBefore, Long pairId) {
        Participation participation = participationRepository
                .findByTournamentIdAndPlayerId(tournamentId, playerId)
                .orElseGet(() -> new Participation(tournamentId, playerId));

        participation.setPairId(pairId);
        participation.setPlace((short) result.place());
        participation.setRatingBefore(ratingBefore);

        BigDecimal delta = result.ratingDelta().orElse(null);
        participation.setRatingDelta(delta);
        if (ratingBefore != null && delta != null) {
            participation.setRatingAfter(ratingBefore.add(delta));
        }

        SnapshotSupport.parseBalance(result.matchesBalance()).ifPresent(wl -> {
            participation.setMatchesWon((short) wl[0]);
            participation.setMatchesLost((short) wl[1]);
        });
        SnapshotSupport.parseBalance(result.setsBalance()).ifPresent(wl -> {
            participation.setSetsWon((short) wl[0]);
            participation.setSetsLost((short) wl[1]);
        });

        participationRepository.save(participation);
    }
}
