package com.bettercontent.systemicsalience.metabolism;

import com.bettercontent.systemicsalience.config.SalienceConfig;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MetabolicMathTest {
    @Test
    void ordinaryNutritionDecayRemainsDietOwned() {
        assertFalse(hasPublicMethod(MetabolicMath.class, "nutrientDecayPerTick"));
        assertFalse(hasPublicMethod(MetabolicMath.class, "baselineNutrientDecayPerTick"));
        assertFalse(hasPublicMethod(MetabolicMath.class, "nutrientDecayMultiplier"));
        assertFalse(hasPublicField(MetabolicMath.class, "BASE_NUTRIENT_DECAY_PER_MINUTE"));
        assertFalse(hasPublicField(SalienceConfig.class, "BASE_DECAY_PER_MINUTE"));
        assertFalse(hasPublicField(SalienceConfig.class, "DECAY_EXPONENT"));
        assertTrue(hasPublicMethod(com.bettercontent.systemicsalience.nutrition.DietBridge.class, "snapshot"));
    }

    @Test
    void sugarOwnsTempoWithoutAmplifyingOtherIdentities() {
        assertEquals(1.0, MetabolicMath.effectiveNutrient(1.0, 1.0, 0.0), 1.0e-9);
        assertEquals(1.0, MetabolicMath.thresholdPotency(1.0), 1.0e-9);
        assertEquals(0.65, MetabolicMath.cooldownMultiplier(1.0, 0.0), 1.0e-9);
    }

    @Test
    void debtSlowsTempoWithoutSuppressingOtherIdentities() {
        assertEquals(1.0, MetabolicMath.effectiveNutrient(1.0, 0.0, 1.0), 1.0e-9);
        assertEquals(1.5, MetabolicMath.cooldownMultiplier(0.0, 1.0), 1.0e-9);
        assertEquals(1.0, MetabolicMath.tickDebt(1.0, 0.25), 1.0e-9);
        assertTrue(MetabolicMath.tickDebt(1.0, 0.24) < 1.0);
    }

    @Test
    void alcoholHasMidpointPeakAndBrutalTopEnd() {
        assertEquals(1.0, MetabolicMath.alcoholPositive(0.5), 1.0e-9);
        assertEquals(0.0, MetabolicMath.alcoholPositive(0.85), 1.0e-9);
        assertEquals(1.0, MetabolicMath.alcoholImpairment(1.0), 1.0e-9);
        assertTrue(MetabolicMath.alcoholImpairment(0.9) > MetabolicMath.alcoholImpairment(0.5));
    }

    @Test
    void halfLivesMatchTheDesignContract() {
        double sugar = 1.0;
        for (int i = 0; i < MetabolicMath.SUGAR_HALF_LIFE_TICKS; i++) sugar = MetabolicMath.tickSugar(sugar);
        assertEquals(0.5, sugar, 1.0e-8);

        double debt = 1.0;
        for (int i = 0; i < MetabolicMath.DEBT_HALF_LIFE_TICKS; i++) debt = MetabolicMath.tickDebt(debt, 0.0);
        assertEquals(0.5, debt, 1.0e-8);
    }

    private static boolean hasPublicMethod(Class<?> type, String name) {
        for (Method method : type.getMethods()) {
            if (method.getName().equals(name)) return true;
        }
        return false;
    }

    private static boolean hasPublicField(Class<?> type, String name) {
        for (Field field : type.getFields()) {
            if (field.getName().equals(name)) return true;
        }
        return false;
    }
}
