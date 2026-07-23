package ru.badmintonlab.bot.view;

import ru.badmintonlab.core.domain.PairCompositionType;

/** Человекочитаемые названия типа пары для UI. */
public final class PairCompositionLabels {

    private PairCompositionLabels() {
    }

    public static String label(PairCompositionType type) {
        return switch (type) {
            case MD -> "мужская пара";
            case WD -> "женская пара";
            case XD -> "микст";
            case UNKNOWN -> "неизвестно";
        };
    }

    /** Название типа пары для карточки игрока — с заглавной буквы. */
    public static String cardLabel(PairCompositionType type) {
        String label = label(type);
        if (label.isEmpty()) {
            return label;
        }
        return Character.toUpperCase(label.charAt(0)) + label.substring(1);
    }
}
