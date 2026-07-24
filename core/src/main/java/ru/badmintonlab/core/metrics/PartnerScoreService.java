package ru.badmintonlab.core.metrics;

import org.springframework.stereotype.Service;
import ru.badmintonlab.core.config.MetricsProperties;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.OptionalDouble;

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
            OptionalDouble candidateForm,
            double tournamentCategoryDelta,
            /** Зона стабильности кандидата (§2.8); пусто — множитель 1.0 при Form &gt; 0. */
            Optional<StabilityLevel> candidateStability
    ) {}

    public record Result(
            double score,
            double cLimit,
            double cDelta,
            double cPlayability,
            double cForm,
            double cAccent
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
        double cForm = cForm(input.candidateForm(), input.candidateStability());
        double cAccent = input.tournamentCategoryDelta() > 0
                ? MetricMath.sigmoid(input.tournamentCategoryDelta() / metrics.dScale().doubleValue())
                : 0.0;

        double w1 = metrics.w1().doubleValue();
        double w2 = metrics.w2().doubleValue();
        double w3 = metrics.w3().doubleValue();
        double w4 = metrics.w4().doubleValue();
        double w5 = metrics.w5().doubleValue();
        double scoreBase = 100.0 * (w1 * cLimit + w2 * cDelta + w3 * cS + w4 * cForm + w5 * cAccent);
        double score = clampScore(scoreBase);
        return new Result(score, cLimit, cDelta, cS, cForm, cAccent);
    }

    private double cForm(OptionalDouble candidateForm, Optional<StabilityLevel> stability) {
        if (candidateForm.isEmpty()) {
            return 0.0;
        }
        double form = candidateForm.getAsDouble();
        if (form == 0.0) {
            return 0.0;
        }
        double scale = metrics.partnerFormScale().doubleValue();
        if (form > 0) {
            double effectiveForm = form;
            if (stability.isPresent()) {
                effectiveForm = form * metrics.partnerFormStability().multiplier(stability.get());
            }
            double sig = MetricMath.sigmoid(effectiveForm / scale);
            return Math.min(1.0, Math.max(0.0, (sig - 0.5) / 0.5));
        }
        return -Math.min(1.0, Math.abs(form) / scale);
    }

    private static double clampScore(double score) {
        if (score < 0) {
            return 0.0;
        }
        if (score > 100) {
            return 100.0;
        }
        return score;
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
}
