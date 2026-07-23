package ru.badmintonlab.core.repository.projection;

import java.math.BigDecimal;
import java.time.Instant;

/** Матч игрока с контекстом турнира — вход для {@link ru.badmintonlab.core.metrics.StabilityService}. */
public interface StabilityMatchView {

    Long getTournamentId();

    Instant getTournamentStartsAt();

    BigDecimal getRatingDelta();
}
