package ru.badmintonlab.bot.view;

import org.junit.jupiter.api.Test;
import ru.badmintonlab.bot.model.LastTournamentInfo;
import ru.badmintonlab.bot.model.PartnerCandidateRow;
import ru.badmintonlab.bot.model.PartnerPickPage;
import ru.badmintonlab.bot.model.PlayerCard;
import ru.badmintonlab.bot.model.PlayerSearchResult;
import ru.badmintonlab.bot.model.RatingLine;
import ru.badmintonlab.bot.model.RivalRow;
import ru.badmintonlab.bot.model.RivalsPage;
import ru.badmintonlab.core.domain.Discipline;
import ru.badmintonlab.core.domain.PairCompositionType;
import ru.badmintonlab.core.metrics.StabilityLevel;

import java.math.BigDecimal;
import java.time.Instant;
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
                1.2,
                null,
                null,
                new LastTournamentInfo("Кубок LAB", LocalDate.of(2026, 6, 15), (short) 2, "2-е место"),
                LocalDate.of(2026, 7, 20));

        String text = texts.card(card);

        assertTrue(text.contains("<b>Иванов Иван</b>"), text);
        assertTrue(text.contains("Rocket</a>"), text);
        assertTrue(text.contains("Москва"), text);
        assertTrue(text.contains(MessageEmoji.SINGLE + "  320"), text);
        assertFalse(text.contains("<code>"), text);
        assertTrue(text.contains(MessageEmoji.DOUBLE + "  535"), text);
        assertTrue(text.contains("Форма  " + MessageEmoji.FORM + "  +1.2"), text);
        assertTrue(text.contains("2-е место"), text);
        assertFalse(text.contains("Данные на"), text);
    }

    @Test
    void cardRendersStabilityWithLabelAndZoneEmoji() {
        PlayerCard card = new PlayerCard(
                1L, "Rocket", "Иванов Иван", "Москва", List.of(), 1.2,
                StabilityLevel.STABLE.emoji(), null, null, null);

        String text = texts.card(card);

        assertTrue(text.contains("Форма  " + MessageEmoji.FORM + "  +1.2"), text);
        assertTrue(text.contains("Стабильность  " + StabilityLevel.STABLE.emoji()), text);
    }

    @Test
    void cardRendersGameAccentOnSeparateLines() {
        var accent = new ru.badmintonlab.core.metrics.GameAccentResult(
                ru.badmintonlab.core.domain.PairCompositionType.XD,
                0.58,
                14,
                ru.badmintonlab.core.domain.PairCompositionType.WD,
                2.3,
                0.78);
        PlayerCard card = new PlayerCard(
                1L, "Rocket", "Иванов Иван", "Москва", List.of(), null, null, accent, null, null);

        String text = texts.card(card);

        assertTrue(text.contains("<b>Игровой акцент</b> " + MessageEmoji.GAME_ACCENT_PREFERENCE), text);
        assertTrue(text.contains("Микст\n14 игр за полгода"), text);
        assertTrue(text.contains("<b>Рекомендуемая категория</b> " + MessageEmoji.RECOMMENDED_CATEGORY), text);
        assertTrue(text.contains("Женская пара"), text);
    }

    @Test
    void cardShowsUnknownPlaceholderForMissingMetrics() {
        PlayerCard card = new PlayerCard(
                1L, "Rocket", "Иванов Иван", "Москва", List.of(), null, null, null, null, null);

        String text = texts.card(card);

        assertTrue(text.contains("<b>Рейтинги</b>\n" + MessageEmoji.UNKNOWN), text);
        assertTrue(text.contains("Форма  " + MessageEmoji.FORM + "  " + MessageEmoji.UNKNOWN), text);
        assertTrue(text.contains("Стабильность  " + MessageEmoji.UNKNOWN), text);
        assertTrue(text.contains("<b>Игровой акцент</b> " + MessageEmoji.GAME_ACCENT_PREFERENCE + "\n" + MessageEmoji.UNKNOWN), text);
        assertTrue(text.contains("<b>Рекомендуемая категория</b> " + MessageEmoji.RECOMMENDED_CATEGORY + "\n" + MessageEmoji.UNKNOWN), text);
        assertTrue(text.contains("<b>Последний турнир</b>\n" + MessageEmoji.UNKNOWN), text);
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
        assertFalse(text.contains("Foe"), text);
        assertTrue(text.contains(MessageEmoji.WIN + "3  " + MessageEmoji.LOSS + "1  "
                + MessageEmoji.WIN_RATE + "75%"), text);
        assertFalse(text.contains("Москва"), text);
    }

    @Test
    void rivalsAlignsStatsInColumn() {
        RivalsPage page = new RivalsPage(
                1L,
                "Иванов Иван",
                null,
                List.of(
                        new RivalRow(2L, null, "Петров Пётр", null, 3, 1),
                        new RivalRow(3L, null, "Сидорова Анна", null, 2, 2)),
                0,
                8,
                1,
                List.of(Discipline.MD));

        String text = texts.rivals(page);

        assertTrue(text.contains("<pre>"), text);
        int petrov = text.indexOf("Петров");
        int sidorova = text.indexOf("Сидорова");
        int winPetrov = text.indexOf(MessageEmoji.WIN, petrov);
        int winSidorova = text.indexOf(MessageEmoji.WIN, sidorova);
        assertEquals(winPetrov - petrov, winSidorova - sidorova);
    }

    @Test
    void rivalsHandlesEmptyDiscipline() {
        RivalsPage page = new RivalsPage(1L, "Иванов", Discipline.WD, List.of(), 0, 8, 0, List.of());
        assertTrue(texts.rivals(page).contains("Встреч в разряде WD нет"));
    }

    @Test
    void partnerPickRowShowsFioRatingsAndSuitability() {
        PartnerCandidateRow row = new PartnerCandidateRow(
                1L,
                "Петров Пётр Сергеевич",
                "Petrov",
                "Москва",
                395.0,
                388.0,
                62.0,
                true,
                true,
                true,
                true,
                PairCompositionType.XD);
        PartnerPickPage page = new PartnerPickPage(
                12836L,
                "Space XDC",
                Instant.parse("2026-07-25T10:00:00Z"),
                new BigDecimal("650"),
                new BigDecimal("700"),
                18499L,
                "Крупская Ольга (Olya_fox)",
                380.0,
                List.of(row),
                List.of());

        String text = texts.partnerPick(page);

        assertTrue(text.contains("<b>Петров Пётр Сергеевич</b>"), text);
        assertTrue(text.contains(MessageEmoji.DOUBLE + " 395"), text);
        assertTrue(text.contains("ср. 388"), text);
        assertTrue(text.contains("Макс. игрок: 700"), text);
        assertTrue(text.contains("подходимость: хорошая (62%)"), text);
        assertFalse(text.contains("Petrov"), text);
    }
}
