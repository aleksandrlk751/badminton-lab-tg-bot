package ru.badmintonlab.bot.util;

/**
 * Ссылки на профиль игрока на badminton4u.ru.
 */
public final class ProfileLinks {

    private static final String BASE = "https://badminton4u.ru/players/";

    private ProfileLinks() {
    }

    public static String url(long playerId) {
        return BASE + playerId;
    }
}
