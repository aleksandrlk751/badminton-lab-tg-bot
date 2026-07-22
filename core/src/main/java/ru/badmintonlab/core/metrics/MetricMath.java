package ru.badmintonlab.core.metrics;

import java.time.Duration;
import java.time.Instant;

/**
 * Общие математические примитивы метрик (§2 {@code docs/FORMULAR.md}): затухание по полураспаду и сигмоида.
 * Пакетно-приватный — используется только сервисами метрик и их тестами.
 */
final class MetricMath {

    private MetricMath() {
    }

    /**
     * Вес свежести встречи с полураспадом: {@code 0.5^(Δt / H)}.
     * <p>{@code Δt} — время (в днях) от события до опорной точки. Событие в будущем относительно
     * опорной точки (отрицательное {@code Δt}) клампится к нулю, чтобы вес не превышал 1.
     *
     * @param reference    опорная точка отсчёта (обычно «сейчас» или момент прогноза)
     * @param event        момент встречи/матча
     * @param halfLifeDays период полураспада H в днях ({@code > 0})
     */
    static double decayWeight(Instant reference, Instant event, double halfLifeDays) {
        double dtDays = Math.max(0.0, daysBetween(event, reference));
        return Math.pow(0.5, dtDays / halfLifeDays);
    }

    /** Разница {@code to - from} в днях (может быть дробной и отрицательной). */
    static double daysBetween(Instant from, Instant to) {
        return Duration.between(from, to).toMillis() / 86_400_000.0;
    }

    /** Логистическая сигмоида {@code 1 / (1 + e^-x)} — для нормировки в диапазон (0, 1). */
    static double sigmoid(double x) {
        return 1.0 / (1.0 + Math.exp(-x));
    }
}
