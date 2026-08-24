package com.bettercontent.systemicsalience.runtime;

import com.bettercontent.systemicsalience.SystemicSalienceMod;
import com.bettercontent.systemicsalience.compat.BrewingCompat;
import com.bettercontent.systemicsalience.compat.ColdSweatCompat;
import com.bettercontent.systemicsalience.compat.EpicFightCompat;
import com.bettercontent.systemicsalience.compat.ThirstCompat;
import com.bettercontent.systemicsalience.config.SalienceConfig;
import com.bettercontent.systemicsalience.metabolism.ConsumableProfiles;
import com.bettercontent.systemicsalience.metabolism.MetabolicMath;
import com.bettercontent.systemicsalience.metabolism.MetabolicState;
import com.bettercontent.systemicsalience.metabolism.MetabolicStateStore;
import com.bettercontent.systemicsalience.mixin.MobEffectInstanceAccessor;
import com.bettercontent.systemicsalience.network.SalienceNetwork;
import com.bettercontent.systemicsalience.nutrition.DietBridge;
import com.bettercontent.systemicsalience.nutrition.NutritionSnapshot;
import com.illusivesoulworks.diet.api.DietEvent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.living.LivingHealEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.LivingKnockBackEvent;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = SystemicSalienceMod.MOD_ID)
public final class SalienceEvents {
    private static final UUID ALCOHOL_MOVEMENT = UUID.fromString("41d93978-1b06-4df4-81aa-499531e2c635");
    private static final UUID ALCOHOL_TIMING = UUID.fromString("fb8c17f4-59c7-4cb4-b6e9-c31316286787");
    private static final Map<UUID, PendingDurability> PENDING_DURABILITY = new HashMap<>();
    private static final Map<UUID, Integer> BLOCK_SEQUENCE = new HashMap<>();
    private static final Map<UUID, Long> LAST_BLOCK_BREAK = new HashMap<>();
    private static final Map<UUID, Boolean> WAS_SPRINTING = new HashMap<>();
    private static final Set<UUID> PAYING_DEFERRED = new HashSet<>();

    private SalienceEvents() {}

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void disableDietDecay(DietEvent.ApplyDecay event) {
        event.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void disableDietEffects(DietEvent.ApplyEffect event) {
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) return;
        MetabolicState state = MetabolicStateStore.get(player);
        state.tickTransient();
        NutritionSnapshot nutrition = DietBridge.snapshot(player);
        long gameTime = player.level().getGameTime();

        resolvePendingDurability(player, gameTime);
        resolvePendingEffects(player, state);
        applyThresholds(player, state, nutrition, gameTime);
        applyAlcohol(player, state);
        accelerateHarmfulEffects(player, state, nutrition);

        if (player.tickCount % 20 == 0) {
            double extraDepletion = DietBridge.applyCustomDecay(player, state);
            state.addDebt(2.0 * extraDepletion);
            BrewingCompat.suppressNumbedHearts(player);
            if (state.sugar < SalienceConfig.SUGAR_DEBT_GATE.get() && state.debt > 0.0 && player.isSprinting()) {
                ThirstCompat.addExhaustion(player, (float) (0.01 * state.debt));
            }
            repayDeferredDamage(player, state);
            MetabolicStateStore.save(player);
            SalienceNetwork.sync(player, DietBridge.snapshot(player), state);
        }
    }

    @SubscribeEvent
    public static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        MetabolicState state = MetabolicStateStore.get(player);
        long now = player.level().getGameTime();
        if (state.lastWorkSeenTick < now - 5L) state.workStartTick = now;
        state.lastWorkSeenTick = now;
        double impairment = MetabolicMath.alcoholImpairment(state.alcohol);
        event.setNewSpeed((float) (event.getNewSpeed() * (1.0 - 0.5 * impairment)));
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onBlockBroken(BlockEvent.BreakEvent event) {
        if (event.isCanceled() || !(event.getPlayer() instanceof ServerPlayer player)) return;
        MetabolicState state = MetabolicStateStore.get(player);
        NutritionSnapshot nutrition = DietBridge.snapshot(player);
        if (nutrition.effective(NutritionSnapshot.Group.GRAINS, state) < SalienceConfig.GRAIN_DURABILITY.get()) return;

        long now = player.level().getGameTime();
        long last = LAST_BLOCK_BREAK.getOrDefault(player.getUUID(), -100L);
        int sequence = last >= now - 40L ? BLOCK_SEQUENCE.getOrDefault(player.getUUID(), 0) + 1 : 1;
        LAST_BLOCK_BREAK.put(player.getUUID(), now);
        BLOCK_SEQUENCE.put(player.getUUID(), sequence);
        if (sequence % 4 != 0) return;

        ItemStack tool = player.getMainHandItem();
        if (!tool.isDamageableItem()) return;
        PENDING_DURABILITY.put(player.getUUID(), new PendingDurability(
                player.getInventory().selected, tool.getItem(), tool.getDamageValue(), now + 1L
        ));
    }

    @SubscribeEvent
    public static void onUseStart(LivingEntityUseItemEvent.Start event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!event.getItem().isEdible() && !ThirstCompat.isDrink(event.getItem())) return;
        MetabolicStateStore.get(player).recentConsumptionTicks = Math.max(5, event.getDuration() + 5);
    }

    @SubscribeEvent
    public static void onUseFinish(LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        ItemStack stack = event.getItem();
        MetabolicState state = MetabolicStateStore.get(player);
        NutritionSnapshot nutrition = DietBridge.snapshot(player);

        double sugar = ConsumableProfiles.sugar(stack);
        if (sugar > 0.0) state.addSugar(sugar);
        double alcohol = ConsumableProfiles.alcohol(stack);
        if (alcohol > 0.0) {
            state.addAlcohol(alcohol);
            ThirstCompat.addExhaustion(player, (float) (2.0 * alcohol));
        }

        double fruits = nutrition.effective(NutritionSnapshot.Group.FRUITS, state);
        if (fruits >= SalienceConfig.FRUIT_DRINK.get() && alcohol == 0.0 && ThirstCompat.isDrink(stack)) {
            int quenched = Math.max(2, (int) Math.round(2.0 * MetabolicMath.thresholdPotency(state.sugar)));
            ThirstCompat.addQuenched(player, quenched);
        }
    }

    @SubscribeEvent
    public static void onAttack(AttackEntityEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !(event.getTarget() instanceof LivingEntity target)) return;
        MetabolicState state = MetabolicStateStore.get(player);
        double protein = DietBridge.snapshot(player).effective(NutritionSnapshot.Group.PROTEINS, state);
        if (protein < SalienceConfig.PROTEIN_IMPACT.get()) return;

        double potency = MetabolicMath.thresholdPotency(state.sugar);
        double dx = player.getX() - target.getX();
        double dz = player.getZ() - target.getZ();
        target.knockback(0.18 * potency, dx, dz);
        EpicFightCompat.applyImpact(target, 0.15 * potency);

        if (protein >= SalienceConfig.PROTEIN_BRACE.get() && state.braceCooldown == 0) {
            state.braced = true;
            state.braceCooldown = GameplayHooks.cooldown(45 * 20, state);
            EpicFightCompat.braceStunShield(player);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onKnockback(LivingKnockBackEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        MetabolicState state = MetabolicStateStore.get(player);
        if (!state.braced) return;
        state.braced = false;
        event.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onHurt(LivingHurtEvent event) {
        if (event.getEntity() instanceof ServerPlayer target) {
            MetabolicState state = MetabolicStateStore.get(target);
            double vegetables = DietBridge.snapshot(target).effective(NutritionSnapshot.Group.VEGETABLES, state);
            if (vegetables >= SalienceConfig.VEGETABLE_EMERGENCY.get() && state.vegetableEmergencyCooldown == 0 && ColdSweatCompat.isTemperatureDamage(event.getSource())) {
                event.setCanceled(true);
                state.vegetableEmergencyCooldown = GameplayHooks.cooldown(180 * 20, state);
                ColdSweatCompat.pullSafe(target);
                return;
            }

            double positive = MetabolicMath.alcoholPositive(state.alcohol);
            if (positive > 0.0 && !PAYING_DEFERRED.contains(target.getUUID())) {
                float deferred = (float) (event.getAmount() * Math.min(0.15, 0.15 * positive));
                if (deferred > 0.0f) {
                    event.setAmount(event.getAmount() - deferred);
                    state.deferredDamage += deferred;
                    state.deferredGraceTicks = 60;
                }
            }
        }

        if (event.getSource().getEntity() instanceof ServerPlayer attacker) {
            MetabolicState state = MetabolicStateStore.get(attacker);
            if (state.nextStrikeTicks > 0) state.nextStrikeTicks = 0;
        }
    }

    @SubscribeEvent
    public static void onHeal(LivingHealEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        double impairment = MetabolicMath.alcoholImpairment(MetabolicStateStore.get(player).alcohol);
        event.setAmount((float) (event.getAmount() * (1.0 - 0.6 * impairment)));
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer player)) return;
        rewardProteinSuccess(player);
    }

    @SubscribeEvent
    public static void onEffectAdded(MobEffectEvent.Added event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        MobEffect effect = event.getEffectInstance().getEffect();
        MetabolicState state = MetabolicStateStore.get(player);
        double vegetables = DietBridge.snapshot(player).effective(NutritionSnapshot.Group.VEGETABLES, state);
        boolean poison = effect == MobEffects.POISON;
        boolean nausea = effect == MobEffects.CONFUSION;
        boolean wither = effect == MobEffects.WITHER;

        if (vegetables >= SalienceConfig.VEGETABLE_EMERGENCY.get() && state.vegetableEmergencyCooldown == 0 && (poison || wither)) {
            markEffectRemoval(state, effect);
            state.vegetableEmergencyCooldown = GameplayHooks.cooldown(180 * 20, state);
            ColdSweatCompat.pullSafe(player);
            return;
        }
        if (vegetables >= SalienceConfig.VEGETABLE_RECENT_FOOD.get() && state.recentConsumptionTicks > 0 && (poison || nausea)
                && player.getRandom().nextFloat() < 0.25f) {
            markEffectRemoval(state, effect);
        }
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        EpicFightCompat.register(player);
        SalienceNetwork.sync(player, DietBridge.snapshot(player), MetabolicStateStore.get(player));
    }

    @SubscribeEvent
    public static void onDimensionChange(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        EpicFightCompat.register(player);
        SalienceNetwork.sync(player, DietBridge.snapshot(player), MetabolicStateStore.get(player));
    }

    @SubscribeEvent
    public static void onClone(PlayerEvent.Clone event) {
        if (!(event.getEntity() instanceof ServerPlayer replacement)) return;
        if (event.isWasDeath()) MetabolicStateStore.reset(replacement);
        else MetabolicStateStore.copy(event.getOriginal(), replacement);
        EpicFightCompat.register(replacement);
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        MetabolicStateStore.save(player);
        MetabolicStateStore.unload(player);
        ColdSweatCompat.unload(player);
        PENDING_DURABILITY.remove(player.getUUID());
        BLOCK_SEQUENCE.remove(player.getUUID());
        LAST_BLOCK_BREAK.remove(player.getUUID());
        WAS_SPRINTING.remove(player.getUUID());
    }

    private static void applyThresholds(ServerPlayer player, MetabolicState state, NutritionSnapshot nutrition, long gameTime) {
        double fruits = nutrition.effective(NutritionSnapshot.Group.FRUITS, state);
        double grains = nutrition.effective(NutritionSnapshot.Group.GRAINS, state);
        double vegetables = nutrition.effective(NutritionSnapshot.Group.VEGETABLES, state);
        double potency = MetabolicMath.thresholdPotency(state.sugar);

        if (fruits >= SalienceConfig.FRUIT_SECOND_WIND.get() && state.fruitSecondWindCooldown == 0
                && EpicFightCompat.restoreIfLow(player, 0.20 * potency, 0.20)) {
            state.fruitSecondWindCooldown = GameplayHooks.cooldown(90 * 20, state);
        }

        boolean sprinting = player.isSprinting();
        boolean wasSprinting = Boolean.TRUE.equals(WAS_SPRINTING.put(player.getUUID(), sprinting));
        if (fruits >= SalienceConfig.FRUIT_SPRINT.get() && sprinting && !wasSprinting) {
            player.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
            state.sprintExhaustionFreeTicks = Math.max(state.sprintExhaustionFreeTicks, 5 * 20);
        }

        if (fruits >= SalienceConfig.FRUIT_FEAST.get() && state.fruitFeastCooldown == 0
                && (EpicFightCompat.isStaminaBelow(player, 0.70) || ThirstCompat.needsQuenched(player))) {
            EpicFightCompat.restoreStamina(player, 0.40 * potency);
            ThirstCompat.addQuenched(player, Math.max(4, (int) Math.round(4.0 * potency)));
            state.fruitFeastCooldown = GameplayHooks.cooldown(45 * 20, state);
        }

        boolean lowResources = player.getFoodData().getFoodLevel() <= 6 || ThirstCompat.isLow(player);
        boolean moving = player.getDeltaMovement().horizontalDistanceSqr() > 0.0004;
        if (grains >= SalienceConfig.GRAIN_LONG_HAUL.get() && state.grainLongHaulCooldown == 0 && lowResources && moving) {
            state.grainLongHaulTicks = 15 * 20;
            state.grainLongHaulCooldown = GameplayHooks.cooldown(180 * 20, state);
        }
        if (state.grainLongHaulTicks > 0 && moving) player.setSprinting(true);

        if (vegetables >= SalienceConfig.VEGETABLE_DRIFT.get()) ColdSweatCompat.dampenDrift(player);
        EpicFightCompat.updateThresholdAttributes(player, fruits >= SalienceConfig.FRUIT_FEAST.get(), state.nextStrikeTicks > 0);
    }

    private static void applyAlcohol(ServerPlayer player, MetabolicState state) {
        double impairment = MetabolicMath.alcoholImpairment(state.alcohol);
        updateAttribute(player, "minecraft:generic.movement_speed", ALCOHOL_MOVEMENT, -0.40 * impairment,
                AttributeModifier.Operation.MULTIPLY_TOTAL, "systemic_salience_alcohol_movement");
        updateAttribute(player, "minecraft:generic.attack_speed", ALCOHOL_TIMING, -0.35 * impairment,
                AttributeModifier.Operation.MULTIPLY_TOTAL, "systemic_salience_alcohol_timing");
        EpicFightCompat.reinforceStunShield(player, MetabolicMath.alcoholPositive(state.alcohol));
        if (state.alcohol >= SalienceConfig.ALCOHOL_POSITIVE_CUTOFF.get()) player.setSprinting(false);
    }

    private static void accelerateHarmfulEffects(ServerPlayer player, MetabolicState state, NutritionSnapshot nutrition) {
        if (player.tickCount % 5 != 0 || nutrition.effective(NutritionSnapshot.Group.VEGETABLES, state) < SalienceConfig.VEGETABLE_EFFECT_RECOVERY.get()) return;
        for (MobEffectInstance effect : player.getActiveEffects()) {
            if (effect.getEffect().getCategory() != MobEffectCategory.HARMFUL || effect.isInfiniteDuration()) continue;
            MobEffectInstanceAccessor accessor = (MobEffectInstanceAccessor) effect;
            accessor.systemicSalience$setDuration(Math.max(1, accessor.systemicSalience$getDuration() - 1));
        }
    }

    private static void resolvePendingEffects(ServerPlayer player, MetabolicState state) {
        if (state.pendingRemoveNausea) player.removeEffect(MobEffects.CONFUSION);
        if (state.pendingRemovePoison) player.removeEffect(MobEffects.POISON);
        if (state.pendingRemoveWither) player.removeEffect(MobEffects.WITHER);
        state.pendingRemoveNausea = false;
        state.pendingRemovePoison = false;
        state.pendingRemoveWither = false;
    }

    private static void markEffectRemoval(MetabolicState state, MobEffect effect) {
        if (effect == MobEffects.CONFUSION) state.pendingRemoveNausea = true;
        if (effect == MobEffects.POISON) state.pendingRemovePoison = true;
        if (effect == MobEffects.WITHER) state.pendingRemoveWither = true;
    }

    private static void rewardProteinSuccess(ServerPlayer player) {
        MetabolicState state = MetabolicStateStore.get(player);
        double protein = DietBridge.snapshot(player).effective(NutritionSnapshot.Group.PROTEINS, state);
        if (protein < SalienceConfig.PROTEIN_SUCCESS.get()) return;
        EpicFightCompat.restoreStamina(player, 0.20 * MetabolicMath.thresholdPotency(state.sugar));
        state.nextStrikeTicks = 4 * 20;
    }

    private static void repayDeferredDamage(ServerPlayer player, MetabolicState state) {
        if (state.deferredDamage <= 0.0f || state.deferredGraceTicks > 0 || !player.isAlive()) return;
        float payment = Math.min(0.5f, state.deferredDamage);
        state.deferredDamage -= payment;
        PAYING_DEFERRED.add(player.getUUID());
        try {
            player.hurt(player.damageSources().magic(), payment);
        } finally {
            PAYING_DEFERRED.remove(player.getUUID());
        }
    }

    private static void resolvePendingDurability(ServerPlayer player, long gameTime) {
        PendingDurability pending = PENDING_DURABILITY.get(player.getUUID());
        if (pending == null || pending.applyAt > gameTime) return;
        PENDING_DURABILITY.remove(player.getUUID());
        ItemStack current = player.getInventory().getItem(pending.slot);
        if (current.getItem() == pending.item && current.isDamageableItem() && current.getDamageValue() > pending.damage) {
            current.setDamageValue(Math.max(pending.damage, current.getDamageValue() - 1));
        }
    }

    private static void updateAttribute(ServerPlayer player, String id, UUID uuid, double amount,
                                        AttributeModifier.Operation operation, String name) {
        Attribute attribute = ForgeRegistries.ATTRIBUTES.getValue(new net.minecraft.resources.ResourceLocation(id));
        if (attribute == null) return;
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance == null) return;
        AttributeModifier existing = instance.getModifier(uuid);
        if (existing != null) instance.removeModifier(existing);
        if (amount != 0.0) instance.addTransientModifier(new AttributeModifier(uuid, name, amount, operation));
    }

    private record PendingDurability(int slot, Item item, int damage, long applyAt) {}
}
