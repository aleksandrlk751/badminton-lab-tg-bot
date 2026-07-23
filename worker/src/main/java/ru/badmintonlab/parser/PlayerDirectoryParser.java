package ru.badmintonlab.parser;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import ru.badmintonlab.parser.model.PlayerDirectoryEntry;
import ru.badmintonlab.parser.support.ParseUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Парсер справочника игроков: таблица в {@code section.players-list} и AJAX-фрагменты строк {@code <tr>}.
 */
public class PlayerDirectoryParser {

    /**
     * Токен сессии для AJAX-пагинации ({@code data-rand} на первой странице).
     */
    public Optional<String> extractSessionToken(Document document) {
        Element section = document.selectFirst("section.players-list");
        if (section == null) {
            return Optional.empty();
        }
        String token = section.attr("data-rand");
        return token.isBlank() ? Optional.empty() : Optional.of(token);
    }

    public List<PlayerDirectoryEntry> parse(Document document) {
        Element table = selectTable(document);
        if (table == null) {
            return List.of();
        }
        return parseRows(table.select("tr"));
    }

    /**
     * Разбор HTML-фрагмента из AJAX-ответа ({@code players={token}&limit=N}).
     */
    public List<PlayerDirectoryEntry> parseFragment(String html) {
        if (html == null || html.isBlank()) {
            return List.of();
        }
        Document fragment = Jsoup.parse("<table><tbody>" + html + "</tbody></table>");
        return parseRows(fragment.select("tr"));
    }

    private Element selectTable(Document document) {
        Element section = document.selectFirst("section.players-list");
        if (section != null) {
            return section.selectFirst("table");
        }
        return document.selectFirst("table");
    }

    private List<PlayerDirectoryEntry> parseRows(Elements rows) {
        List<PlayerDirectoryEntry> entries = new ArrayList<>();
        for (Element row : rows) {
            if (!row.select("th").isEmpty()) {
                continue;
            }
            parseRow(row).ifPresent(entries::add);
        }
        return entries;
    }

    private Optional<PlayerDirectoryEntry> parseRow(Element row) {
        Elements cells = row.select("td");
        if (cells.size() < 3) {
            return Optional.empty();
        }
        Element loginCell = cells.get(2);
        Element playerLink = loginCell.selectFirst("a[href*=players/]");
        Optional<String> nick = Optional.empty();
        if (playerLink != null) {
            nick = Optional.of(playerLink.text().trim()).filter(s -> !s.isEmpty());
        } else {
            playerLink = cells.get(1).selectFirst("a[href*=players/]");
        }
        if (playerLink == null) {
            return Optional.empty();
        }
        Optional<Long> id = ParseUtils.extractPlayerId(playerLink.attr("href"));
        if (id.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new PlayerDirectoryEntry(id.get(), nick));
    }
}
