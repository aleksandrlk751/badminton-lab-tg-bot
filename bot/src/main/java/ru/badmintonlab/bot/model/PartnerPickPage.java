package ru.badmintonlab.bot.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record PartnerPickPage(
        long tournamentId,
        String tournamentName,
        Instant tournamentStartsAt,
        BigDecimal ratingLimit,
        long userId,
        String userLabel,
        double userRating,
        List<PartnerCandidateRow> successful,
        List<PartnerCandidateRow> newcomers
) {}
