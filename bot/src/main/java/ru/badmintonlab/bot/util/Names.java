package ru.badmintonlab.bot.util;

import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Сборка ФИО из отдельных полей игрока (пропускает пустые части).
 */
public final class Names {

    private Names() {
    }

    public static String fullName(String lastName, String firstName, String patronymic) {
        String joined = Stream.of(lastName, firstName, patronymic)
                .filter(s -> s != null && !s.isBlank())
                .map(String::trim)
                .collect(Collectors.joining(" "));
        return joined.isBlank() ? "" : joined;
    }

    /** «Иванов И.» для строки прогноза. */
    public static String shortName(String fullName) {
        if (fullName == null || fullName.isBlank()) {
            return "";
        }
        String[] parts = fullName.trim().split("\\s+");
        if (parts.length == 1) {
            return parts[0];
        }
        return parts[0] + " " + parts[1].charAt(0) + ".";
    }
}
