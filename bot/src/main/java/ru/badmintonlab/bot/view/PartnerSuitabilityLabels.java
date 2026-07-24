package ru.badmintonlab.bot.view;

/**
 * Текстовый уровень score совместимости партнёра (0–100, §2.5 {@code FORMULAR.md}).
 */
public final class PartnerSuitabilityLabels {

    private PartnerSuitabilityLabels() {
    }

    /** Например: {@code подходимость: хорошая (72%)}. */
    public static String line(double score) {
        int pct = (int) Math.round(clampPercent(score));
        return "подходимость: " + tier(pct) + " (" + pct + "%)";
    }

    static String tier(int percent) {
        if (percent >= 75) {
            return "высокая";
        }
        if (percent >= 50) {
            return "хорошая";
        }
        if (percent >= 30) {
            return "средняя";
        }
        return "низкая";
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
