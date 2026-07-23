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
    /** Победа. */
    public static final String WIN = "\uD83D\uDFE2";
    /** Поражение. */
    public static final String LOSS = "\uD83D\uDD34";
    /** Доля побед, %. */
    public static final String WIN_RATE = "\uD83D\uDCCA";
    /** Форма игрока (метрика Form). */
    public static final String FORM = "\uD83D\uDC4A";
    /** Предпочтение в «Игровом акценте». */
    public static final String GAME_ACCENT_PREFERENCE = "\uD83C\uDFAF";
    /** Сильная сторона в «Игровом акценте». */
    public static final String GAME_ACCENT_STRENGTH = "\uD83D\uDCAA";

    private MessageEmoji() {
    }

    public static String rating(Discipline discipline) {
        return discipline == Discipline.S ? SINGLE : DOUBLE;
    }
}
