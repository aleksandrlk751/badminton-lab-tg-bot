package ru.badmintonlab.core.metrics;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ForecastServiceTest {

    private final ForecastService service = new ForecastService(TestMetrics.defaults());

    @Test
    void equalPlayersNoHistoryGiveCoinFlip() {
        ForecastResult r = service.forecast(400, 400, 0, 0, 0, 0, 0);
        assertEquals(0.5, r.pModel(), 1e-9);
        assertEquals(0.5, r.pH2h(), 1e-9);
        assertEquals(0.0, r.blendWeight(), 1e-9);
        assertEquals(0.5, r.probabilityA(), 1e-9);
        assertEquals(0.5, r.probabilityB(), 1e-9);
    }

    @Test
    void hundredPointRatingGapMatchesLogisticScale() {
        // масштаб 100: разрыв 100 → 1/(1 + 10^-1) ≈ 0.9091
        ForecastResult r = service.forecast(500, 400, 0, 0, 0, 0, 0);
        assertEquals(1.0 / 1.1, r.pModel(), 1e-9);
        assertEquals(1.0 / 1.1, r.probabilityA(), 1e-9); // S=0 → чистая модель
        assertTrue(r.favoriteIsA());
    }

    @Test
    void formShiftsEffectiveRating() {
        // k=0.5, Form_A=20 → R_eff_A = 410; R_eff_B = 400
        ForecastResult r = service.forecast(400, 400, 20, 0, 0, 0, 0);
        assertEquals(410.0, r.ratingEffA(), 1e-9);
        assertEquals(400.0, r.ratingEffB(), 1e-9);
        assertTrue(r.probabilityA() > 0.5);
    }

    @Test
    void highPlayabilityShiftsWeightTowardsH2h() {
        // S большой → w≈1 → P≈P_h2h; несмотря на равный рейтинг, история 5:0 делает A фаворитом
        ForecastResult r = service.forecast(400, 400, 0, 0, 5, 0, 1000);
        assertEquals(6.0 / 7.0, r.pH2h(), 1e-9);
        assertTrue(r.blendWeight() > 0.99);
        assertEquals(6.0 / 7.0, r.probabilityA(), 1e-2);
        assertTrue(r.favoriteIsA());
    }

    @Test
    void combinedRealisticCase() {
        // R_A=420, R_B=380; Form_A=+8, Form_B=-4; H2H 3:1; S=2
        // R_eff: 424 vs 378; P_model≈0.7425; P_h2h=4/6≈0.6667; w=2/3 → P≈0.6920
        ForecastResult r = service.forecast(420, 380, 8, -4, 3, 1, 2);
        assertEquals(424.0, r.ratingEffA(), 1e-9);
        assertEquals(378.0, r.ratingEffB(), 1e-9);
        assertEquals(0.74254, r.pModel(), 1e-4);
        assertEquals(4.0 / 6.0, r.pH2h(), 1e-9);
        assertEquals(2.0 / 3.0, r.blendWeight(), 1e-9);
        assertEquals(0.69196, r.probabilityA(), 1e-4);
        assertTrue(r.favoriteIsA());
        assertFalse(r.probabilityA() > 0.9);
    }

    @Test
    void pairForecastReusesGenericMethod() {
        // парный прогноз (§2.4): прогнозные рейтинги пар + средняя форма партнёров
        PairRatingService pairs = new PairRatingService(TestMetrics.defaults());
        double pairA = pairs.pairRatingForForecast(430, 410, 3);   // сыгранная сильная пара
        double pairB = pairs.pairRatingForForecast(420, 400, 0);   // новая пара
        ForecastResult r = service.forecast(pairA, pairB, 5, -2, 1, 0, 1);
        assertTrue(pairA > pairB);
        assertTrue(r.favoriteIsA());
    }
}
