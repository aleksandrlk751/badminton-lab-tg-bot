package ru.badmintonlab.core.repository.projection;

import ru.badmintonlab.core.domain.Discipline;
import ru.badmintonlab.core.domain.MatchSide;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Строка матча head-to-head двух игроков (на разных сторонах).
 */
public interface H2hMatchView {

    Long getMatchId();

    Instant getPlayedAt();

    String getScoreSets();

    String getStage();

    Discipline getDiscipline();

    String getTournamentName();

    MatchSide getPlayerSide();

    BigDecimal getRatingDelta();
}
