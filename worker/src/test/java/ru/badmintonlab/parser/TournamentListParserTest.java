package ru.badmintonlab.parser;

import org.jsoup.nodes.Document;
import ru.badmintonlab.parser.support.HtmlFixtures;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TournamentListParserTest {

    private final TournamentListParser parser = new TournamentListParser();

    @Test
    void parsesPairTournamentFromWinnersTable() {
        Document doc = HtmlFixtures.load("tournaments-list-r77-pairs.html");

        var entry = parser.parse(doc).stream()
                .filter(e -> e.id() == 12713L)
                .findFirst()
                .orElseThrow();

        assertEquals("Женская лига WDC", entry.name());
        assertTrue(entry.doubles());
        assertEquals(new BigDecimal("550"), entry.ratingLimit().orElseThrow());
        assertEquals(3, entry.medalists().size());
        assertEquals(19080L, entry.medalists().get(0).player1Id());
        assertEquals(18870L, entry.medalists().get(0).player2Id());
    }

    @Test
    void parsesTournamentDateAndTime() {
        Document doc = HtmlFixtures.load("tournaments-list-r77-pairs.html");

        var entry = parser.parse(doc).stream()
                .filter(e -> e.id() == 12713L)
                .findFirst()
                .orElseThrow();

        assertEquals(2026, entry.date().getYear());
        assertEquals(6, entry.date().getMonthValue());
        assertEquals(14, entry.date().getDayOfMonth());
        assertEquals(12, entry.time().orElseThrow().getHour());
        assertEquals(0, entry.time().orElseThrow().getMinute());
    }
}
