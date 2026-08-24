package com.bettercontent.systemicsalience.config;

import net.minecraftforge.common.ForgeConfigSpec;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Server-owned balance surface. Defaults are the authored pack contract; worlds may tune without rebuilding the mod. */
public final class SalienceConfig {
    public static final List<String> DEFAULT_ALCOHOL_POTENCIES = List.of(
            "brewinandchewin:kombucha=0.08", "brewinandchewin:beer=0.14", "brewinandchewin:pale_jane=0.16",
            "brewinandchewin:strongroot_ale=0.18", "brewinandchewin:steel_toe_stout=0.20",
            "brewinandchewin:mead=0.22", "brewinandchewin:rice_wine=0.22", "brewinandchewin:egg_grog=0.24",
            "brewinandchewin:bloody_mary=0.24", "brewinandchewin:salty_folly=0.25",
            "brewinandchewin:glittering_grenadine=0.26", "brewinandchewin:saccharine_rum=0.28",
            "brewinandchewin:red_rum=0.30", "brewinandchewin:dread_nog=0.32",
            "brewinandchewin:vodka=0.38", "brewinandchewin:withering_dross=0.42"
    );

    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.DoubleValue BASE_DECAY_PER_MINUTE;
    public static final ForgeConfigSpec.DoubleValue DECAY_EXPONENT;
    public static final ForgeConfigSpec.DoubleValue SUGAR_HALF_LIFE_MINUTES;
    public static final ForgeConfigSpec.DoubleValue DEBT_HALF_LIFE_MINUTES;
    public static final ForgeConfigSpec.DoubleValue ALCOHOL_CLEAR_MINUTES;
    public static final ForgeConfigSpec.DoubleValue SUGAR_DECAY_SCALE;
    public static final ForgeConfigSpec.DoubleValue SUGAR_POTENCY_SCALE;
    public static final ForgeConfigSpec.DoubleValue NUTRIENT_AMPLIFICATION_SCALE;
    public static final ForgeConfigSpec.DoubleValue DEBT_SUPPRESSION_SCALE;
    public static final ForgeConfigSpec.DoubleValue ALCOHOL_POSITIVE_CUTOFF;
    public static final ForgeConfigSpec.DoubleValue SUGAR_DEBT_GATE;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> ALCOHOL_POTENCIES;

    public static final ForgeConfigSpec.DoubleValue FRUIT_DRINK;
    public static final ForgeConfigSpec.DoubleValue FRUIT_SECOND_WIND;
    public static final ForgeConfigSpec.DoubleValue FRUIT_SPRINT;
    public static final ForgeConfigSpec.DoubleValue FRUIT_FEAST;
    public static final ForgeConfigSpec.DoubleValue GRAIN_SUSTAINED_WORK;
    public static final ForgeConfigSpec.DoubleValue GRAIN_DURABILITY;
    public static final ForgeConfigSpec.DoubleValue GRAIN_LONG_HAUL;
    public static final ForgeConfigSpec.DoubleValue PROTEIN_IMPACT;
    public static final ForgeConfigSpec.DoubleValue PROTEIN_BRACE;
    public static final ForgeConfigSpec.DoubleValue PROTEIN_SUCCESS;
    public static final ForgeConfigSpec.DoubleValue VEGETABLE_RECENT_FOOD;
    public static final ForgeConfigSpec.DoubleValue VEGETABLE_EFFECT_RECOVERY;
    public static final ForgeConfigSpec.DoubleValue VEGETABLE_DRIFT;
    public static final ForgeConfigSpec.DoubleValue VEGETABLE_EMERGENCY;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.comment("Superlinear nutrition, sugar/debt, and inverted-U alcohol tuning.").push("metabolism");
        BASE_DECAY_PER_MINUTE = range(builder, "baseNutrientDecayPerMinute", 0.004, 0.0001, 0.1);
        DECAY_EXPONENT = range(builder, "fullnessDecayExponent", 4.0, 0.1, 12.0);
        SUGAR_HALF_LIFE_MINUTES = range(builder, "sugarHalfLifeMinutes", 4.0, 0.1, 120.0);
        DEBT_HALF_LIFE_MINUTES = range(builder, "debtHalfLifeMinutes", 10.0, 0.1, 240.0);
        ALCOHOL_CLEAR_MINUTES = range(builder, "alcoholClearMinutes", 20.0, 0.1, 240.0);
        SUGAR_DECAY_SCALE = range(builder, "sugarDecayScale", 4.0, 0.0, 20.0);
        SUGAR_POTENCY_SCALE = range(builder, "sugarPotencyScale", 0.5, 0.0, 4.0);
        NUTRIENT_AMPLIFICATION_SCALE = range(builder, "nutrientAmplificationScale", 0.25, 0.0, 4.0);
        DEBT_SUPPRESSION_SCALE = range(builder, "debtSuppressionScale", 0.4, 0.0, 1.0);
        ALCOHOL_POSITIVE_CUTOFF = range(builder, "alcoholPositiveCutoff", 0.85, 0.5, 1.0);
        SUGAR_DEBT_GATE = range(builder, "sugarDebtRecoveryGate", 0.25, 0.0, 1.0);
        ALCOHOL_POTENCIES = builder.comment("item_id=load entries; replace this list to tune drinks or add compat drinks.")
                .defineList("alcoholPotencies", DEFAULT_ALCOHOL_POTENCIES, SalienceConfig::validAlcoholEntry);
        builder.pop();

        builder.comment("Effective nutrient fractions at which each behavioral threshold becomes available.").push("thresholds");
        FRUIT_DRINK = threshold(builder, "fruitDrinkSupport", 0.25);
        FRUIT_SECOND_WIND = threshold(builder, "fruitSecondWind", 0.55);
        FRUIT_SPRINT = threshold(builder, "fruitSprintSupport", 0.80);
        FRUIT_FEAST = threshold(builder, "fruitFeast", 0.95);
        GRAIN_SUSTAINED_WORK = threshold(builder, "grainSustainedWork", 0.35);
        GRAIN_DURABILITY = threshold(builder, "grainDurability", 0.65);
        GRAIN_LONG_HAUL = threshold(builder, "grainLongHaul", 0.90);
        PROTEIN_IMPACT = threshold(builder, "proteinImpact", 0.40);
        PROTEIN_BRACE = threshold(builder, "proteinBrace", 0.92);
        PROTEIN_SUCCESS = threshold(builder, "proteinSuccessRecovery", 0.70);
        VEGETABLE_RECENT_FOOD = threshold(builder, "vegetableRecentFood", 0.20);
        VEGETABLE_EFFECT_RECOVERY = threshold(builder, "vegetableEffectRecovery", 0.50);
        VEGETABLE_DRIFT = threshold(builder, "vegetableTemperatureDrift", 0.75);
        VEGETABLE_EMERGENCY = threshold(builder, "vegetableEmergency", 0.95);
        builder.pop();
        SPEC = builder.build();
    }

    private SalienceConfig() {}

    public static double value(ForgeConfigSpec.DoubleValue configured, double fallback) {
        try { return configured.get(); }
        catch (IllegalStateException ignored) { return fallback; }
    }

    public static Map<String, Double> alcoholPotencies() {
        List<? extends String> entries;
        try { entries = ALCOHOL_POTENCIES.get(); }
        catch (IllegalStateException ignored) { entries = DEFAULT_ALCOHOL_POTENCIES; }
        Map<String, Double> parsed = new LinkedHashMap<>();
        entries.forEach(entry -> {
            int split = entry.lastIndexOf('=');
            if (split <= 0) return;
            try { parsed.put(entry.substring(0, split), Double.parseDouble(entry.substring(split + 1))); }
            catch (NumberFormatException ignored) { }
        });
        return Map.copyOf(parsed);
    }

    private static ForgeConfigSpec.DoubleValue range(ForgeConfigSpec.Builder builder, String key, double value, double min, double max) {
        return builder.defineInRange(key, value, min, max);
    }

    private static ForgeConfigSpec.DoubleValue threshold(ForgeConfigSpec.Builder builder, String key, double value) {
        return builder.defineInRange(key, value, 0.0, 1.0);
    }

    private static boolean validAlcoholEntry(Object value) {
        if (!(value instanceof String entry)) return false;
        int split = entry.lastIndexOf('=');
        if (split <= 0 || split == entry.length() - 1) return false;
        try {
            double load = Double.parseDouble(entry.substring(split + 1));
            return entry.substring(0, split).contains(":") && load > 0.0 && load <= 1.0;
        } catch (NumberFormatException ignored) {
            return false;
        }
    }
}
