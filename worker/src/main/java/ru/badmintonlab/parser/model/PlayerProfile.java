package ru.badmintonlab.parser.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public record PlayerProfile(
        long id,
        String nick,
        String fullName,
        Optional<String> city,
        Optional<String> playingHand,
        Map<Discipline, BigDecimal> ratings,
        Map<Discipline, List<RatingPoint>> ratingHistories
) {
    public record RatingPoint(LocalDate date, BigDecimal rating) {}
}
