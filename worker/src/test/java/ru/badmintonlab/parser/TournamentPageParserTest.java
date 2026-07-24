package ru.badmintonlab.parser;

import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Test;
import ru.badmintonlab.parser.support.HtmlFixtures;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TournamentPageParserTest {

    private final TournamentPageParser parser = new TournamentPageParser();

    @Test
    void parsesUpcomingFixture() {
        Document doc = HtmlFixtures.load("tournament-upcoming-12834.html");
        var meta = parser.parse(doc);
        assertEquals(12834L, meta.id());
        assertTrue(meta.doubles());
        assertEquals("XDE", meta.categoryCode());
        assertEquals(450, meta.ratingLimit().orElseThrow().intValue());
    }
}
