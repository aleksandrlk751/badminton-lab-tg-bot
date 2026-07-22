package ru.badmintonlab.bot.view;

import org.junit.jupiter.api.Test;
import ru.badmintonlab.core.domain.Discipline;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class CallbackDataTest {

    @Test
    void cardRoundTrip() {
        String data = CallbackData.card(18499L);
        assertEquals("card:18499", data);
        assertArrayEquals(new String[]{"card", "18499"}, CallbackData.parse(data));
    }

    @Test
    void rivalsDefaultRoundTrip() {
        assertEquals("rv:42", CallbackData.rivalsDefault(42L));
    }

    @Test
    void rivalsPageRoundTrip() {
        String data = CallbackData.rivalsPage(42L, Discipline.XD, 3);
        assertEquals("rvp:42:XD:3", data);
        String[] parts = CallbackData.parse(data);
        assertEquals("rvp", parts[0]);
        assertEquals(42L, Long.parseLong(parts[1]));
        assertEquals(Discipline.XD, Discipline.valueOf(parts[2]));
        assertEquals(3, Integer.parseInt(parts[3]));
    }

    @Test
    void parseHandlesNull() {
        assertEquals(0, CallbackData.parse(null).length);
    }
}
