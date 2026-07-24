package ru.badmintonlab.core.metrics;

import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.OptionalDouble;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PartnerScoreServiceTest {

    private static final OptionalDouble NO_FORM = OptionalDouble.empty();
    private static final Optional<StabilityLevel> NO_STABILITY = Optional.empty();

    private final PartnerScoreService service = new PartnerScoreService(TestMetrics.defaults());

    @Test
    void newCandidateScoreMostlyFromLimit() {
        var result = service.score(input(400, 380, 500.0, null, 0, 0, NO_FORM, 0, NO_STABILITY));
        assertEquals(27.3, result.score(), 0.5);
        assertEquals(0.78, result.cLimit(), 0.01);
    }

    @Test
    void jointDeltaRaisesScoreThroughCDeltaOnly() {
        var without = service.score(input(400, 400, 500.0, null, 0, 0, NO_FORM, 0, NO_STABILITY));
        var withDelta = service.score(input(400, 400, 500.0, null, 15, 2, NO_FORM, 0, NO_STABILITY));
        assertEquals(without.cDelta(), 0.0, 1e-9);
        assertTrue(withDelta.cDelta() > 0);
        assertTrue(withDelta.score() > without.score());
    }

    @Test
    void positiveFormInScoreBase() {
        var base = service.score(input(400, 400, 500.0, null, 0, 0, NO_FORM, 0, NO_STABILITY));
        var withForm = service.score(input(400, 400, 500.0, null, 0, 0, OptionalDouble.of(10.0), 0, NO_STABILITY));
        assertTrue(withForm.cForm() > 0);
        assertTrue(withForm.score() > base.score());
        assertEquals(base.score() + 100 * 0.10 * withForm.cForm(), withForm.score(), 0.5);
    }

    @Test
    void positiveFormScaledByStabilityZone() {
        var stable = service.score(input(
                400, 400, 500.0, null, 0, 0, OptionalDouble.of(10.0), 0, Optional.of(StabilityLevel.STABLE)));
        var superStable = service.score(input(
                400, 400, 500.0, null, 0, 0, OptionalDouble.of(10.0), 0, Optional.of(StabilityLevel.SUPER_STABLE)));
        var unstable = service.score(input(
                400, 400, 500.0, null, 0, 0, OptionalDouble.of(10.0), 0, Optional.of(StabilityLevel.UNSTABLE)));
        assertTrue(superStable.cForm() > stable.cForm());
        assertTrue(stable.cForm() > unstable.cForm());
        assertTrue(superStable.score() > stable.score());
        assertTrue(stable.score() > unstable.score());
    }

    @Test
    void negativeFormIgnoresStability() {
        var a = service.score(input(
                400, 400, 500.0, null, 0, 0, OptionalDouble.of(-10.0), 0, Optional.of(StabilityLevel.SUPER_STABLE)));
        var b = service.score(input(
                400, 400, 500.0, null, 0, 0, OptionalDouble.of(-10.0), 0, Optional.of(StabilityLevel.VERY_UNSTABLE)));
        assertEquals(a.cForm(), b.cForm(), 1e-9);
        assertEquals(a.score(), b.score(), 1e-9);
    }

    @Test
    void negativeFormInScoreBase() {
        var base = service.score(input(400, 400, 500.0, null, 0, 0, NO_FORM, 0, NO_STABILITY));
        var withForm = service.score(input(400, 400, 500.0, null, 0, 0, OptionalDouble.of(-10.0), 0, NO_STABILITY));
        assertTrue(withForm.cForm() < 0);
        assertTrue(withForm.score() < base.score());
        assertEquals(base.score() + 100 * 0.10 * withForm.cForm(), withForm.score(), 0.5);
    }

    @Test
    void tournamentCategoryDeltaOnlyWhenPositive() {
        var noAccent = service.score(input(400, 400, 500.0, null, 0, 0, NO_FORM, 0, NO_STABILITY));
        var zeroDelta = service.score(input(400, 400, 500.0, null, 0, 0, NO_FORM, -5.0, NO_STABILITY));
        var withDelta = service.score(input(400, 400, 500.0, null, 0, 0, NO_FORM, 8.0, NO_STABILITY));
        assertEquals(0.0, zeroDelta.cAccent(), 1e-9);
        assertEquals(noAccent.score(), zeroDelta.score(), 1e-9);
        assertTrue(withDelta.cAccent() > 0);
        assertTrue(withDelta.score() > noAccent.score());
    }

    @Test
    void pairAboveLimitScoresZeroOnLimitComponent() {
        var result = service.score(input(520, 520, 500.0, null, 0, 0, NO_FORM, 0, NO_STABILITY));
        assertEquals(0.0, result.cLimit(), 1e-9);
        assertEquals(0.0, result.score(), 1e-9);
    }

    @Test
    void perPlayerCapCanReduceLimitComponent() {
        var result = service.score(input(680, 620, 650.0, 700.0, 0, 0, NO_FORM, 0, NO_STABILITY));
        assertEquals(0.887, result.cLimit(), 0.01);
    }

    @Test
    void playerAboveMaxScoresZeroOnLimitComponent() {
        var result = service.score(input(710, 600, 650.0, 700.0, 0, 0, NO_FORM, 0, NO_STABILITY));
        assertEquals(0.0, result.cLimit(), 1e-9);
    }

    private static PartnerScoreService.Input input(double userRating,
                                                   double candidateRating,
                                                   Double limit,
                                                   Double maxPlayer,
                                                   double jointDelta,
                                                   double playability,
                                                   OptionalDouble form,
                                                   double tournamentCategoryDelta,
                                                   Optional<StabilityLevel> stability) {
        return new PartnerScoreService.Input(
                userRating,
                candidateRating,
                limit,
                maxPlayer,
                jointDelta,
                playability,
                form,
                tournamentCategoryDelta,
                stability);
    }
}
