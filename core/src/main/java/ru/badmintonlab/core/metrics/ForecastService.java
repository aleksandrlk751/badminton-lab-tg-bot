package ru.badmintonlab.core.metrics;

import org.springframework.stereotype.Service;
import ru.badmintonlab.core.config.MetricsProperties;

/**
 * Прогноз исхода встречи P3 (§2.3 {@code docs/FORMULAR.md}):
 * <pre>
 * R_eff_A = R_A + k · Form_A
 * R_eff_B = R_B + k · Form_B
 * P_model = 1 / (1 + 10^(-(R_eff_A - R_eff_B) / 100))   # логистика, масштаб 100 — не параметр
 * P_h2h   = (W_A + 1) / (W_A + W_B + 2)                  # сглаживание Лапласа
 * w       = S / (S + S_ref)
 * P       = (1 - w) · P_model + w · P_h2h
 * </pre>
 * Константы {@code k}, {@code S_ref} — из {@link MetricsProperties}; масштаб {@code 100} — из
 * официальной шкалы (§1.1), хардкод.
 * <p><b>Парный прогноз</b> (§2.4) использует тот же метод: подставляются прогнозный рейтинг пары
 * ({@link PairRatingService#pairRatingForForecast}), средняя форма партнёров, W/L и {@code S} между
 * парами.
 */
@Service
public class ForecastService {

    private static final double LOGISTIC_SCALE = 100.0;

    private final MetricsProperties metrics;

    public ForecastService(MetricsProperties metrics) {
        this.metrics = metrics;
    }

    /**
     * @param ratingA          официальный рейтинг стороны A (для пары — прогнозный рейтинг пары)
     * @param ratingB          официальный рейтинг стороны B
     * @param formA            форма стороны A ({@link FormService})
     * @param formB            форма стороны B
     * @param winsA            число побед A над B в личной истории
     * @param winsB            число побед B над A в личной истории
     * @param playabilityIndex индекс сыгранности {@code S} между сторонами ({@link PlayabilityIndexService})
     */
    public ForecastResult forecast(double ratingA, double ratingB,
                                   double formA, double formB,
                                   int winsA, int winsB,
                                   double playabilityIndex) {
        double k = metrics.formK().doubleValue();
        double ratingEffA = ratingA + k * formA;
        double ratingEffB = ratingB + k * formB;

        double pModel = 1.0 / (1.0 + Math.pow(10.0, -(ratingEffA - ratingEffB) / LOGISTIC_SCALE));
        double pH2h = (winsA + 1.0) / (winsA + winsB + 2.0);

        double sRef = metrics.sRef().doubleValue();
        double weight = playabilityIndex / (playabilityIndex + sRef);
        double probabilityA = (1.0 - weight) * pModel + weight * pH2h;

        return new ForecastResult(probabilityA, pModel, pH2h, weight, ratingEffA, ratingEffB);
    }
}
