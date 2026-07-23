package ru.badmintonlab.core.domain;

import java.util.Collection;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;

/**
 * Вывод пола по дисциплинам рейтинга, пар или коду категории турнира.
 * Generic D/XD и смешанные категории (X*) не используются; конфликт M+F → {@code null}.
 */
public final class PlayerSexInference {

    private static final Set<Discipline> MALE_DISCIPLINES = EnumSet.of(Discipline.MS, Discipline.MD);
    private static final Set<Discipline> FEMALE_DISCIPLINES = EnumSet.of(Discipline.WS, Discipline.WD);

    private PlayerSexInference() {
    }

    public static PlayerSex inferFromDisciplines(Collection<Discipline> disciplines) {
        boolean male = disciplines.stream().anyMatch(MALE_DISCIPLINES::contains);
        boolean female = disciplines.stream().anyMatch(FEMALE_DISCIPLINES::contains);
        if (male && !female) {
            return PlayerSex.M;
        }
        if (female && !male) {
            return PlayerSex.F;
        }
        return null;
    }

    public static PlayerSex inferFromCategoryCodes(Collection<String> categoryCodes) {
        boolean male = false;
        boolean female = false;
        for (String code : categoryCodes) {
            PlayerSex sex = sexFromCategoryCode(code);
            if (sex == PlayerSex.M) {
                male = true;
            } else if (sex == PlayerSex.F) {
                female = true;
            }
        }
        if (male && !female) {
            return PlayerSex.M;
        }
        if (female && !male) {
            return PlayerSex.F;
        }
        return null;
    }

    /**
     * Префикс кода категории: MS/MD → M, WS/WD → F; X* и generic D — не используются.
     */
    static PlayerSex sexFromCategoryCode(String categoryCode) {
        if (categoryCode == null || categoryCode.isBlank()) {
            return null;
        }
        String upper = categoryCode.trim().toUpperCase(Locale.ROOT);
        if (upper.startsWith("X")) {
            return null;
        }
        if (upper.startsWith("WD") || upper.startsWith("WS")) {
            return PlayerSex.F;
        }
        if (upper.startsWith("MD") || upper.startsWith("MS")) {
            return PlayerSex.M;
        }
        return null;
    }
}
