package com.bettercontent.systemicsalience.nutrition;

import com.bettercontent.systemicsalience.metabolism.MetabolicMath;
import com.bettercontent.systemicsalience.metabolism.MetabolicState;
import com.illusivesoulworks.diet.api.type.IDietTracker;
import com.illusivesoulworks.diet.platform.Services;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.Optional;

public final class DietBridge {
    private DietBridge() {}

    public static Optional<? extends IDietTracker> tracker(Player player) {
        return Services.CAPABILITY.get(player);
    }

    public static NutritionSnapshot snapshot(Player player) {
        return tracker(player).map(DietBridge::snapshot).orElse(NutritionSnapshot.EMPTY);
    }

    public static double applyCustomDecay(ServerPlayer player, MetabolicState state) {
        Optional<? extends IDietTracker> optional = tracker(player);
        if (optional.isEmpty()) return 0.0;

        IDietTracker tracker = optional.get();
        double extraDepletion = 0.0;
        for (NutritionSnapshot.Group group : NutritionSnapshot.Group.values()) {
            double current = tracker.getValue(group.id);
            double baseline = MetabolicMath.nutrientDecayPerTick(current, 0.0) * 20.0;
            double actual = MetabolicMath.nutrientDecayPerTick(current, state.sugar) * 20.0;
            tracker.setValue(group.id, (float) Math.max(0.0, current - actual));
            extraDepletion += Math.max(0.0, actual - baseline);
        }
        tracker.sync();
        return extraDepletion / NutritionSnapshot.Group.values().length;
    }

    private static NutritionSnapshot snapshot(IDietTracker tracker) {
        return new NutritionSnapshot(
                tracker.getValue(NutritionSnapshot.Group.FRUITS.id),
                tracker.getValue(NutritionSnapshot.Group.GRAINS.id),
                tracker.getValue(NutritionSnapshot.Group.PROTEINS.id),
                tracker.getValue(NutritionSnapshot.Group.VEGETABLES.id)
        );
    }
}
