package com.bettercontent.systemicsalience.runtime;

import com.bettercontent.systemicsalience.metabolism.MetabolicMath;
import com.bettercontent.systemicsalience.metabolism.MetabolicState;
import com.bettercontent.systemicsalience.metabolism.MetabolicStateStore;
import com.bettercontent.systemicsalience.nutrition.DietBridge;
import com.bettercontent.systemicsalience.nutrition.NutritionSnapshot;
import net.minecraft.server.level.ServerPlayer;

public final class GameplayHooks {
    private GameplayHooks() {}

    public static float exhaustionMultiplier(ServerPlayer player) {
        MetabolicState state = MetabolicStateStore.get(player);
        if (state.sprintExhaustionFreeTicks > 0) return 0.0f;

        float multiplier = 1.0f;
        NutritionSnapshot nutrition = DietBridge.snapshot(player);
        if (nutrition.effective(NutritionSnapshot.Group.GRAINS, state) >= 0.35
                && isSustainedWork(state, player.level().getGameTime())) {
            multiplier *= 0.75f;
        }
        if (state.sugar < 0.25) multiplier *= (float) (1.0 + 0.5 * state.debt);
        return multiplier;
    }

    public static boolean isSustainedWork(MetabolicState state, long gameTime) {
        return state.workStartTick >= 0L && state.lastWorkSeenTick >= gameTime - 5L && gameTime - state.workStartTick >= 80L;
    }

    public static int cooldown(int baseTicks, MetabolicState state) {
        return MetabolicMath.adjustedCooldown(baseTicks, state.sugar, state.debt);
    }
}
