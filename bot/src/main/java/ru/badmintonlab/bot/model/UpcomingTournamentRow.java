package ru.badmintonlab.bot.model;

import java.math.BigDecimal;
import java.time.Instant;

public record UpcomingTournamentRow(
        long id,
        String name,
        Instant startsAt,
        BigDecimal ratingLimit,
        int registeredPairs
) {}
