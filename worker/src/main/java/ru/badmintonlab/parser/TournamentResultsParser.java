package ru.badmintonlab.parser;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import ru.badmintonlab.parser.model.TournamentPairResult;
import ru.badmintonlab.parser.model.TournamentResults;
import ru.badmintonlab.parser.support.ParseUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TournamentResultsParser {

    private static final Pattern TOURNAMENT_ID = Pattern.compile("tournaments/(\\d+)");

    public TournamentResults parse(Document document) {
        long tournamentId = extractTournamentId(document);
        Element table = document.selectFirst("table.tour-doubles");
        if (table == null) {
            return new TournamentResults(tournamentId, List.of());
        }

        List<TournamentPairResult> pairs = new ArrayList<>();
        for (Element row : table.select("tbody tr")) {
            pairs.add(parseRow(row));
        }
        return new TournamentResults(tournamentId, pairs);
    }

    private long extractTournamentId(Document document) {
        Element commentButton = document.selectFirst("button[onclick^=addComment]");
        if (commentButton != null) {
            Matcher matcher = Pattern.compile("addComment\\((\\d+)").matcher(commentButton.attr("onclick"));
            if (matcher.find()) {
                return Long.parseLong(matcher.group(1));
            }
        }
        Element pagesLink = document.selectFirst("p.pages a[href*=tournaments/]");
        if (pagesLink != null) {
            return ParseUtils.extractTournamentId(pagesLink.attr("href")).orElseThrow();
        }
        throw new IllegalStateException("Tournament id not found");
    }

    private TournamentPairResult parseRow(Element row) {
        Elements cells = row.select("td");
        int place = Integer.parseInt(cells.get(0).text().trim());

        Element pairCell = cells.get(1);
        Elements playerLinks = pairCell.select("a[href*=players/]");
        if (playerLinks.size() < 2) {
            throw new IllegalStateException("Expected pair with two players in row place=" + place);
        }

        long player1Id = ParseUtils.extractPlayerId(playerLinks.get(0).attr("href")).orElseThrow();
        long player2Id = ParseUtils.extractPlayerId(playerLinks.get(1).attr("href")).orElseThrow();

        org.jsoup.select.Elements ratingDfns = pairCell.select("dfn");
        Optional<BigDecimal> player1Rating = ratingFromDfns(ratingDfns, 1);
        Optional<BigDecimal> player2Rating = ratingFromDfns(ratingDfns, 3);

        Element pairRatingCell = cells.get(2);
        Optional<BigDecimal> pairRating = pairRatingCell.select("dfn").stream()
                .skip(1)
                .findFirst()
                .flatMap(dfn -> ParseUtils.parseDecimal(dfn.text()));

        Element deltaCell = cells.get(3);
        Optional<BigDecimal> delta = Optional.ofNullable(deltaCell.attr("data-sort"))
                .filter(s -> !s.isBlank())
                .flatMap(ParseUtils::parseDecimal)
                .or(() -> ParseUtils.parseDecimal(deltaCell.text()));

        String matches = cells.get(4).text().trim();
        String sets = cells.get(5).text().trim();

        return new TournamentPairResult(
                place,
                player1Id,
                player2Id,
                player1Rating,
                player2Rating,
                pairRating,
                delta,
                matches,
                sets
        );
    }

    private Optional<BigDecimal> ratingFromDfns(org.jsoup.select.Elements dfns, int index) {
        if (dfns.size() <= index) {
            return Optional.empty();
        }
        return ParseUtils.parseDecimal(dfns.get(index).text());
    }
}
