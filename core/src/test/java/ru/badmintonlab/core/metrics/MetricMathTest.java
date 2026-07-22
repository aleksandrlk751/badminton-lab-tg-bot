package ru.badmintonlab.core.metrics;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MetricMathTest {

    private static final Instant NOW = Instant.parse("2026-07-22T12:00:00Z");
    private static final double H = 180.0;
    private static final double W_MAX = 0.8;
    private static final double ALPHA = 0.5;

    @Test
    void firstPeriodStartsAtEarlyDecayMax() {
        assertEquals(W_MAX, weight(0), 1e-9);
    }

    @Test
    void firstPeriodEndsAtHalf() {
        assertEquals(0.5, weight(H), 1e-9);
    }

    @Test
    void secondPeriodMatchesPureExponential() {
        assertEquals(0.25, weight(2 * H), 1e-9);
        assertEquals(0.125, weight(3 * H), 1e-9);
    }

    @Test
    void legacyCurveWhenMaxOneAndAlphaOne() {
        assertEquals(1.0, weight(NOW, NOW, 1.0, 1.0), 1e-9);
        assertEquals(0.5, weight(NOW, NOW.minus(Duration.ofDays(180)), 1.0, 1.0), 1e-9);
        assertEquals(Math.pow(0.5, 30.0 / H), weight(NOW, NOW.minus(Duration.ofDays(30)), 1.0, 1.0), 1e-9);
    }

    @Test
    void futureEventClampedToEarlyDecayMax() {
        assertEquals(W_MAX, weight(NOW, NOW.plus(Duration.ofDays(7)), W_MAX, ALPHA), 1e-9);
    }

    @Test
    void alphaLessThanOneDropsFasterInFirstDays() {
        double withAlpha = weight(NOW, NOW.minus(Duration.ofDays(7)), W_MAX, 0.5);
        double linearAlpha = weight(NOW, NOW.minus(Duration.ofDays(7)), W_MAX, 1.0);
        assert withAlpha < linearAlpha;
    }

    private static double weight(double daysAgo) {
        return weight(NOW, NOW.minus(Duration.ofDays((long) daysAgo)), W_MAX, ALPHA);
    }

    private static double weight(Instant reference, Instant event, double earlyMax, double alpha) {
        return MetricMath.decayWeight(reference, event, H, earlyMax, alpha);
    }
}
