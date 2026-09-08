package com.bettercontent.systemicsalience.nutrition;

import com.bettercontent.systemicsalience.compat.ThreadsBridge;
import com.bettercontent.systemicsalience.metabolism.MetabolicState;
import com.bettercontent.systemicsalience.metabolism.MetabolicStateStore;
import net.minecraft.server.level.ServerPlayer;

/** Authoritative server-side Diet transitions exposed as optional learning-surface evidence. */
public final class NutritionThreadBoundary {
    public static final String THREAD_ID = "hunger_is_not_nutrition";
    public static final String WARNING_TYPE = "nutrition_warning";
    public static final String WARNING_VALUE = "hunger_full_nutrition_low";
    public static final String RECOVERED_TYPE = "nutrition_recovered";
    public static final String RECOVERED_VALUE = "balanced_meal";

    private NutritionThreadBoundary() {}

    /** Observes the post-decay Diet snapshot; called once per authoritative server second. */
    public static void onAuthoritativeTick(ServerPlayer player, NutritionSnapshot nutrition, double ordinaryThreshold) {
        if (player.getFoodData().getFoodLevel() < 20 || !isNutritionLow(nutrition, ordinaryThreshold)) return;
        MetabolicState state = MetabolicStateStore.get(player);
        String active = ThreadsBridge.activeCorrelation(player, THREAD_ID);
        boolean changed = active != null && !active.equals(state.nutritionThreadToken);
        if (active != null) state.nutritionThreadToken = active;
        if (state.nutritionThreadToken.isBlank()) {
            state.nutritionThreadToken = player.getUUID() + ":nutrition:" + player.server.getTickCount();
            changed = true;
        }
        if (active == null && ThreadsBridge.available()) {
            ThreadsBridge.emit(player, WARNING_TYPE, WARNING_VALUE, state.nutritionThreadToken);
        }
        if (changed) MetabolicStateStore.save(player);
    }

    /** Completes only when a finished edible use moves the settled Diet snapshot into balance. */
    public static void onMealSettled(ServerPlayer player, NutritionSnapshot before,
                                     NutritionSnapshot after, double ordinaryThreshold) {
        if (!isQualifyingRecovery(before, after, ordinaryThreshold)) return;
        MetabolicState state = MetabolicStateStore.get(player);
        String token = ThreadsBridge.activeCorrelation(player, THREAD_ID);
        if (token == null) token = state.nutritionThreadToken;
        if (token.isBlank()) return;
        if (ThreadsBridge.emit(player, RECOVERED_TYPE, RECOVERED_VALUE, token)) {
            state.nutritionThreadToken = "";
            MetabolicStateStore.save(player);
        }
    }

    public static boolean isNutritionLow(NutritionSnapshot nutrition, double ordinaryThreshold) {
        for (NutritionSnapshot.Group group : NutritionSnapshot.Group.values()) {
            if (nutrition.actual(group) < ordinaryThreshold) return true;
        }
        return false;
    }

    public static boolean isBalanced(NutritionSnapshot nutrition, double ordinaryThreshold) {
        return !isNutritionLow(nutrition, ordinaryThreshold);
    }

    public static boolean isQualifyingRecovery(NutritionSnapshot before, NutritionSnapshot after,
                                               double ordinaryThreshold) {
        return isNutritionLow(before, ordinaryThreshold) && isBalanced(after, ordinaryThreshold);
    }
}
