package ru.badmintonlab.bot.service;

import org.junit.jupiter.api.Test;
import ru.badmintonlab.core.domain.Discipline;
import ru.badmintonlab.core.domain.MatchSide;
import ru.badmintonlab.core.repository.projection.H2hMatchView;

import java.math.BigDecimal;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class H2hServiceTest {

    @Test
    void playerAWonOnSideA() {
        assertTrue(H2hService.playerAWon(match(MatchSide.A, "2:0")));
    }

    @Test
    void playerAWonOnSideB() {
        assertTrue(H2hService.playerAWon(match(MatchSide.B, "0:2")));
    }

    @Test
    void playerALostOnSideA() {
        assertFalse(H2hService.playerAWon(match(MatchSide.A, "0:2")));
    }

    private static H2hMatchView match(MatchSide side, String score) {
        return new H2hMatchView() {
            @Override
            public Long getMatchId() {
                return 1L;
            }

            @Override
            public Instant getPlayedAt() {
                return Instant.parse("2026-06-14T09:00:00Z");
            }

            @Override
            public String getScoreSets() {
                return score;
            }

            @Override
            public String getStage() {
                return "фин";
            }

            @Override
            public Discipline getDiscipline() {
                return Discipline.WD;
            }

            @Override
            public String getTournamentName() {
                return "Test";
            }

            @Override
            public MatchSide getPlayerSide() {
                return side;
            }

            @Override
            public BigDecimal getRatingDelta() {
                return BigDecimal.valueOf(4.5);
            }
        };
    }
}
