package ru.badmintonlab.core.metrics;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StabilityServiceTest {

    private static final Instant NOW = Instant.parse("2026-07-01T12:00:00Z");
    private static final Instant T1 = NOW.minus(30, ChronoUnit.DAYS);
    private static final Instant T2 = NOW.minus(60, ChronoUnit.DAYS);
    private static final Instant T3 = NOW.minus(90, ChronoUnit.DAYS);

    private final StabilityService service = new StabilityService(TestMetrics.defaults());

    @Test
    void ownLevelWinThenHigherLevelLossIsStable() {
        List<StabilityMatchEvent> events = List.of(
                event(1, T1, 18.0),
                event(1, T1, 16.0),
                event(2, T2, -2.0),
                event(2, T2, 0.0));
        double stability = service.stability(NOW, events).orElseThrow();
        assertTrue(stability >= 95.0, "expected high stability, got " + stability);
    }

    @Test
    void mixedSurprisesInsideTournamentReduceStability() {
        List<StabilityMatchEvent> events = List.of(
                event(1, T1, 20.0),
                event(1, T1, -18.0),
                event(1, T1, 17.0),
                event(1, T1, -16.0));
        double stability = service.stability(NOW, events).orElseThrow();
        assertTrue(stability < 60.0, "expected low stability, got " + stability);
    }

    @Test
    void oscillatingTournamentToneReducesStability() {
        List<StabilityMatchEvent> events = List.of(
                event(1, T3, 20.0),
                event(1, T3, 18.0),
                event(2, T2, -20.0),
                event(2, T2, -17.0),
                event(3, T1, 19.0),
                event(3, T1, 16.0));
        double stability = service.stability(NOW, events).orElseThrow();
        assertTrue(stability < 75.0, "expected reduced stability, got " + stability);
    }

    @Test
    void noSignificantSurprisesReturnsEmpty() {
        assertTrue(service.stability(NOW, List.of(
                event(1, T1, 2.0),
                event(1, T1, -1.0))).isEmpty());
    }

    @Test
    void withinScoreMixedSigns() {
        assertEquals(0.0, StabilityService.withinScore(List.of(20.0, -18.0), 10.0), 1e-9);
        assertEquals(1.0, StabilityService.withinScore(List.of(20.0, 18.0), 10.0), 1e-9);
    }

    private static StabilityMatchEvent event(long tournamentId, Instant at, double delta) {
        return new StabilityMatchEvent(tournamentId, at, delta);
    }
}
