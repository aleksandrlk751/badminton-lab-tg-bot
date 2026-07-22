package ru.badmintonlab.core.metrics;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FormServiceTest {

    private static final Instant NOW = Instant.parse("2026-07-22T12:00:00Z");
    private static final Duration HALF_LIFE = Duration.ofDays(180);

    private final FormService service = new FormService(TestMetrics.defaults());

    @Test
    void emptyEventsGiveZero() {
        assertEquals(0.0, service.form(NOW, List.of()), 1e-9);
    }

    @Test
    void freshDeltaUsesEarlyDecayMax() {
        assertEquals(8.0, service.form(NOW, List.of(new RatingDeltaEvent(NOW, 10.0))), 1e-9);
    }

    @Test
    void deltaOneHalfLifeAgoIsHalved() {
        RatingDeltaEvent event = new RatingDeltaEvent(NOW.minus(HALF_LIFE), 10.0);
        assertEquals(5.0, service.form(NOW, List.of(event)), 1e-9);
    }

    @Test
    void signedDeltasSumWithFreshness() {
        // свежая победа +12 (вес 0.8) и поражение -8 два периода назад (вес 0.25) → 9.6 - 2 = 7.6
        List<RatingDeltaEvent> events = List.of(
                new RatingDeltaEvent(NOW, 12.0),
                new RatingDeltaEvent(NOW.minus(HALF_LIFE.multipliedBy(2)), -8.0));
        assertEquals(7.6, service.form(NOW, events), 1e-9);
    }

    @Test
    void streakOfLossesGivesNegativeForm() {
        List<RatingDeltaEvent> events = List.of(
                new RatingDeltaEvent(NOW, -5.0),
                new RatingDeltaEvent(NOW.minus(HALF_LIFE), -6.0));
        assertTrue(service.form(NOW, events) < 0.0);
    }
}
