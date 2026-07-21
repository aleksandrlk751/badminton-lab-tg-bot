package ru.badmintonlab.parser;

import org.jsoup.nodes.Document;
import ru.badmintonlab.parser.support.HtmlFixtures;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TournamentGamesParserTest {

    private final TournamentGamesParser parser = new TournamentGamesParser();

    @Test
    void parsesPairVsPairMatchesFromTournamentGamesPage() {
        Document doc = HtmlFixtures.load("games-tournament-12713.html");

        var matches = parser.parse(doc);

        assertFalse(matches.isEmpty());
        var finalMatch = matches.stream()
                .filter(m -> "фин".equals(m.stage()))
                .findFirst()
                .orElseThrow();

        assertEquals(12713L, finalMatch.tournamentId());
        assertEquals(2, finalMatch.sideA().size());
        assertEquals(2, finalMatch.sideB().size());
        assertEquals(18153L, finalMatch.sideA().get(0).playerId());
        assertEquals(16426L, finalMatch.sideA().get(1).playerId());
        assertEquals(19080L, finalMatch.sideB().get(0).playerId());
        assertEquals(18870L, finalMatch.sideB().get(1).playerId());
        assertEquals("1:2", finalMatch.scoreSets());
        assertFalse(finalMatch.externalKey().isBlank());
    }
}
