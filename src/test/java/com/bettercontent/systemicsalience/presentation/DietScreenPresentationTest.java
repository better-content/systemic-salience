package com.bettercontent.systemicsalience.presentation;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class DietScreenPresentationTest {
    @Test
    void packDefaultScaleFitsSixNutritionRowsWithoutScrolling() {
        DietScreenPresentation.Layout layout = DietScreenPresentation.layout(427, 240);
        int viewport = layout.contentHeight() - DietScreenPresentation.SECTION_HEADER_HEIGHT;

        assertTrue(layout.columnWidth() >= 180);
        assertTrue(layout.nutrientRowHeight() >= 22);
        assertTrue(layout.nutrientRowHeight() * 6 <= viewport);
        assertTrue(layout.footerTop() <= layout.panelY() + layout.panelHeight());
        assertFalse(layout.tooNarrow());
    }

    @Test
    void lowestNutritionTierUsesApprovedPlayerFacingName() {
        assertEquals("systemic_salience.ui.tier.undernourished",
                DietScreenPresentation.tierKey(NutritionTier.BUILDING));
    }

    @Test
    void metabolicLabelsMatchGameplayThresholds() {
        assertEquals("systemic_salience.ui.sugar.baseline",
                DietScreenPresentation.sugarStateKey(.10, .10));
        assertEquals("systemic_salience.ui.sugar.crash",
                DietScreenPresentation.sugarStateKey(.10, .25));
        assertEquals("systemic_salience.ui.sugar.tempo_one",
                DietScreenPresentation.sugarStateKey(.25, .80));
        assertEquals("systemic_salience.ui.sugar.tempo_two",
                DietScreenPresentation.sugarStateKey(.60, .80));
        assertEquals("systemic_salience.ui.alcohol.low",
                DietScreenPresentation.alcoholStateKey(.34));
        assertEquals("systemic_salience.ui.alcohol.composed",
                DietScreenPresentation.alcoholStateKey(.35));
        assertEquals("systemic_salience.ui.alcohol.impaired",
                DietScreenPresentation.alcoholStateKey(.651));
    }

    @Test
    void everyPresentationStateHasPlayerFacingCopy() throws IOException {
        String language;
        try (var stream = getClass().getResourceAsStream("/assets/systemic_salience/lang/en_us.json")) {
            assertTrue(stream != null);
            language = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
        for (String label : new String[]{"Undernourished", "Supported", "Prepared", "Feast",
                "Baseline", "Tempo I", "Tempo II", "Metabolic crash", "Composed", "Impaired"}) {
            assertTrue(language.contains(label), label);
        }
        assertFalse(language.contains("\"systemic_salience.ui.tier.undernourished\": \"Building\""));
    }
}
