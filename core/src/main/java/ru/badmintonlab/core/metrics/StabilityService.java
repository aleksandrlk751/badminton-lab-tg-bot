package ru.badmintonlab.core.metrics;

import org.springframework.stereotype.Service;
import ru.badmintonlab.core.config.MetricsProperties;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Стабильность игрока (§2.6 {@code docs/FORMULAR.md}): однородность сюрпризов внутри турнира
 * и отсутствие «качки» между соседними турнирами. Результат — {@code [0, 100]}.
 */
@Service
public class StabilityService {

    private final MetricsProperties metrics;

    public StabilityService(MetricsProperties metrics) {
        this.metrics = metrics;
    }

    /**
     * @param reference опорная точка затухания
     * @param events    матчи с δ и датой турнира; пустой набор → empty
     */
    public Optional<Double> stability(Instant reference, Collection<StabilityMatchEvent> events) {
        if (events == null || events.isEmpty()) {
            return Optional.empty();
        }
        double epsilon = metrics.stabilitySurpriseThreshold().doubleValue();
        if (!hasAnySurprise(events, epsilon)) {
            return Optional.empty();
        }
        List<TournamentBlock> tournaments = groupByTournament(events);
        if (tournaments.isEmpty()) {
            return Optional.empty();
        }

        double halfLife = metrics.halfLifeDays();
        double earlyMax = metrics.earlyDecayMax().doubleValue();
        double earlyPower = metrics.earlyDecayPower().doubleValue();

        double weightSum = 0.0;
        double scoreSum = 0.0;

        for (TournamentBlock block : tournaments) {
            double within = withinScore(block.deltas(), epsilon);
            double w = MetricMath.decayWeight(reference, block.tournamentAt(), halfLife, earlyMax, earlyPower);
            weightSum += w;
            scoreSum += within * w;
        }

        for (int i = 1; i < tournaments.size(); i++) {
            TournamentBlock prev = tournaments.get(i - 1);
            TournamentBlock curr = tournaments.get(i);
            double between = betweenScore(prev.deltas(), curr.deltas(), epsilon);
            double w = MetricMath.decayWeight(reference, curr.tournamentAt(), halfLife, earlyMax, earlyPower);
            weightSum += w;
            scoreSum += between * w;
        }

        if (weightSum <= 0.0) {
            return Optional.empty();
        }
        return Optional.of(100.0 * scoreSum / weightSum);
    }

    /** Стабильность относительно текущего момента. */
    public Optional<Double> stability(Collection<StabilityMatchEvent> events) {
        return stability(Instant.now(), events);
    }

    static double withinScore(List<Double> deltas, double epsilon) {
        int positive = 0;
        int negative = 0;
        for (double delta : deltas) {
            if (delta > epsilon) {
                positive++;
            } else if (delta < -epsilon) {
                negative++;
            }
        }
        if (positive == 0 || negative == 0) {
            return 1.0;
        }
        return 1.0 - (double) Math.min(positive, negative) / Math.max(positive, negative);
    }

    static double betweenScore(List<Double> prevDeltas, List<Double> currDeltas, double epsilon) {
        double prevTone = tone(prevDeltas, epsilon);
        double currTone = tone(currDeltas, epsilon);
        if (prevTone * currTone < 0.0) {
            return 0.0;
        }
        return 1.0;
    }

    static double tone(List<Double> deltas, double epsilon) {
        double sum = 0.0;
        for (double delta : deltas) {
            if (Math.abs(delta) > epsilon) {
                sum += delta;
            }
        }
        return sum;
    }

    private static List<TournamentBlock> groupByTournament(Collection<StabilityMatchEvent> events) {
        Map<Long, TournamentBlockBuilder> builders = new LinkedHashMap<>();
        for (StabilityMatchEvent event : events) {
            builders.computeIfAbsent(event.tournamentId(), id -> new TournamentBlockBuilder(event.tournamentAt()))
                    .addDelta(event.delta());
        }
        return builders.values().stream()
                .map(TournamentBlockBuilder::build)
                .sorted(Comparator.comparing(TournamentBlock::tournamentAt))
                .toList();
    }

    private static boolean hasAnySurprise(Collection<StabilityMatchEvent> events, double epsilon) {
        for (StabilityMatchEvent event : events) {
            if (Math.abs(event.delta()) > epsilon) {
                return true;
            }
        }
        return false;
    }

    private record TournamentBlock(Instant tournamentAt, List<Double> deltas) {
    }

    private static final class TournamentBlockBuilder {
        private final Instant tournamentAt;
        private final List<Double> deltas = new ArrayList<>();

        private TournamentBlockBuilder(Instant tournamentAt) {
            this.tournamentAt = tournamentAt;
        }

        private void addDelta(double delta) {
            deltas.add(delta);
        }

        private TournamentBlock build() {
            return new TournamentBlock(tournamentAt, List.copyOf(deltas));
        }
    }
}
