package ru.badmintonlab.core.metrics;

import org.springframework.stereotype.Service;
import ru.badmintonlab.core.config.GameAccentMetrics;
import ru.badmintonlab.core.config.MetricsProperties;
import ru.badmintonlab.core.domain.PairCompositionType;

import java.time.Instant;
import java.util.Collection;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;

/**
 * Игровой акцент (§2.7 {@code docs/FORMULAR.md}): предпочитаемый тип парных игр и сильная сторона
 * по взвешенным матчам MD/WD/XD.
 */
@Service
public class GameAccentService {

    private final MetricsProperties metrics;

    public GameAccentService(MetricsProperties metrics) {
        this.metrics = metrics;
    }

    /**
     * @param reference опорная точка отсчёта затухания
     * @param events    матчи с известным типом пары; пустой набор или недостаточный суммарный вес → пусто
     */
    public Optional<GameAccentResult> accent(Instant reference, Collection<GameAccentEvent> events) {
        GameAccentMetrics config = metrics.gameAccent();
        double halfLife = config.halfLifeDays();
        double earlyMax = config.earlyDecayMax().doubleValue();
        double earlyPower = config.earlyDecayPower().doubleValue();
        double minWeight = config.minWeightSum().doubleValue();
        int displayDays = config.displayWindowDays();

        Map<PairCompositionType, Double> prefByType = new EnumMap<>(PairCompositionType.class);
        Map<PairCompositionType, Double> weightByType = new EnumMap<>(PairCompositionType.class);
        Map<PairCompositionType, Double> deltaWeightedByType = new EnumMap<>(PairCompositionType.class);
        Map<PairCompositionType, Double> winWeightedByType = new EnumMap<>(PairCompositionType.class);
        Map<PairCompositionType, Double> winDenomByType = new EnumMap<>(PairCompositionType.class);
        Map<PairCompositionType, Integer> gamesInWindowByType = new EnumMap<>(PairCompositionType.class);

        double totalWeight = 0.0;

        for (GameAccentEvent event : events) {
            if (event.compositionType() == PairCompositionType.UNKNOWN) {
                continue;
            }
            double w = MetricMath.decayWeight(
                    reference, event.playedAt(), halfLife, earlyMax, earlyPower);
            PairCompositionType type = event.compositionType();

            totalWeight += w;
            prefByType.merge(type, w, Double::sum);
            weightByType.merge(type, w, Double::sum);
            deltaWeightedByType.merge(type, event.delta() * w, Double::sum);

            if (event.delta() != 0.0) {
                double outcome = event.delta() > 0 ? 1.0 : 0.0;
                winWeightedByType.merge(type, outcome * w, Double::sum);
                winDenomByType.merge(type, w, Double::sum);
            }

            if (MetricMath.daysBetween(event.playedAt(), reference) <= displayDays) {
                gamesInWindowByType.merge(type, 1, Integer::sum);
            }
        }

        if (totalWeight < minWeight) {
            return Optional.empty();
        }

        PairCompositionType preferenceType = bestByWeight(prefByType);
        double preferenceShare = prefByType.get(preferenceType) / totalWeight;

        PairCompositionType strengthType = bestStrengthType(weightByType, deltaWeightedByType);
        double typeWeight = weightByType.get(strengthType);
        double strengthAvgDelta = deltaWeightedByType.get(strengthType) / typeWeight;
        double winWeight = winWeightedByType.getOrDefault(strengthType, 0.0);
        double winDenom = winDenomByType.getOrDefault(strengthType, 0.0);
        double strengthWinRate = winDenom > 0.0 ? winWeight / winDenom : 0.0;

        return Optional.of(new GameAccentResult(
                preferenceType,
                preferenceShare,
                gamesInWindowByType.getOrDefault(preferenceType, 0),
                strengthType,
                strengthAvgDelta,
                strengthWinRate));
    }

    /** Акцент относительно текущего момента. */
    public Optional<GameAccentResult> accent(Collection<GameAccentEvent> events) {
        return accent(Instant.now(), events);
    }

    /**
     * Взвешенная средняя δ (§2.7, {@code Strength(type)}) по матчам заданного типа пары.
     * Не требует порога {@code W_min} карточки — для подбора партнёра по разряду турнира.
     *
     * @return пусто, если матчей этого типа нет или тип {@link PairCompositionType#UNKNOWN}
     */
    public OptionalDouble avgWeightedDeltaForType(Instant reference,
                                                  Collection<GameAccentEvent> events,
                                                  PairCompositionType type) {
        if (type == null || type == PairCompositionType.UNKNOWN) {
            return OptionalDouble.empty();
        }
        GameAccentMetrics config = metrics.gameAccent();
        double halfLife = config.halfLifeDays();
        double earlyMax = config.earlyDecayMax().doubleValue();
        double earlyPower = config.earlyDecayPower().doubleValue();
        double weightSum = 0.0;
        double deltaWeightedSum = 0.0;
        for (GameAccentEvent event : events) {
            if (event.compositionType() != type) {
                continue;
            }
            double w = MetricMath.decayWeight(
                    reference, event.playedAt(), halfLife, earlyMax, earlyPower);
            weightSum += w;
            deltaWeightedSum += event.delta() * w;
        }
        if (weightSum <= 0.0) {
            return OptionalDouble.empty();
        }
        return OptionalDouble.of(deltaWeightedSum / weightSum);
    }

    public OptionalDouble avgWeightedDeltaForType(Collection<GameAccentEvent> events,
                                                  PairCompositionType type) {
        return avgWeightedDeltaForType(Instant.now(), events, type);
    }

    private static PairCompositionType bestByWeight(Map<PairCompositionType, Double> weights) {
        PairCompositionType best = null;
        double bestWeight = Double.NEGATIVE_INFINITY;
        for (Map.Entry<PairCompositionType, Double> entry : weights.entrySet()) {
            if (entry.getValue() > bestWeight) {
                bestWeight = entry.getValue();
                best = entry.getKey();
            }
        }
        return best;
    }

    private static PairCompositionType bestStrengthType(
            Map<PairCompositionType, Double> weightByType,
            Map<PairCompositionType, Double> deltaWeightedByType) {
        PairCompositionType best = null;
        double bestAvg = Double.NEGATIVE_INFINITY;
        double bestWeight = Double.NEGATIVE_INFINITY;
        for (PairCompositionType type : weightByType.keySet()) {
            double weight = weightByType.get(type);
            double avg = deltaWeightedByType.get(type) / weight;
            if (avg > bestAvg || (avg == bestAvg && weight > bestWeight)) {
                bestAvg = avg;
                bestWeight = weight;
                best = type;
            }
        }
        return best;
    }
}
