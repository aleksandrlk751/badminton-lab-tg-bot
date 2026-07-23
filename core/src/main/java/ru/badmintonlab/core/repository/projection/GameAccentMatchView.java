package ru.badmintonlab.core.repository.projection;

import ru.badmintonlab.core.domain.PlayerSex;

import java.math.BigDecimal;
import java.time.Instant;

/** Матч игрока с полом партнёра на той же стороне — для «Игрового акцента». */
public interface GameAccentMatchView {

    Instant getPlayedAt();

    BigDecimal getRatingDelta();

    PlayerSex getPartnerSex();

    PlayerSex getPlayerSex();
}
