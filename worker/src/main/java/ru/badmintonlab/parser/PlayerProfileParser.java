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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
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
        for (Discipline discipline : ratingDisciplines(info, ratings)) {
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
            return ownText.replaceAll("\\s*/\\s*$", "").trim();
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

    /**
     * Текущий рейтинг: вкладки SINGLE/DOUBLE у графика; если в tab нет числа — fallback из {@code h3} у ника.
     * Не используем chartAllData (история) — там точки прошлых турниров.
     */
    private Map<Discipline, BigDecimal> parseRatings(Element info) {
        Map<Discipline, BigDecimal> ratings = new LinkedHashMap<>();
        parseRatingsFromTabs(info, ratings);
        parseRatingsFromHeader(info, ratings);
        return ratings;
    }

    private void parseRatingsFromTabs(Element info, Map<Discipline, BigDecimal> ratings) {
        for (Element tab : info.select("#tabs li[data-tab]")) {
            Discipline discipline = Discipline.fromRatingTab(tab.attr("data-tab")).orElse(null);
            if (discipline == null) {
                continue;
            }
            lastDecimal(tab.select("dfn")).ifPresent(value -> ratings.put(discipline, value));
        }
    }

    private void parseRatingsFromHeader(Element info, Map<Discipline, BigDecimal> ratings) {
        Element h3 = info.selectFirst("h3");
        if (h3 == null) {
            return;
        }
        Elements dfns = h3.select("dfn");
        List<BigDecimal> values = dfns.stream()
                .map(dfn -> ParseUtils.parseDecimal(dfn.text()))
                .flatMap(Optional::stream)
                .toList();
        if (values.isEmpty()) {
            return;
        }
        if (h3.html().contains(" / ") && values.size() >= 2) {
            ratings.putIfAbsent(Discipline.S, values.get(0));
            ratings.putIfAbsent(Discipline.D, values.get(values.size() - 1));
            return;
        }
        if (dfns.size() >= 2 && ParseUtils.parseDecimal(dfns.get(0).text()).isEmpty()) {
            lastDecimal(dfns).ifPresent(value -> ratings.putIfAbsent(Discipline.D, value));
            return;
        }
        if (values.size() == 1) {
            resolveActiveDiscipline(info).ifPresent(d -> ratings.putIfAbsent(d, values.get(0)));
        }
    }

    private Optional<Discipline> resolveActiveDiscipline(Element info) {
        Element tab = info.selectFirst("#tabs li.act[data-tab]");
        if (tab == null) {
            tab = info.selectFirst("#tabs li[data-tab]");
        }
        if (tab == null) {
            return Optional.empty();
        }
        return Discipline.fromRatingTab(tab.attr("data-tab"));
    }

    private Set<Discipline> ratingDisciplines(Element info, Map<Discipline, BigDecimal> ratings) {
        Set<Discipline> disciplines = new LinkedHashSet<>();
        for (Element tab : info.select("#tabs li[data-tab]")) {
            Discipline.fromRatingTab(tab.attr("data-tab")).ifPresent(disciplines::add);
        }
        disciplines.addAll(ratings.keySet());
        return disciplines;
    }

    private Optional<BigDecimal> lastDecimal(Elements dfns) {
        Optional<BigDecimal> last = Optional.empty();
        for (Element dfn : dfns) {
            Optional<BigDecimal> parsed = ParseUtils.parseDecimal(dfn.text());
            if (parsed.isPresent()) {
                last = parsed;
            }
        }
        return last;
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
