package ru.badmintonlab.parser;

import org.jsoup.nodes.Document;
import ru.badmintonlab.parser.model.Discipline;
import ru.badmintonlab.parser.support.HtmlFixtures;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RivalsParserTest {

    private final RivalsParser parser = new RivalsParser();

    @Test
    void returnsEmptyListWhenRivalsTableHasNoRows() {
        Document doc = HtmlFixtures.load("rivals-18499.html");

        var rivals = parser.parse(doc, Discipline.D);

        assertTrue(rivals.isEmpty());
    }

    @Test
    void parsesRivalsFromSupplementaryFixture() {
        Document doc = HtmlFixtures.load("rivals-19080-d.html");

        var rivals = parser.parse(doc, Discipline.D);

        assertFalse(rivals.isEmpty());
        assertTrue(rivals.stream().anyMatch(r -> r.opponentId() == 6299L && r.wins() == 3));
    }
}
