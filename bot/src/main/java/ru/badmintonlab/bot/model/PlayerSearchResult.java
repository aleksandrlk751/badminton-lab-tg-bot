package ru.badmintonlab.bot.model;

/**
 * Строка результата поиска игрока: ник, ФИО, город.
 */
public record PlayerSearchResult(long playerId, String nick, String fullName, String city) {
}
