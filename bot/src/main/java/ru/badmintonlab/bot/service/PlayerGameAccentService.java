package ru.badmintonlab.bot.service;

import org.springframework.stereotype.Service;
import ru.badmintonlab.core.domain.Discipline;
import ru.badmintonlab.core.domain.PairCompositionType;
import ru.badmintonlab.core.metrics.GameAccentEvent;
import ru.badmintonlab.core.metrics.GameAccentResult;
import ru.badmintonlab.core.metrics.GameAccentService;
import ru.badmintonlab.core.metrics.PairCompositionService;
import ru.badmintonlab.core.repository.H2hRepository;
import ru.badmintonlab.core.repository.projection.GameAccentMatchView;

import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.Set;

/**
 * Игровой акцент игрока по парным матчам MD/WD/XD — для карточки.
 */
@Service
public class PlayerGameAccentService {

    static final Set<Discipline> PAIR_DISCIPLINES =
            EnumSet.of(Discipline.D, Discipline.MD, Discipline.WD, Discipline.XD);

    private final H2hRepository h2hRepository;
    private final PairCompositionService pairCompositionService;
    private final GameAccentService gameAccentService;

    public PlayerGameAccentService(H2hRepository h2hRepository,
                                   PairCompositionService pairCompositionService,
                                   GameAccentService gameAccentService) {
        this.h2hRepository = h2hRepository;
        this.pairCompositionService = pairCompositionService;
        this.gameAccentService = gameAccentService;
    }

    public Optional<GameAccentResult> accentForCard(long playerId) {
        List<GameAccentEvent> events = loadAccentEvents(playerId);
        return gameAccentService.accent(events);
    }

    /**
     * Средняя δ по типу пары (MD/WD/XD) для разряда турнира — без привязки к «рекомендуемой» категории.
     */
    public OptionalDouble avgDeltaForComposition(long playerId, PairCompositionType compositionType) {
        if (compositionType == null || compositionType == PairCompositionType.UNKNOWN) {
            return OptionalDouble.empty();
        }
        return gameAccentService.avgWeightedDeltaForType(loadAccentEvents(playerId), compositionType);
    }

    private List<GameAccentEvent> loadAccentEvents(long playerId) {
        return h2hRepository.findAccentMatches(playerId, PAIR_DISCIPLINES).stream()
                .map(this::toEvent)
                .filter(e -> e.compositionType() != PairCompositionType.UNKNOWN)
                .toList();
    }

    private GameAccentEvent toEvent(GameAccentMatchView view) {
        return new GameAccentEvent(
                view.getPlayedAt(),
                view.getRatingDelta().doubleValue(),
                pairCompositionService.resolve(view.getPlayerSex(), view.getPartnerSex()));
    }
}
