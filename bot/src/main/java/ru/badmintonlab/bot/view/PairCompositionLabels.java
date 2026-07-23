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
}
