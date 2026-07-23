package ru.badmintonlab.parser;

import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Test;
import ru.badmintonlab.parser.support.HtmlFixtures;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerDirectoryParserTest {

    private final PlayerDirectoryParser parser = new PlayerDirectoryParser();

    @Test
    void parsesMaleDirectoryFirstPage() {
        Document doc = HtmlFixtures.load("players-directory-sex-m-r77.html");

        assertEquals("42", parser.extractSessionToken(doc).orElseThrow());

        var entries = parser.parse(doc);
        assertEquals(3, entries.size());
        assertEquals(21626L, entries.get(0).id());
        assertEquals("Vladimalkov", entries.get(0).nick().orElseThrow());
        assertTrue(entries.get(1).nick().isEmpty());
        assertEquals(121L, entries.get(2).id());
    }

    @Test
    void parsesFemaleDirectoryFirstPage() {
        Document doc = HtmlFixtures.load("players-directory-sex-f-r77.html");

        var entries = parser.parse(doc);
        assertEquals(2, entries.size());
        assertEquals(19080L, entries.get(0).id());
        assertEquals("AnnIvanova", entries.get(0).nick().orElseThrow());
    }

    @Test
    void parsesAjaxFragment() throws Exception {
        String html;
        try (InputStream in = getClass().getClassLoader().getResourceAsStream("html/players-directory-ajax-page2.html")) {
            html = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }

        var entries = parser.parseFragment(html);
        assertEquals(2, entries.size());
        assertEquals(24245L, entries.get(0).id());
        assertEquals("Ahyo_97", entries.get(0).nick().orElseThrow());
        assertEquals(25124L, entries.get(1).id());
    }
}
