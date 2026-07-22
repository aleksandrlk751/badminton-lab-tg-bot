package ru.badmintonlab.bot.view;

import org.junit.jupiter.api.Test;
import ru.badmintonlab.bot.model.PlayerCard;
import ru.badmintonlab.bot.model.RatingLine;
import ru.badmintonlab.bot.model.RivalRow;
import ru.badmintonlab.bot.model.RivalsPage;
import ru.badmintonlab.core.domain.Discipline;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TextsTest {

    private final Texts texts = new Texts();

    @Test
    void formatRatingStripsTrailingZero() {
        assertEquals("412", Texts.formatRating(new BigDecimal("412.0")));
        assertEquals("412.5", Texts.formatRating(new BigDecimal("412.5")));
        assertEquals("—", Texts.formatRating(null));
    }

    @Test
    void escapeHandlesHtmlSpecials() {
        assertEquals("a &amp; b &lt;c&gt;", Texts.escape("a & b <c>"));
        assertEquals("", Texts.escape(null));
    }

    @Test
    void cardContainsKeyFieldsAndFooter() {
        PlayerCard card = new PlayerCard(
                18499L,
                "Rocket",
                "Иванов Иван",
                "Москва",
                List.of(new RatingLine(Discipline.MD, new BigDecimal("380.0")),
                        new RatingLine(Discipline.XD, new BigDecimal("410.5"))),
                null,
                LocalDate.of(2026, 7, 20));

        String text = texts.card(card);

        assertTrue(text.contains("Rocket"), text);
        assertTrue(text.contains("Иванов Иван"), text);
        assertTrue(text.contains("Москва"), text);
        assertTrue(text.contains("MD: <b>380</b>"), text);
        assertTrue(text.contains("XD: <b>410.5</b>"), text);
        assertTrue(text.contains("Данные на 20.07.2026"), text);
    }

    @Test
    void rivalsRendersRowsAndPaginationInfo() {
        RivalsPage page = new RivalsPage(
                1L,
                Discipline.MD,
                List.of(new RivalRow(2L, "Foe", "Петров Пётр", "Москва", 3, 1)),
                0,
                8,
                1,
                List.of(Discipline.MD));

        String text = texts.rivals(page);

        assertTrue(text.contains("Соперники"), text);
        assertTrue(text.contains("MD"), text);
        assertTrue(text.contains("Foe"), text);
        assertTrue(text.contains("W-L: 3-1"), text);
    }

    @Test
    void rivalsHandlesEmptyDiscipline() {
        RivalsPage page = new RivalsPage(1L, Discipline.WD, List.of(), 0, 8, 0, List.of());
        assertTrue(texts.rivals(page).contains("Встреч в этой дисциплине нет"));
    }
}
