package ru.badmintonlab.core.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;

@ConfigurationProperties(prefix = "badminton-lab.metrics")
public record MetricsProperties(
        int halfLifeDays,
        BigDecimal earlyDecayMax,
        BigDecimal earlyDecayPower,
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
        if (earlyDecayMax == null || earlyDecayMax.compareTo(new BigDecimal("0.5")) <= 0
                || earlyDecayMax.compareTo(BigDecimal.ONE) > 0) {
            throw new IllegalArgumentException("earlyDecayMax must be in (0.5, 1]");
        }
        if (earlyDecayPower == null || earlyDecayPower.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("earlyDecayPower must be positive");
        }
        if (partnerHistoryMonths <= 0) {
            throw new IllegalArgumentException("partnerHistoryMonths must be positive");
        }
    }
}
