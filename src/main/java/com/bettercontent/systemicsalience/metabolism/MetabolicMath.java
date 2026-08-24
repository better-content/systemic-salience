package com.bettercontent.systemicsalience.metabolism;

import com.bettercontent.systemicsalience.config.SalienceConfig;

public final class MetabolicMath {
    public static final double BASE_NUTRIENT_DECAY_PER_MINUTE = 0.004;
    public static final int SUGAR_HALF_LIFE_TICKS = 4 * 60 * 20;
    public static final int DEBT_HALF_LIFE_TICKS = 10 * 60 * 20;
    public static final int ALCOHOL_CLEAR_TICKS = 20 * 60 * 20;

    private MetabolicMath() {}

    public static double nutrientDecayPerTick(double nutrient, double sugar) {
        double baseline = configured(SalienceConfig.BASE_DECAY_PER_MINUTE, BASE_NUTRIENT_DECAY_PER_MINUTE)
                * Math.exp(configured(SalienceConfig.DECAY_EXPONENT, 4.0) * (clamp01(nutrient) - 0.5))
                / (60.0 * 20.0);
        return baseline * nutrientDecayMultiplier(sugar);
    }

    public static double baselineNutrientDecayPerTick(double nutrient) {
        return nutrientDecayPerTick(nutrient, 0.0);
    }

    public static double nutrientDecayMultiplier(double sugar) {
        double s = clamp01(sugar);
        return 1.0 + configured(SalienceConfig.SUGAR_DECAY_SCALE, 4.0) * s * s;
    }

    public static double thresholdPotency(double sugar) {
        double s = clamp01(sugar);
        return 1.0 + configured(SalienceConfig.SUGAR_POTENCY_SCALE, 0.5) * s * s;
    }

    public static double cooldownMultiplier(double sugar, double debt) {
        double s = clamp01(sugar);
        return Math.max(0.1, (1.0 - 0.35 * s * s) * (1.0 + clamp01(debt)));
    }

    public static int adjustedCooldown(int baseTicks, double sugar, double debt) {
        return Math.max(1, (int) Math.round(baseTicks * cooldownMultiplier(sugar, debt)));
    }

    public static double effectiveNutrient(double nutrient, double sugar, double debt) {
        double s = clamp01(sugar);
        double amplified = clamp01(nutrient) * (1.0 + configured(SalienceConfig.NUTRIENT_AMPLIFICATION_SCALE, 0.25) * s * s);
        double suppressed = amplified * (1.0 - configured(SalienceConfig.DEBT_SUPPRESSION_SCALE, 0.4) * clamp01(debt));
        return clamp01(suppressed);
    }

    public static double alcoholPositive(double alcohol) {
        double a = clamp01(alcohol);
        return a >= configured(SalienceConfig.ALCOHOL_POSITIVE_CUTOFF, 0.85) ? 0.0 : 4.0 * a * (1.0 - a);
    }

    public static double alcoholImpairment(double alcohol) {
        double a = clamp01(alcohol);
        return a * a * a;
    }

    public static double tickSugar(double sugar) {
        int halfLife = ticks(configured(SalienceConfig.SUGAR_HALF_LIFE_MINUTES, 4.0));
        return clamp01(sugar * Math.pow(0.5, 1.0 / halfLife));
    }

    public static double tickAlcohol(double alcohol) {
        return clamp01(alcohol - 1.0 / ticks(configured(SalienceConfig.ALCOHOL_CLEAR_MINUTES, 20.0)));
    }

    public static double tickDebt(double debt, double sugar) {
        if (sugar >= configured(SalienceConfig.SUGAR_DEBT_GATE, 0.25)) return clamp01(debt);
        int halfLife = ticks(configured(SalienceConfig.DEBT_HALF_LIFE_MINUTES, 10.0));
        return clamp01(debt * Math.pow(0.5, 1.0 / halfLife));
    }

    public static double clamp01(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }

    private static int ticks(double minutes) {
        return Math.max(1, (int) Math.round(minutes * 60.0 * 20.0));
    }

    private static double configured(net.minecraftforge.common.ForgeConfigSpec.DoubleValue value, double fallback) {
        return SalienceConfig.value(value, fallback);
    }
}
