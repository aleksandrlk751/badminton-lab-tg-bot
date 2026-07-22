package ru.badmintonlab.bot.model;

/**
 * Строка экрана «Соперники»: соперник и баланс встреч (по данным rival_summary).
 */
public record RivalRow(long opponentId, String nick, String fullName, String city, int wins, int losses) {

    public int games() {
        return wins + losses;
    }
}
