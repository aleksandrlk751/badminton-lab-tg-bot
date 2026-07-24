package ru.badmintonlab.bot.session;

/**
 * Состояние чата для H2H-wizard и подбора партнёра (in-memory, сбрасывается при рестарте).
 */
public record ChatSession(
        Mode mode,
        Long playerAId,
        Integer messageId,
        boolean fromMenu,
        Long tournamentId
) {
    public enum Mode {
        H2H_STEP1,
        H2H_STEP2,
        H2H_CHANGE_OPPONENT,
        H2H_RESULT,
        PARTNER_PICK_USER,
        PARTNER_PICK_LINK
    }

    public static ChatSession step1(boolean fromMenu, int messageId) {
        return new ChatSession(Mode.H2H_STEP1, null, messageId, fromMenu, null);
    }

    public ChatSession withPlayerA(long playerAId) {
        return new ChatSession(Mode.H2H_STEP2, playerAId, messageId, fromMenu, tournamentId);
    }

    public ChatSession changeOpponent() {
        return new ChatSession(Mode.H2H_CHANGE_OPPONENT, playerAId, messageId, fromMenu, tournamentId);
    }

    public static ChatSession partnerPickUser(long tournamentId, int messageId) {
        return new ChatSession(Mode.PARTNER_PICK_USER, null, messageId, true, tournamentId);
    }

    public static ChatSession partnerPickLink(int messageId) {
        return new ChatSession(Mode.PARTNER_PICK_LINK, null, messageId, true, null);
    }
}
