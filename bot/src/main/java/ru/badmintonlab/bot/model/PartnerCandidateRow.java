package ru.badmintonlab.bot.model;

import ru.badmintonlab.core.domain.PairCompositionType;

import java.time.Instant;

public record PartnerCandidateRow(
        long playerId,
        String fullName,
        String nick,
        String city,
        double rating,
        double pairRatingAvg,
        double score,
        boolean successfulHistory,
        boolean categoryMatch,
        boolean goodForm,
        boolean ideal,
        PairCompositionType futurePairType
) {}
