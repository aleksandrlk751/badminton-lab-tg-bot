package ru.badmintonlab.bot.util;

import java.util.Locale;

/**
 * Подпись результата турнира для карточки: призовое место или стадия вылета.
 */
public final class TournamentResults {

    private TournamentResults() {
    }

    public static String label(Short place, String lastMatchStage) {
        if (place != null && place >= 1 && place <= 3) {
            return place + "-е место";
        }
        if (lastMatchStage == null || lastMatchStage.isBlank()) {
            return "участие без матчей";
        }
        String stage = lastMatchStage.trim();
        if (stage.toLowerCase(Locale.ROOT).contains("групп")) {
            return "не вышел из группы";
        }
        return "вылет в " + stage;
    }
}
