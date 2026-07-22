package ru.badmintonlab.bot.service;

import org.springframework.stereotype.Service;
import ru.badmintonlab.core.domain.Discipline;
import ru.badmintonlab.core.metrics.FormService;
import ru.badmintonlab.core.metrics.RatingDeltaEvent;
import ru.badmintonlab.core.repository.H2hRepository;
import ru.badmintonlab.core.repository.projection.RatingDeltaView;

import java.util.EnumSet;
import java.util.List;
import java.util.OptionalDouble;
import java.util.Set;

/**
 * Форма игрока по парным матчам (D/MD/WD/XD) — общий расчёт для карточки и H2H.
 */
@Service
public class PlayerFormService {

    static final Set<Discipline> PAIR_DISCIPLINES =
            EnumSet.of(Discipline.D, Discipline.MD, Discipline.WD, Discipline.XD);

    private final H2hRepository h2hRepository;
    private final FormService formService;

    public PlayerFormService(H2hRepository h2hRepository, FormService formService) {
        this.h2hRepository = h2hRepository;
        this.formService = formService;
    }

    /** Форма, если есть хотя бы один матч с дельтой; иначе пусто (не показываем на карточке). */
    public OptionalDouble formIfKnown(long playerId) {
        List<RatingDeltaEvent> events = loadEvents(playerId);
        if (events.isEmpty()) {
            return OptionalDouble.empty();
        }
        return OptionalDouble.of(formService.form(events));
    }

    /** Значение для карточки: {@code null}, если нет матчей с дельтой. */
    public Double formForCard(long playerId) {
        OptionalDouble form = formIfKnown(playerId);
        return form.isPresent() ? form.getAsDouble() : null;
    }

    /** Форма для метрик; без матчей — {@code 0}. */
    public double form(long playerId) {
        return formIfKnown(playerId).orElse(0.0);
    }

    private List<RatingDeltaEvent> loadEvents(long playerId) {
        return h2hRepository.findRatingDeltas(playerId, PAIR_DISCIPLINES).stream()
                .map(PlayerFormService::toEvent)
                .toList();
    }

    private static RatingDeltaEvent toEvent(RatingDeltaView view) {
        return new RatingDeltaEvent(view.getPlayedAt(), view.getRatingDelta().doubleValue());
    }
}
