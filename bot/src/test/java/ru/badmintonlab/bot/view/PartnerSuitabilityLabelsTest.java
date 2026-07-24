package ru.badmintonlab.bot.view;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PartnerSuitabilityLabelsTest {

    @Test
    void lineFormatsPercentOnly() {
        assertEquals(MessageEmoji.PARTNER_SUITABILITY + " 62%", PartnerSuitabilityLabels.line(62.4));
    }

    @Test
    void percentClampsAndRounds() {
        assertEquals(100, PartnerSuitabilityLabels.percent(150));
        assertEquals(0, PartnerSuitabilityLabels.percent(-5));
        assertEquals(75, PartnerSuitabilityLabels.percent(74.6));
    }
}
