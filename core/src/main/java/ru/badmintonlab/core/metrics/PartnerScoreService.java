package ru.badmintonlab.core.metrics;

import org.springframework.stereotype.Service;
import ru.badmintonlab.core.config.MetricsProperties;

import java.math.BigDecimal;

/**
 * Score совместимости партнёра (0–100), §2.5 {@code docs/FORMULAR.md}.
 */
@Service
public class PartnerScoreService {

    private final MetricsProperties metrics;

    public PartnerScoreService(MetricsProperties metrics) {
        this.metrics = metrics;
    }

    public record Input(
            double ratingUser,
            double ratingCandidate,
            Double ratingLimit,
            Double maxPlayerRatingLimit,
            double jointDeltaSum,
            double partnerPlayability,
            boolean successfulHistoryBlock
    ) {}

    public record Result(
            double score,
            double cLimit,
            double cDelta,
            double cPlayability
    ) {}

    public Result score(Input input) {
        double cLimit = cLimit(
                input.ratingUser(),
                input.ratingCandidate(),
                input.ratingLimit(),
                input.maxPlayerRatingLimit());
        double cDelta = input.jointDeltaSum() > 0
                ? MetricMath.sigmoid(input.jointDeltaSum() / metrics.dScale().doubleValue())
                : 0.0;
        double sRef = sRefPartner();
        double cS = input.partnerPlayability() > 0
                ? input.partnerPlayability() / (input.partnerPlayability() + sRef)
                : 0.0;

        double w1 = metrics.w1().doubleValue();
        double w2 = metrics.w2().doubleValue();
        double w3 = metrics.w3().doubleValue();
        double base = 100.0 * (w1 * cLimit + w2 * cDelta + w3 * cS);
        double boost = input.successfulHistoryBlock() ? partnerBoost() : 1.0;
        return new Result(base * boost, cLimit, cDelta, cS);
    }

    private static double cLimit(double ratingUser,
                                 double ratingCandidate,
                                 Double pairLimit,
                                 Double maxPlayerLimit) {
        double cPair = cLimitPairAverage(ratingUser, ratingCandidate, pairLimit);
        double cPlayer = cLimitPerPlayer(ratingUser, ratingCandidate, maxPlayerLimit);
        return Math.min(cPair, cPlayer);
    }

    private static double cLimitPairAverage(double ratingUser, double ratingCandidate, Double limit) {
        if (limit == null || limit <= 0) {
            return 1.0;
        }
        double pairAvg = (ratingUser + ratingCandidate) / 2.0;
        if (pairAvg > limit) {
            return 0.0;
        }
        return Math.min(1.0, pairAvg / limit);
    }

    private static double cLimitPerPlayer(double ratingUser, double ratingCandidate, Double maxPlayer) {
        if (maxPlayer == null || maxPlayer <= 0) {
            return 1.0;
        }
        if (ratingUser > maxPlayer || ratingCandidate > maxPlayer) {
            return 0.0;
        }
        double tightest = Math.min(ratingUser, ratingCandidate);
        return Math.min(1.0, tightest / maxPlayer);
    }

    private double sRefPartner() {
        BigDecimal ref = metrics.sRefPartner();
        return ref != null ? ref.doubleValue() : metrics.sRef().doubleValue();
    }

    private double partnerBoost() {
        BigDecimal boost = metrics.partnerBoost();
        return boost != null ? boost.doubleValue() : 1.2;
    }
}
