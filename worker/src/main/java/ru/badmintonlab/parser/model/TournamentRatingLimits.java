package ru.badmintonlab.parser.model;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * Лимиты рейтинга с страницы турнира (секция «информация»).
 *
 * @param pairRatingLimit       лимит на средний рейтинг пары; пусто — «без лимита» на странице
 * @param maxPlayerRatingLimit  макс. рейтинг одного игрока; при отсутствии фразы на сайте = pairRatingLimit
 */
public record TournamentRatingLimits(
        Optional<BigDecimal> pairRatingLimit,
        Optional<BigDecimal> maxPlayerRatingLimit
) {
    public static TournamentRatingLimits noLimit() {
        return new TournamentRatingLimits(Optional.empty(), Optional.empty());
    }
}
