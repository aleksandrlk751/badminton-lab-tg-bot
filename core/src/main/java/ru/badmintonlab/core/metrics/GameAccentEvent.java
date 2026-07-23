package ru.badmintonlab.core.metrics;

import ru.badmintonlab.core.domain.PairCompositionType;

import java.time.Instant;

/** Один парный матч с известным типом пары — вход для {@link GameAccentService}. */
public record GameAccentEvent(
        Instant playedAt,
        double delta,
        PairCompositionType compositionType
) {
}
