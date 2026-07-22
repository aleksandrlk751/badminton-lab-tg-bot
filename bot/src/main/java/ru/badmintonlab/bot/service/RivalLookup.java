package ru.badmintonlab.bot.service;

import ru.badmintonlab.bot.model.RivalsPage;
import ru.badmintonlab.core.domain.Discipline;

/** Контракт экрана «Соперники». */
public interface RivalLookup {

    boolean hasRivals(long playerId);

    RivalsPage rivals(long playerId, Discipline discipline, int page);
}
