package ru.badmintonlab.bot.service;

import ru.badmintonlab.bot.model.PlayerCard;

import java.util.Optional;

/** Контракт загрузки карточки игрока. */
public interface PlayerCardLoader {

    Optional<PlayerCard> card(long playerId);
}
