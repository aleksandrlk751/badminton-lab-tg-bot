package ru.badmintonlab.bot.view;

import org.springframework.stereotype.Component;
import ru.badmintonlab.bot.model.H2hResult;
import ru.badmintonlab.bot.model.LastTournamentInfo;
import ru.badmintonlab.bot.model.PlayerCard;
import ru.badmintonlab.bot.model.RatingLine;
import ru.badmintonlab.bot.model.RivalRow;
import ru.badmintonlab.bot.model.RivalsPage;
import ru.badmintonlab.bot.util.Names;
import ru.badmintonlab.bot.util.ProfileLinks;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Формирование текстов сообщений бота (parse mode = HTML).
 */
@Component
public class Texts {

    public static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    public String menu() {
        return """
                <b>Badminton LAB</b>

                Статистика игроков Москвы и МО: рейтинги, соперники, H2H.

                Введите фамилию или ник — или выберите действие:

                <i>Данные по Москве и МО · обновление ежедневно</i>""";
    }

    public String help() {
        return """
                <b>Справка</b>

                • 🔍 Найти игрока — введите фамилию или ник (от 3 символов)
                • Карточка — рейтинги 👤/👥, последний турнир, 🤺 соперники
                • 🆚 Сравнить (H2H) — сравнение двух игроков по всем встречам
                • 📈 История рейтинга — график (скоро)

                <i>Данные: игроки Москвы и МО, глубина 3 года, обновление раз в сутки.</i>""";
    }

    public String queryTooShort() {
        return "Введите минимум 3 символа — фамилию или ник.";
    }

    public String notFound(String query) {
        return "По запросу «" + escape(query) + "» ничего не найдено.\n\n"
                + "• Проверьте написание фамилии или ника\n"
                + "• В базе только игроки Москвы и МО за последние 3 года";
    }

    public String h2hStep1() {
        return """
                🆚 <b>H2H · шаг 1/2</b>

                Введите фамилию или ник первого игрока:""";
    }

    public String h2hStep2(String playerFullName, long playerId, String nick) {
        StringBuilder sb = new StringBuilder();
        sb.append("🆚 <b>H2H · шаг 2/2</b>\n\n");
        appendPlayerHeader(sb, playerId, playerFullName, nick);
        sb.append("\nВведите фамилию или ник второго игрока:");
        return sb.toString();
    }

    public String h2hSamePlayer() {
        return "Выберите другого игрока — нельзя сравнить игрока с самим собой.";
    }

    public String h2hResult(H2hResult result) {
        StringBuilder sb = new StringBuilder();
        sb.append("🆚 <b>H2H</b>\n\n");
        appendH2hPlayerLine(sb, result.playerA());
        sb.append("  vs  ");
        appendH2hPlayerLineInline(sb, result.playerB());
        sb.append("\n\n");

        if (result.hadMeetings()) {
            int total = result.winsA() + result.winsB();
            int pct = Math.round(result.winsA() * 100f / total);
            sb.append("<b>Встречи</b>           ")
                    .append(result.winsA()).append("–").append(result.winsB())
                    .append(" (").append(pct).append("%)\n");
        } else {
            sb.append("<b>Встречи</b>           не было\n");
        }
        sb.append("<b>Форма</b>             ")
                .append(formatForm(result.formA())).append(" / ")
                .append(formatForm(result.formB())).append('\n');

        appendForecast(sb, result);
        appendRecentMatches(sb, result);
        return sb.toString().trim();
    }

    private void appendH2hPlayerLine(StringBuilder sb, H2hResult.H2hPlayerSide side) {
        appendPlayerHeader(sb, side.playerId(), side.fullName(), side.nick());
        appendRatingsInline(sb, side.ratingS(), side.ratingD());
    }

    private void appendH2hPlayerLineInline(StringBuilder sb, H2hResult.H2hPlayerSide side) {
        if (side.fullName() != null && !side.fullName().isBlank()) {
            sb.append("<b>").append(escape(side.fullName())).append("</b>");
        } else {
            sb.append("<b>").append(playerLink(side.playerId(), side.nick())).append("</b>");
        }
        appendRatingsInline(sb, side.ratingS(), side.ratingD());
    }

    private void appendRatingsInline(StringBuilder sb, BigDecimal ratingS, BigDecimal ratingD) {
        sb.append('\n');
        if (ratingS != null) {
            sb.append(MessageEmoji.SINGLE).append(' ').append(formatRating(ratingS)).append("  ");
        }
        if (ratingD != null) {
            sb.append(MessageEmoji.DOUBLE).append(' ').append(formatRating(ratingD));
        }
    }

    private void appendForecast(StringBuilder sb, H2hResult result) {
        sb.append('\n');
        if (result.hadMeetings()) {
            sb.append("<b>Прогноз</b>\n");
        } else {
            sb.append("<b>Прогноз (по рейтингу и форме)</b>\n");
        }
        double pA = result.forecast().probabilityA();
        String favoriteName;
        int pct;
        if (pA >= 0.5) {
            favoriteName = Names.shortName(result.playerA().fullName());
            if (favoriteName.isBlank()) {
                favoriteName = result.playerA().nick() != null ? result.playerA().nick()
                        : String.valueOf(result.playerA().playerId());
            }
            pct = (int) Math.round(pA * 100);
        } else {
            favoriteName = Names.shortName(result.playerB().fullName());
            if (favoriteName.isBlank()) {
                favoriteName = result.playerB().nick() != null ? result.playerB().nick()
                        : String.valueOf(result.playerB().playerId());
            }
            pct = (int) Math.round((1.0 - pA) * 100);
        }
        sb.append("Фаворит: ").append(escape(favoriteName)).append(" (≈").append(pct).append("%)");
    }

    private void appendRecentMatches(StringBuilder sb, H2hResult result) {
        if (result.recentMatches().isEmpty()) {
            return;
        }
        sb.append("\n\n<b>Последние встречи</b>\n");
        for (H2hResult.H2hMatchLine line : result.recentMatches()) {
            sb.append(line.date().format(DATE)).append(" · ")
                    .append(escape(line.tournamentName())).append(" · ")
                    .append(escape(line.scoreSets()));
            if (line.ratingDelta() != null) {
                sb.append(" · Δ ").append(formatSignedDelta(line.ratingDelta()));
            }
            sb.append('\n');
        }
    }

    private static String formatForm(double form) {
        return String.format(Locale.US, "%+.1f", form);
    }

    private static String formatSignedDelta(BigDecimal delta) {
        if (delta == null) {
            return "0";
        }
        BigDecimal stripped = delta.stripTrailingZeros();
        String num = stripped.scale() <= 0
                ? stripped.toBigInteger().toString()
                : stripped.toPlainString();
        if (delta.signum() > 0) {
            return "+" + num;
        }
        return num;
    }

    public String historyStub() {
        return """
                График истории рейтинга — скоро.
                Текущие значения — на карточке.""";
    }

    public String searchResultsHeader(int count) {
        return searchResultsHeaderLine(count) + "\n\nВыберите из предложенных вариантов";
    }

    private static String searchResultsHeaderLine(int count) {
        if (count == 1) {
            return "Нашёл 1 игрока, подходящего под критерии поиска";
        }
        if (count >= 2 && count <= 4) {
            return "Нашёл " + count + " игрока, подходящих под критерии поиска";
        }
        return "Нашёл " + count + " игроков, подходящих под критерии поиска";
    }

    public String card(PlayerCard card) {
        StringBuilder sb = new StringBuilder();
        appendPlayerHeader(sb, card.playerId(), card.fullName(), card.nick());
        if (card.city() != null && !card.city().isBlank()) {
            sb.append("\n").append(escape(card.city()));
        }

        if (!card.ratings().isEmpty()) {
            sb.append("\n\n<b>Рейтинги</b>\n");
            for (RatingLine line : card.ratings()) {
                sb.append(DisciplineLabels.ratingLabel(line.discipline())).append("  ")
                        .append(formatRating(line.rating())).append('\n');
            }
        }

        if (card.lastTournament() != null) {
            appendLastTournament(sb, card.lastTournament());
        }
        return sb.toString().trim();
    }

    private void appendLastTournament(StringBuilder sb, LastTournamentInfo t) {
        sb.append("\n\n<b>Последний турнир</b>\n");
        sb.append(escape(t.name()));
        if (t.date() != null || t.resultLabel() != null) {
            sb.append("\n");
            if (t.date() != null) {
                sb.append(t.date().format(DATE));
            }
            if (t.resultLabel() != null && !t.resultLabel().isBlank()) {
                if (t.date() != null) {
                    sb.append(" · ");
                }
                sb.append(escape(t.resultLabel()));
            }
        }
    }

    public String rivals(RivalsPage page) {
        StringBuilder sb = new StringBuilder();
        sb.append("<b>Соперники</b>");
        if (page.playerFullName() != null && !page.playerFullName().isBlank()) {
            sb.append(" · ").append(escape(page.playerFullName()));
        }
        sb.append(" · ").append(escape(page.disciplineFilterLabel()));

        if (page.rows().isEmpty()) {
            sb.append("\n\n");
            if (page.allDisciplines()) {
                sb.append("Соперников пока нет.");
            } else {
                sb.append("Встреч в разряде ").append(page.discipline().name()).append(" нет.");
            }
            return sb.toString();
        }

        sb.append("\n\n");
        appendRivalsTable(sb, page);
        sb.append("\nСтр. ").append(page.page() + 1).append("/").append(page.totalPages())
                .append(" · всего ").append(page.total());
        return sb.toString();
    }

    /**
     * Таблица соперников в {@code <pre>}: моноширинный шрифт Telegram сохраняет отступы,
     * эмодзи W/L/% выстраиваются в один столбец после самого длинного ФИО на странице.
     */
    private void appendRivalsTable(StringBuilder sb, RivalsPage page) {
        int index = page.page() * page.pageSize();
        List<String> prefixes = new ArrayList<>();
        List<String> stats = new ArrayList<>();

        for (RivalRow row : page.rows()) {
            index++;
            prefixes.add(index + ". " + rivalDisplayName(row));
            stats.add(wlWithPercent(row.wins(), row.losses()));
        }

        int maxPrefixLen = prefixes.stream().mapToInt(String::length).max().orElse(0);
        int gap = 2;

        sb.append("<pre>");
        for (int i = 0; i < prefixes.size(); i++) {
            String prefix = prefixes.get(i);
            sb.append(escape(prefix));
            sb.append(" ".repeat(Math.max(gap, maxPrefixLen - prefix.length() + gap)));
            sb.append(stats.get(i));
            if (i < prefixes.size() - 1) {
                sb.append('\n');
            }
        }
        sb.append("</pre>");
    }

    private void appendPlayerHeader(StringBuilder sb, long playerId, String fullName, String nick) {
        if (fullName != null && !fullName.isBlank()) {
            sb.append("<b>").append(escape(fullName)).append("</b> (");
            sb.append(playerLink(playerId, nick));
            sb.append(")");
        } else {
            sb.append("<b>").append(playerLink(playerId, nick)).append("</b>");
        }
    }

    private String rivalDisplayName(RivalRow row) {
        if (row.fullName() != null && !row.fullName().isBlank()) {
            return row.fullName();
        }
        if (row.nick() != null && !row.nick().isBlank()) {
            return row.nick();
        }
        return String.valueOf(row.opponentId());
    }

    private String playerLink(long playerId, String nick) {
        String label = (nick != null && !nick.isBlank()) ? escape(nick) : String.valueOf(playerId);
        return "<a href=\"" + ProfileLinks.url(playerId) + "\">" + label + "</a>";
    }

    static String wlWithPercent(int wins, int losses) {
        int total = wins + losses;
        if (total == 0) {
            return MessageEmoji.WIN + "0  " + MessageEmoji.LOSS + "0  " + MessageEmoji.WIN_RATE + "0%";
        }
        int pct = Math.round(wins * 100f / total);
        return MessageEmoji.WIN + wins + "  " + MessageEmoji.LOSS + losses + "  "
                + MessageEmoji.WIN_RATE + pct + "%";
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
