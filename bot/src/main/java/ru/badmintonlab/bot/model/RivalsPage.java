package ru.badmintonlab.bot.model;

import ru.badmintonlab.core.domain.Discipline;

import java.util.List;

/**
 * Страница экрана «Соперники»: строки текущей страницы, выбранная дисциплина,
 * доступные дисциплины (для переключателя) и данные пагинации.
 */
public record RivalsPage(
        long playerId,
        Discipline discipline,
        List<RivalRow> rows,
        int page,
        int pageSize,
        long total,
        List<Discipline> availableDisciplines
) {

    public int totalPages() {
        if (total <= 0) {
            return 1;
        }
        return (int) ((total + pageSize - 1) / pageSize);
    }

    public boolean hasPrev() {
        return page > 0;
    }

    public boolean hasNext() {
        return page + 1 < totalPages();
    }
}
