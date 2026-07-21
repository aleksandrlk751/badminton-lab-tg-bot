package ru.badmintonlab.parser;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import ru.badmintonlab.parser.model.Discipline;
import ru.badmintonlab.parser.model.RivalSummaryEntry;
import ru.badmintonlab.parser.support.ParseUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class RivalsParser {

    public List<RivalSummaryEntry> parse(Document document, Discipline discipline) {
        String sectionClass = switch (discipline) {
            case S -> "rat_s";
            case D -> "rat_d";
            case MS -> "rat_ms";
            case WS -> "rat_ws";
            case MD -> "rat_md";
            case WD -> "rat_wd";
            case XD -> "rat_xd";
        };

        Element section = document.selectFirst("section." + sectionClass + " section.player-rivals");
        if (section == null) {
            section = findRivalsSectionWithData(document);
        }
        if (section == null) {
            return List.of();
        }

        Element table = section.selectFirst("table");
        if (table == null) {
            return List.of();
        }

        List<RivalSummaryEntry> entries = new ArrayList<>();
        for (Element row : table.select("tbody tr")) {
            Elements cells = row.select("td");
            if (cells.size() < 7) {
                continue;
            }
            Element opponentLink = cells.get(0).selectFirst("a[href*=players/]");
            if (opponentLink == null) {
                continue;
            }

            long opponentId = ParseUtils.extractPlayerId(opponentLink.attr("href")).orElseThrow();
            Optional<java.math.BigDecimal> rating = cells.get(1).select("dfn").stream()
                    .findFirst()
                    .flatMap(dfn -> ParseUtils.parseDecimal(dfn.text()));

            int games = parseInt(cells.get(2).text());
            int wins = parseInt(cells.get(3).text());
            int losses = parseInt(cells.get(4).text());
            Optional<java.math.BigDecimal> delta = Optional.ofNullable(cells.get(6).attr("data-sort"))
                    .filter(s -> !s.isBlank())
                    .flatMap(ParseUtils::parseDecimal)
                    .or(() -> ParseUtils.parseDecimal(cells.get(6).text()));

            entries.add(new RivalSummaryEntry(opponentId, rating, games, wins, losses, delta));
        }
        return entries;
    }

    private Element findRivalsSectionWithData(Document document) {
        for (Element section : document.select("section.player-rivals")) {
            if (!section.select("tbody tr td a[href*=players/]").isEmpty()) {
                return section;
            }
        }
        return null;
    }

    private int parseInt(String text) {
        try {
            return Integer.parseInt(text.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
