package ru.badmintonlab.bot.view;

import ru.badmintonlab.core.domain.Discipline;

/**
 * Эмодзи для текстов бота — единый набор, чтобы рендер в Telegram был предсказуемым.
 */
public final class MessageEmoji {

    /** Одиночный рейтинг (S): один человек. */
    public static final String SINGLE = "\uD83D\uDC64";
    /** Парный рейтинг (D): два человека. */
    public static final String DOUBLE = "\uD83D\uDC65";
    /** Средний рейтинг пары (R_A + R_B) / 2 при подборе партнёра. */
    public static final String PAIR_RATING_AVG = "\u2696\uFE0F";
    /** Score совместимости партнёра, %. */
    public static final String PARTNER_SUITABILITY = "\uD83C\uDFAF";
    /** Победа. */
    public static final String WIN = "\uD83D\uDFE2";
    /** Поражение. */
    public static final String LOSS = "\uD83D\uDD34";
    /** Доля побед, %. */
    public static final String WIN_RATE = "\uD83D\uDCCA";
    /** Форма игрока (метрика Form). */
    public static final String FORM = "\uD83D\uDC4A";
    /** Доля и число игр (ось предпочтения игрового акцента). */
    public static final String GAME_ACCENT_PREFERENCE = "\uD83D\uDCCA";
    /** Средняя δ и рост (ось сильной стороны игрового акцента). */
    public static final String GAME_ACCENT_STRENGTH = "\uD83D\uDCC8";
    /** Типичная δ за матч в рекомендуемой категории (ось 2). */
    public static final String RECOMMENDED_CATEGORY = "\uD83E\uDDEE";
    /** Нет данных / недостаточно информации для показателя. */
    public static final String UNKNOWN = "\uD83E\uDD37\u200D\u2642\uFE0F";

    private MessageEmoji() {
    }

    public static String rating(Discipline discipline) {
        return discipline == Discipline.S ? SINGLE : DOUBLE;
    }
}
