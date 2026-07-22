package ru.badmintonlab.bot.model;

import ru.badmintonlab.core.metrics.ForecastResult;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Результат экрана H2H (игрок A vs B, все разряды).
 */
public record H2hResult(
        H2hPlayerSide playerA,
        H2hPlayerSide playerB,
        int winsA,
        int winsB,
        double formA,
        double formB,
        ForecastResult forecast,
        List<H2hMatchLine> recentMatches
) {
    public boolean hadMeetings() {
        return winsA + winsB > 0;
    }

    public record H2hPlayerSide(
            long playerId,
            String fullName,
            String nick,
            BigDecimal ratingS,
            BigDecimal ratingD
    ) {}

    public record H2hMatchLine(
            LocalDate date,
            String tournamentName,
            String scoreSets,
            BigDecimal ratingDelta,
            boolean playerAWon
    ) {}
}
