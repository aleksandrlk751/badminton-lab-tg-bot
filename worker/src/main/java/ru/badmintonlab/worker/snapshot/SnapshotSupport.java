package ru.badmintonlab.worker.snapshot;

import ru.badmintonlab.core.domain.Discipline;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Вспомогательные преобразования для слепка: конвертация дисциплин parser→core,
 * парсинг баланса «W-L» и работа с часовым поясом источника (МСК).
 */
public final class SnapshotSupport {

    public static final ZoneId SOURCE_ZONE = ZoneId.of("Europe/Moscow");

    private static final Pattern BALANCE = Pattern.compile("\\((\\d+)\\s*-\\s*(\\d+)\\)");
    private static final Pattern PLAYER_TYPE = Pattern.compile("type=([a-z]+)", Pattern.CASE_INSENSITIVE);

    private SnapshotSupport() {
    }

    /**
     * Дисциплина из {@code ?type=} первой ссылки на игрока в итоговой таблице пар.
     */
    public static Discipline inferDisciplineFromPage(Document document) {
        Element link = document.selectFirst("table.tour-doubles a[href*=type=]");
        if (link == null) {
            throw new IllegalStateException("Discipline not found: no player links with type= on tournament page");
        }
        Matcher matcher = PLAYER_TYPE.matcher(link.attr("href"));
        if (!matcher.find()) {
            throw new IllegalStateException("Discipline not found in href: " + link.attr("href"));
        }
        return Discipline.valueOf(matcher.group(1).toUpperCase(Locale.ROOT));
    }

    public static Discipline toCore(ru.badmintonlab.parser.model.Discipline discipline) {
        return Discipline.valueOf(discipline.name());
    }

    public static java.time.Instant toInstant(LocalDateTime localDateTime) {
        return localDateTime.atZone(SOURCE_ZONE).toInstant();
    }

    /**
     * Извлекает выигранные/проигранные из строки вида «5 (5-0)» → [5, 0].
     */
    public static Optional<int[]> parseBalance(String text) {
        if (text == null) {
            return Optional.empty();
        }
        Matcher matcher = BALANCE.matcher(text);
        if (matcher.find()) {
            return Optional.of(new int[]{Integer.parseInt(matcher.group(1)), Integer.parseInt(matcher.group(2))});
        }
        return Optional.empty();
    }
}
