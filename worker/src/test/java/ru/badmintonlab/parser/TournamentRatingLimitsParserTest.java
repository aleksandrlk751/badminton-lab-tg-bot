package ru.badmintonlab.parser;

import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Test;
import ru.badmintonlab.parser.support.HtmlFixtures;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TournamentRatingLimitsParserTest {

    private final TournamentRatingLimitsParser parser = new TournamentRatingLimitsParser();

    @Test
    void upcomingMixedParsesPairAndMaxPlayer() {
        Document doc = HtmlFixtures.load("tournament-upcoming-12834.html");
        var limits = parser.parse(doc, "XDE", "Space ВДНХ XDE");
        assertEquals(450, limits.pairRatingLimit().orElseThrow().intValue());
        assertEquals(500, limits.maxPlayerRatingLimit().orElseThrow().intValue());
    }

    @Test
    void womensWithoutMaxPhraseFallsBackToPairLimit() {
        Document doc = HtmlFixtures.load("tournament-completed-1517.html");
        var limits = parser.parse(doc, "Дубровка", "Дубровка");
        assertTrue(limits.pairRatingLimit().isPresent());
        assertEquals(300, limits.pairRatingLimit().orElseThrow().intValue());
        assertEquals(300, limits.maxPlayerRatingLimit().orElseThrow().intValue());
    }

    @Test
    void womensDisciplinePicksWomensBlock() {
        Document doc = HtmlFixtures.load("tournament-completed-12713.html");
        var limits = parser.parse(doc, "WDC", "Женская лига WDC");
        assertEquals(550, limits.pairRatingLimit().orElseThrow().intValue());
        assertEquals(575, limits.maxPlayerRatingLimit().orElseThrow().intValue());
    }

    @Test
    void rangeVarUsesUpperBound() {
        Document doc = HtmlFixtures.load("tournament-completed-4748.html");
        var limits = parser.parse(doc, "MBC DF", "MBC DF");
        assertEquals(400, limits.pairRatingLimit().orElseThrow().intValue());
        assertEquals(400, limits.maxPlayerRatingLimit().orElseThrow().intValue());
    }

    @Test
    void tripleBlockMixedUsesParsedMax() {
        Document doc = HtmlFixtures.load("tournament-completed-12125.html");
        var limits = parser.parse(doc, "XDE", "Newton XDE");
        assertEquals(650, limits.pairRatingLimit().orElseThrow().intValue());
        assertEquals(750, limits.maxPlayerRatingLimit().orElseThrow().intValue());
    }
}
