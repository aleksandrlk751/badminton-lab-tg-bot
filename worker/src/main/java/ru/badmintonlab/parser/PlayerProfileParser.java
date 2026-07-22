package ru.badmintonlab.parser;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import ru.badmintonlab.parser.model.Discipline;
import ru.badmintonlab.parser.model.PlayerProfile;
import ru.badmintonlab.parser.support.ParseUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PlayerProfileParser {

    private static final DateTimeFormatter CHART_DATE =
            DateTimeFormatter.ofPattern("dd.MM.yyyy", Locale.forLanguageTag("ru"));
    private static final Pattern CHART_ALL =
            Pattern.compile("var chartAllLabels_(\\w+) = \\[(.*?)];var chartAllData_\\1 = \\[(.*?)];", Pattern.DOTALL);

    public PlayerProfile parse(Document document) {
        long id = extractPlayerId(document);
        Element info = document.selectFirst("section.player-info");
        if (info == null) {
            throw new IllegalStateException("Player info section not found");
        }

        String nick = parseNick(info);
        String fullName = info.selectFirst("h1") != null ? info.selectFirst("h1").text().trim() : "";
        Optional<String> city = parseLabeledValue(document, "город:");
        Optional<String> hand = parseLabeledValue(document, "Игровая рука:");

        Map<Discipline, BigDecimal> ratings = parseRatings(info);
        Map<Discipline, List<PlayerProfile.RatingPoint>> histories = new LinkedHashMap<>();
        for (Discipline discipline : ratings.keySet()) {
            List<PlayerProfile.RatingPoint> history = parseRatingHistory(document, discipline.name().toLowerCase(Locale.ROOT));
            if (!history.isEmpty()) {
                histories.put(discipline, history);
            }
        }

        return new PlayerProfile(id, nick, fullName, city, hand, ratings, histories);
    }

    private long extractPlayerId(Document document) {
        Element breadcrumb = document.selectFirst("ol.breadcrumbs a[href*=players/]");
        if (breadcrumb != null) {
            Optional<Long> id = ParseUtils.extractPlayerId(breadcrumb.attr("href"));
            if (id.isPresent()) {
                return id.get();
            }
        }
        Element alternate = document.selectFirst("link[rel=alternate][href*=players/]");
        if (alternate != null) {
            Matcher matcher = Pattern.compile("players/(\\d+)").matcher(alternate.attr("href"));
            if (matcher.find()) {
                return Long.parseLong(matcher.group(1));
            }
        }
        throw new IllegalStateException("Player id not found");
    }

    private String parseNick(Element info) {
        Element h3 = info.selectFirst("h3");
        if (h3 == null) {
            return "";
        }
        String ownText = h3.ownText();
        if (!ownText.isBlank()) {
            return ownText.trim();
        }
        return h3.text().replaceAll("\\s*\\d+([.,]\\d+)?\\s*$", "").trim();
    }

    private Optional<String> parseLabeledValue(Document document, String label) {
        for (Element p : document.select("section.player-info p")) {
            if (p.text().contains(label)) {
                Element strong = p.selectFirst("strong");
                if (strong != null) {
                    return Optional.of(strong.text().trim());
                }
            }
        }
        return Optional.empty();
    }

    private Map<Discipline, BigDecimal> parseRatings(Element info) {
        Map<Discipline, BigDecimal> ratings = new LinkedHashMap<>();
        for (Element tab : info.select("#tabs li[data-tab]")) {
            Discipline discipline = Discipline.fromRatingTab(tab.attr("data-tab")).orElse(null);
            if (discipline == null) {
                continue;
            }
            Elements dfns = tab.select("dfn");
            for (Element dfn : dfns) {
                ParseUtils.parseDecimal(dfn.text()).ifPresent(value -> ratings.putIfAbsent(discipline, value));
            }
        }
        return ratings;
    }

    List<PlayerProfile.RatingPoint> parseRatingHistory(Document document, String siteCode) {
        Matcher matcher = CHART_ALL.matcher(document.html());
        while (matcher.find()) {
            if (!matcher.group(1).equalsIgnoreCase(siteCode)) {
                continue;
            }
            return parseChartPoints(matcher.group(2), matcher.group(3));
        }
        return List.of();
    }

    private List<PlayerProfile.RatingPoint> parseChartPoints(String labelsRaw, String dataRaw) {
        String[] labels = labelsRaw.split(",");
        String[] data = dataRaw.split(",");
        List<PlayerProfile.RatingPoint> points = new ArrayList<>();
        int count = Math.min(labels.length, data.length);
        for (int i = 0; i < count; i++) {
            String label = stripQuotes(labels[i]).trim();
            String value = stripQuotes(data[i]).trim();
            if (label.isEmpty() || value.isEmpty()) {
                continue;
            }
            LocalDate date = LocalDate.parse(label, CHART_DATE);
            BigDecimal rating = new BigDecimal(value.replace(",", "."));
            points.add(new PlayerProfile.RatingPoint(date, rating));
        }
        return points;
    }

    private String stripQuotes(String value) {
        return value.replace("\"", "").trim();
    }
}
