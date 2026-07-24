package ru.badmintonlab.bot.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TournamentLinksTest {

    @Test
    void parsesFullUrl() {
        assertEquals(12834L, TournamentLinks.parseTournamentId("https://badminton4u.ru/tournaments/12834").orElseThrow());
    }

    @Test
    void parsesMobileUrl() {
        assertEquals(99L, TournamentLinks.parseTournamentId("https://m.badminton4u.ru/tournaments/99").orElseThrow());
    }

    @Test
    void parsesRelativePath() {
        assertEquals(12713L, TournamentLinks.parseTournamentId("tournaments/12713").orElseThrow());
    }

    @Test
    void rejectsGarbage() {
        assertTrue(TournamentLinks.parseTournamentId("hello").isEmpty());
    }
}
