package ru.badmintonlab.core.domain;

import java.util.Locale;

/**
 * Разряд парного турнира по коду категории в названии (SE, XDE, DF, …).
 */
public final class TournamentDisciplineSupport {

    private TournamentDisciplineSupport() {
    }

    /**
     * Парный разряд турнира для лимита рейтинга и фильтра пола.
     * {@link Discipline#D} — generic «пары» без уточнения MD/WD/XD.
     */
    public static Discipline pairDiscipline(String categoryCode, String tournamentName) {
        String token = normalizeToken(categoryCode, tournamentName);
        if (token == null) {
            return Discipline.D;
        }
        if (isMixed(token)) {
            return Discipline.XD;
        }
        if (isWomensDoubles(token)) {
            return Discipline.WD;
        }
        if (isMensDoubles(token)) {
            return Discipline.MD;
        }
        return Discipline.D;
    }

    /**
     * Тип пары для сравнения с игровым акцентом (рекомендуемая категория).
     */
    public static PairCompositionType compositionForTournament(Discipline pairDiscipline) {
        return switch (pairDiscipline) {
            case MD -> PairCompositionType.MD;
            case WD -> PairCompositionType.WD;
            case XD -> PairCompositionType.XD;
            default -> null;
        };
    }

    private static String normalizeToken(String categoryCode, String tournamentName) {
        if (categoryCode != null && !categoryCode.isBlank()) {
            return categoryCode.trim().toUpperCase(Locale.ROOT);
        }
        if (tournamentName == null || tournamentName.isBlank()) {
            return null;
        }
        String trimmed = tournamentName.trim();
        int space = trimmed.lastIndexOf(' ');
        if (space < 0 || space == trimmed.length() - 1) {
            return null;
        }
        String last = trimmed.substring(space + 1).trim();
        return last.length() <= 16 ? last.toUpperCase(Locale.ROOT) : null;
    }

    private static boolean isMixed(String token) {
        return token.startsWith("X") || token.contains("XD") || token.endsWith("XDE")
                || token.endsWith("XDC") || token.endsWith("XDF") || token.endsWith("XDG");
    }

    private static boolean isWomensDoubles(String token) {
        return token.startsWith("W") || token.endsWith("DF") || token.endsWith("WDE")
                || token.endsWith("WDC") || token.endsWith("WDF") || token.equals("WF");
    }

    private static boolean isMensDoubles(String token) {
        return token.endsWith("DG") || token.endsWith("MDG") || token.endsWith("MDF")
                || token.equals("MG") || token.startsWith("MSD");
    }
}
