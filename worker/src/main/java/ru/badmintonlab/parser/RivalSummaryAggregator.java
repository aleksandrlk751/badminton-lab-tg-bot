package ru.badmintonlab.parser;

import ru.badmintonlab.parser.model.PairMatch;
import ru.badmintonlab.parser.model.RivalSummaryEntry;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Строит player↔opponent сводку из матчей gamesd (вариант C: только games/W/L).
 */
public class RivalSummaryAggregator {

    public List<RivalSummaryEntry> aggregate(List<PairMatch> matches) {
        Map<RivalKey, int[]> stats = new HashMap<>();

        for (PairMatch match : matches) {
            boolean sideAWon = sideAWon(match.scoreSets());
            accumulateSide(stats, match.sideA(), match.sideB(), sideAWon);
            accumulateSide(stats, match.sideB(), match.sideA(), !sideAWon);
        }

        List<RivalSummaryEntry> entries = new ArrayList<>(stats.size());
        for (Map.Entry<RivalKey, int[]> e : stats.entrySet()) {
            RivalKey key = e.getKey();
            int[] wl = e.getValue();
            entries.add(new RivalSummaryEntry(key.playerId(), key.opponentId(), wl[0], wl[1]));
        }
        entries.sort(Comparator
                .comparingLong(RivalSummaryEntry::playerId)
                .thenComparing(Comparator.comparingInt(RivalSummaryEntry::games).reversed())
                .thenComparingLong(RivalSummaryEntry::opponentId));
        return entries;
    }

    private void accumulateSide(
            Map<RivalKey, int[]> stats,
            List<PairMatch.MatchPlayer> side,
            List<PairMatch.MatchPlayer> opponents,
            boolean sideWon
    ) {
        for (PairMatch.MatchPlayer player : side) {
            for (PairMatch.MatchPlayer opponent : opponents) {
                if (player.playerId() == opponent.playerId()) {
                    continue;
                }
                int[] wl = stats.computeIfAbsent(
                        new RivalKey(player.playerId(), opponent.playerId()),
                        k -> new int[2]
                );
                if (sideWon) {
                    wl[0]++;
                } else {
                    wl[1]++;
                }
            }
        }
    }

    private boolean sideAWon(String scoreSets) {
        String[] parts = scoreSets.split(":");
        if (parts.length != 2) {
            throw new IllegalArgumentException("Invalid scoreSets: " + scoreSets);
        }
        int setsA = Integer.parseInt(parts[0].trim());
        int setsB = Integer.parseInt(parts[1].trim());
        if (setsA == setsB) {
            throw new IllegalArgumentException("Tie scoreSets not supported: " + scoreSets);
        }
        return setsA > setsB;
    }

    private record RivalKey(long playerId, long opponentId) {}
}
