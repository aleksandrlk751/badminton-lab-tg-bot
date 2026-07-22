package ru.badmintonlab.worker.snapshot;

import org.junit.jupiter.api.Test;
import ru.badmintonlab.core.domain.Discipline;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SnapshotSupportTest {

    @Test
    void parsesMatchBalance() {
        int[] wl = SnapshotSupport.parseBalance("5 (5-0)").orElseThrow();
        assertEquals(5, wl[0]);
        assertEquals(0, wl[1]);
    }

    @Test
    void parsesSetsBalance() {
        int[] wl = SnapshotSupport.parseBalance("12 (10-2)").orElseThrow();
        assertEquals(10, wl[0]);
        assertEquals(2, wl[1]);
    }

    @Test
    void returnsEmptyForUnparseableBalance() {
        assertTrue(SnapshotSupport.parseBalance("нет данных").isEmpty());
        assertTrue(SnapshotSupport.parseBalance(null).isEmpty());
    }

    @Test
    void infersDisciplineFromTournamentPage() {
        assertEquals(Discipline.D,
                SnapshotSupport.inferDisciplineFromPage(
                        ru.badmintonlab.parser.support.HtmlFixtures.load("tournament-completed-12125.html")));
    }

    @Test
    void mapsParserDisciplineToCore() {
        assertEquals(Discipline.WD, SnapshotSupport.toCore(ru.badmintonlab.parser.model.Discipline.WD));
        assertEquals(Discipline.XD, SnapshotSupport.toCore(ru.badmintonlab.parser.model.Discipline.XD));
    }

    @Test
    void convertsMoscowLocalTimeToInstant() {
        // 14.06.2026 12:00 МСК (UTC+3) → 09:00 UTC
        LocalDateTime local = LocalDateTime.of(2026, 6, 14, 12, 0);
        var instant = SnapshotSupport.toInstant(local);
        assertEquals(LocalDateTime.of(2026, 6, 14, 9, 0).toInstant(ZoneOffset.UTC), instant);
    }
}
