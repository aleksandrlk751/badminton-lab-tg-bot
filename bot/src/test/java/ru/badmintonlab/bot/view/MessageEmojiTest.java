package ru.badmintonlab.bot.view;

import org.junit.jupiter.api.Test;
import ru.badmintonlab.core.domain.Discipline;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MessageEmojiTest {

    @Test
    void ratingUsesSingleAndDoubleIcons() {
        assertEquals(MessageEmoji.SINGLE, MessageEmoji.rating(Discipline.S));
        assertEquals(MessageEmoji.DOUBLE, MessageEmoji.rating(Discipline.D));
    }
}
