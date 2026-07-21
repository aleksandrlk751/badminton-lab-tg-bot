package ru.badmintonlab.parser.support;

import org.jsoup.nodes.Element;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ParseUtils {

    private static final Pattern PLAYER_ID = Pattern.compile("players/(\\d+)");

    private ParseUtils() {
    }

    public static Optional<Long> extractPlayerId(String href) {
        if (href == null) {
            return Optional.empty();
        }
        Matcher matcher = PLAYER_ID.matcher(href);
        if (matcher.find()) {
            return Optional.of(Long.parseLong(matcher.group(1)));
        }
        return Optional.empty();
    }

    public static Optional<Long> extractTournamentId(String href) {
        if (href == null) {
            return Optional.empty();
        }
        Matcher matcher = Pattern.compile("tournaments/(\\d+)").matcher(href);
        if (matcher.find()) {
            return Optional.of(Long.parseLong(matcher.group(1)));
        }
        return Optional.empty();
    }

    public static Optional<BigDecimal> parseDecimal(String text) {
        if (text == null || text.isBlank()) {
            return Optional.empty();
        }
        String normalized = text.trim()
                .replace('\u2212', '-')
                .replace("+", "")
                .replace(",", ".");
        if (normalized.isBlank() || "отк".equalsIgnoreCase(normalized)) {
            return Optional.empty();
        }
        return Optional.of(new BigDecimal(normalized));
    }

    public static Optional<BigDecimal> parseVar(Element parent) {
        if (parent == null) {
            return Optional.empty();
        }
        Element var = parent.selectFirst("var");
        if (var == null) {
            return Optional.empty();
        }
        String text = var.text().trim();
        if (text.isEmpty() || "отк".equalsIgnoreCase(text)) {
            return Optional.empty();
        }
        return Optional.of(new BigDecimal(text.replace(",", ".")));
    }
}
