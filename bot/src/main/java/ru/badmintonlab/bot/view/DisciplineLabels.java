package ru.badmintonlab.bot.view;

import ru.badmintonlab.core.domain.Discipline;

/**
 * Ярлыки дисциплин для UI. Используем коды как на badminton4u.ru (D/MD/WD/XD/S/MS/WS),
 * чтобы не изобретать локализацию — они знакомы игрокам ЛАБ.
 */
public final class DisciplineLabels {

    private DisciplineLabels() {
    }

    public static String label(Discipline discipline) {
        return discipline.name();
    }

    /** Префикс личного рейтинга: 👤 (single) / 👥 (double). */
    public static String ratingLabel(Discipline discipline) {
        return MessageEmoji.rating(discipline);
    }
}
