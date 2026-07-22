package ru.badmintonlab.core.repository.projection;

import java.math.BigDecimal;
import java.time.Instant;

/** Дельта рейтинга игрока за матч — вход для расчёта Form. */
public interface RatingDeltaView {

    Instant getPlayedAt();

    BigDecimal getRatingDelta();
}
