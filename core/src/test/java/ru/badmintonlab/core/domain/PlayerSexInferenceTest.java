package ru.badmintonlab.core.domain;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class PlayerSexInferenceTest {

    @Test
    void infersFromDisciplines() {
        assertEquals(PlayerSex.M, PlayerSexInference.inferFromDisciplines(List.of(Discipline.MD)));
        assertEquals(PlayerSex.F, PlayerSexInference.inferFromDisciplines(List.of(Discipline.WD, Discipline.D)));
        assertNull(PlayerSexInference.inferFromDisciplines(List.of(Discipline.D, Discipline.XD)));
        assertNull(PlayerSexInference.inferFromDisciplines(List.of(Discipline.MD, Discipline.WD)));
    }

    @Test
    void infersFromCategoryCodes() {
        assertEquals(PlayerSex.F, PlayerSexInference.inferFromCategoryCodes(List.of("WDB", "WDA")));
        assertEquals(PlayerSex.M, PlayerSexInference.inferFromCategoryCodes(List.of("MDB", "MSA")));
        assertNull(PlayerSexInference.inferFromCategoryCodes(List.of("D", "DB+", "XDB")));
        assertNull(PlayerSexInference.inferFromCategoryCodes(List.of("WDB", "MDB")));
    }

    @Test
    void infersFromPatronymic() {
        assertEquals(PlayerSex.M, PlayerSexInference.inferFromPatronymic("Сергеевич"));
        assertEquals(PlayerSex.M, PlayerSexInference.inferFromPatronymic("Ильич"));
        assertEquals(PlayerSex.F, PlayerSexInference.inferFromPatronymic("Дмитриевна"));
        assertEquals(PlayerSex.F, PlayerSexInference.inferFromPatronymic("Алексанровна"));
        assertNull(PlayerSexInference.inferFromPatronymic(null));
    }

    @Test
    void infersFromName() {
        assertEquals(PlayerSex.M, PlayerSexInference.inferFromName("Дмитрий", "Ярошинский", "Александрович"));
        assertEquals(PlayerSex.F, PlayerSexInference.inferFromName("Анна", "Козлова", null));
        assertEquals(PlayerSex.F, PlayerSexInference.inferFromName(null, "Юлия", null));
        assertEquals(PlayerSex.M, PlayerSexInference.inferFromName("Даниэле", "Сордже", null));
        assertNull(PlayerSexInference.inferFromName("Вита", "Магнат", null));
        assertNull(PlayerSexInference.inferFromName(null, "36724", null));
        assertNull(PlayerSexInference.inferFromName("Иван", "Петров", "Ивановна"));
    }
}
