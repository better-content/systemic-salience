package com.bettercontent.systemicsalience.mixin;

import com.illusivesoulworks.diet.api.type.IDietGroup;
import com.illusivesoulworks.diet.client.screen.DietScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Set;

/** Read the exact group iteration order that Diet draws. */
@Mixin(value = DietScreen.class, remap = false)
public interface DietScreenAccessor {
    @Accessor("groups")
    Set<IDietGroup> systemicSalience$groups();
}
