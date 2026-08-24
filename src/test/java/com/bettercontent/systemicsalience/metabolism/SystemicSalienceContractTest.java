package com.bettercontent.systemicsalience.metabolism;

import com.bettercontent.systemicsalience.config.SalienceConfig;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SystemicSalienceContractTest {
    @Test
    void ordinaryFoodsAreNotPresentedAsSingleAspectBuckets() throws IOException {
        String language;
        try (var stream = getClass().getResourceAsStream("/assets/systemic_salience/lang/en_us.json")) {
            assertTrue(stream != null);
            language = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
        assertFalse(language.contains("Fruits ·"));
        assertFalse(language.contains("Grains ·"));
        assertFalse(language.contains("Proteins ·"));
        assertFalse(language.contains("Vegetables ·"));
        assertTrue(language.contains("Alcohol · best near center"));
        assertTrue(language.contains("Composure"));
        assertTrue(language.contains("Impairment"));
    }

    @Test
    void configuredThresholdsPreservePreparationOrdering() {
        assertTrue(SalienceConfig.FRUIT_DRINK.getDefault() < SalienceConfig.FRUIT_SECOND_WIND.getDefault());
        assertTrue(SalienceConfig.FRUIT_SECOND_WIND.getDefault() < SalienceConfig.FRUIT_SPRINT.getDefault());
        assertTrue(SalienceConfig.FRUIT_SPRINT.getDefault() < SalienceConfig.FRUIT_FEAST.getDefault());
        assertTrue(SalienceConfig.GRAIN_SUSTAINED_WORK.getDefault() < SalienceConfig.GRAIN_DURABILITY.getDefault());
        assertTrue(SalienceConfig.GRAIN_DURABILITY.getDefault() < SalienceConfig.GRAIN_LONG_HAUL.getDefault());
        assertTrue(SalienceConfig.PROTEIN_IMPACT.getDefault() < SalienceConfig.PROTEIN_BRACE.getDefault());
        assertTrue(SalienceConfig.VEGETABLE_RECENT_FOOD.getDefault() < SalienceConfig.VEGETABLE_EFFECT_RECOVERY.getDefault());
        assertTrue(SalienceConfig.VEGETABLE_EFFECT_RECOVERY.getDefault() < SalienceConfig.VEGETABLE_DRIFT.getDefault());
        assertTrue(SalienceConfig.VEGETABLE_DRIFT.getDefault() < SalienceConfig.VEGETABLE_EMERGENCY.getDefault());
    }
}
