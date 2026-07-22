package ru.badmintonlab.core.repository.projection;

/**
 * Проекция строки экрана «Соперники»: соперник + баланс встреч в дисциплине.
 */
public interface RivalView {

    Long getOpponentId();

    String getNick();

    String getLastName();

    String getFirstName();

    String getCity();

    Short getWins();

    Short getLosses();
}
