package ru.badmintonlab.bot.model;

import ru.badmintonlab.core.domain.Discipline;

import java.util.List;

/**
 * Страница экрана «Соперники». {@code discipline == null} — фильтр «Все» (сумма W–L по разрядам).
 */
public record RivalsPage(
        long playerId,
        String playerFullName,
        String playerNick,
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

    public boolean allDisciplines() {
        return discipline == null;
    }

    public String disciplineFilterLabel() {
        return discipline == null ? "Все" : discipline.name();
    }
}
