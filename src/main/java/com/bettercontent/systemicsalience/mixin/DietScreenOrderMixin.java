package com.bettercontent.systemicsalience.mixin;

import com.illusivesoulworks.diet.api.type.IDietGroup;
import com.illusivesoulworks.diet.client.screen.DietScreen;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Comparator;
import java.util.Set;
import java.util.TreeSet;

/** Diet 2.1.1 copies groups into a HashSet, losing their configured display order. */
@Mixin(value = DietScreen.class, remap = false)
abstract class DietScreenOrderMixin {
    @Shadow @Final @Mutable private Set<IDietGroup> groups;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void systemicSalience$useGroupOrder(boolean fromInventory, CallbackInfo callback) {
        groups = new TreeSet<>(Comparator.comparingInt(IDietGroup::getOrder).thenComparing(IDietGroup::getName));
    }
}
