package ru.badmintonlab.core.metrics;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PlayabilityIndexServiceTest {

    private static final Instant NOW = Instant.parse("2026-07-22T12:00:00Z");
    private static final Duration HALF_LIFE = Duration.ofDays(180);

    private final PlayabilityIndexService service = new PlayabilityIndexService(TestMetrics.defaults());

    @Test
    void emptyMeetingsGiveZero() {
        assertEquals(0.0, service.index(NOW, List.of()), 1e-9);
    }

    @Test
    void meetingAtReferenceHasFullWeight() {
        assertEquals(1.0, service.index(NOW, List.of(NOW)), 1e-9);
    }

    @Test
    void meetingOneHalfLifeAgoHasHalfWeight() {
        Instant oneHalfLifeAgo = NOW.minus(HALF_LIFE);
        assertEquals(0.5, service.index(NOW, List.of(oneHalfLifeAgo)), 1e-9);
    }

    @Test
    void weightsAccumulateAcrossMeetings() {
        // сейчас (1.0) + один период полураспада назад (0.5) + два периода назад (0.25) = 1.75
        List<Instant> meetings = List.of(
                NOW,
                NOW.minus(HALF_LIFE),
                NOW.minus(HALF_LIFE.multipliedBy(2)));
        assertEquals(1.75, service.index(NOW, meetings), 1e-9);
    }

    @Test
    void futureMeetingIsClampedToFullWeight() {
        // встреча «в будущем» относительно опорной точки не должна давать вес > 1
        Instant future = NOW.plus(HALF_LIFE);
        assertEquals(1.0, service.index(NOW, List.of(future)), 1e-9);
    }
}
