package ru.badmintonlab.bot.view;

import ru.badmintonlab.core.domain.Discipline;

/**
 * Кодирование/декодирование callback_data inline-кнопок (Telegram лимит — 64 байта).
 * Формат: {@code action[:arg1[:arg2 ...]]}.
 */
public final class CallbackData {

    public static final String SEP = ":";

    // Меню
    public static final String MENU_SEARCH = "menu:search";
    public static final String MENU_H2H = "menu:h2h";
    public static final String MENU_HELP = "menu:help";

    // Действия с игроком
    public static final String CARD = "card";      // card:{playerId}
    public static final String RIVALS = "rv";       // rv:{playerId} — дисциплина по умолчанию, стр. 0
    public static final String RIVALS_PAGE = "rvp"; // rvp:{playerId}:{discipline}:{page}
    public static final String H2H = "h2h";         // h2h:{playerId} — с карточки, шаг 2
    public static final String H2H_SELECT_A = "h2s1"; // h2s1:{playerId}
    public static final String H2H_SELECT_B = "h2s2"; // h2s2:{playerA}:{playerB}
    public static final String H2H_CHANGE = "h2c";    // h2c:{playerA}
    public static final String MENU_MAIN = "menu:main";
    public static final String HISTORY = "hist";    // hist:{playerId} — заглушка (этап 7)

    public static final String NOOP = "noop";

    private CallbackData() {
    }

    public static String card(long playerId) {
        return CARD + SEP + playerId;
    }

    public static String rivalsDefault(long playerId) {
        return RIVALS + SEP + playerId;
    }

    public static String rivalsPage(long playerId, Discipline discipline, int page) {
        String disc = discipline == null ? "ALL" : discipline.name();
        return RIVALS_PAGE + SEP + playerId + SEP + disc + SEP + page;
    }

    public static Discipline parseRivalsDiscipline(String token) {
        if (token == null || "ALL".equals(token)) {
            return null;
        }
        return Discipline.valueOf(token);
    }

    public static String h2h(long playerId) {
        return H2H + SEP + playerId;
    }

    public static String h2hSelectA(long playerId) {
        return H2H_SELECT_A + SEP + playerId;
    }

    public static String h2hSelectB(long playerAId, long playerBId) {
        return H2H_SELECT_B + SEP + playerAId + SEP + playerBId;
    }

    public static String h2hChangeOpponent(long playerAId) {
        return H2H_CHANGE + SEP + playerAId;
    }

    public static String history(long playerId) {
        return HISTORY + SEP + playerId;
    }

    public static String[] parse(String data) {
        return data == null ? new String[0] : data.split(SEP);
    }
}
