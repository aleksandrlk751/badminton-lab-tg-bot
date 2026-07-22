package ru.badmintonlab.core.repository.projection;

import java.time.Instant;

/**
 * Проекция «последний турнир игрока» для карточки: название, дата старта, занятое место.
 */
public interface LastTournamentView {

    String getName();

    Instant getStartsAt();

    Short getPlace();
}
