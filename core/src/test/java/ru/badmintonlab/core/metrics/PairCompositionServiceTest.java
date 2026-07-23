package ru.badmintonlab.core.metrics;

import org.junit.jupiter.api.Test;
import ru.badmintonlab.core.domain.PairCompositionType;
import ru.badmintonlab.core.domain.PlayerSex;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PairCompositionServiceTest {

    private final PairCompositionService service = new PairCompositionService();

    @Test
    void mensDoubles() {
        assertEquals(PairCompositionType.MD, service.resolve(PlayerSex.M, PlayerSex.M));
    }

    @Test
    void womensDoubles() {
        assertEquals(PairCompositionType.WD, service.resolve(PlayerSex.F, PlayerSex.F));
    }

    @Test
    void mixedDoublesBothOrders() {
        assertEquals(PairCompositionType.XD, service.resolve(PlayerSex.M, PlayerSex.F));
        assertEquals(PairCompositionType.XD, service.resolve(PlayerSex.F, PlayerSex.M));
    }

    @Test
    void unknownWhenSexMissing() {
        assertEquals(PairCompositionType.UNKNOWN, service.resolve(null, PlayerSex.M));
        assertEquals(PairCompositionType.UNKNOWN, service.resolve(PlayerSex.F, null));
        assertEquals(PairCompositionType.UNKNOWN, service.resolve(null, null));
    }
}
