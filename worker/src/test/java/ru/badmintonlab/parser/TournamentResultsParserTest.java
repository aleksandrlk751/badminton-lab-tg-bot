package ru.badmintonlab.parser;

import org.jsoup.nodes.Document;
import ru.badmintonlab.parser.support.HtmlFixtures;

import org.junit.jupiter.api.Test;

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
}
