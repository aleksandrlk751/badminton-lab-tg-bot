package ru.badmintonlab.parser.model;

import java.util.Optional;

/**
 * Строка справочника игроков ({@code players/?sex_m=1} / {@code sex_f=1}).
 */
public record PlayerDirectoryEntry(long id, Optional<String> nick) {
}
