package ru.badmintonlab.parser;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import ru.badmintonlab.parser.model.Discipline;
import ru.badmintonlab.parser.model.PlayerProfileSexEvidence;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Участия и разряды из SSR профиля {@code /players/{id}} — все регионы, без {@code cities[]=}.
 */
public class PlayerProfileSexEvidenceParser {

    private static final Pattern CHART_ALL =
            Pattern.compile("var chartAllLabels_(\\w+) = \\[(.*?)];var chartAllData_\\1 = \\[(.*?)];", Pattern.DOTALL);
    private static final Pattern CATEGORY_TOKEN =
            Pattern.compile("^[MW][DS][A-Z0-9+]*$|^[MW]S[A-Z0-9+]*$");

    public PlayerProfileSexEvidence parse(Document document) {
        Set<Discipline> disciplines = new LinkedHashSet<>();
        collectRatingDisciplines(document, disciplines);

        List<String> categoryCodes = new ArrayList<>();
        for (Element link : document.select("main section[class^=rat_] a[href*=tournaments/]")) {
            extractCategoryCode(link.text()).ifPresent(categoryCodes::add);
        }

        return new PlayerProfileSexEvidence(List.copyOf(disciplines), List.copyOf(categoryCodes));
    }

    private void collectRatingDisciplines(Document document, Set<Discipline> disciplines) {
        Element tabs = document.selectFirst("#tabs[data-tabs]");
        if (tabs == null) {
            return;
        }
        for (String tab : tabs.attr("data-tabs").split(",")) {
            Discipline.fromRatingTab(tab.trim()).ifPresent(discipline -> {
                if (hasRatingHistory(document, discipline)) {
                    disciplines.add(discipline);
                }
            });
        }
    }

    private boolean hasRatingHistory(Document document, Discipline discipline) {
        String siteCode = discipline.name().toLowerCase(Locale.ROOT);
        Matcher matcher = CHART_ALL.matcher(document.html());
        while (matcher.find()) {
            if (!matcher.group(1).equalsIgnoreCase(siteCode)) {
                continue;
            }
            return countNonEmptyChartPoints(matcher.group(2)) >= 2;
        }
        return false;
    }

    private int countNonEmptyChartPoints(String labelsRaw) {
        int count = 0;
        for (String label : labelsRaw.split(",")) {
            if (!stripQuotes(label).isEmpty()) {
                count++;
            }
        }
        return count;
    }

    Optional<String> extractCategoryCode(String linkText) {
        if (linkText == null || linkText.isBlank()) {
            return Optional.empty();
        }
        String[] tokens = linkText.trim().split("\\s+");
        for (int i = tokens.length - 1; i >= 0; i--) {
            String candidate = tokens[i].toUpperCase(Locale.ROOT);
            if (CATEGORY_TOKEN.matcher(candidate).matches()) {
                return Optional.of(candidate);
            }
        }
        return Optional.empty();
    }

    private String stripQuotes(String value) {
        return value.replace("\"", "").trim();
    }
}
