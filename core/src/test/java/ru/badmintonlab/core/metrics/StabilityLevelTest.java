package ru.badmintonlab.core.metrics;

import org.junit.jupiter.api.Test;
import ru.badmintonlab.core.config.StabilityZoneMetrics;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StabilityLevelTest {

    private static final StabilityZoneMetrics ZONES = TestMetrics.stabilityZoneDefaults();

    @Test
    void mapsScoreToFiveZones() {
        assertEquals(StabilityLevel.VERY_UNSTABLE, StabilityLevel.fromScore(59.9, ZONES));
        assertEquals(StabilityLevel.UNSTABLE, StabilityLevel.fromScore(60.0, ZONES));
        assertEquals(StabilityLevel.UNSTABLE, StabilityLevel.fromScore(74.9, ZONES));
        assertEquals(StabilityLevel.MIDDLE, StabilityLevel.fromScore(75.0, ZONES));
        assertEquals(StabilityLevel.MIDDLE, StabilityLevel.fromScore(84.9, ZONES));
        assertEquals(StabilityLevel.STABLE, StabilityLevel.fromScore(85.0, ZONES));
        assertEquals(StabilityLevel.STABLE, StabilityLevel.fromScore(89.9, ZONES));
        assertEquals(StabilityLevel.SUPER_STABLE, StabilityLevel.fromScore(90.0, ZONES));
        assertEquals(StabilityLevel.SUPER_STABLE, StabilityLevel.fromScore(100.0, ZONES));
    }

    @Test
    void superStableZoneUsesFireEmoji() {
        assertEquals("\uD83D\uDD25", StabilityLevel.SUPER_STABLE.emoji());
    }

    @Test
    void rejectsNonMonotonicZones() {
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () ->
                new StabilityZoneMetrics(
                        new BigDecimal("80"),
                        new BigDecimal("75"),
                        new BigDecimal("85"),
                        new BigDecimal("90")));
    }
}
