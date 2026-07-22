package ru.badmintonlab.bot.view;

import org.junit.jupiter.api.Test;
import ru.badmintonlab.bot.model.PlayerSearchResult;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SearchButtonLabelTest {

    @Test
    void fullFormatWithCityAndRatings() {
        var r = new PlayerSearchResult(
                1L, "Rocket", "Иванов", "Иван", "Олегович", "Москва",
                new BigDecimal("320"), new BigDecimal("535"));
        String label = SearchButtonLabel.format(r);
        assertEquals("Иванов Иван Олегович (Москва) 🆂 320 🅳 535", label);
    }

    @Test
    void truncatesPatronymicFirst() {
        var r = new PlayerSearchResult(
                1L, "nick", "Константинопольский", "Станислав", "Александрович", "Москва",
                new BigDecimal("300"), new BigDecimal("400"));
        String label = SearchButtonLabel.format(r);
        assertTrue(label.contains("Станислав"));
        assertTrue(label.length() <= SearchButtonLabel.TELEGRAM_BUTTON_LIMIT);
    }

    @Test
    void usesNickWhenNoFullName() {
        var r = new PlayerSearchResult(
                1L, "Rocket", null, null, null, "Москва",
                null, new BigDecimal("500"));
        assertEquals("Rocket (Москва) 🅳 500", SearchButtonLabel.format(r));
    }
}
