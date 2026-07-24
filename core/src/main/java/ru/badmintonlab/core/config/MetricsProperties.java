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
        BigDecimal w4,
        BigDecimal w5,
        BigDecimal dScale,
        int partnerHistoryMonths,
        BigDecimal sRefPartner,
        BigDecimal partnerFormScale,
        PartnerFormStabilityMultipliers partnerFormStability,
        BigDecimal stabilitySurpriseThreshold,
        StabilityZoneMetrics stabilityZones,
        GameAccentMetrics gameAccent
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
        if (partnerFormScale == null || partnerFormScale.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("partnerFormScale must be positive");
        }
        if (partnerFormStability == null) {
            throw new IllegalArgumentException("partnerFormStability must be configured");
        }
        validateUnitFraction(w4, "w4");
        validateUnitFraction(w5, "w5");
        if (stabilitySurpriseThreshold == null || stabilitySurpriseThreshold.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("stabilitySurpriseThreshold must be positive");
        }
        if (stabilityZones == null) {
            throw new IllegalArgumentException("stabilityZones must be configured");
        }
        if (gameAccent == null) {
            throw new IllegalArgumentException("gameAccent must be configured");
        }
    }

    private static void validateUnitFraction(BigDecimal value, String name) {
        if (value == null || value.compareTo(BigDecimal.ZERO) < 0 || value.compareTo(BigDecimal.ONE) > 0) {
            throw new IllegalArgumentException(name + " must be in [0, 1]");
        }
    }
}
