package ru.badmintonlab.core.domain;

/**
 * Тип пары по полу двух игроков (для H2H и подбора партнёра).
 * {@code MD}/{@code WD}/{@code XD} — однополая мужская / женская / микст;
 * {@code UNKNOWN} — пол хотя бы одного игрока неизвестен.
 */
public enum PairCompositionType {
    MD,
    WD,
    XD,
    UNKNOWN
}
