package ru.badmintonlab.core.metrics;

import org.junit.jupiter.api.Test;
import ru.badmintonlab.core.config.StabilityZoneMetrics;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StabilityLevelTest {

    private static final StabilityZoneMetrics ZONES = TestMetrics.stabilityZoneDefaults();

    @Test
    void mapsScoreToFiveZones() {
        assertEquals(StabilityLevel.VERY_UNSTABLE, StabilityLevel.fromScore(69.9, ZONES));
        assertEquals(StabilityLevel.UNSTABLE, StabilityLevel.fromScore(70.0, ZONES));
        assertEquals(StabilityLevel.UNSTABLE, StabilityLevel.fromScore(79.9, ZONES));
        assertEquals(StabilityLevel.MIDDLE, StabilityLevel.fromScore(80.0, ZONES));
        assertEquals(StabilityLevel.MIDDLE, StabilityLevel.fromScore(85.9, ZONES));
        assertEquals(StabilityLevel.STABLE, StabilityLevel.fromScore(86.0, ZONES));
        assertEquals(StabilityLevel.STABLE, StabilityLevel.fromScore(91.9, ZONES));
        assertEquals(StabilityLevel.SUPER_STABLE, StabilityLevel.fromScore(92.0, ZONES));
        assertEquals(StabilityLevel.SUPER_STABLE, StabilityLevel.fromScore(100.0, ZONES));
    }

    @Test
    void middleZoneUsesWhiteCircleEmoji() {
        assertEquals("\u26AA", StabilityLevel.MIDDLE.emoji());
    }

    @Test
    void unstableZoneUsesYellowCircleEmoji() {
        assertEquals("\uD83D\uDFE1", StabilityLevel.UNSTABLE.emoji());
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
