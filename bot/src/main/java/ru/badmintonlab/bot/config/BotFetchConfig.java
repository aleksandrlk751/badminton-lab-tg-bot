package ru.badmintonlab.bot.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import ru.badmintonlab.worker.config.ParserProperties;
import ru.badmintonlab.worker.config.SnapshotProperties;
import ru.badmintonlab.worker.http.Badminton4uClient;
import ru.badmintonlab.worker.http.HttpFetcher;
import ru.badmintonlab.worker.snapshot.MatchUpsertService;
import ru.badmintonlab.worker.snapshot.PairInserter;
import ru.badmintonlab.worker.snapshot.PairService;
import ru.badmintonlab.worker.snapshot.PlayerUpsertService;
import ru.badmintonlab.worker.snapshot.UpcomingTournamentsSyncService;

/**
 * HTTP-клиент, upsert матчей для lazy-fetch H2H и синхронизация турнира по ссылке (подбор партнёра).
 */
@Configuration
@EnableConfigurationProperties({ParserProperties.class, SnapshotProperties.class})
@Import({
        HttpFetcher.class,
        Badminton4uClient.class,
        MatchUpsertService.class,
        PairInserter.class,
        PairService.class,
        PlayerUpsertService.class,
        UpcomingTournamentsSyncService.class
})
public class BotFetchConfig {
}
