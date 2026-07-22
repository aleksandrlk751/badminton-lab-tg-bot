package ru.badmintonlab.worker.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Параметры вежливого парсинга badminton4u.ru: базовый URL, User-Agent, пул потоков,
 * ограничение частоты запросов и настройки retry.
 */
@ConfigurationProperties(prefix = "badminton-lab.parser")
public record ParserProperties(
        String baseUrl,
        String userAgent,
        int threads,
        double maxRps,
        int connectTimeoutMs,
        int retryMax,
        long retryBackoffMs
) {
    public ParserProperties {
        if (baseUrl == null || baseUrl.isBlank()) {
            baseUrl = "https://badminton4u.ru/";
        }
        if (!baseUrl.endsWith("/")) {
            baseUrl = baseUrl + "/";
        }
        if (userAgent == null || userAgent.isBlank()) {
            userAgent = "BadmintonLabBot/0.1 (+contact@example.com)";
        }
        if (threads <= 0) {
            threads = 8;
        }
        if (maxRps <= 0) {
            maxRps = 10.0;
        }
        if (connectTimeoutMs <= 0) {
            connectTimeoutMs = 15_000;
        }
        if (retryMax < 0) {
            retryMax = 3;
        }
        if (retryBackoffMs <= 0) {
            retryBackoffMs = 1_000;
        }
    }
}
