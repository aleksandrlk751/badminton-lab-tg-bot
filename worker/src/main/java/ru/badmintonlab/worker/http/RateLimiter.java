package ru.badmintonlab.worker.http;

/**
 * Простой потокобезопасный ограничитель частоты: гарантирует минимальный интервал
 * между запросами на всех потоках слепка (глобально ≤ maxRps на хост).
 */
public class RateLimiter {

    private final long minIntervalNanos;
    private long nextAllowedNanos;

    public RateLimiter(double maxRps) {
        this.minIntervalNanos = maxRps <= 0 ? 0 : (long) (1_000_000_000L / maxRps);
        this.nextAllowedNanos = System.nanoTime();
    }

    /**
     * Блокирует вызывающий поток до момента, когда очередной запрос разрешён.
     */
    public void acquire() {
        long waitNanos;
        synchronized (this) {
            long now = System.nanoTime();
            if (now < nextAllowedNanos) {
                waitNanos = nextAllowedNanos - now;
                nextAllowedNanos += minIntervalNanos;
            } else {
                waitNanos = 0;
                nextAllowedNanos = now + minIntervalNanos;
            }
        }
        if (waitNanos > 0) {
            try {
                Thread.sleep(waitNanos / 1_000_000L, (int) (waitNanos % 1_000_000L));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Прерван при ожидании rate-limit", e);
            }
        }
    }
}
