package com.bettercontent.systemicsalience.client;

import com.bettercontent.systemicsalience.SystemicSalienceMod;
import com.bettercontent.systemicsalience.metabolism.ConsumableProfiles;
import com.bettercontent.systemicsalience.metabolism.MetabolicMath;
import com.bettercontent.systemicsalience.network.MetabolicSyncPacket;
import com.bettercontent.systemicsalience.presentation.AspectIdentity;
import com.bettercontent.systemicsalience.presentation.NutritionTier;
import com.bettercontent.systemicsalience.presentation.PresentationFlags;
import com.illusivesoulworks.diet.api.DietApi;
import com.illusivesoulworks.diet.api.type.IDietGroup;
import com.illusivesoulworks.diet.client.screen.DietScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Mod.EventBusSubscriber(modid = SystemicSalienceMod.MOD_ID, value = Dist.CLIENT)
public final class ClientSalienceEvents {
    private static final int PANEL_WIDTH = 212;
    private static final ResourceLocation ICONS = new ResourceLocation(SystemicSalienceMod.MOD_ID, "textures/gui/nutrition_states.png");

    private ClientSalienceEvents() {}

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) MealRecap.tick();
    }

    @SubscribeEvent
    public static void onHud(RenderGuiOverlayEvent.Post event) {
        if (!event.getOverlay().id().equals(VanillaGuiOverlay.HOTBAR.id())) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.options.hideGui || minecraft.player == null) return;
        MetabolicSyncPacket state = ClientMetabolicState.snapshot();
        List<Icon> active = activeIcons(state);
        int y = event.getWindow().getGuiScaledHeight() - 72;
        if (!active.isEmpty()) {
            int width = active.size() * 20 - 2;
            int x = (event.getWindow().getGuiScaledWidth() - width) / 2;
            for (Icon icon : active) {
                event.getGuiGraphics().blit(ICONS, x, y, icon.aspect.icon * 18, 0, 18, 18, 144, 18);
                if (icon.overlay != null) event.getGuiGraphics().drawString(minecraft.font, icon.overlay, x + 12, y + 10, 0xffffffff, true);
                x += 20;
            }
        }
        MealRecap.render(event.getGuiGraphics(), minecraft, y);
    }

    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || !minecraft.isSameThread()) return;
        List<AspectIdentity> identities = new ArrayList<>();
        try {
            for (IDietGroup group : DietApi.getInstance().getGroups(minecraft.player, event.getItemStack())) {
                AspectIdentity identity = AspectIdentity.fromGroupName(group.getName());
                if (identity != null && !identities.contains(identity)) identities.add(identity);
            }
        } catch (RuntimeException ignored) { return; }
        if (ConsumableProfiles.sugar(event.getItemStack()) > 0 && !identities.contains(AspectIdentity.TEMPO)) identities.add(AspectIdentity.TEMPO);
        if (ConsumableProfiles.isAlcohol(event.getItemStack()) && !identities.contains(AspectIdentity.CONTROL)) identities.add(AspectIdentity.CONTROL);
        if (identities.isEmpty()) return;
        identities.sort(AspectIdentity.displayOrder());
        MutableComponent line = Component.literal("Nourishes: ").withStyle(style -> style.withColor(0x999999));
        for (int index = 0; index < identities.size(); index++) {
            AspectIdentity identity = identities.get(index);
            if (index > 0) line.append(Component.literal(" · ").withStyle(style -> style.withColor(0x777777)));
            String name = identity == AspectIdentity.TEMPO ? "Sugar" : identity == AspectIdentity.CONTROL ? "Alcohol"
                    : identity.representative.substring(0, 1).toUpperCase() + identity.representative.substring(1);
            line.append(Component.literal(identity.glyph + " " + name).withStyle(style -> style.withColor(identity.color)));
        }
        event.getToolTip().add(line);
    }

    @SubscribeEvent
    public static void onDietScreen(ScreenEvent.Render.Post event) {
        if (!(event.getScreen() instanceof DietScreen)) return;
        Minecraft minecraft = Minecraft.getInstance();
        GuiGraphics graphics = event.getGuiGraphics();
        MetabolicSyncPacket state = ClientMetabolicState.snapshot();
        int x = event.getScreen().width - PANEL_WIDTH - 12;
        int y = 24;
        graphics.fill(x - 6, y - 8, x + PANEL_WIDTH + 6, y + 224, 0xD0101216);
        graphics.drawString(minecraft.font, Component.translatable("systemic_salience.ui.title"), x, y, 0xFFF1F1F1, false);
        y += 15;
        AspectIdentity[] aspects = {AspectIdentity.IMPACT, AspectIdentity.WORK, AspectIdentity.MOBILITY,
                AspectIdentity.ENDURANCE, AspectIdentity.ROBUSTNESS, AspectIdentity.RENEWAL};
        String[] labels = {"proteins", "grains", "fruits", "fats", "vegetables", "dairy"};
        for (int index = 0; index < 6; index++) {
            int rowY = y;
            y = profileBar(graphics, minecraft, x, y, Component.translatable("systemic_salience.ui." + labels[index]),
                    state.nutrient(index), aspects[index].color, aspect(aspects[index]), state);
            if (inside(event.getMouseX(), event.getMouseY(), x, rowY, PANEL_WIDTH, 21)) {
                renderNutrientTooltip(graphics, minecraft, aspects[index], state.nutrient(index), state.nutrientSeconds(index), state,
                        event.getMouseX(), event.getMouseY());
            }
        }
        y += 3;
        int sugarY = y;
        y = profileBar(graphics, minecraft, x, y, Component.translatable("systemic_salience.ui.sugar"), state.sugar(), AspectIdentity.TEMPO.color,
                aspect(AspectIdentity.TEMPO), state);
        y = bar(graphics, minecraft, x, y, Component.translatable("systemic_salience.ui.debt"), state.debt(), 0x956641, false, state);
        int alcoholY = y;
        y = alcoholBar(graphics, minecraft, x, y, state);
        if (inside(event.getMouseX(), event.getMouseY(), x, sugarY, PANEL_WIDTH, 43))
            renderSpecialTooltip(graphics, minecraft, AspectIdentity.TEMPO, sugarTooltip(state), event.getMouseX(), event.getMouseY());
        else if (inside(event.getMouseX(), event.getMouseY(), x, alcoholY, PANEL_WIDTH, y - alcoholY))
            renderSpecialTooltip(graphics, minecraft, AspectIdentity.CONTROL, alcoholTooltip(state), event.getMouseX(), event.getMouseY());
    }

    private static List<Icon> activeIcons(MetabolicSyncPacket state) {
        List<Icon> result = new ArrayList<>(); int flags = state.flags();
        if (PresentationFlags.has(flags, PresentationFlags.IMPACT_READY)) result.add(new Icon(AspectIdentity.IMPACT, null));
        if (state.workSequence() > 0) result.add(new Icon(AspectIdentity.WORK, Integer.toString(state.workSequence())));
        if (PresentationFlags.has(flags, PresentationFlags.MOBILITY_STRIDE)) result.add(new Icon(AspectIdentity.MOBILITY, null));
        if (PresentationFlags.has(flags, PresentationFlags.ENDURANCE_ACTIVE)) result.add(new Icon(AspectIdentity.ENDURANCE, "!"));
        else if (PresentationFlags.has(flags, PresentationFlags.ENDURANCE_READY)) result.add(new Icon(AspectIdentity.ENDURANCE, null));
        if (PresentationFlags.has(flags, PresentationFlags.ROBUSTNESS_READY)) result.add(new Icon(AspectIdentity.ROBUSTNESS, null));
        if (PresentationFlags.has(flags, PresentationFlags.RENEWAL_READY)) result.add(new Icon(AspectIdentity.RENEWAL, null));
        if (PresentationFlags.has(flags, PresentationFlags.TEMPO_TWO)) result.add(new Icon(AspectIdentity.TEMPO, "2"));
        else if (PresentationFlags.has(flags, PresentationFlags.TEMPO_ONE)) result.add(new Icon(AspectIdentity.TEMPO, "1"));
        else if (PresentationFlags.has(flags, PresentationFlags.TEMPO_CRASH)) result.add(new Icon(AspectIdentity.TEMPO, "↓"));
        if (PresentationFlags.has(flags, PresentationFlags.CONTROL_IMPAIRED)) result.add(new Icon(AspectIdentity.CONTROL, "!"));
        else if (PresentationFlags.has(flags, PresentationFlags.CONTROL_COMPOSED)) result.add(new Icon(AspectIdentity.CONTROL, null));
        result.sort(Comparator.comparingInt(icon -> icon.aspect.icon));
        return result;
    }

    private static int profileBar(GuiGraphics graphics, Minecraft minecraft, int x, int y, Component label, float value,
                                  int color, MutableComponent identity, MetabolicSyncPacket state) {
        y = bar(graphics, minecraft, x, y, label, value, color, false, state);
        graphics.drawString(minecraft.font, Component.literal("  ").append(identity), x, y - 2, 0xFFD8D8D8, false);
        return y + 9;
    }

    private static MutableComponent aspect(AspectIdentity aspect) {
        return Component.literal(aspect.glyph + " " + aspect.displayName).withStyle(style -> style.withColor(aspect.color));
    }

    private static int alcoholBar(GuiGraphics graphics, Minecraft minecraft, int x, int y, MetabolicSyncPacket state) {
        y = bar(graphics, minecraft, x, y, Component.translatable("systemic_salience.ui.alcohol"), state.alcohol(), AspectIdentity.CONTROL.color, true, state);
        graphics.drawString(minecraft.font, Component.literal("  ").append(aspect(AspectIdentity.CONTROL)), x, y - 2, 0xFFD8D8D8, false);
        y += 9;
        y = bar(graphics, minecraft, x, y, Component.translatable("systemic_salience.ui.alcohol_benefit"),
                (float) MetabolicMath.alcoholPositive(state.alcohol()), AspectIdentity.CONTROL.color, false, state);
        return bar(graphics, minecraft, x, y, Component.translatable("systemic_salience.ui.alcohol_impairment"),
                (float) MetabolicMath.alcoholImpairment(state.alcohol()), AspectIdentity.IMPACT.color, false, state);
    }

    private static int bar(GuiGraphics graphics, Minecraft minecraft, int x, int y, Component label, float value, int color,
                           boolean centerMarker, MetabolicSyncPacket state) {
        int width = 52; float clamped = Math.max(0, Math.min(1, value));
        graphics.drawString(minecraft.font, label, x, y, 0xFFD8D8D8, false);
        int barX = x + PANEL_WIDTH - width;
        graphics.fill(barX, y + 1, barX + width, y + 8, 0xFF30343B);
        graphics.fill(barX + 1, y + 2, barX + 1 + Math.round((width - 2) * clamped), y + 7, 0xff000000 | color);
        if (centerMarker) graphics.fill(barX + width / 2, y, barX + width / 2 + 1, y + 9, 0xFFFFFFFF);
        else for (float threshold : new float[]{state.ordinary(), state.prepared(), state.feast()}) {
            int marker = barX + 1 + Math.round((width - 2) * threshold);
            graphics.fill(marker, y, marker + 1, y + 9, 0xCCFFFFFF);
        }
        return y + 13;
    }

    private static void renderNutrientTooltip(GuiGraphics graphics, Minecraft minecraft, AspectIdentity aspect, float value, int seconds,
                                              MetabolicSyncPacket state, int mouseX, int mouseY) {
        NutritionTier tier = NutritionTier.of(value, state.ordinary(), state.prepared(), state.feast());
        List<Component> lines = new ArrayList<>(); lines.add(aspect(aspect));
        lines.add(Component.literal(tier == NutritionTier.BUILDING ? "Building · " + Math.round(value * 100) + "%"
                : title(tier.name()) + " · " + MealRecap.duration(seconds)));
        lines.add(Component.translatable("systemic_salience.detail." + aspect.representative + "." + tier.name().toLowerCase()));
        graphics.renderComponentTooltip(minecraft.font, lines, mouseX, mouseY);
    }

    private static void renderSpecialTooltip(GuiGraphics graphics, Minecraft minecraft, AspectIdentity aspect, List<Component> lines,
                                             int mouseX, int mouseY) {
        List<Component> copy = new ArrayList<>(); copy.add(aspect(aspect)); copy.addAll(lines);
        graphics.renderComponentTooltip(minecraft.font, copy, mouseX, mouseY);
    }

    private static List<Component> sugarTooltip(MetabolicSyncPacket state) {
        String tier = state.sugar() >= .60 ? "Tempo II · nutrition burns 5×" : state.sugar() >= .25 ? "Tempo I · nutrition burns 2×"
                : state.debt() >= .25 ? "Metabolic crash" : "Baseline";
        int seconds = state.sugar() >= .25 ? state.sugarSeconds() : state.debtSeconds();
        return List.of(Component.literal(tier), Component.literal("Transition " + MealRecap.duration(seconds)));
    }

    private static List<Component> alcoholTooltip(MetabolicSyncPacket state) {
        String tier = state.alcohol() > .65 ? "Impaired handling" : state.alcohol() >= .35 ? "Composed" : "Low load";
        return List.of(Component.literal(tier), Component.literal("Transition " + MealRecap.duration(state.alcoholSeconds())));
    }

    private static boolean inside(int mouseX, int mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    private static String title(String value) { return value.substring(0, 1) + value.substring(1).toLowerCase(); }
    private record Icon(AspectIdentity aspect, String overlay) {}
}
