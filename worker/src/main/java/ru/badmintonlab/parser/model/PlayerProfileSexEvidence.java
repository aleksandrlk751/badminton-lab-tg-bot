package ru.badmintonlab.parser.model;

import java.util.List;

/**
 * Признаки пола из профиля игрока: дисциплины рейтинга и коды категорий турниров (участия без фильтра региона).
 */
public record PlayerProfileSexEvidence(
        List<Discipline> ratingDisciplines,
        List<String> tournamentCategoryCodes
) {
}
