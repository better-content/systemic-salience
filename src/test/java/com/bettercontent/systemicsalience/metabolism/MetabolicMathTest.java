package com.bettercontent.systemicsalience.metabolism;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MetabolicMathTest {
    @Test
    void nutritionDecayRisesExponentiallyWithFullness() {
        double atQuarter = MetabolicMath.nutrientDecayPerTick(0.25, 0.0);
        double atHalf = MetabolicMath.nutrientDecayPerTick(0.50, 0.0);
        double atFull = MetabolicMath.nutrientDecayPerTick(1.00, 0.0);

        assertEquals(Math.exp(-1.0), atQuarter / atHalf, 1.0e-9);
        assertEquals(Math.exp(2.0), atFull / atHalf, 1.0e-9);
    }

    @Test
    void sugarAmplifiesEffectsAndAcceleratesDecay() {
        assertEquals(1.25, MetabolicMath.effectiveNutrient(1.0, 1.0, 0.0), 0.25);
        assertEquals(1.5, MetabolicMath.thresholdPotency(1.0), 1.0e-9);
        assertEquals(5.0, MetabolicMath.nutrientDecayMultiplier(1.0), 1.0e-9);
        assertEquals(0.65, MetabolicMath.cooldownMultiplier(1.0, 0.0), 1.0e-9);
    }

    @Test
    void debtSuppressesNutritionAndExtendsCooldownsAfterSugarFalls() {
        assertEquals(0.6, MetabolicMath.effectiveNutrient(1.0, 0.0, 1.0), 1.0e-9);
        assertEquals(2.0, MetabolicMath.cooldownMultiplier(0.0, 1.0), 1.0e-9);
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
}
