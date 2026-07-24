package ru.badmintonlab.core.metrics;

import ru.badmintonlab.core.config.MetricsProperties;

import java.math.BigDecimal;

/** Дефолтные метрики из {@code application-core.yml} для юнит-тестов (без Spring-контекста). */
final class TestMetrics {

    private TestMetrics() {
    }

    /** Значения по умолчанию: H=180, w=0.35/0.25/0.25/0.10/0.05, D_scale=10, T=12. */
    static MetricsProperties defaults() {
        return new MetricsProperties(
                180,
                new BigDecimal("0.8"),
                new BigDecimal("0.5"),
                new BigDecimal("0.5"),
                new BigDecimal("1.0"),
                new BigDecimal("20.0"),
                new BigDecimal("1.0"),
                new BigDecimal("0.35"),
                new BigDecimal("0.25"),
                new BigDecimal("0.25"),
                new BigDecimal("0.10"),
                new BigDecimal("0.05"),
                new BigDecimal("10.0"),
                12,
                new BigDecimal("1.0"),
                new BigDecimal("10.0"),
                partnerFormStabilityDefaults(),
                new BigDecimal("11.5"),
                stabilityZoneDefaults(),
                gameAccentDefaults());
    }

    static ru.badmintonlab.core.config.PartnerFormStabilityMultipliers partnerFormStabilityDefaults() {
        return new ru.badmintonlab.core.config.PartnerFormStabilityMultipliers(
                new BigDecimal("1.1"),
                new BigDecimal("1.0"),
                new BigDecimal("0.85"),
                new BigDecimal("0.7"),
                new BigDecimal("0.5"));
    }

    static ru.badmintonlab.core.config.StabilityZoneMetrics stabilityZoneDefaults() {
        return new ru.badmintonlab.core.config.StabilityZoneMetrics(
                new BigDecimal("70"),
                new BigDecimal("80"),
                new BigDecimal("86"),
                new BigDecimal("92"));
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
