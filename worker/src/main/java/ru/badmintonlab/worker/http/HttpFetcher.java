package ru.badmintonlab.worker.http;

import org.jsoup.Connection;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import ru.badmintonlab.worker.config.ParserProperties;

import java.io.IOException;

/**
 * Вежливый HTTP-фетчер поверх jsoup: единый User-Agent, глобальный rate-limit,
 * retry с экспоненциальным backoff. 404 не ретраится (страница отсутствует).
 */
@Component
public class HttpFetcher {

    private static final Logger log = LoggerFactory.getLogger(HttpFetcher.class);

    private final ParserProperties properties;
    private final RateLimiter rateLimiter;

    public HttpFetcher(ParserProperties properties) {
        this.properties = properties;
        this.rateLimiter = new RateLimiter(properties.maxRps());
    }

    public Document get(String url) {
        return execute(url, null);
    }

    public Document post(String url, java.util.Map<String, String> data) {
        return execute(url, data);
    }

    private Document execute(String url, java.util.Map<String, String> data) {
        IOException last = null;
        for (int attempt = 0; attempt <= properties.retryMax(); attempt++) {
            rateLimiter.acquire();
            try {
                Connection connection = Jsoup.connect(url)
                        .userAgent(properties.userAgent())
                        .timeout(properties.connectTimeoutMs())
                        .maxBodySize(0)
                        .ignoreContentType(false);
                // jsoup: .get() всегда выполняет GET (перекрывает .method(...)); для POST нужен .post().
                if (data != null) {
                    return connection.data(data).post();
                }
                return connection.get();
            } catch (HttpStatusException e) {
                if (e.getStatusCode() == 404) {
                    throw new FetchException("Страница не найдена (404): " + url, e);
                }
                last = e;
                log.warn("HTTP {} для {} (попытка {}/{})", e.getStatusCode(), url, attempt + 1, properties.retryMax() + 1);
            } catch (IOException e) {
                last = e;
                log.warn("Ошибка запроса {} (попытка {}/{}): {}", url, attempt + 1, properties.retryMax() + 1, e.toString());
            }
            backoff(attempt);
        }
        throw new FetchException("Не удалось получить " + url + " после " + (properties.retryMax() + 1) + " попыток", last);
    }

    private void backoff(int attempt) {
        long delay = properties.retryBackoffMs() * (1L << Math.min(attempt, 5));
        try {
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Прерван при backoff", e);
        }
    }

    public static class FetchException extends RuntimeException {
        public FetchException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
