package ru.badmintonlab.bot.model;

import java.time.LocalDate;

/**
 * Краткая информация о последнем турнире игрока для карточки.
 */
public record LastTournamentInfo(String name, LocalDate date, Short place, String resultLabel) {
}
