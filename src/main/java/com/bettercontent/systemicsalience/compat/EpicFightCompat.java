package com.bettercontent.systemicsalience.compat;

import com.bettercontent.systemicsalience.metabolism.MetabolicMath;
import com.bettercontent.systemicsalience.metabolism.MetabolicStateStore;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.entity.eventlistener.PlayerEventListener;
import yesman.epicfight.world.capabilities.entitypatch.HurtableEntityPatch;
import yesman.epicfight.world.damagesource.StunType;

import java.util.UUID;

public final class EpicFightCompat {
    private static final UUID STAMINA_COST_LISTENER = UUID.fromString("89c31fb6-7fe0-4a17-ae4c-3b7039e67f01");
    private static final UUID FRUIT_STAMINA_MODIFIER = UUID.fromString("17c3dfc3-945b-4d6e-b45a-7caa1274a600");
    private static final UUID PROTEIN_STRIKE_MODIFIER = UUID.fromString("fa41bcaa-34ed-44e3-aa5d-75429c24b8bf");

    private EpicFightCompat() {}

    public static void register(ServerPlayer player) {
        if (!ModList.get().isLoaded("epicfight")) return;
        var patch = EpicFightCapabilities.getServerPlayerPatch(player);
        if (patch == null) return;
        patch.getEventListener().removeListener(PlayerEventListener.EventType.STAMINA_CONSUME_EVENT, STAMINA_COST_LISTENER);
        patch.getEventListener().addEventListener(PlayerEventListener.EventType.STAMINA_CONSUME_EVENT, STAMINA_COST_LISTENER, event -> {
            double impairment = MetabolicMath.alcoholImpairment(MetabolicStateStore.get(player).alcohol);
            event.setAmount((float) (event.getAmount() * (1.0 + 0.75 * impairment)));
        });
    }

    public static boolean restoreStamina(ServerPlayer player, double fraction) {
        if (!ModList.get().isLoaded("epicfight")) return false;
        var patch = EpicFightCapabilities.getServerPlayerPatch(player);
        if (patch == null) return false;
        float max = patch.getMaxStamina();
        patch.setStamina(Math.min(max, patch.getStamina() + (float) (max * fraction)));
        return true;
    }

    public static boolean restoreIfLow(ServerPlayer player, double fraction, double threshold) {
        if (!ModList.get().isLoaded("epicfight")) return false;
        var patch = EpicFightCapabilities.getServerPlayerPatch(player);
        if (patch == null || patch.getMaxStamina() <= 0.0f || patch.getStamina() / patch.getMaxStamina() > threshold) return false;
        patch.setStamina(Math.min(patch.getMaxStamina(), patch.getStamina() + (float) (patch.getMaxStamina() * fraction)));
        return true;
    }

    public static boolean isStaminaBelow(ServerPlayer player, double threshold) {
        if (!ModList.get().isLoaded("epicfight")) return false;
        var patch = EpicFightCapabilities.getServerPlayerPatch(player);
        return patch != null && patch.getMaxStamina() > 0.0f && patch.getStamina() / patch.getMaxStamina() <= threshold;
    }

    public static void reinforceStunShield(ServerPlayer player, double positive) {
        if (!ModList.get().isLoaded("epicfight") || positive <= 0.0) return;
        var patch = EpicFightCapabilities.getServerPlayerPatch(player);
        if (patch == null) return;
        patch.setStunShield(Math.min(patch.getMaxStunShield(), patch.getStunShield() + (float) (0.04 * positive)));
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static void applyImpact(LivingEntity target, double amount) {
        if (!ModList.get().isLoaded("epicfight") || amount <= 0.0) return;
        HurtableEntityPatch patch = EpicFightCapabilities.getEntityPatch(target, HurtableEntityPatch.class);
        if (patch != null) patch.applyStun(StunType.SHORT, (float) amount);
    }

    public static void braceStunShield(ServerPlayer player) {
        if (!ModList.get().isLoaded("epicfight")) return;
        var patch = EpicFightCapabilities.getServerPlayerPatch(player);
        if (patch != null) patch.setStunShield(patch.getMaxStunShield());
    }

    public static void updateThresholdAttributes(ServerPlayer player, boolean fruitFeast, boolean nextStrike) {
        if (!ModList.get().isLoaded("epicfight")) return;
        updateModifier(player, "epicfight:staminar", FRUIT_STAMINA_MODIFIER, fruitFeast ? 0.4 : 0.0,
                AttributeModifier.Operation.MULTIPLY_TOTAL, "systemic_salience_fruit_stamina");
        updateModifier(player, "epicfight:max_strikes", PROTEIN_STRIKE_MODIFIER, nextStrike ? 1.0 : 0.0,
                AttributeModifier.Operation.ADDITION, "systemic_salience_protein_strike");
    }

    private static void updateModifier(ServerPlayer player, String id, UUID uuid, double amount,
                                       AttributeModifier.Operation operation, String name) {
        Attribute attribute = ForgeRegistries.ATTRIBUTES.getValue(new net.minecraft.resources.ResourceLocation(id));
        if (attribute == null) return;
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance == null) return;
        AttributeModifier existing = instance.getModifier(uuid);
        if (existing != null) instance.removeModifier(existing);
        if (amount != 0.0) instance.addTransientModifier(new AttributeModifier(uuid, name, amount, operation));
    }
}
