package ru.badmintonlab.bot.service;

import org.springframework.stereotype.Service;
import ru.badmintonlab.core.config.MetricsProperties;
import ru.badmintonlab.core.metrics.StabilityLevel;
import ru.badmintonlab.core.metrics.StabilityMatchEvent;
import ru.badmintonlab.core.metrics.StabilityService;
import ru.badmintonlab.core.repository.H2hRepository;
import ru.badmintonlab.core.repository.projection.StabilityMatchView;

import java.util.List;
import java.util.Optional;

/**
 * Стабильность игрока по парным матчам — emoji-зона для карточки (§2.8 {@code docs/FORMULAR.md}).
 */
@Service
public class PlayerStabilityService {

    private final H2hRepository h2hRepository;
    private final StabilityService stabilityService;
    private final MetricsProperties metrics;

    public PlayerStabilityService(H2hRepository h2hRepository,
                                  StabilityService stabilityService,
                                  MetricsProperties metrics) {
        this.h2hRepository = h2hRepository;
        this.stabilityService = stabilityService;
        this.metrics = metrics;
    }

    /** Emoji зоны стабильности; пусто — строка на карточке скрыта. */
    public Optional<String> stabilityEmojiForCard(long playerId) {
        return stabilityLevelIfKnown(playerId).map(StabilityLevel::emoji);
    }

    /** Зона стабильности; пусто — нет матчей с сюрпризом (§2.8). */
    public Optional<StabilityLevel> stabilityLevelIfKnown(long playerId) {
        return stabilityService.stability(loadEvents(playerId))
                .map(score -> StabilityLevel.fromScore(score, metrics.stabilityZones()));
    }

    private List<StabilityMatchEvent> loadEvents(long playerId) {
        return h2hRepository.findStabilityMatches(playerId, PlayerFormService.PAIR_DISCIPLINES).stream()
                .map(PlayerStabilityService::toEvent)
                .toList();
    }

    private static StabilityMatchEvent toEvent(StabilityMatchView view) {
        return new StabilityMatchEvent(
                view.getTournamentId(),
                view.getTournamentStartsAt(),
                view.getRatingDelta().doubleValue());
    }
}
