package ru.badmintonlab.bot.view;

import ru.badmintonlab.bot.model.PlayerSearchResult;

import java.math.BigDecimal;

/**
 * Подпись inline-кнопки результата поиска: {@code Фамилия Имя (город) 👤 … 👥 …} с усечением до 64 символов.
 * Отчество на кнопке не показываем — экономим место; полное ФИО видно на карточке.
 */
final class SearchButtonLabel {

    static final int TELEGRAM_BUTTON_LIMIT = 64;

    private SearchButtonLabel() {
    }

    static String format(PlayerSearchResult r) {
        String full = build(r, true, true);
        if (full.length() <= TELEGRAM_BUTTON_LIMIT) {
            return full;
        }
        String step1 = build(r, false, true);
        if (step1.length() <= TELEGRAM_BUTTON_LIMIT) {
            return step1;
        }
        String step2 = build(r, false, false);
        if (step2.length() <= TELEGRAM_BUTTON_LIMIT) {
            return step2;
        }
        return step2.substring(0, TELEGRAM_BUTTON_LIMIT - 1) + "…";
    }

    private static String build(PlayerSearchResult r, boolean withCity, boolean withS) {
        StringBuilder sb = new StringBuilder();
        sb.append(displayNameWithoutPatronymic(r));
        if (withCity && r.city() != null && !r.city().isBlank()) {
            sb.append(" (").append(r.city().trim()).append(")");
        }
        appendRating(sb, withS, MessageEmoji.SINGLE, r.ratingS());
        appendRating(sb, true, MessageEmoji.DOUBLE, r.ratingD());
        return sb.toString().trim();
    }

    private static String displayNameWithoutPatronymic(PlayerSearchResult r) {
        String name = r.fullNameWithoutPatronymic();
        if (!name.isBlank()) {
            return name;
        }
        return r.displayName();
    }

    private static void appendRating(StringBuilder sb, boolean include, String label, BigDecimal rating) {
        if (!include || rating == null) {
            return;
        }
        if (!sb.isEmpty()) {
            sb.append(' ');
        }
        sb.append(label).append(' ').append(Texts.formatRating(rating));
    }
}
