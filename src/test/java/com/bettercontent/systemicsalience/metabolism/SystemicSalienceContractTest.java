package com.bettercontent.systemicsalience.metabolism;

import com.bettercontent.systemicsalience.config.SalienceConfig;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SystemicSalienceContractTest {
    @Test
    void allEightNutritionIdentitiesArePresentedCategorically() throws IOException {
        String language;
        try (var stream = getClass().getResourceAsStream("/assets/systemic_salience/lang/en_us.json")) {
            assertTrue(stream != null);
            language = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
        for (String group : new String[]{"Fruits", "Grains", "Proteins", "Fats", "Vegetables", "Dairy", "Sugar", "Alcohol"}) {
            assertTrue(language.contains(group), group);
        }
        assertTrue(language.contains("Alcohol · best near center"));
        assertTrue(language.contains("Composure"));
        assertTrue(language.contains("Impairment"));
    }

    @Test
    void configuredThresholdsPreservePreparationOrdering() {
        assertTrue(SalienceConfig.ORDINARY_THRESHOLD.getDefault() < SalienceConfig.PREPARED_THRESHOLD.getDefault());
        assertTrue(SalienceConfig.PREPARED_THRESHOLD.getDefault() < SalienceConfig.FEAST_THRESHOLD.getDefault());
        assertTrue(SalienceConfig.FEAST_THRESHOLD.getDefault() < 1.0);
    }
}
