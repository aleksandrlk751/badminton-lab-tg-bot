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
        var historyD = profile.ratingHistories().get(Discipline.D);
        assertFalse(historyD.isEmpty());
        assertEquals(new BigDecimal("535"), historyD.get(historyD.size() - 1).rating());
    }

    @Test
    void parsesSingleAndDoubleRatingsFromTabsAndHeader() {
        Document doc = HtmlFixtures.load("player-4148.html");

        var profile = parser.parse(doc);

        assertEquals(4148L, profile.id());
        assertEquals("gulena_badmintona", profile.nick());
        assertEquals(new BigDecimal("379"), profile.ratings().get(Discipline.S));
        assertEquals(new BigDecimal("352"), profile.ratings().get(Discipline.D));
    }

    @Test
    void usesHeaderRatingWhenTabDfnsAreEmpty() {
        String html = """
                <ol class="breadcrumbs">
                  <li><a href="https://badminton4u.ru/players/1">x</a></li>
                </ol>
                <section class="player-info">
                  <h3>nick<dfn></dfn><dfn>536</dfn></h3>
                  <h1>Test Player</h1>
                  <ul id="tabs"><li class="act" data-tab="rat_d"><b>double</b> <dfn></dfn></li></ul>
                </section>
                """;
        Document doc = org.jsoup.Jsoup.parse(html, "https://badminton4u.ru/players/1");

        var profile = parser.parse(doc);

        assertEquals(new BigDecimal("536"), profile.ratings().get(Discipline.D));
    }
}
