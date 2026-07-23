package ru.badmintonlab.core.metrics;

import java.time.Instant;

/**
 * Матч с дельтой и контекстом турнира — вход для {@link StabilityService}.
 */
public record StabilityMatchEvent(
        long tournamentId,
        Instant tournamentAt,
        double delta
) {
}
