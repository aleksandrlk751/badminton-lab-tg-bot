package ru.badmintonlab.core.metrics;

import org.springframework.stereotype.Service;
import ru.badmintonlab.core.config.MetricsProperties;

import java.time.Instant;
import java.util.Collection;

/**
 * Форма игрока {@code Form} (§2.2 {@code docs/FORMULAR.md}):
 * <pre>Form = Σ_i δ_i · 0.5^(Δt_i / H)</pre>
 * Сумма знаковых дельт ЛАБ, взвешенных свежестью. Дельта кодирует ожидаемость результата,
 * поэтому положительный {@code Form} — «в форме», отрицательный — «не в форме».
 * Период полураспада {@code H} — из {@link MetricsProperties}.
 */
@Service
public class FormService {

    private final MetricsProperties metrics;

    public FormService(MetricsProperties metrics) {
        this.metrics = metrics;
    }

    /**
     * @param reference опорная точка отсчёта затухания
     * @param events    матчи с дельтами рейтинга; пустой набор → {@code 0}
     */
    public double form(Instant reference, Collection<RatingDeltaEvent> events) {
        double halfLife = metrics.halfLifeDays();
        double earlyMax = metrics.earlyDecayMax().doubleValue();
        double earlyPower = metrics.earlyDecayPower().doubleValue();
        double sum = 0.0;
        for (RatingDeltaEvent event : events) {
            sum += event.delta() * MetricMath.decayWeight(
                    reference, event.playedAt(), halfLife, earlyMax, earlyPower);
        }
        return sum;
    }

    /** Форма относительно текущего момента. */
    public double form(Collection<RatingDeltaEvent> events) {
        return form(Instant.now(), events);
    }
}
