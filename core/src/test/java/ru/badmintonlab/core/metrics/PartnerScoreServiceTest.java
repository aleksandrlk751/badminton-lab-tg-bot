package ru.badmintonlab.core.metrics;

import org.junit.jupiter.api.Test;
import ru.badmintonlab.core.config.GameAccentMetrics;
import ru.badmintonlab.core.config.MetricsProperties;
import ru.badmintonlab.core.config.StabilityZoneMetrics;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PartnerScoreServiceTest {

    private final PartnerScoreService service = new PartnerScoreService(properties());

    @Test
    void newCandidateScoreMostlyFromLimit() {
        var result = service.score(new PartnerScoreService.Input(400, 380, 500.0, 0, 0, false));
        assertEquals(31.2, result.score(), 0.5);
        assertEquals(0.78, result.cLimit(), 0.01);
    }

    @Test
    void successfulHistoryGetsBoost() {
        var base = service.score(new PartnerScoreService.Input(400, 400, 500.0, 15, 2, false));
        var boosted = service.score(new PartnerScoreService.Input(400, 400, 500.0, 15, 2, true));
        assertEquals(base.score() * 1.2, boosted.score(), 0.01);
    }

    @Test
    void pairAboveLimitScoresZeroOnLimitComponent() {
        var result = service.score(new PartnerScoreService.Input(520, 520, 500.0, 0, 0, false));
        assertEquals(0.0, result.cLimit(), 1e-9);
        assertEquals(0.0, result.score(), 1e-9);
    }

    private static MetricsProperties properties() {
        return new MetricsProperties(
                180,
                new BigDecimal("0.8"),
                new BigDecimal("0.5"),
                new BigDecimal("0.5"),
                new BigDecimal("1.0"),
                new BigDecimal("20"),
                new BigDecimal("1.0"),
                new BigDecimal("0.4"),
                new BigDecimal("0.3"),
                new BigDecimal("0.3"),
                new BigDecimal("10"),
                12,
                new BigDecimal("1.0"),
                new BigDecimal("1.2"),
                new BigDecimal("11.5"),
                new StabilityZoneMetrics(
                        new BigDecimal("70"),
                        new BigDecimal("80"),
                        new BigDecimal("86"),
                        new BigDecimal("92")),
                new GameAccentMetrics(180, new BigDecimal("0.8"), new BigDecimal("0.5"), new BigDecimal("3"), 180)
        );
    }
}
