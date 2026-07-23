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
import java.util.Map;

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

    /** Сессия с общим cookie-jar для цепочки GET → POST (справочник игроков). */
    public HttpSession openSession() {
        Connection session = Jsoup.newSession()
                .userAgent(properties.userAgent())
                .timeout(properties.connectTimeoutMs())
                .maxBodySize(0);
        return new HttpSession(session, properties, rateLimiter);
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

    /**
     * HTTP-сессия jsoup с cookie-jar; rate-limit и retry — как у {@link HttpFetcher}.
     */
    public static final class HttpSession {

        private final Connection session;
        private final ParserProperties properties;
        private final RateLimiter rateLimiter;

        private HttpSession(Connection session, ParserProperties properties, RateLimiter rateLimiter) {
            this.session = session;
            this.properties = properties;
            this.rateLimiter = rateLimiter;
        }

        public Document get(String url) {
            return execute(url, null, null);
        }

        public String postForm(String url, String body, String referer) {
            IOException last = null;
            for (int attempt = 0; attempt <= properties.retryMax(); attempt++) {
                rateLimiter.acquire();
                try {
                    Connection.Response response = session.newRequest()
                            .url(url)
                            .header("Content-Type", "application/x-www-form-urlencoded")
                            .header("Referer", referer)
                            .requestBody(body)
                            .method(Connection.Method.POST)
                            .execute();
                    if (response.statusCode() == 404) {
                        throw new FetchException("Страница не найдена (404): " + url, null);
                    }
                    if (response.statusCode() >= 400) {
                        throw new HttpStatusException(
                                "HTTP error fetching URL", response.statusCode(), url);
                    }
                    return response.body();
                } catch (HttpStatusException e) {
                    if (e.getStatusCode() == 404) {
                        throw new FetchException("Страница не найдена (404): " + url, e);
                    }
                    last = e;
                } catch (IOException e) {
                    last = e;
                }
                backoff(properties, attempt);
            }
            throw new FetchException("Не удалось POST " + url + " после " + (properties.retryMax() + 1) + " попыток", last);
        }

        private Document execute(String url, Map<String, String> data, String referer) {
            IOException last = null;
            for (int attempt = 0; attempt <= properties.retryMax(); attempt++) {
                rateLimiter.acquire();
                try {
                    Connection connection = session.newRequest().url(url);
                    if (referer != null) {
                        connection.header("Referer", referer);
                    }
                    if (data != null) {
                        return connection.data(data).post();
                    }
                    return connection.get();
                } catch (HttpStatusException e) {
                    if (e.getStatusCode() == 404) {
                        throw new FetchException("Страница не найдена (404): " + url, e);
                    }
                    last = e;
                } catch (IOException e) {
                    last = e;
                }
                backoff(properties, attempt);
            }
            throw new FetchException("Не удалось получить " + url + " после " + (properties.retryMax() + 1) + " попыток", last);
        }

        private static void backoff(ParserProperties properties, int attempt) {
            long delay = properties.retryBackoffMs() * (1L << Math.min(attempt, 5));
            try {
                Thread.sleep(delay);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Прерван при backoff", e);
            }
        }
    }
}
