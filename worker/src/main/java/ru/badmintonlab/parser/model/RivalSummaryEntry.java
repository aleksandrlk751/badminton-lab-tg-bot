package ru.badmintonlab.parser.model;

import java.math.BigDecimal;
import java.util.Optional;

public record RivalSummaryEntry(
        long opponentId,
        Optional<BigDecimal> opponentRating,
        int games,
        int wins,
        int losses,
        Optional<BigDecimal> deltaSum
) {
}
