package ru.badmintonlab.core.metrics;

import org.junit.jupiter.api.Test;
import ru.badmintonlab.core.domain.PairCompositionType;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GameAccentServiceTest {

    private static final Instant NOW = Instant.parse("2026-07-22T12:00:00Z");
    private static final Duration HALF_LIFE = Duration.ofDays(180);
    private static final Duration TWO_WEEKS = Duration.ofDays(14);

    private final GameAccentService service = new GameAccentService(TestMetrics.defaults());

    @Test
    void emptyEventsGiveEmpty() {
        assertTrue(service.accent(NOW, List.of()).isEmpty());
    }

    @Test
    void belowMinWeightIsEmpty() {
        // одна свежая игра: w≈0.8 < minWeight=3
        var events = List.of(event(NOW, 5.0, PairCompositionType.XD));
        assertTrue(service.accent(NOW, events).isEmpty());
    }

    @Test
    void recentGamesWeighMoreForPreference() {
        var old = event(NOW.minus(HALF_LIFE), 0.0, PairCompositionType.MD);
        var recent = event(NOW.minus(TWO_WEEKS), 0.0, PairCompositionType.XD);
        var events = List.of(old, old, old, recent, recent, recent);

        GameAccentResult result = service.accent(NOW, events).orElseThrow();

        assertEquals(PairCompositionType.XD, result.preferenceType());
        assertTrue(result.preferenceShare() > 0.5);
    }

    @Test
    void strengthUsesWeightedAverageDelta() {
        var mdWin = event(NOW.minus(HALF_LIFE), 10.0, PairCompositionType.MD);
        var xdLoss = event(NOW.minus(TWO_WEEKS), -2.0, PairCompositionType.XD);
        var events = List.of(mdWin, mdWin, mdWin, xdLoss, xdLoss, xdLoss);

        GameAccentResult result = service.accent(NOW, events).orElseThrow();

        assertEquals(PairCompositionType.MD, result.strengthType());
        assertEquals(10.0, result.strengthAvgDelta(), 0.01);
        assertEquals(1.0, result.strengthWinRate(), 0.01);
    }

    @Test
    void unknownCompositionIsSkipped() {
        var known = event(NOW, 1.0, PairCompositionType.WD);
        var events = List.of(known, known, known, known);
        Optional<GameAccentResult> result = service.accent(NOW, events);
        assertTrue(result.isPresent());
        assertEquals(PairCompositionType.WD, result.orElseThrow().preferenceType());
    }

    @Test
    void gamesInWindowCountsOnlyRecentHalfYear() {
        var inWindow = event(NOW.minus(TWO_WEEKS), 1.0, PairCompositionType.XD);
        var outWindow = event(NOW.minus(Duration.ofDays(200)), 1.0, PairCompositionType.XD);
        var events = List.of(inWindow, inWindow, inWindow, outWindow, outWindow, outWindow);

        GameAccentResult result = service.accent(NOW, events).orElseThrow();

        assertEquals(3, result.preferenceGamesInWindow());
    }

    private static GameAccentEvent event(Instant at, double delta, PairCompositionType type) {
        return new GameAccentEvent(at, delta, type);
    }
}
