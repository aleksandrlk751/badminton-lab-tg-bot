package ru.badmintonlab.bot.view;

import org.springframework.stereotype.Component;
import ru.badmintonlab.bot.model.PlayerCard;
import ru.badmintonlab.bot.model.RatingLine;
import ru.badmintonlab.bot.model.RivalRow;
import ru.badmintonlab.bot.model.RivalsPage;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Формирование текстов сообщений бота (parse mode = HTML).
 */
@Component
public class Texts {

    public static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    public String menu() {
        return """
                <b>Badminton LAB</b> — аналитика любительского бадминтона.

                Отправьте ник или фамилию игрока (от 3 символов) — я найду карточку.
                Либо выберите пункт меню ниже.""";
    }

    public String help() {
        return """
                <b>Справка</b>

                • Найти игрока — отправьте ник или фамилию (от 3 символов); можно неполную фамилию.
                • Карточка — рейтинги D/MD/WD/XD, последний турнир, соперники.
                • H2H (сравнение двух игроков) и график истории рейтинга — в разработке.

                Данные — по региону Москва и МО, обновляются ежедневно.""";
    }

    public String queryTooShort() {
        return "Введите минимум 3 символа — ник или фамилию игрока.";
    }

    public String notFound(String query) {
        return "По запросу «" + escape(query) + "» никого не нашёл.\n\n"
                + "Проверьте написание. Возможно, игрок отсутствует в базе региона (Москва и МО).";
    }

    public String h2hStub() {
        return "Сравнение игроков (H2H) появится на следующем этапе.";
    }

    public String historyStub() {
        return "График истории рейтинга появится на следующем этапе.";
    }

    public String searchResultsHeader(String query, int count) {
        if (count == 1) {
            return "Нашёл 1 игрока по запросу «" + escape(query) + "»:";
        }
        return "Нашёл " + count + " игроков по запросу «" + escape(query) + "». Выберите:";
    }

    public String card(PlayerCard card) {
        StringBuilder sb = new StringBuilder();
        sb.append("<b>").append(escape(card.nick())).append("</b>");
        if (card.fullName() != null && !card.fullName().isBlank()) {
            sb.append("\n").append(escape(card.fullName()));
        }
        if (card.city() != null && !card.city().isBlank()) {
            sb.append("\n📍 ").append(escape(card.city()));
        }

        sb.append("\n\n<b>Рейтинги</b>\n");
        if (card.ratings().isEmpty()) {
            sb.append("нет данных по парным разрядам");
        } else {
            sb.append(ratingsLine(card.ratings()));
        }

        if (card.lastTournament() != null) {
            sb.append("\n\n<b>Последний турнир</b>\n");
            sb.append(escape(card.lastTournament().name()));
            LocalDate date = card.lastTournament().date();
            if (date != null) {
                sb.append(" (").append(date.format(DATE)).append(")");
            }
            Short place = card.lastTournament().place();
            if (place != null) {
                sb.append("\nМесто: ").append(place);
            }
        }

        sb.append("\n\n").append(footer(card.snapshotDate()));
        return sb.toString();
    }

    private String ratingsLine(List<RatingLine> ratings) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < ratings.size(); i++) {
            RatingLine line = ratings.get(i);
            if (i > 0) {
                sb.append("   ");
            }
            sb.append(DisciplineLabels.label(line.discipline()))
                    .append(": <b>").append(formatRating(line.rating())).append("</b>");
        }
        return sb.toString();
    }

    public String rivals(RivalsPage page) {
        StringBuilder sb = new StringBuilder();
        sb.append("<b>Соперники</b> · ").append(DisciplineLabels.label(page.discipline()));
        if (page.rows().isEmpty()) {
            sb.append("\n\nВстреч в этой дисциплине нет.");
            return sb.toString();
        }
        sb.append("\n\n");
        int index = page.page() * page.pageSize();
        for (RivalRow row : page.rows()) {
            index++;
            sb.append(index).append(". <b>").append(escape(row.nick())).append("</b>");
            if (row.fullName() != null && !row.fullName().isBlank()) {
                sb.append(" — ").append(escape(row.fullName()));
            }
            sb.append("\n    W-L: ").append(row.wins()).append("-").append(row.losses())
                    .append("  (встреч: ").append(row.games()).append(")");
            if (row.city() != null && !row.city().isBlank()) {
                sb.append("  · ").append(escape(row.city()));
            }
            sb.append("\n");
        }
        sb.append("\nСтраница ").append(page.page() + 1).append("/").append(page.totalPages())
                .append(" · всего соперников: ").append(page.total());
        return sb.toString();
    }

    private String footer(LocalDate snapshotDate) {
        if (snapshotDate == null) {
            return "<i>Данные загружаются</i>";
        }
        return "<i>Данные на " + snapshotDate.format(DATE) + "</i>";
    }

    /**
     * Рейтинг без лишнего дробного нуля: 412.0 → «412», 412.5 → «412.5».
     */
    public static String formatRating(BigDecimal rating) {
        if (rating == null) {
            return "—";
        }
        BigDecimal stripped = rating.stripTrailingZeros();
        if (stripped.scale() <= 0) {
            return stripped.toBigInteger().toString();
        }
        return stripped.toPlainString();
    }

    /**
     * Экранирование спецсимволов HTML для parse mode = HTML.
     */
    public static String escape(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }
}
