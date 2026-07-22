package ru.badmintonlab.bot.view;

import org.junit.jupiter.api.Test;
import ru.badmintonlab.bot.model.PlayerCard;
import ru.badmintonlab.bot.model.PlayerSearchResult;
import ru.badmintonlab.bot.model.RatingLine;
import ru.badmintonlab.bot.model.RivalRow;
import ru.badmintonlab.bot.model.RivalsPage;
import ru.badmintonlab.bot.model.LastTournamentInfo;
import ru.badmintonlab.core.domain.Discipline;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
    void cardUsesFioFirstRatingsAndNoFooter() {
        PlayerCard card = new PlayerCard(
                18499L,
                "Rocket",
                "Иванов Иван",
                "Москва",
                List.of(new RatingLine(Discipline.S, new BigDecimal("320.0")),
                        new RatingLine(Discipline.D, new BigDecimal("535.0"))),
                new LastTournamentInfo("Кубок LAB", LocalDate.of(2026, 6, 15), (short) 2, "2-е место"),
                LocalDate.of(2026, 7, 20));

        String text = texts.card(card);

        assertTrue(text.contains("<b>Иванов Иван</b>"), text);
        assertTrue(text.contains("Rocket</a>"), text);
        assertTrue(text.contains("Москва"), text);
        assertTrue(text.contains("🆂"), text);
        assertTrue(text.contains("<code>320</code>"), text);
        assertTrue(text.contains("🅳"), text);
        assertTrue(text.contains("<code>535</code>"), text);
        assertTrue(text.contains("2-е место"), text);
        assertFalse(text.contains("Данные на"), text);
    }

    @Test
    void searchResultsHeaderPluralizes() {
        assertTrue(texts.searchResultsHeader(1).contains("1 игрока"));
        assertTrue(texts.searchResultsHeader(3).contains("3 игрока"));
        assertTrue(texts.searchResultsHeader(5).contains("5 игроков"));
    }

    @Test
    void rivalsRendersRowsAndPaginationInfo() {
        RivalsPage page = new RivalsPage(
                1L,
                "Иванов Иван",
                null,
                List.of(new RivalRow(2L, "Foe", "Петров Пётр", "Москва", 3, 1)),
                0,
                8,
                1,
                List.of(Discipline.MD));

        String text = texts.rivals(page);

        assertTrue(text.contains("Соперники"), text);
        assertTrue(text.contains("Все"), text);
        assertTrue(text.contains("Петров Пётр"), text);
        assertTrue(text.contains("3–1 (75%)"), text);
        assertFalse(text.contains("Москва"), text);
    }

    @Test
    void rivalsHandlesEmptyDiscipline() {
        RivalsPage page = new RivalsPage(1L, "Иванов", Discipline.WD, List.of(), 0, 8, 0, List.of());
        assertTrue(texts.rivals(page).contains("Встреч в разряде WD нет"));
    }
}
