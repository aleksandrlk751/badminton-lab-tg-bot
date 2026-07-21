package ru.badmintonlab.parser;

import org.jsoup.nodes.Document;
import ru.badmintonlab.parser.support.HtmlFixtures;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TournamentRegistrationParserTest {

    private final TournamentRegistrationParser parser = new TournamentRegistrationParser();

    @Test
    void parsesRegisteredPairsOnUpcomingTournament() {
        Document doc = HtmlFixtures.load("tournament-upcoming-12834.html");

        var registration = parser.parse(doc);

        assertEquals(12834L, registration.tournamentId());
        assertEquals(14, registration.pairs().size());
        assertTrue(registration.pairs().stream().anyMatch(pair ->
                pair.player1Id() == 19045L && pair.player2Id() == 19048L));
        assertTrue(doc.getElementById("tour-reg-list2").children().isEmpty());
    }
}
