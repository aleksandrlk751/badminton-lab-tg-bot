package ru.badmintonlab.core.metrics;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PairRatingServiceTest {

    private final PairRatingService service = new PairRatingService(TestMetrics.defaults());

    @Test
    void officialPairRatingIsAverage() {
        assertEquals(350.0, service.pairRating(400.0, 300.0), 1e-9);
    }

    @Test
    void officialPairRatingIsOrderIndependent() {
        assertEquals(service.pairRating(400.0, 300.0), service.pairRating(300.0, 400.0), 1e-9);
    }

    @Test
    void forecastRatingWithoutPlayabilityEqualsOfficial() {
        // S_partner = 0 → бонус 0
        assertEquals(350.0, service.pairRatingForForecast(400.0, 300.0, 0.0), 1e-9);
    }

    @Test
    void forecastRatingAtS0GivesHalfOfMaxBonus() {
        // S_partner = S0 (=1) → бонус = Bmax·(1 - 0.5) = 20·0.5 = 10 → 350 + 10 = 360
        assertEquals(360.0, service.pairRatingForForecast(400.0, 300.0, 1.0), 1e-9);
    }

    @Test
    void forecastBonusSaturatesTowardsMax() {
        // при большой сыгранности бонус → Bmax (20): 350 + ~20
        assertEquals(370.0, service.pairRatingForForecast(400.0, 300.0, 100.0), 1e-3);
    }
}
