package ru.badmintonlab.parser;

import org.jsoup.nodes.Document;
import ru.badmintonlab.parser.support.HtmlFixtures;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TournamentResultsParserTest {

    private final TournamentResultsParser parser = new TournamentResultsParser();

    @Test
    void parsesCompletedDoublesTournament() {
        Document doc = HtmlFixtures.load("tournament-completed-12713.html");

        var results = parser.parse(doc);

        assertEquals(12713L, results.tournamentId());
        assertTrue(results.pairs().size() >= 10);

        var gold = results.pairs().stream()
                .filter(p -> p.place() == 1)
                .findFirst()
                .orElseThrow();

        assertEquals(19080L, gold.player1Id());
        assertEquals(18870L, gold.player2Id());
        assertEquals(new BigDecimal("577"), gold.player1RatingBefore().orElseThrow());
        assertEquals(new BigDecimal("514"), gold.player2RatingBefore().orElseThrow());
        assertEquals(new BigDecimal("546"), gold.pairRatingBefore().orElseThrow());
        assertEquals(new BigDecimal("27.3"), gold.ratingDelta().orElseThrow());
        assertTrue(gold.matchesBalance().contains("5-0"));
    }

    @ParameterizedTest
    @CsvSource({
            "'-', 8, 9",
            "'4', 3, 4",
            "'1', 0, 1"
    })
    void resolvePlaceHandlesDashAsIncrement(String raw, int lastPlace, int expected) {
        assertEquals(expected, TournamentResultsParser.resolvePlace(raw, lastPlace));
    }

    @Test
    void parsesTournamentWithoutCommentButtonFromAlternateLink() {
        var results = parser.parse(HtmlFixtures.load("tournament-completed-12125.html"));

        assertEquals(12125L, results.tournamentId());
        assertEquals(12, results.pairs().size());
        assertTrue(results.pairs().stream().allMatch(p -> p.place() >= 1));
    }

    @Test
    void parsesRowWithDashPlaceAsIncrement() {
        var results = parser.parse(HtmlFixtures.load("tournament-completed-12126.html"));

        assertEquals(12126L, results.tournamentId());
        var dashedPair = results.pairs().stream()
                .filter(p -> p.player1Id() == 25452L && p.player2Id() == 29388L)
                .findFirst()
                .orElseThrow();
        assertEquals(9, dashedPair.place());
    }

    @Test
    void parsesAllPreviouslyFailedTournaments() {
        long[] ids = {4748L, 11931L, 1517L};
        for (long id : ids) {
            var results = parser.parse(HtmlFixtures.load("tournament-completed-" + id + ".html"));
            assertEquals(id, results.tournamentId());
            assertTrue(results.pairs().size() > 0, "tournament " + id);
            assertTrue(results.pairs().stream().anyMatch(p -> p.place() > 0));
        }
    }

    @Test
    void parsesNewtonDc12125ResultsAndGames() {
        var results = parser.parse(HtmlFixtures.load("tournament-completed-12125.html"));
        var matches = new TournamentGamesParser().parse(HtmlFixtures.load("games-tournament-12125.html"));

        assertEquals(12125L, results.tournamentId());
        assertEquals(12, results.pairs().size());
        assertEquals(25, matches.size());
        assertTrue(matches.stream().allMatch(m -> m.tournamentId() == 12125L));

        var gold = results.pairs().stream().filter(p -> p.place() == 1).findFirst().orElseThrow();
        assertEquals(24983L, gold.player1Id());
        assertEquals(37122L, gold.player2Id());
    }
}
