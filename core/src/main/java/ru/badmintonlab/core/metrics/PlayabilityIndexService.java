package ru.badmintonlab.core.metrics;

import org.springframework.stereotype.Service;
import ru.badmintonlab.core.config.MetricsProperties;

import java.time.Instant;
import java.util.Collection;

/**
 * Индекс сыгранности {@code S} (§2.1 {@code docs/FORMULAR.md}):
 * <pre>S = Σ_i 0.5^(Δt_i / H)</pre>
 * Сумма весов свежести по каждой встрече. Применяется к соперникам (H2H) и партнёрам
 * (совместные парные турниры). Период полураспада {@code H} — из {@link MetricsProperties}.
 */
@Service
public class PlayabilityIndexService {

    private final MetricsProperties metrics;

    public PlayabilityIndexService(MetricsProperties metrics) {
        this.metrics = metrics;
    }

    /**
     * @param reference    опорная точка отсчёта затухания
     * @param meetingTimes моменты встреч (матчей или совместных турниров); пустой набор → {@code 0}
     */
    public double index(Instant reference, Collection<Instant> meetingTimes) {
        double halfLife = metrics.halfLifeDays();
        double sum = 0.0;
        for (Instant meeting : meetingTimes) {
            sum += MetricMath.decayWeight(reference, meeting, halfLife);
        }
        return sum;
    }

    /** Индекс относительно текущего момента. */
    public double index(Collection<Instant> meetingTimes) {
        return index(Instant.now(), meetingTimes);
    }
}
