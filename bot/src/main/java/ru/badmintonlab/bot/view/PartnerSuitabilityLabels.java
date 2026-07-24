package ru.badmintonlab.bot.view;

/**
 * Формат score совместимости партнёра (0–100, §2.5 {@code FORMULAR.md}) для UI.
 */
public final class PartnerSuitabilityLabels {

    private PartnerSuitabilityLabels() {
    }

    /** Например: {@code 🎯 72%}. */
    public static String line(double score) {
        return MessageEmoji.PARTNER_SUITABILITY + " " + percent(score) + "%";
    }

    static int percent(double score) {
        return (int) Math.round(clampPercent(score));
    }

    private static double clampPercent(double score) {
        if (score < 0) {
            return 0;
        }
        if (score > 100) {
            return 100;
        }
        return score;
    }
}
