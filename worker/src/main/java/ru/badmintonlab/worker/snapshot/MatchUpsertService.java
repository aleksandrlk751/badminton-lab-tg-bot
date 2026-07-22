package ru.badmintonlab.worker.snapshot;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.badmintonlab.core.domain.Discipline;
import ru.badmintonlab.core.domain.MatchSide;
import ru.badmintonlab.core.entity.Match;
import ru.badmintonlab.core.entity.MatchPlayer;
import ru.badmintonlab.core.entity.MatchPlayerId;
import ru.badmintonlab.core.repository.MatchPlayerRepository;
import ru.badmintonlab.core.repository.MatchRepository;
import ru.badmintonlab.parser.model.PairMatch;

import java.math.BigDecimal;
import java.util.List;

/**
 * Идемпотентный upsert матчей pair-vs-pair (gamesd) и участников матча.
 * Идемпотентность — по {@code (source, external_key)}; существующие матчи не дублируются.
 */
@Service
public class MatchUpsertService {

    private static final String SOURCE = "badminton4u";

    private final MatchRepository matchRepository;
    private final MatchPlayerRepository matchPlayerRepository;

    public MatchUpsertService(MatchRepository matchRepository,
                              MatchPlayerRepository matchPlayerRepository) {
        this.matchRepository = matchRepository;
        this.matchPlayerRepository = matchPlayerRepository;
    }

    /**
     * @return число вставленных новых матчей (уже существующие пропущены).
     */
    @Transactional
    public int upsert(List<PairMatch> matches, Discipline discipline) {
        int inserted = 0;
        for (PairMatch match : matches) {
            if (matchRepository.findBySourceAndExternalKey(SOURCE, match.externalKey()).isPresent()) {
                continue;
            }
            Match saved = insertMatch(match, discipline);
            insertPlayers(saved.getId(), match.sideA(), MatchSide.A, match.deltaA().orElse(null));
            insertPlayers(saved.getId(), match.sideB(), MatchSide.B, match.deltaB().orElse(null));
            inserted++;
        }
        return inserted;
    }

    private Match insertMatch(PairMatch match, Discipline discipline) {
        Match entity = new Match(
                match.tournamentId(),
                discipline,
                SnapshotSupport.toInstant(match.playedAt()),
                match.scoreSets(),
                SOURCE);
        entity.setStage(truncate(match.stage()));
        entity.setDurationMin(match.durationMin().map(Integer::shortValue).orElse(null));
        entity.setExternalKey(match.externalKey());
        return matchRepository.save(entity);
    }

    private void insertPlayers(Long matchId, List<PairMatch.MatchPlayer> players, MatchSide side, BigDecimal delta) {
        for (PairMatch.MatchPlayer player : players) {
            MatchPlayerId id = new MatchPlayerId(matchId, player.playerId());
            matchPlayerRepository.save(new MatchPlayer(id, side, player.ratingBefore().orElse(null), delta));
        }
    }

    private String truncate(String stage) {
        if (stage == null) {
            return null;
        }
        return stage.length() > 64 ? stage.substring(0, 64) : stage;
    }
}
