package ru.badmintonlab.core.metrics;

import org.junit.jupiter.api.Test;
import ru.badmintonlab.core.config.GameAccentMetrics;
import ru.badmintonlab.core.config.MetricsProperties;
import ru.badmintonlab.core.config.StabilityZoneMetrics;

import java.math.BigDecimal;
import java.util.OptionalDouble;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PartnerScoreServiceTest {

    private static final OptionalDouble NO_FORM = OptionalDouble.empty();

    private final PartnerScoreService service = new PartnerScoreService(properties());

    @Test
    void newCandidateScoreMostlyFromLimit() {
        var result = service.score(new PartnerScoreService.Input(400, 380, 500.0, null, 0, 0, NO_FORM));
        assertEquals(31.2, result.score(), 0.5);
        assertEquals(0.78, result.cLimit(), 0.01);
    }

    @Test
    void jointDeltaRaisesScoreThroughCDeltaOnly() {
        var without = service.score(new PartnerScoreService.Input(400, 400, 500.0, null, 0, 0, NO_FORM));
        var withDelta = service.score(new PartnerScoreService.Input(400, 400, 500.0, null, 15, 2, NO_FORM));
        assertEquals(without.cDelta(), 0.0, 1e-9);
        assertTrue(withDelta.cDelta() > 0);
        assertTrue(withDelta.score() > without.score());
    }

    @Test
    void positiveFormAddsBonus() {
        var base = service.score(new PartnerScoreService.Input(400, 400, 500.0, null, 0, 0, NO_FORM));
        var withForm = service.score(new PartnerScoreService.Input(
                400, 400, 500.0, null, 0, 0, OptionalDouble.of(10.0)));
        assertTrue(withForm.score() > base.score());
    }

    @Test
    void negativeFormAppliesPenalty() {
        var base = service.score(new PartnerScoreService.Input(400, 400, 500.0, null, 0, 0, NO_FORM));
        var withForm = service.score(new PartnerScoreService.Input(
                400, 400, 500.0, null, 0, 0, OptionalDouble.of(-10.0)));
        assertTrue(withForm.score() < base.score());
        assertEquals(base.score() - 10.0, withForm.score(), 0.5);
    }

    @Test
    void pairAboveLimitScoresZeroOnLimitComponent() {
        var result = service.score(new PartnerScoreService.Input(520, 520, 500.0, null, 0, 0, NO_FORM));
        assertEquals(0.0, result.cLimit(), 1e-9);
        assertEquals(0.0, result.score(), 1e-9);
    }

    @Test
    void perPlayerCapCanReduceLimitComponent() {
        var result = service.score(new PartnerScoreService.Input(680, 620, 650.0, 700.0, 0, 0, NO_FORM));
        assertEquals(0.887, result.cLimit(), 0.01);
    }

    @Test
    void playerAboveMaxScoresZeroOnLimitComponent() {
        var result = service.score(new PartnerScoreService.Input(710, 600, 650.0, 700.0, 0, 0, NO_FORM));
        assertEquals(0.0, result.cLimit(), 1e-9);
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
                new BigDecimal("10.0"),
                new BigDecimal("0.10"),
                new BigDecimal("0.10"),
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
