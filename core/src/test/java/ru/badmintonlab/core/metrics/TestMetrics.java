package ru.badmintonlab.core.metrics;

import ru.badmintonlab.core.config.MetricsProperties;

import java.math.BigDecimal;

/** Дефолтные метрики из {@code application-core.yml} для юнит-тестов (без Spring-контекста). */
final class TestMetrics {

    private TestMetrics() {
    }

    /** Значения по умолчанию: H=180, W_max=0.8, α=0.5, k=0.5, S_ref=1.0, Bmax=20.0, S0=1.0, w=0.4/0.3/0.3, D_scale=10, T=12. */
    static MetricsProperties defaults() {
        return new MetricsProperties(
                180,
                new BigDecimal("0.8"),
                new BigDecimal("0.5"),
                new BigDecimal("0.5"),
                new BigDecimal("1.0"),
                new BigDecimal("20.0"),
                new BigDecimal("1.0"),
                new BigDecimal("0.4"),
                new BigDecimal("0.3"),
                new BigDecimal("0.3"),
                new BigDecimal("10.0"),
                12,
                new BigDecimal("10.0"),
                stabilityZoneDefaults(),
                gameAccentDefaults());
    }

    static ru.badmintonlab.core.config.StabilityZoneMetrics stabilityZoneDefaults() {
        return new ru.badmintonlab.core.config.StabilityZoneMetrics(
                new BigDecimal("60"),
                new BigDecimal("75"),
                new BigDecimal("85"),
                new BigDecimal("90"));
    }

    static ru.badmintonlab.core.config.GameAccentMetrics gameAccentDefaults() {
        return new ru.badmintonlab.core.config.GameAccentMetrics(
                180,
                new BigDecimal("0.8"),
                new BigDecimal("0.5"),
                new BigDecimal("3.0"),
                180);
    }
}
