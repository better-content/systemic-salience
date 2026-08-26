package com.bettercontent.systemicsalience.presentation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class NutritionPresentationTest {
    @Test
    void tiersUseTheServerThresholdContract() {
        assertEquals(NutritionTier.BUILDING, NutritionTier.of(.49, .50, .75, .90));
        assertEquals(NutritionTier.SUPPORTED, NutritionTier.of(.50, .50, .75, .90));
        assertEquals(NutritionTier.PREPARED, NutritionTier.of(.75, .50, .75, .90));
        assertEquals(NutritionTier.FEAST, NutritionTier.of(.90, .50, .75, .90));
    }

    @Test
    void higherSugarNeverExtendsAStoredNutrientTier() {
        int baseline = NutritionEstimates.nutrientSeconds(.95, .90, 0.0);
        int amplified = NutritionEstimates.nutrientSeconds(.95, .90, .70);
        assertTrue(amplified < baseline);
    }

    @Test
    void aspectOrderAndDietIdentitiesAreExact() {
        assertEquals(8, AspectIdentity.values().length);
        assertEquals(AspectIdentity.IMPACT, AspectIdentity.fromGroupName("diet:PROTEINS"));
        assertEquals(AspectIdentity.RENEWAL, AspectIdentity.fromGroupName("Dairy"));
        for (int index = 0; index < AspectIdentity.values().length; index++) {
            assertEquals(index, AspectIdentity.values()[index].icon);
        }
    }
}
