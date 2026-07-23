package ru.badmintonlab.parser;

import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Test;
import ru.badmintonlab.parser.model.Discipline;
import ru.badmintonlab.parser.model.PlayerProfileSexEvidence;
import ru.badmintonlab.parser.support.HtmlFixtures;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerProfileSexEvidenceParserTest {

    private final PlayerProfileSexEvidenceParser parser = new PlayerProfileSexEvidenceParser();

    @Test
    void extractsCategoryCodeFromTournamentLinkText() {
        assertEquals(Optional.of("WDB"), parser.extractCategoryCode("17.01.2026 13:30 отк БК Обнинск WDB"));
        assertEquals(Optional.of("WDC"), parser.extractCategoryCode("14.06.2026 12:00 550 Женская лига WDC"));
        assertEquals(Optional.of("MSA"), parser.extractCategoryCode("11:00 отк BC Maximum MSA"));
        assertTrue(parser.extractCategoryCode("14.12.2025 18:00 1000 MOSCOW OPEN DB+").isEmpty());
        assertTrue(parser.extractCategoryCode("09.08.2025 12:00 775 Space ВДНХ XDB").isEmpty());
    }

    @Test
    void parsesParticipationsFromPlayer4148Fixture() {
        Document doc = HtmlFixtures.load("player-4148.html");
        PlayerProfileSexEvidence evidence = parser.parse(doc);

        assertTrue(evidence.tournamentCategoryCodes().contains("WDC"));
        assertTrue(evidence.tournamentCategoryCodes().contains("WDE"));
        assertTrue(evidence.ratingDisciplines().contains(Discipline.D));
    }
}
