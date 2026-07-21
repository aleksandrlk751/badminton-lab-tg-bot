package ru.badmintonlab.parser;

import org.jsoup.nodes.Document;
import ru.badmintonlab.parser.model.PairMatch;
import ru.badmintonlab.parser.model.RivalSummaryEntry;
import ru.badmintonlab.parser.support.HtmlFixtures;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RivalSummaryAggregatorTest {

    private final TournamentGamesParser gamesParser = new TournamentGamesParser();
    private final RivalSummaryAggregator aggregator = new RivalSummaryAggregator();

    @Test
    void aggregatesPlayerVsPlayerFromFinalMatch() {
        Document doc = HtmlFixtures.load("games-tournament-12713.html");
        var finalMatch = gamesParser.parse(doc).stream()
                .filter(m -> "фин".equals(m.stage()))
                .findFirst()
                .orElseThrow();

        var summary = aggregator.aggregate(List.of(finalMatch));

        assertRival(summary, 19080L, 18153L, 1, 0);
        assertRival(summary, 19080L, 16426L, 1, 0);
        assertRival(summary, 18153L, 19080L, 0, 1);
        assertRival(summary, 18870L, 16426L, 1, 0);
    }

    @Test
    void aggregatesFullTournamentFixture() {
        Document doc = HtmlFixtures.load("games-tournament-12713.html");
        var matches = gamesParser.parse(doc);

        var summary = aggregator.aggregate(matches);

        assertFalse(summary.isEmpty());
        assertTrue(summary.stream().allMatch(e -> e.games() == e.wins() + e.losses()));

        long matchesFor19080 = matches.stream()
                .filter(m -> sideContains(m, 19080L))
                .count();
        int meetingsFor19080 = summary.stream()
                .filter(e -> e.playerId() == 19080L)
                .mapToInt(RivalSummaryEntry::games)
                .sum();
        assertEquals((int) matchesFor19080 * 2, meetingsFor19080);
    }

    private boolean sideContains(PairMatch match, long playerId) {
        return match.sideA().stream().anyMatch(p -> p.playerId() == playerId)
                || match.sideB().stream().anyMatch(p -> p.playerId() == playerId);
    }

    private void assertRival(List<RivalSummaryEntry> summary, long playerId, long opponentId, int wins, int losses) {
        var entry = summary.stream()
                .filter(e -> e.playerId() == playerId && e.opponentId() == opponentId)
                .findFirst()
                .orElseThrow(() -> new AssertionError("Missing rival " + playerId + " vs " + opponentId));
        assertEquals(wins, entry.wins(), "wins for " + playerId + " vs " + opponentId);
        assertEquals(losses, entry.losses(), "losses for " + playerId + " vs " + opponentId);
    }
}
