package ru.badmintonlab.parser;

import org.jsoup.nodes.Document;
import ru.badmintonlab.parser.model.Discipline;
import ru.badmintonlab.parser.support.HtmlFixtures;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerProfileParserTest {

    private final PlayerProfileParser parser = new PlayerProfileParser();

    @Test
    void parsesReferencePlayerProfile() {
        Document doc = HtmlFixtures.load("player-18499.html");

        var profile = parser.parse(doc);

        assertEquals(18499L, profile.id());
        assertEquals("Olya_fox", profile.nick());
        assertEquals("Крупская Ольга Андреевна", profile.fullName());
        assertEquals("Красногорск", profile.city().orElseThrow());
        assertEquals("правая", profile.playingHand().orElseThrow());
        assertEquals(new BigDecimal("535"), profile.ratings().get(Discipline.D));
        assertFalse(profile.ratingHistory().isEmpty());
        assertEquals(new BigDecimal("535"), profile.ratingHistory().get(profile.ratingHistory().size() - 1).rating());
    }
}
