package ru.badmintonlab.core.repository.projection;

import ru.badmintonlab.core.domain.Discipline;

/**
 * Проекция «сколько встреч у игрока в дисциплине» — для выбора дисциплины по умолчанию
 * и переключателя на экране соперников.
 */
public interface DisciplineGamesView {

    Discipline getDiscipline();

    Long getGames();
}
