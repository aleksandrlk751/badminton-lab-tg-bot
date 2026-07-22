package ru.badmintonlab.bot.model;

import ru.badmintonlab.core.domain.Discipline;

import java.math.BigDecimal;

/**
 * Рейтинг игрока в одной дисциплине для сводки на карточке.
 */
public record RatingLine(Discipline discipline, BigDecimal rating) {
}
