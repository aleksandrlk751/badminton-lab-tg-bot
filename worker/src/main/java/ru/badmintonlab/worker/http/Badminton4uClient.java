package ru.badmintonlab.worker.http;

import org.jsoup.nodes.Document;
import org.springframework.stereotype.Component;
import ru.badmintonlab.core.domain.Discipline;
import ru.badmintonlab.worker.config.ParserProperties;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Высокоуровневый клиент badminton4u.ru: строит URL страниц и возвращает разобранный HTML.
 * Логику извлечения данных выполняют парсеры (модуль parser).
 */
@Component
public class Badminton4uClient {

    private static final DateTimeFormatter FILTER_DATE =
            DateTimeFormatter.ofPattern("dd.MM.yyyy", Locale.forLanguageTag("ru"));

    private final HttpFetcher fetcher;
    private final String baseUrl;

    public Badminton4uClient(HttpFetcher fetcher, ParserProperties properties) {
        this.fetcher = fetcher;
        this.baseUrl = properties.baseUrl();
    }

    /**
     * Список завершённых турниров («призёры») по региону, дисциплине и окну дат.
     */
    public Document completedTournaments(String regionCode, Discipline discipline, LocalDate from, LocalDate to) {
        String url = baseUrl + "tournaments/?winners=1"
                + "&cities[]=" + regionCode
                + "&types[]=" + typeParam(discipline)
                + "&date_from=" + from.format(FILTER_DATE)
                + "&date_to=" + to.format(FILTER_DATE);
        return fetcher.get(url);
    }

    public Document tournamentPage(long tournamentId) {
        return fetcher.get(baseUrl + "tournaments/" + tournamentId);
    }

    public Document tournamentGames(long tournamentId) {
        return fetcher.get(baseUrl + "gamesd/?tourID=" + tournamentId);
    }

    public Document playerProfile(long playerId) {
        return fetcher.get(baseUrl + "players/" + playerId);
    }

    /** Первая страница справочника игроков по полу и региону. */
    public Document playerDirectoryPage(String regionCode, boolean male) {
        String sexParam = male ? "sex_m=1" : "sex_f=1";
        return fetcher.get(baseUrl + "players/?" + sexParam + "&cities[]=" + regionCode);
    }

    /** История игр двух игроков (SSR-таблица, если есть; иначе пустая). */
    public Document playerGames(long user1Id, long user2Id) {
        return fetcher.get(baseUrl + "games/?user1ID=" + user1Id + "&user2ID=" + user2Id);
    }

    private String typeParam(Discipline discipline) {
        return discipline.name().toLowerCase(Locale.ROOT);
    }
}
