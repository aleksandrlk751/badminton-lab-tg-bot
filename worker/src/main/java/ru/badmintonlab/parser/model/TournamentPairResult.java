package ru.badmintonlab.parser.model;

import java.math.BigDecimal;
import java.util.Optional;

public record TournamentPairResult(
        int place,
        long player1Id,
        long player2Id,
        Optional<BigDecimal> player1RatingBefore,
        Optional<BigDecimal> player2RatingBefore,
        Optional<BigDecimal> pairRatingBefore,
        Optional<BigDecimal> ratingDelta,
        String matchesBalance,
        String setsBalance
) {
}
