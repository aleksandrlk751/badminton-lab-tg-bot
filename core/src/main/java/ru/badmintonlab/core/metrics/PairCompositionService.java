package ru.badmintonlab.core.metrics;

import org.springframework.stereotype.Service;
import ru.badmintonlab.core.domain.PairCompositionType;
import ru.badmintonlab.core.domain.PlayerSex;

/**
 * Определяет тип пары по полу двух игроков.
 * Справочник пола — {@code player.sex} (этап 5); при {@code null} — {@link PairCompositionType#UNKNOWN}.
 */
@Service
public class PairCompositionService {

    public PairCompositionType resolve(PlayerSex sexA, PlayerSex sexB) {
        if (sexA == null || sexB == null) {
            return PairCompositionType.UNKNOWN;
        }
        if (sexA == PlayerSex.M && sexB == PlayerSex.M) {
            return PairCompositionType.MD;
        }
        if (sexA == PlayerSex.F && sexB == PlayerSex.F) {
            return PairCompositionType.WD;
        }
        return PairCompositionType.XD;
    }
}
