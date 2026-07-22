package ru.badmintonlab.bot.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import ru.badmintonlab.worker.config.ParserProperties;
import ru.badmintonlab.worker.http.Badminton4uClient;
import ru.badmintonlab.worker.http.HttpFetcher;
import ru.badmintonlab.worker.snapshot.MatchUpsertService;

/**
 * HTTP-клиент и upsert матчей для lazy-fetch H2H (без worker snapshot pipeline).
 */
@Configuration
@EnableConfigurationProperties(ParserProperties.class)
@Import({HttpFetcher.class, Badminton4uClient.class, MatchUpsertService.class})
public class BotFetchConfig {
}
