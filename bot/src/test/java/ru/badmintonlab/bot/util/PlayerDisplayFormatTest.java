package ru.badmintonlab.bot.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PlayerDisplayFormatTest {

    @Test
    void rivalsRowLabelShowsFioOnly() {
        assertEquals("Петров Пётр", PlayerDisplayFormat.rivalsRowLabel("Петров Пётр", "Foe", 2L));
    }

    @Test
    void rivalsRowLabelWithoutFioUsesNick() {
        assertEquals("Rocket", PlayerDisplayFormat.rivalsRowLabel("", "Rocket", 1L));
    }

    @Test
    void rivalsRowLabelWithoutNickUsesId() {
        assertEquals("42", PlayerDisplayFormat.rivalsRowLabel(null, null, 42L));
    }

    @Test
    void profileLinkLabelPrefersNick() {
        assertEquals("Rocket", PlayerDisplayFormat.profileLinkLabel("Rocket", 99L));
        assertEquals("99", PlayerDisplayFormat.profileLinkLabel(null, 99L));
    }
}
