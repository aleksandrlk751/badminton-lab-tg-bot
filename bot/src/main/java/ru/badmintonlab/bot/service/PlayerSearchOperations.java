package ru.badmintonlab.bot.service;

import ru.badmintonlab.bot.model.PlayerSearchResult;

import java.util.List;

/** Контракт поиска игрока (для тестов и подмены). */
public interface PlayerSearchOperations {

    boolean isQueryTooShort(String rawQuery);

    List<PlayerSearchResult> search(String rawQuery);
}
