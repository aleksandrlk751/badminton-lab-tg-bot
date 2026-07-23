package ru.badmintonlab.worker.http;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import ru.badmintonlab.core.domain.PlayerSex;
import ru.badmintonlab.parser.PlayerDirectoryParser;
import ru.badmintonlab.parser.model.PlayerDirectoryEntry;
import ru.badmintonlab.worker.config.ParserProperties;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Загрузка полного справочника игроков по полу: первая SSR-страница + AJAX-пагинация при скролле.
 */
@Component
public class PlayerDirectoryLoader {

    private static final Logger log = LoggerFactory.getLogger(PlayerDirectoryLoader.class);

    private final HttpFetcher fetcher;
    private final ParserProperties properties;
    private final PlayerDirectoryParser parser;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public PlayerDirectoryLoader(HttpFetcher fetcher,
                                 ParserProperties properties) {
        this.fetcher = fetcher;
        this.properties = properties;
        this.parser = new PlayerDirectoryParser();
    }

    /**
     * Все игроки региона с заданным полом — одиночный справочник (type=s по умолчанию сайта).
     */
    public List<PlayerDirectoryEntry> loadAll(String regionCode, PlayerSex sex) {
        return loadAll(regionCode, sex, PlayerDirectoryListType.SINGLES);
    }

    /**
     * Все игроки региона с заданным полом и типом списка (первая страница + AJAX до {@code showAll}).
     */
    public List<PlayerDirectoryEntry> loadAll(String regionCode, PlayerSex sex, PlayerDirectoryListType listType) {
        String listUrl = directoryUrl(regionCode, sex, listType);
        HttpFetcher.HttpSession session = fetcher.openSession();

        Document firstPage = session.get(listUrl);
        String token = parser.extractSessionToken(firstPage)
                .orElseThrow(() -> new IllegalStateException("data-rand не найден на " + listUrl));

        Map<Long, PlayerDirectoryEntry> byId = new LinkedHashMap<>();
        for (PlayerDirectoryEntry entry : parser.parse(firstPage)) {
            byId.putIfAbsent(entry.id(), entry);
        }

        int dataRows = firstPage.select("section.players-list > table tr").size() - 1;
        boolean complete = false;
        while (!complete) {
            String body = "players=" + token + "&limit=" + dataRows;
            String json = session.postForm(properties.baseUrl() + "?ajax", body, listUrl);
            PlayersDirectoryAjaxResponse response = parseAjax(json);
            if (!response.isOk()) {
                throw new IllegalStateException("AJAX справочника игроков: " + response.err());
            }
            List<PlayerDirectoryEntry> pageEntries = parser.parseFragment(response.html());
            for (PlayerDirectoryEntry entry : pageEntries) {
                byId.putIfAbsent(entry.id(), entry);
            }
            dataRows += pageEntries.size();
            complete = response.isComplete();
        }

        log.info("Справочник игроков {} {} type={}: {} записей", regionCode, sex, listType.siteType(), byId.size());
        return List.copyOf(byId.values());
    }

    private PlayersDirectoryAjaxResponse parseAjax(String json) {
        try {
            return objectMapper.readValue(json, PlayersDirectoryAjaxResponse.class);
        } catch (Exception e) {
            throw new IllegalStateException("Не удалось разобрать AJAX-ответ справочника", e);
        }
    }

    private String directoryUrl(String regionCode, PlayerSex sex, PlayerDirectoryListType listType) {
        String sexParam = sex == PlayerSex.M ? "sex_m=1" : "sex_f=1";
        String typeParam = listType == PlayerDirectoryListType.DOUBLES ? "type=" + listType.siteType() + "&" : "";
        return properties.baseUrl() + "players/?" + typeParam + sexParam + "&cities[]=" + regionCode;
    }
}
