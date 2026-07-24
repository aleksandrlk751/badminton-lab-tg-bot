package ru.badmintonlab.parser;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import ru.badmintonlab.parser.model.TournamentPageMeta;
import ru.badmintonlab.parser.support.ParseUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Метаданные страницы турнира (в т.ч. будущего): лимит, дата, категория в названии.
 */
public class TournamentPageParser {

    private static final DateTimeFormatter MARK_DATE =
            DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.forLanguageTag("ru"));
    private static final Pattern META_TITLE_DATE = Pattern.compile(
            "турнир\\s+(\\d{1,2}\\s+\\p{L}+\\s+\\d{4})\\s+в\\s+(\\d{1,2}:\\d{2})",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

    public TournamentPageMeta parse(Document document) {
        long id = extractTournamentId(document);
        Element h1 = document.selectFirst("h1");
        Optional<BigDecimal> limit = Optional.empty();
        String displayName = "";
        if (h1 != null) {
            limit = h1.select("var").stream()
                    .findFirst()
                    .flatMap(v -> ParseUtils.parseDecimal(v.text()));
            displayName = h1.text().replaceAll("\\s+", " ").trim();
            h1.select("var").forEach(Element::remove);
            displayName = h1.text().replaceAll("\\s+", " ").trim();
        }
        String category = extractCategory(displayName);
        LocalDateTime starts = parseStarts(document);
        boolean doubles = document.selectFirst("a.doubles, a.doubles-green, table.tour-doubles") != null
                || displayName.toUpperCase(Locale.ROOT).contains("XDE")
                || displayName.toUpperCase(Locale.ROOT).contains("XDC")
                || displayName.toUpperCase(Locale.ROOT).endsWith(" DF")
                || displayName.toUpperCase(Locale.ROOT).endsWith(" DG");
        return new TournamentPageMeta(id, displayName, category, limit, starts, doubles);
    }

    private String extractCategory(String displayName) {
        if (displayName == null || displayName.isBlank()) {
            return null;
        }
        int space = displayName.trim().lastIndexOf(' ');
        if (space < 0) {
            return null;
        }
        String token = displayName.trim().substring(space + 1);
        return token.length() <= 16 ? token : null;
    }

    private LocalDateTime parseStarts(Document document) {
        Element markDate = document.selectFirst("section.tour-desc p mark");
        Element markTime = document.selectFirst("section.tour-desc p mark:nth-of-type(2)");
        if (markDate != null && markTime != null) {
            try {
                LocalDate date = LocalDate.parse(markDate.text().trim(), MARK_DATE);
                LocalTime time = LocalTime.parse(markTime.text().trim());
                return LocalDateTime.of(date, time);
            } catch (RuntimeException ignored) {
                // fallback
            }
        }
        Element meta = document.selectFirst("meta[name=title]");
        if (meta != null) {
            Matcher m = META_TITLE_DATE.matcher(meta.attr("content"));
            if (m.find()) {
                try {
                    LocalDate date = LocalDate.parse(m.group(1).trim(), MARK_DATE);
                    LocalTime time = LocalTime.parse(m.group(2).trim());
                    return LocalDateTime.of(date, time);
                } catch (RuntimeException ignored) {
                    // fallback
                }
            }
        }
        return LocalDateTime.now();
    }

    private long extractTournamentId(Document document) {
        Element commentButton = document.selectFirst("button[onclick^=addComment]");
        if (commentButton != null) {
            Matcher matcher = Pattern.compile("addComment\\((\\d+)").matcher(commentButton.attr("onclick"));
            if (matcher.find()) {
                return Long.parseLong(matcher.group(1));
            }
        }
        Element regButton = document.selectFirst("[onclick*=regToTourDouble], [onclick*=regToTourTeamUser]");
        if (regButton != null) {
            Matcher matcher = Pattern.compile("regToTour(?:Double|TeamUser)\\((\\d+)").matcher(regButton.attr("onclick"));
            if (matcher.find()) {
                return Long.parseLong(matcher.group(1));
            }
        }
        Element breadcrumb = document.selectFirst("ol.breadcrumbs a[href*=tournaments/]");
        if (breadcrumb != null) {
            return ParseUtils.extractTournamentId(breadcrumb.attr("href")).orElseThrow();
        }
        throw new IllegalStateException("Tournament id not found on page");
    }
}
