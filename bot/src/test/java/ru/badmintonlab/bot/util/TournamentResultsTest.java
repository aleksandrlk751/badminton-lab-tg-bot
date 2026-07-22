package ru.badmintonlab.bot.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TournamentResultsTest {

    @Test
    void prizePlace() {
        assertEquals("2-е место", TournamentResults.label((short) 2, "1/4"));
    }

    @Test
    void eliminationStage() {
        assertEquals("вылет в 1/4", TournamentResults.label((short) 5, "1/4"));
    }

    @Test
    void groupExit() {
        assertEquals("не вышел из группы", TournamentResults.label(null, "группа A"));
    }

    @Test
    void noMatches() {
        assertEquals("участие без матчей", TournamentResults.label(null, null));
    }
}
