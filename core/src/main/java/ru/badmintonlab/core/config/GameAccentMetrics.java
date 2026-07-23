package ru.badmintonlab.core.config;

import java.math.BigDecimal;

/** Параметры метрики «Игровой акцент» (§2.7 {@code docs/FORMULAR.md}). */
public record GameAccentMetrics(
        int halfLifeDays,
        BigDecimal earlyDecayMax,
        BigDecimal earlyDecayPower,
        BigDecimal minWeightSum,
        int displayWindowDays
) {
    public GameAccentMetrics {
        if (halfLifeDays <= 0) {
            throw new IllegalArgumentException("gameAccent.halfLifeDays must be positive");
        }
        if (earlyDecayMax == null || earlyDecayMax.compareTo(new BigDecimal("0.5")) <= 0
                || earlyDecayMax.compareTo(BigDecimal.ONE) > 0) {
            throw new IllegalArgumentException("gameAccent.earlyDecayMax must be in (0.5, 1]");
        }
        if (earlyDecayPower == null || earlyDecayPower.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("gameAccent.earlyDecayPower must be positive");
        }
        if (minWeightSum == null || minWeightSum.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("gameAccent.minWeightSum must be positive");
        }
        if (displayWindowDays <= 0) {
            throw new IllegalArgumentException("gameAccent.displayWindowDays must be positive");
        }
    }
}
