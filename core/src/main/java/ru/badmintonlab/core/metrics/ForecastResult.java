package ru.badmintonlab.core.metrics;

/**
 * Результат прогноза P3 (§2.3 {@code docs/FORMULAR.md}) с разложением на компоненты — для показа
 * обоснования пользователю («Фаворит A ≈62%: рейтинг, форма, H2H, S»).
 *
 * @param probabilityA вероятность победы стороны A ({@code P} после смешивания), диапазон (0, 1)
 * @param pModel       вероятность по эффективному рейтингу (логистика)
 * @param pH2h         вероятность из личной истории (сглаживание Лапласа)
 * @param blendWeight  вес смешивания {@code w = S / (S + S_ref)}
 * @param ratingEffA   эффективный рейтинг A ({@code R_A + k · Form_A})
 * @param ratingEffB   эффективный рейтинг B ({@code R_B + k · Form_B})
 */
public record ForecastResult(
        double probabilityA,
        double pModel,
        double pH2h,
        double blendWeight,
        double ratingEffA,
        double ratingEffB
) {

    /** Вероятность победы стороны B. */
    public double probabilityB() {
        return 1.0 - probabilityA;
    }

    /** {@code true}, если фаворит — сторона A (или равные шансы). */
    public boolean favoriteIsA() {
        return probabilityA >= 0.5;
    }
}
