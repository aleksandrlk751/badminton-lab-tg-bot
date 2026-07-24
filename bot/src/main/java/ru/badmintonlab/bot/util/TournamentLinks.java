package ru.badmintonlab.bot.util;

import java.util.OptionalLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Извлечение id турнира badminton4u из текста или URL.
 */
public final class TournamentLinks {

    private static final Pattern TOURNAMENT_ID =
            Pattern.compile("(?:https?://)?(?:m\\.)?badminton4u\\.ru/tournaments/(\\d+)", Pattern.CASE_INSENSITIVE);

    private TournamentLinks() {
    }

    public static OptionalLong parseTournamentId(String text) {
        if (text == null || text.isBlank()) {
            return OptionalLong.empty();
        }
        String trimmed = text.trim();
        Matcher matcher = TOURNAMENT_ID.matcher(trimmed);
        if (matcher.find()) {
            return OptionalLong.of(Long.parseLong(matcher.group(1)));
        }
        Matcher relative = Pattern.compile("tournaments/(\\d+)").matcher(trimmed);
        if (relative.find()) {
            return OptionalLong.of(Long.parseLong(relative.group(1)));
        }
        return OptionalLong.empty();
    }
}
