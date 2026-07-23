package ru.badmintonlab.core.metrics;

import ru.badmintonlab.core.config.StabilityZoneMetrics;

/**
 * Пять зон стабильности для карточки — только цветной emoji, без текста (§2.8 {@code docs/FORMULAR.md}).
 */
public enum StabilityLevel {

    VERY_UNSTABLE("\uD83D\uDD34"),
    UNSTABLE("\uD83D\uDFE1"),
    MIDDLE("\u26AA"),
    STABLE("\uD83D\uDFE2"),
    SUPER_STABLE("\uD83D\uDD25");

    private final String emoji;

    StabilityLevel(String emoji) {
        this.emoji = emoji;
    }

    public String emoji() {
        return emoji;
    }

    public static StabilityLevel fromScore(double score, StabilityZoneMetrics zones) {
        double z2 = zones.zone2Min().doubleValue();
        double z3 = zones.zone3Min().doubleValue();
        double z4 = zones.zone4Min().doubleValue();
        double z5 = zones.zone5Min().doubleValue();
        if (score < z2) {
            return VERY_UNSTABLE;
        }
        if (score < z3) {
            return UNSTABLE;
        }
        if (score < z4) {
            return MIDDLE;
        }
        if (score < z5) {
            return STABLE;
        }
        return SUPER_STABLE;
    }
}
