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
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.LivingKnockBackEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = SystemicSalienceMod.MOD_ID)
public final class SalienceEvents {
    private static final UUID FRUIT_MOVE = UUID.fromString("613818db-da4d-42f6-ac8e-dd9c7f226c1c");
    private static final UUID FRUIT_SWIM = UUID.fromString("0d498917-ed03-4503-a129-4d403c947398");
    private static final UUID FRUIT_STEP = UUID.fromString("426336df-f8cb-42af-9fe8-a57797e8fcf3");
    private static final UUID VEGETABLE_KNOCKBACK = UUID.fromString("bd4c1015-e215-4fd1-bbc7-a47aca7cd645");
    private static final UUID SUGAR_ATTACK_SPEED = UUID.fromString("07ecf601-8b76-4ef0-a897-a332902b9f1e");
    private static final UUID SUGAR_USE_SPEED = UUID.fromString("3295f760-2016-4cbb-8803-331b8b17e9b5");
    private static final UUID ALCOHOL_ATTACK_TIMING = UUID.fromString("fb8c17f4-59c7-4cb4-b6e9-c31316286787");
    private static final UUID ALCOHOL_RECOIL = UUID.fromString("6b26319b-81c4-4b3e-8dd6-d2586fd23467");
    private static final UUID ALCOHOL_DISPERSION = UUID.fromString("219ddb4f-3df3-43c6-828c-f1bf7aff5bb8");
    private static final Map<UUID, List<MobEffectInstance>> PRESERVED_MILK_EFFECTS = new HashMap<>();

    private SalienceEvents() {}

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void disableDietDecay(DietEvent.ApplyDecay event) { event.setCanceled(true); }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void disableDietEffects(DietEvent.ApplyEffect event) { event.setCanceled(true); }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) return;
        MetabolicState state = MetabolicStateStore.get(player);
        state.tickTransient();
        NutritionSnapshot nutrition = DietBridge.snapshot(player);

        state.sprintTicks = player.isSprinting() ? state.sprintTicks + 1 : 0;
        applyIdentityModifiers(player, state, nutrition);
        applyRenewal(player, state, nutrition);
        applyEnduranceReserve(player, state, nutrition);

        if (player.tickCount % 20 == 0) {
            double extraDepletion = DietBridge.applyCustomDecay(player, state);
            state.addDebt(2.0 * extraDepletion);
            BrewingCompat.suppressNumbedHearts(player);
            MetabolicStateStore.save(player);
            SalienceNetwork.sync(player, DietBridge.snapshot(player), state);
        }
    }

    @SubscribeEvent
    public static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        MetabolicState state = MetabolicStateStore.get(player);
        double grains = DietBridge.snapshot(player).actual(NutritionSnapshot.Group.GRAINS);
        double bonus = grains >= feast() ? 0.20 + Math.min(0.20, state.workSequence * 0.04)
                : grains >= prepared() ? 0.20 : grains >= ordinary() ? 0.10 : 0.0;
        event.setNewSpeed((float) (event.getNewSpeed() * (1.0 + bonus)));
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onBlockBroken(BlockEvent.BreakEvent event) {
        if (event.isCanceled() || !(event.getPlayer() instanceof ServerPlayer player)) return;
        MetabolicState state = MetabolicStateStore.get(player);
        double grains = DietBridge.snapshot(player).actual(NutritionSnapshot.Group.GRAINS);
        if (grains < feast() || !player.hasCorrectToolForDrops(event.getState())) {
            state.workSequence = 0;
            return;
        }
        long now = player.level().getGameTime();
        state.workSequence = state.lastBreakTick >= now - 60L ? Math.min(5, state.workSequence + 1) : 1;
        state.lastBreakTick = now;
    }

    @SubscribeEvent
    public static void onUseStart(LivingEntityUseItemEvent.Start event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !event.getItem().is(net.minecraft.world.item.Items.MILK_BUCKET)) return;
        if (DietBridge.snapshot(player).actual(NutritionSnapshot.Group.DAIRY) < prepared()) return;
        List<MobEffectInstance> beneficial = player.getActiveEffects().stream()
                .filter(effect -> effect.getEffect().isBeneficial())
                .map(MobEffectInstance::new)
                .toList();
        PRESERVED_MILK_EFFECTS.put(player.getUUID(), beneficial);
    }

    @SubscribeEvent
    public static void onUseFinish(LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        ItemStack stack = event.getItem();
        MetabolicState state = MetabolicStateStore.get(player);
        double sugar = ConsumableProfiles.sugar(stack);
        if (sugar > 0.0) state.addSugar(sugar);
        double alcohol = ConsumableProfiles.alcohol(stack);
        if (alcohol > 0.0) {
            state.addAlcohol(alcohol);
            ThirstCompat.addExhaustion(player, (float) (2.0 * alcohol));
        }
        if (stack.is(net.minecraft.world.item.Items.MILK_BUCKET)) {
            List<MobEffectInstance> preserved = PRESERVED_MILK_EFFECTS.remove(player.getUUID());
            if (preserved != null) preserved.forEach(player::addEffect);
        }
    }

    @SubscribeEvent
    public static void onAttack(AttackEntityEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !(event.getTarget() instanceof LivingEntity target)) return;
        MetabolicState state = MetabolicStateStore.get(player);
        double proteins = DietBridge.snapshot(player).actual(NutritionSnapshot.Group.PROTEINS);
        double force = proteins >= prepared() ? 0.20 : proteins >= ordinary() ? 0.10 : 0.0;
        long now = player.level().getGameTime();
        boolean heavy = proteins >= feast() && state.heavyBlowCooldown == 0
                && now - state.lastAttackTick >= 80L && player.getAttackStrengthScale(0.5f) >= 0.90f;
        if (heavy) {
            force += 1.0;
            state.heavyBlowCooldown = 8 * 20;
            EpicFightCompat.applyImpact(target, 1.0);
        } else if (force > 0.0) {
            EpicFightCompat.applyImpact(target, force);
        }
        if (force > 0.0) target.knockback(force, player.getX() - target.getX(), player.getZ() - target.getZ());
        state.lastAttackTick = now;
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onKnockback(LivingKnockBackEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        MetabolicState state = MetabolicStateStore.get(player);
        if (DietBridge.snapshot(player).actual(NutritionSnapshot.Group.VEGETABLES) >= feast() && state.weatheredCooldown == 0) {
            event.setCanceled(true);
            state.weatheredCooldown = 90 * 20;
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onHurt(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !ColdSweatCompat.isTemperatureDamage(event.getSource())) return;
        MetabolicState state = MetabolicStateStore.get(player);
        if (DietBridge.snapshot(player).actual(NutritionSnapshot.Group.VEGETABLES) >= feast() && state.weatheredCooldown == 0) {
            ColdSweatCompat.pullSafe(player);
            state.weatheredCooldown = 90 * 20;
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
        PRESERVED_MILK_EFFECTS.remove(player.getUUID());
    }

    private static void applyIdentityModifiers(ServerPlayer player, MetabolicState state, NutritionSnapshot nutrition) {
        double fruits = nutrition.actual(NutritionSnapshot.Group.FRUITS);
        double movement = fruits >= feast() && state.sprintTicks >= 60 ? 0.15 : fruits >= prepared() ? 0.10 : fruits >= ordinary() ? 0.05 : 0.0;
        double step = fruits >= feast() && state.sprintTicks >= 60 ? 1.0 : fruits >= prepared() ? 0.5 : 0.0;
        updateAttribute(player, "minecraft:generic.movement_speed", FRUIT_MOVE, movement, AttributeModifier.Operation.MULTIPLY_TOTAL, "salience_fruit_stride");
        updateAttribute(player, "forge:swim_speed", FRUIT_SWIM, movement, AttributeModifier.Operation.MULTIPLY_TOTAL, "salience_fruit_swim");
        updateAttribute(player, "forge:step_height_addition", FRUIT_STEP, step, AttributeModifier.Operation.ADDITION, "salience_fruit_step");

        double vegetables = nutrition.actual(NutritionSnapshot.Group.VEGETABLES);
        updateAttribute(player, "minecraft:generic.knockback_resistance", VEGETABLE_KNOCKBACK,
                vegetables >= prepared() ? 0.10 : 0.0, AttributeModifier.Operation.ADDITION, "salience_vegetable_weathered");
        double driftResistance = vegetables >= prepared() ? 0.30 : vegetables >= ordinary() ? 0.15 : 0.0;
        if (driftResistance > 0.0) ColdSweatCompat.dampenDrift(player, driftResistance);

        double tempo = state.sugar >= 0.60 ? 0.35 : state.sugar >= 0.25 ? 0.15 : 0.0;
        updateAttribute(player, "minecraft:generic.attack_speed", SUGAR_ATTACK_SPEED, tempo,
                AttributeModifier.Operation.MULTIPLY_TOTAL, "salience_sugar_tempo");
        updateAttribute(player, "tconstruct:player.use_item_speed", SUGAR_USE_SPEED, tempo,
                AttributeModifier.Operation.MULTIPLY_TOTAL, "salience_sugar_use_tempo");

        double control = 0.30 * MetabolicMath.alcoholPositive(state.alcohol) - 0.60 * MetabolicMath.alcoholImpairment(state.alcohol);
        updateAttribute(player, "rpg_stats:recoil_reduction", ALCOHOL_RECOIL, control,
                AttributeModifier.Operation.ADDITION, "salience_alcohol_recoil");
        updateAttribute(player, "rpg_stats:dispersion_reduction", ALCOHOL_DISPERSION, control,
                AttributeModifier.Operation.ADDITION, "salience_alcohol_dispersion");
        updateAttribute(player, "minecraft:generic.attack_speed", ALCOHOL_ATTACK_TIMING,
                -0.35 * MetabolicMath.alcoholImpairment(state.alcohol), AttributeModifier.Operation.MULTIPLY_TOTAL,
                "salience_alcohol_timing");
        EpicFightCompat.reinforceStunShield(player, MetabolicMath.alcoholPositive(state.alcohol));
        if (state.alcohol >= 0.90 && player.tickCount % 80 == 0) {
            player.push((player.getRandom().nextDouble() - 0.5) * 0.45, 0.0, (player.getRandom().nextDouble() - 0.5) * 0.45);
        }
    }

    private static void applyRenewal(ServerPlayer player, MetabolicState state, NutritionSnapshot nutrition) {
        double dairy = nutrition.actual(NutritionSnapshot.Group.DAIRY);
        if (dairy >= feast() && state.dairyCleanseCooldown == 0) {
            for (MobEffectInstance effect : new ArrayList<>(player.getActiveEffects())) {
                if (effect.getEffect().getCategory() == MobEffectCategory.HARMFUL && !effect.isInfiniteDuration()) {
                    player.removeEffect(effect.getEffect());
                    state.dairyCleanseCooldown = 90 * 20;
                    break;
                }
            }
        }
        int interval = dairy >= prepared() ? 5 : dairy >= ordinary() ? 10 : 0;
        if (interval == 0 || player.tickCount % interval != 0) return;
        for (MobEffectInstance effect : player.getActiveEffects()) {
            if (effect.getEffect().getCategory() != MobEffectCategory.HARMFUL || effect.isInfiniteDuration()) continue;
            MobEffectInstanceAccessor accessor = (MobEffectInstanceAccessor) effect;
            accessor.systemicSalience$setDuration(Math.max(1, accessor.systemicSalience$getDuration() - 1));
        }
    }

    private static void applyEnduranceReserve(ServerPlayer player, MetabolicState state, NutritionSnapshot nutrition) {
        if (nutrition.actual(NutritionSnapshot.Group.FATS) < feast() || state.enduranceReserveCooldown > 0) return;
        if (player.getFoodData().getFoodLevel() <= 2 || ThirstCompat.isLow(player) || EpicFightCompat.isStaminaBelow(player, 0.10)) {
            state.enduranceReserveTicks = 5 * 20;
            state.enduranceReserveCooldown = 2 * 60 * 20;
        }
    }

    private static double ordinary() { return SalienceConfig.ORDINARY_THRESHOLD.get(); }
    private static double prepared() { return SalienceConfig.PREPARED_THRESHOLD.get(); }
    private static double feast() { return SalienceConfig.FEAST_THRESHOLD.get(); }

    private static void updateAttribute(ServerPlayer player, String id, UUID uuid, double amount,
                                        AttributeModifier.Operation operation, String name) {
        Attribute attribute = ForgeRegistries.ATTRIBUTES.getValue(new ResourceLocation(id));
        if (attribute == null) return;
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance == null) return;
        AttributeModifier existing = instance.getModifier(uuid);
        if (existing != null) instance.removeModifier(existing);
        if (amount != 0.0) instance.addTransientModifier(new AttributeModifier(uuid, name, amount, operation));
    }
}
