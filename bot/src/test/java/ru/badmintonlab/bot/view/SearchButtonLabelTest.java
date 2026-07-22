package ru.badmintonlab.bot.view;

import org.junit.jupiter.api.Test;
import ru.badmintonlab.bot.model.PlayerSearchResult;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SearchButtonLabelTest {

    @Test
    void fullFormatWithCityAndRatings() {
        var r = new PlayerSearchResult(
                1L, "Rocket", "Иванов", "Иван", "Олегович", "Москва",
                new BigDecimal("320"), new BigDecimal("535"));
        String label = SearchButtonLabel.format(r);
        assertEquals("Иванов Иван (Москва) " + MessageEmoji.SINGLE + " 320 "
                + MessageEmoji.DOUBLE + " 535", label);
    }

    @Test
    void truncatesCityWhenTooLong() {
        var r = new PlayerSearchResult(
                1L, "nick", "Константинопольский", "Станислав", "Александрович", "Москва",
                new BigDecimal("300"), new BigDecimal("400"));
        String label = SearchButtonLabel.format(r);
        assertTrue(label.contains("Станислав"));
        assertFalse(label.contains("Олегович"));
        assertTrue(label.length() <= SearchButtonLabel.TELEGRAM_BUTTON_LIMIT);
    }

    @Test
    void usesNickWhenNoFullName() {
        var r = new PlayerSearchResult(
                1L, "Rocket", null, null, null, "Москва",
                null, new BigDecimal("500"));
        assertEquals("Rocket (Москва) " + MessageEmoji.DOUBLE + " 500", SearchButtonLabel.format(r));
    }
}
