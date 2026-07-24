package ru.badmintonlab.bot.util;

/**
 * Единое правило подписи игрока в текстах бота (см. {@code docs/messages/00-principles.md}).
 * <p>
 * HTML-сообщения: {@code Texts#appendPlayerHeader}. Список соперников в {@code <pre>}:
 * {@link #rivalsRowLabel} (без ника в скобках — ссылки там не кликабельны).
 */
public final class PlayerDisplayFormat {

    private PlayerDisplayFormat() {
    }

    /** Текст ссылки на профиль: ник или числовой id. */
    public static String profileLinkLabel(String nick, long playerId) {
        return (nick != null && !nick.isBlank()) ? nick.trim() : String.valueOf(playerId);
    }

    /**
     * Подпись строки в списке соперников ({@code <pre>}): только ФИО; без ника в скобках.
     * Нет ФИО — ник или id plain text.
     */
    public static String rivalsRowLabel(String fullName, String nick, long playerId) {
        if (fullName != null && !fullName.isBlank()) {
            return fullName.trim();
        }
        return profileLinkLabel(nick, playerId);
    }
}
