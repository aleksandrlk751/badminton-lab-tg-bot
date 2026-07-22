package ru.badmintonlab.core.metrics;

import java.time.Instant;

/**
 * Одна знаковая дельта рейтинга ЛАБ, привязанная к моменту матча — вход для {@link FormService}.
 * <p>Дельта берётся as-is из источника ({@code match_player.rating_delta}); для парного матча она
 * одинакова у обоих игроков стороны (см. {@code docs/BRIEF.md} §2).
 *
 * @param playedAt момент матча
 * @param delta    знаковая дельта рейтинга (положительная — победа, отрицательная — поражение)
 */
public record RatingDeltaEvent(Instant playedAt, double delta) {
}
