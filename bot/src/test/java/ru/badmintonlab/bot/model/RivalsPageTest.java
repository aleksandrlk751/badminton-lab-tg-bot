package ru.badmintonlab.bot.model;

import org.junit.jupiter.api.Test;
import ru.badmintonlab.core.domain.Discipline;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RivalsPageTest {

    private RivalsPage page(int page, long total) {
        return new RivalsPage(1L, "Иванов", null, Discipline.MD, List.of(), page, 8, total, List.of(Discipline.MD));
    }

    @Test
    void totalPagesRoundsUp() {
        assertEquals(1, page(0, 0).totalPages());
        assertEquals(1, page(0, 8).totalPages());
        assertEquals(2, page(0, 9).totalPages());
        assertEquals(3, page(0, 17).totalPages());
    }

    @Test
    void navigationFlags() {
        assertFalse(page(0, 20).hasPrev());
        assertTrue(page(0, 20).hasNext());
        assertTrue(page(1, 20).hasPrev());
        assertTrue(page(1, 20).hasNext());
        assertFalse(page(2, 20).hasNext());
    }
}
