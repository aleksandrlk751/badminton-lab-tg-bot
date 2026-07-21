package ru.badmintonlab.parser.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public record PairMatch(
        long tournamentId,
        LocalDateTime playedAt,
        String stage,
        List<MatchPlayer> sideA,
        List<MatchPlayer> sideB,
        String scoreSets,
        Optional<BigDecimal> deltaA,
        Optional<BigDecimal> deltaB,
        Optional<Integer> durationMin,
        String externalKey
) {
    public record MatchPlayer(long playerId, Optional<BigDecimal> ratingBefore) {}
}
