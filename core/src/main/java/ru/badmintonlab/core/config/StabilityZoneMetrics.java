package ru.badmintonlab.core.config;

import java.math.BigDecimal;

/**
 * Пороги зон отображения стабильности на карточке (§2.8 {@code docs/FORMULAR.md}).
 * Зона 1: {@code [0, zone2Min)}; далее полуинтервалы до {@code zone5Min}; зона 5: {@code [zone5Min, 100]}.
 */
public record StabilityZoneMetrics(
        BigDecimal zone2Min,
        BigDecimal zone3Min,
        BigDecimal zone4Min,
        BigDecimal zone5Min
) {
    public StabilityZoneMetrics {
        if (zone2Min == null || zone3Min == null || zone4Min == null || zone5Min == null) {
            throw new IllegalArgumentException("stability zone thresholds must be configured");
        }
        if (zone2Min.compareTo(zone3Min) >= 0
                || zone3Min.compareTo(zone4Min) >= 0
                || zone4Min.compareTo(zone5Min) >= 0) {
            throw new IllegalArgumentException("stability zones must be strictly increasing");
        }
    }
}
