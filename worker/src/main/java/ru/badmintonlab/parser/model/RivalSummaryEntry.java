package ru.badmintonlab.parser.model;

/**
 * Агрегат player↔opponent для экрана «Соперники» (вариант C).
 * Только W/L; H2H, Form и delta — из {@link PairMatch}.
 */
public record RivalSummaryEntry(
        long playerId,
        long opponentId,
        int wins,
        int losses
) {
    public int games() {
        return wins + losses;
    }
}
