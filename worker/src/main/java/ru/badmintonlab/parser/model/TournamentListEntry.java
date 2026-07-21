package ru.badmintonlab.parser.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

public record TournamentListEntry(
        long id,
        LocalDate date,
        Optional<LocalTime> time,
        Optional<BigDecimal> ratingLimit,
        String name,
        boolean doubles,
        List<PairPlayers> medalists,
        int participantsCount
) {
    public record PairPlayers(long player1Id, long player2Id) {}
}
