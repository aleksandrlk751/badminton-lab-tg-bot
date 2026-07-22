package ru.badmintonlab.bot.model;

import ru.badmintonlab.bot.util.Names;

import java.math.BigDecimal;

/**
 * Строка результата поиска игрока для списка кнопок и H2H-wizard.
 */
public record PlayerSearchResult(
        long playerId,
        String nick,
        String lastName,
        String firstName,
        String patronymic,
        String city,
        BigDecimal ratingS,
        BigDecimal ratingD
) {

    public String fullName() {
        return Names.fullName(lastName, firstName, patronymic);
    }

    public String fullNameWithoutPatronymic() {
        return Names.fullName(lastName, firstName, null);
    }

    /** Основная подпись: ФИО, иначе ник, иначе «Игрок {id}». */
    public String displayName() {
        if (!fullName().isBlank()) {
            return fullName();
        }
        if (nick != null && !nick.isBlank()) {
            return nick;
        }
        return "Игрок " + playerId;
    }
}
