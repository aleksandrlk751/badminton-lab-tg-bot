package ru.badmintonlab.core.metrics;

import ru.badmintonlab.core.domain.PairCompositionType;

/**
 * Игровой акцент: две оси — предпочтение (объём с затуханием) и сильная сторона
 * (взвешенная δ и винрейт).
 */
public record GameAccentResult(
        PairCompositionType preferenceType,
        double preferenceShare,
        int preferenceGamesInWindow,
        PairCompositionType strengthType,
        double strengthAvgDelta,
        double strengthWinRate
) {
}
