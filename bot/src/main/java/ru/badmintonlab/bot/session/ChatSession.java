package ru.badmintonlab.bot.session;

/**
 * Состояние чата для H2H-wizard (in-memory, сбрасывается при рестарте).
 */
public record ChatSession(
        Mode mode,
        Long playerAId,
        Integer messageId,
        boolean fromMenu
) {
    public enum Mode {
        H2H_STEP1,
        H2H_STEP2,
        H2H_CHANGE_OPPONENT,
        H2H_RESULT
    }

    public static ChatSession step1(boolean fromMenu, int messageId) {
        return new ChatSession(Mode.H2H_STEP1, null, messageId, fromMenu);
    }

    public ChatSession withPlayerA(long playerAId) {
        return new ChatSession(Mode.H2H_STEP2, playerAId, messageId, fromMenu);
    }

    public ChatSession changeOpponent() {
        return new ChatSession(Mode.H2H_CHANGE_OPPONENT, playerAId, messageId, fromMenu);
    }
}
