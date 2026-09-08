package com.bettercontent.systemicsalience.nutrition;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class NutritionThreadBoundaryTest {
    private static final double ORDINARY = 0.50;

    @Test
    void anyLowOrdinaryGroupMakesNutritionLow() {
        NutritionSnapshot lowDairy = new NutritionSnapshot(.75f, .75f, .75f, .75f, .75f, .49f);

        assertTrue(NutritionThreadBoundary.isNutritionLow(lowDairy, ORDINARY));
        assertFalse(NutritionThreadBoundary.isBalanced(lowDairy, ORDINARY));
    }

    @Test
    void allSixGroupsAtThresholdQualifyAsBalanced() {
        NutritionSnapshot boundary = new NutritionSnapshot(.50f, .50f, .50f, .50f, .50f, .50f);

        assertFalse(NutritionThreadBoundary.isNutritionLow(boundary, ORDINARY));
        assertTrue(NutritionThreadBoundary.isBalanced(boundary, ORDINARY));
    }

    @Test
    void recoveryMustCrossFromLowToAllSixBalanced() {
        NutritionSnapshot low = new NutritionSnapshot(.75f, .75f, .75f, .75f, .75f, .49f);
        NutritionSnapshot balanced = new NutritionSnapshot(.75f, .75f, .75f, .75f, .75f, .50f);

        assertTrue(NutritionThreadBoundary.isQualifyingRecovery(low, balanced, ORDINARY));
        assertFalse(NutritionThreadBoundary.isQualifyingRecovery(balanced, balanced, ORDINARY));
        assertFalse(NutritionThreadBoundary.isQualifyingRecovery(low, low, ORDINARY));
    }

    @Test
    void signalNamesMatchThePublishedThreadRoutes() {
        assertTrue(NutritionThreadBoundary.THREAD_ID.equals("hunger_is_not_nutrition"));
        assertTrue(NutritionThreadBoundary.WARNING_TYPE.equals("nutrition_warning"));
        assertTrue(NutritionThreadBoundary.WARNING_VALUE.equals("hunger_full_nutrition_low"));
        assertTrue(NutritionThreadBoundary.RECOVERED_TYPE.equals("nutrition_recovered"));
        assertTrue(NutritionThreadBoundary.RECOVERED_VALUE.equals("balanced_meal"));
    }
}
