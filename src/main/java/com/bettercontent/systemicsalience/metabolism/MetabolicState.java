package com.bettercontent.systemicsalience.metabolism;

import net.minecraft.nbt.CompoundTag;

public final class MetabolicState {
    private static final String SUGAR = "sugar";
    private static final String DEBT = "debt";
    private static final String ALCOHOL = "alcohol";
    private static final String DEFERRED_DAMAGE = "deferred_damage";

    public double sugar;
    public double debt;
    public double alcohol;
    public float deferredDamage;
    public int deferredGraceTicks;

    public int fruitSecondWindCooldown;
    public int fruitFeastCooldown;
    public int grainLongHaulCooldown;
    public int grainLongHaulTicks;
    public int vegetableEmergencyCooldown;
    public int sprintExhaustionFreeTicks;
    public int nextStrikeTicks;
    public int braceCooldown;
    public boolean braced;

    public int recentConsumptionTicks;
    public boolean pendingRemoveNausea;
    public boolean pendingRemovePoison;
    public boolean pendingRemoveWither;

    public long workStartTick = -1L;
    public long lastWorkSeenTick = -100L;
    public long lastMinedTick = -100L;
    public int uninterruptedMinedBlocks;
    public int pendingToolSlot = -1;
    public int pendingToolDamage = -1;
    public long pendingToolRestoreTick = -1L;

    public double lastBodyTemperature = Double.NaN;
    public boolean payingDeferredDamage;

    public void tickTransient() {
        sugar = MetabolicMath.tickSugar(sugar);
        alcohol = MetabolicMath.tickAlcohol(alcohol);
        debt = MetabolicMath.tickDebt(debt, sugar);
        fruitSecondWindCooldown = decrement(fruitSecondWindCooldown);
        fruitFeastCooldown = decrement(fruitFeastCooldown);
        grainLongHaulCooldown = decrement(grainLongHaulCooldown);
        grainLongHaulTicks = decrement(grainLongHaulTicks);
        vegetableEmergencyCooldown = decrement(vegetableEmergencyCooldown);
        sprintExhaustionFreeTicks = decrement(sprintExhaustionFreeTicks);
        nextStrikeTicks = decrement(nextStrikeTicks);
        braceCooldown = decrement(braceCooldown);
        recentConsumptionTicks = decrement(recentConsumptionTicks);
        deferredGraceTicks = decrement(deferredGraceTicks);
    }

    public void addSugar(double amount) {
        sugar = MetabolicMath.clamp01(sugar + Math.max(0.0, amount));
    }

    public void addAlcohol(double amount) {
        alcohol = MetabolicMath.clamp01(alcohol + Math.max(0.0, amount));
    }

    public void addDebt(double amount) {
        debt = MetabolicMath.clamp01(debt + Math.max(0.0, amount));
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putDouble(SUGAR, sugar);
        tag.putDouble(DEBT, debt);
        tag.putDouble(ALCOHOL, alcohol);
        tag.putFloat(DEFERRED_DAMAGE, deferredDamage);
        tag.putInt("deferred_grace", deferredGraceTicks);
        tag.putInt("fruit_second_wind_cd", fruitSecondWindCooldown);
        tag.putInt("fruit_feast_cd", fruitFeastCooldown);
        tag.putInt("grain_long_haul_cd", grainLongHaulCooldown);
        tag.putInt("grain_long_haul_ticks", grainLongHaulTicks);
        tag.putInt("vegetable_emergency_cd", vegetableEmergencyCooldown);
        tag.putInt("sprint_exhaustion_free_ticks", sprintExhaustionFreeTicks);
        tag.putInt("next_strike_ticks", nextStrikeTicks);
        tag.putInt("brace_cd", braceCooldown);
        tag.putBoolean("braced", braced);
        return tag;
    }

    public static MetabolicState load(CompoundTag tag) {
        MetabolicState state = new MetabolicState();
        state.sugar = MetabolicMath.clamp01(tag.getDouble(SUGAR));
        state.debt = MetabolicMath.clamp01(tag.getDouble(DEBT));
        state.alcohol = MetabolicMath.clamp01(tag.getDouble(ALCOHOL));
        state.deferredDamage = Math.max(0.0f, tag.getFloat(DEFERRED_DAMAGE));
        state.deferredGraceTicks = nonnegative(tag.getInt("deferred_grace"));
        state.fruitSecondWindCooldown = nonnegative(tag.getInt("fruit_second_wind_cd"));
        state.fruitFeastCooldown = nonnegative(tag.getInt("fruit_feast_cd"));
        state.grainLongHaulCooldown = nonnegative(tag.getInt("grain_long_haul_cd"));
        state.grainLongHaulTicks = nonnegative(tag.getInt("grain_long_haul_ticks"));
        state.vegetableEmergencyCooldown = nonnegative(tag.getInt("vegetable_emergency_cd"));
        state.sprintExhaustionFreeTicks = nonnegative(tag.getInt("sprint_exhaustion_free_ticks"));
        state.nextStrikeTicks = nonnegative(tag.getInt("next_strike_ticks"));
        state.braceCooldown = nonnegative(tag.getInt("brace_cd"));
        state.braced = tag.getBoolean("braced");
        return state;
    }

    private static int decrement(int ticks) {
        return Math.max(0, ticks - 1);
    }

    private static int nonnegative(int value) {
        return Math.max(0, value);
    }
}
