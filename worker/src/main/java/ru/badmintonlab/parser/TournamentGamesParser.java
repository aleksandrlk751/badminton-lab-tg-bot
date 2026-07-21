package ru.badmintonlab.parser;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import ru.badmintonlab.parser.model.PairMatch;
import ru.badmintonlab.parser.support.ParseUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class TournamentGamesParser {

    private static final DateTimeFormatter PLAYED_AT =
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm", Locale.forLanguageTag("ru"));

    public List<PairMatch> parse(Document document) {
        Element table = document.selectFirst("main.games table, main table");
        if (table == null) {
            return List.of();
        }

        List<PairMatch> matches = new ArrayList<>();
        for (Element row : table.select("tr")) {
            Elements cells = row.select("td");
            if (cells.size() < 8) {
                continue;
            }
            if (cells.get(3).select("a[href*=players/]").size() < 2) {
                continue;
            }
            matches.add(parseRow(row, cells));
        }
        return matches;
    }

    private PairMatch parseRow(Element row, Elements cells) {
        LocalDateTime playedAt = LocalDateTime.parse(cells.get(0).text().trim(), PLAYED_AT);
        long tournamentId = ParseUtils.extractTournamentId(
                cells.get(1).selectFirst("a[href*=tournaments/]").attr("href")
        ).orElseThrow();
        String stage = cells.get(2).text().trim();

        List<PairMatch.MatchPlayer> sideA = parseSide(cells.get(3));
        Optional<BigDecimal> deltaA = ParseUtils.parseDecimal(cells.get(4).text());
        String scoreSets = cells.get(5).text().trim().replace(" ", "");
        Optional<BigDecimal> deltaB = ParseUtils.parseDecimal(cells.get(6).text());
        List<PairMatch.MatchPlayer> sideB = parseSide(cells.get(7));

        Optional<Integer> durationMin = Optional.empty();
        if (cells.size() > 8) {
            durationMin = parseDuration(cells.get(8).text());
        }

        String externalKey = buildExternalKey(tournamentId, playedAt, sideA, sideB, scoreSets, stage);

        return new PairMatch(
                tournamentId,
                playedAt,
                stage,
                sideA,
                sideB,
                scoreSets,
                deltaA,
                deltaB,
                durationMin,
                externalKey
        );
    }

    private List<PairMatch.MatchPlayer> parseSide(Element cell) {
        Elements links = cell.select("a[href*=players/]");
        List<PairMatch.MatchPlayer> players = new ArrayList<>();
        for (Element link : links) {
            long playerId = ParseUtils.extractPlayerId(link.attr("href")).orElseThrow();
            Optional<BigDecimal> rating = Optional.empty();
            Element parent = link.parent();
            if (parent != null) {
                rating = parent.select("dfn").stream()
                        .findFirst()
                        .flatMap(dfn -> ParseUtils.parseDecimal(dfn.text()));
            }
            players.add(new PairMatch.MatchPlayer(playerId, rating));
        }
        return players;
    }

    private Optional<Integer> parseDuration(String text) {
        Matcher matcher = java.util.regex.Pattern.compile("(\\d+)").matcher(text);
        if (matcher.find()) {
            return Optional.of(Integer.parseInt(matcher.group(1)));
        }
        return Optional.empty();
    }

    private String buildExternalKey(
            long tournamentId,
            LocalDateTime playedAt,
            List<PairMatch.MatchPlayer> sideA,
            List<PairMatch.MatchPlayer> sideB,
            String scoreSets,
            String stage
    ) {
        return "badminton4u:game:"
                + tournamentId + ":"
                + playedAt + ":"
                + sideA.stream().map(p -> Long.toString(p.playerId())).sorted().collect(Collectors.joining(","))
                + "vs"
                + sideB.stream().map(p -> Long.toString(p.playerId())).sorted().collect(Collectors.joining(","))
                + ":" + scoreSets + ":" + stage;
    }
}
