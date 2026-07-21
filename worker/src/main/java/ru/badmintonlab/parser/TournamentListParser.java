package ru.badmintonlab.parser;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import ru.badmintonlab.parser.model.TournamentListEntry;
import ru.badmintonlab.parser.support.ParseUtils;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TournamentListParser {

    private static final DateTimeFormatter DATE_HEADER =
            DateTimeFormatter.ofPattern("dd.MM.yyyy", Locale.forLanguageTag("ru"));
    private static final Pattern TIME = Pattern.compile("(\\d{1,2}:\\d{2})");

    public List<TournamentListEntry> parse(Document document) {
        Element table = document.selectFirst("table.winners");
        if (table == null) {
            return List.of();
        }

        List<TournamentListEntry> entries = new ArrayList<>();
        LocalDate currentDate = null;

        for (Element row : table.select("tr")) {
            if (row.hasClass("date")) {
                currentDate = parseDateHeader(row);
                continue;
            }
            if (currentDate == null) {
                continue;
            }

            Element tournamentLink = row.selectFirst("td a[href*=tournaments/]");
            if (tournamentLink == null) {
                continue;
            }

            long id = ParseUtils.extractTournamentId(tournamentLink.attr("href")).orElseThrow();
            Optional<LocalTime> time = parseTime(row.selectFirst("td") != null ? row.selectFirst("td").text() : null);
            Optional<java.math.BigDecimal> limit = row.select("td var").stream()
                    .findFirst()
                    .flatMap(var -> ParseUtils.parseDecimal(var.text()));
            boolean doubles = tournamentLink.hasClass("doubles");

            List<TournamentListEntry.PairPlayers> medalists = parseMedalists(row);
            int participants = parseParticipants(row);

            entries.add(new TournamentListEntry(
                    id,
                    currentDate,
                    time,
                    limit,
                    tournamentLink.text().trim(),
                    doubles,
                    medalists,
                    participants
            ));
        }
        return entries;
    }

    private LocalDate parseDateHeader(Element row) {
        Element header = row.selectFirst("th[colspan=3]");
        if (header == null) {
            throw new IllegalStateException("Date header not found in winners table");
        }
        String text = header.text().trim();
        int slash = text.indexOf('/');
        String datePart = slash >= 0 ? text.substring(0, slash).trim() : text;
        return LocalDate.parse(datePart, DATE_HEADER);
    }

    private Optional<LocalTime> parseTime(String cellText) {
        if (cellText == null) {
            return Optional.empty();
        }
        Matcher matcher = TIME.matcher(cellText.trim());
        if (matcher.find()) {
            return Optional.of(LocalTime.parse(matcher.group(1)));
        }
        return Optional.empty();
    }

    private List<TournamentListEntry.PairPlayers> parseMedalists(Element row) {
        Elements medalCells = row.select("td");
        if (medalCells.size() < 6) {
            return List.of();
        }
        List<TournamentListEntry.PairPlayers> medalists = new ArrayList<>();
        for (int i = 3; i <= 5; i++) {
            parsePairCell(medalCells.get(i)).ifPresent(medalists::add);
        }
        return medalists;
    }

    private Optional<TournamentListEntry.PairPlayers> parsePairCell(Element cell) {
        Elements links = cell.select("a[href*=players/]");
        if (links.size() < 2) {
            return Optional.empty();
        }
        Optional<Long> p1 = ParseUtils.extractPlayerId(links.get(0).attr("href"));
        Optional<Long> p2 = ParseUtils.extractPlayerId(links.get(1).attr("href"));
        if (p1.isEmpty() || p2.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new TournamentListEntry.PairPlayers(p1.get(), p2.get()));
    }

    private int parseParticipants(Element row) {
        Element kbd = row.selectFirst("td kbd");
        if (kbd == null) {
            return 0;
        }
        try {
            return Integer.parseInt(kbd.text().trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
