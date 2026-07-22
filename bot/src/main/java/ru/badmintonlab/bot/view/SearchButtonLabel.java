package ru.badmintonlab.bot.view;

import ru.badmintonlab.bot.model.PlayerSearchResult;
import ru.badmintonlab.bot.model.PlayerSearchResult;
import ru.badmintonlab.core.domain.Discipline;

import java.math.BigDecimal;

/**
 * Подпись inline-кнопки результата поиска: {@code ФИО (город) 🆂 … 🅳 …} с усечением до 64 символов.
 */
final class SearchButtonLabel {

    static final int TELEGRAM_BUTTON_LIMIT = 64;
    private static final String EMOJI_S = "🆂";
    private static final String EMOJI_D = "🅳";

    private SearchButtonLabel() {
    }

    static String format(PlayerSearchResult r) {
        String full = build(r, true, true, true);
        if (full.length() <= TELEGRAM_BUTTON_LIMIT) {
            return full;
        }
        String step1 = build(r, false, true, true);
        if (step1.length() <= TELEGRAM_BUTTON_LIMIT) {
            return step1;
        }
        String step2 = build(r, false, false, true);
        if (step2.length() <= TELEGRAM_BUTTON_LIMIT) {
            return step2;
        }
        String step3 = build(r, false, false, false);
        if (step3.length() <= TELEGRAM_BUTTON_LIMIT) {
            return step3;
        }
        return step3.substring(0, TELEGRAM_BUTTON_LIMIT - 1) + "…";
    }

    private static String build(PlayerSearchResult r, boolean withPatronymic, boolean withCity, boolean withS) {
        StringBuilder sb = new StringBuilder();
        sb.append(withPatronymic ? r.displayName() : nameWithoutPatronymic(r));
        if (withCity && r.city() != null && !r.city().isBlank()) {
            sb.append(" (").append(r.city().trim()).append(")");
        }
        appendRating(sb, withS, EMOJI_S, r.ratingS());
        appendRating(sb, true, EMOJI_D, r.ratingD());
        return sb.toString().trim();
    }

    private static String nameWithoutPatronymic(PlayerSearchResult r) {
        String name = r.fullNameWithoutPatronymic();
        if (!name.isBlank()) {
            return name;
        }
        return r.displayName();
    }

    private static void appendRating(StringBuilder sb, boolean include, String emoji, BigDecimal rating) {
        if (!include || rating == null) {
            return;
        }
        if (!sb.isEmpty()) {
            sb.append(' ');
        }
        sb.append(emoji).append(' ').append(Texts.formatRating(rating));
    }
}
