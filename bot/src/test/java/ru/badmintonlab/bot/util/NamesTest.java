package ru.badmintonlab.bot.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NamesTest {

    @Test
    void shortNameFromFullName() {
        assertEquals("Иванов И.", Names.shortName("Иванов Иван"));
    }

    @Test
    void shortNameSingleToken() {
        assertEquals("Rocket", Names.shortName("Rocket"));
    }
}
