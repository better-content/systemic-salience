package com.bettercontent.systemicsalience.nutrition;

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


    private static NutritionSnapshot snapshot(IDietTracker tracker) {
        return new NutritionSnapshot(
                tracker.getValue(NutritionSnapshot.Group.PROTEINS.id),
                tracker.getValue(NutritionSnapshot.Group.GRAINS.id),
                tracker.getValue(NutritionSnapshot.Group.FRUITS.id),
                tracker.getValue(NutritionSnapshot.Group.FATS.id),
                tracker.getValue(NutritionSnapshot.Group.VEGETABLES.id),
                tracker.getValue(NutritionSnapshot.Group.DAIRY.id)
        );
    }
}
