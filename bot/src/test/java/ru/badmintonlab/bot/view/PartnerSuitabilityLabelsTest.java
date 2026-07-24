package ru.badmintonlab.bot.view;

import org.junit.jupiter.api.Test;
import ru.badmintonlab.bot.model.PartnerCandidateRow;
import ru.badmintonlab.bot.model.PartnerPickPage;
import ru.badmintonlab.core.domain.PairCompositionType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PartnerSuitabilityLabelsTest {

    @Test
    void tierThresholds() {
        assertEquals("низкая", PartnerSuitabilityLabels.tier(29));
        assertEquals("средняя", PartnerSuitabilityLabels.tier(30));
        assertEquals("хорошая", PartnerSuitabilityLabels.tier(50));
        assertEquals("высокая", PartnerSuitabilityLabels.tier(75));
    }

    @Test
    void lineFormatsPercentAndTier() {
        assertEquals("подходимость: хорошая (62%)", PartnerSuitabilityLabels.line(62.4));
    }
}
