package ru.badmintonlab.core.metrics;

import org.springframework.stereotype.Service;
import ru.badmintonlab.core.config.MetricsProperties;

/**
 * Рейтинг пары.
 * <ul>
 *   <li><b>Официальный</b> (§1.2 {@code docs/FORMULAR.md}, зеркалим as-is): {@code R_пара = (A + B) / 2}.</li>
 *   <li><b>Прогнозный</b> (§2.4, наша надстройка): к среднему добавляется бонус за сыгранность
 *       партнёров, насыщающийся к {@code Bmax}:
 *       {@code (A + B) / 2 + Bmax · (1 - 0.5^(S_partner / S0))}.</li>
 * </ul>
 * Константы {@code Bmax}, {@code S0} — из {@link MetricsProperties}.
 */
@Service
public class PairRatingService {

    private final MetricsProperties metrics;

    public PairRatingService(MetricsProperties metrics) {
        this.metrics = metrics;
    }

    /** Официальный рейтинг пары {@code (A + B) / 2}. Порядок аргументов не важен. */
    public double pairRating(double ratingA, double ratingB) {
        return (ratingA + ratingB) / 2.0;
    }

    /**
     * Прогнозный рейтинг пары: официальный средний + бонус за сыгранность.
     *
     * @param ratingA            рейтинг одного партнёра
     * @param ratingB            рейтинг второго партнёра
     * @param partnerPlayability индекс сыгранности партнёров {@code S_partner} ({@code ≥ 0})
     */
    public double pairRatingForForecast(double ratingA, double ratingB, double partnerPlayability) {
        double bMax = metrics.bMax().doubleValue();
        double s0 = metrics.s0().doubleValue();
        double bonus = bMax * (1.0 - Math.pow(0.5, partnerPlayability / s0));
        return pairRating(ratingA, ratingB) + bonus;
    }
}
