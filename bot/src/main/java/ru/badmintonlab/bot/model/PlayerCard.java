package ru.badmintonlab.bot.model;

import ru.badmintonlab.core.metrics.GameAccentResult;

import java.time.LocalDate;
import java.util.List;

/**
 * Данные карточки игрока (первый экран): ник, ФИО, город, сводка парных рейтингов,
 * последний турнир и дата актуальности слепка.
 */
public record PlayerCard(
        long playerId,
        String nick,
        String fullName,
        String city,
        List<RatingLine> ratings,
        Double form,
        GameAccentResult gameAccent,
        LastTournamentInfo lastTournament,
        LocalDate snapshotDate
) {
}
