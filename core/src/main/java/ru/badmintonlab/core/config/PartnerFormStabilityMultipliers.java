package ru.badmintonlab.core.config;

import ru.badmintonlab.core.metrics.StabilityLevel;

import java.math.BigDecimal;

/** Множители Form кандидата в score партнёра (§2.5) по зоне стабильности §2.8. */
public record PartnerFormStabilityMultipliers(
        BigDecimal superStable,
        BigDecimal stable,
        BigDecimal middle,
        BigDecimal unstable,
        BigDecimal veryUnstable
) {
    public PartnerFormStabilityMultipliers {
        validatePositive(superStable, "superStable");
        validatePositive(stable, "stable");
        validatePositive(middle, "middle");
        validatePositive(unstable, "unstable");
        validatePositive(veryUnstable, "veryUnstable");
    }

    public double multiplier(StabilityLevel level) {
        BigDecimal value = switch (level) {
            case SUPER_STABLE -> superStable;
            case STABLE -> stable;
            case MIDDLE -> middle;
            case UNSTABLE -> unstable;
            case VERY_UNSTABLE -> veryUnstable;
        };
        return value.doubleValue();
    }

    private static void validatePositive(BigDecimal value, String name) {
        if (value == null || value.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(name + " must be positive");
        }
    }
}
