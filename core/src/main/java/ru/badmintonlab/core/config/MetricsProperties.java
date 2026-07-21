package ru.badmintonlab.core.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;

@ConfigurationProperties(prefix = "badminton-lab.metrics")
public record MetricsProperties(
        int halfLifeDays,
        BigDecimal formK,
        BigDecimal sRef,
        BigDecimal bMax,
        BigDecimal s0,
        BigDecimal w1,
        BigDecimal w2,
        BigDecimal w3,
        BigDecimal dScale,
        int partnerHistoryMonths
) {
    public MetricsProperties {
        if (halfLifeDays <= 0) {
            throw new IllegalArgumentException("halfLifeDays must be positive");
        }
        if (partnerHistoryMonths <= 0) {
            throw new IllegalArgumentException("partnerHistoryMonths must be positive");
        }
    }
}
