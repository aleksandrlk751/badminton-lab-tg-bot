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
}
