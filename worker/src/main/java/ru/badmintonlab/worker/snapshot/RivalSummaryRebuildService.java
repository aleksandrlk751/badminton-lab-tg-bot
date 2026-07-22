package ru.badmintonlab.worker.snapshot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.badmintonlab.core.domain.Discipline;
import ru.badmintonlab.core.domain.MatchSide;
import ru.badmintonlab.core.entity.Match;
import ru.badmintonlab.core.entity.MatchPlayer;
import ru.badmintonlab.core.entity.RivalSummary;
import ru.badmintonlab.core.entity.RivalSummaryId;
import ru.badmintonlab.core.repository.MatchPlayerRepository;
import ru.badmintonlab.core.repository.MatchRepository;
import ru.badmintonlab.core.repository.RivalSummaryRepository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Полностью пересобирает {@code rival_summary} (вариант C: player↔opponent, только W/L)
 * из сохранённых матчей. Идемпотентно и не зависит от объёма конкретного прогона:
 * таблица очищается и наполняется заново из текущего состояния match / match_player.
 */
@Service
public class RivalSummaryRebuildService {

    private static final Logger log = LoggerFactory.getLogger(RivalSummaryRebuildService.class);

    private final MatchRepository matchRepository;
    private final MatchPlayerRepository matchPlayerRepository;
    private final RivalSummaryRepository rivalSummaryRepository;

    public RivalSummaryRebuildService(MatchRepository matchRepository,
                                      MatchPlayerRepository matchPlayerRepository,
                                      RivalSummaryRepository rivalSummaryRepository) {
        this.matchRepository = matchRepository;
        this.matchPlayerRepository = matchPlayerRepository;
        this.rivalSummaryRepository = rivalSummaryRepository;
    }

    @Transactional
    public int rebuild() {
        Map<Long, Match> matchesById = new HashMap<>();
        for (Match match : matchRepository.findAll()) {
            matchesById.put(match.getId(), match);
        }

        // matchId -> список (playerId, side)
        Map<Long, List<MatchPlayer>> playersByMatch = new HashMap<>();
        for (MatchPlayer mp : matchPlayerRepository.findAll()) {
            playersByMatch.computeIfAbsent(mp.getId().getMatchId(), k -> new ArrayList<>()).add(mp);
        }

        Map<RivalSummaryId, int[]> stats = new HashMap<>();
        for (var entry : playersByMatch.entrySet()) {
            Match match = matchesById.get(entry.getKey());
            if (match == null) {
                continue;
            }
            Boolean sideAWon = sideAWon(match.getScoreSets());
            if (sideAWon == null) {
                continue;
            }
            accumulate(stats, match.getDiscipline(), entry.getValue(), sideAWon);
        }

        rivalSummaryRepository.deleteAllInBatch();
        List<RivalSummary> rows = new ArrayList<>(stats.size());
        for (var e : stats.entrySet()) {
            rows.add(new RivalSummary(e.getKey(), (short) e.getValue()[0], (short) e.getValue()[1]));
        }
        rivalSummaryRepository.saveAll(rows);
        log.info("rival_summary пересобран: {} строк из {} матчей", rows.size(), matchesById.size());
        return rows.size();
    }

    private void accumulate(Map<RivalSummaryId, int[]> stats, Discipline discipline,
                            List<MatchPlayer> players, boolean sideAWon) {
        for (MatchPlayer player : players) {
            boolean playerWon = (player.getSide() == MatchSide.A) == sideAWon;
            for (MatchPlayer opponent : players) {
                if (opponent.getSide() == player.getSide()) {
                    continue;
                }
                RivalSummaryId key = new RivalSummaryId(
                        player.getId().getPlayerId(), opponent.getId().getPlayerId(), discipline);
                int[] wl = stats.computeIfAbsent(key, k -> new int[2]);
                if (playerWon) {
                    wl[0]++;
                } else {
                    wl[1]++;
                }
            }
        }
    }

    private Boolean sideAWon(String scoreSets) {
        if (scoreSets == null) {
            return null;
        }
        String[] parts = scoreSets.split(":");
        if (parts.length != 2) {
            return null;
        }
        try {
            int a = Integer.parseInt(parts[0].trim());
            int b = Integer.parseInt(parts[1].trim());
            if (a == b) {
                return null;
            }
            return a > b;
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
