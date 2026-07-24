package ru.badmintonlab.parser.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

public record TournamentPageMeta(
        long id,
        String name,
        String categoryCode,
        Optional<BigDecimal> ratingLimit,
        LocalDateTime startsAt,
        boolean doubles
) {}
