package com.bettercontent.systemicsalience.client;

import com.bettercontent.systemicsalience.SystemicSalienceMod;
import com.bettercontent.systemicsalience.config.SalienceConfig;
import com.bettercontent.systemicsalience.metabolism.MetabolicMath;
import com.bettercontent.systemicsalience.network.MetabolicSyncPacket;
import com.illusivesoulworks.diet.client.screen.DietScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = SystemicSalienceMod.MOD_ID, value = Dist.CLIENT)
public final class ClientSalienceEvents {
    private static final int PANEL_WIDTH = 212;

    private ClientSalienceEvents() {}

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) return;
        MetabolicSyncPacket state = ClientMetabolicState.snapshot();
        if (state.alcohol() >= SalienceConfig.value(SalienceConfig.ALCOHOL_POSITIVE_CUTOFF, 0.85)) minecraft.player.setSprinting(false);
        else if (state.longHaulTicks() > 0 && minecraft.player.input.hasForwardImpulse()) minecraft.player.setSprinting(true);
    }

    @SubscribeEvent
    public static void onScreenRender(ScreenEvent.Render.Post event) {
        if (!(event.getScreen() instanceof DietScreen)) return;
        Minecraft minecraft = Minecraft.getInstance();
        GuiGraphics graphics = event.getGuiGraphics();
        MetabolicSyncPacket state = ClientMetabolicState.snapshot();
        int x = event.getScreen().width - PANEL_WIDTH - 12;
        int y = 24;
        graphics.fill(x - 6, y - 8, x + PANEL_WIDTH + 6, y + 190, 0xD0101216);
        graphics.drawString(minecraft.font, Component.translatable("systemic_salience.ui.title"), x, y, 0xFFF1F1F1, false);
        y += 15;
        y = profileBar(graphics, minecraft, x, y, Component.translatable("systemic_salience.ui.fruits"), state.fruits(), 0xFF6FEDBA,
                aspect("✚", "Renewal", 0xFF6FEDBA), aspect("∞", "Endurance", 0xFF35BBD0));
        y = profileBar(graphics, minecraft, x, y, Component.translatable("systemic_salience.ui.grains"), state.grains(), 0xFFCAA903,
                aspect("⚒", "Work", 0xFFCAA903), aspect("∞", "Endurance", 0xFF35BBD0));
        y = profileBar(graphics, minecraft, x, y, Component.translatable("systemic_salience.ui.proteins"), state.proteins(), 0xFFE4717D,
                aspect("✦", "Impact", 0xFFE4717D), aspect("◆", "Robustness", 0xFF1175FC));
        y = profileBar(graphics, minecraft, x, y, Component.translatable("systemic_salience.ui.vegetables"), state.vegetables(), 0xFF1175FC,
                aspect("◆", "Robustness", 0xFF1175FC), aspect("✚", "Renewal", 0xFF6FEDBA));
        y += 3;
        y = bar(graphics, minecraft, x, y, Component.translatable("systemic_salience.ui.sugar"), state.sugar(), 0xFFAA652B, false);
        y = bar(graphics, minecraft, x, y, Component.translatable("systemic_salience.ui.debt"), state.debt(), 0xFF956641, false);
        y = alcoholBar(graphics, minecraft, x, y, state.alcohol());
        if (state.longHaulTicks() > 0) {
            graphics.drawString(minecraft.font, Component.translatable("systemic_salience.ui.long_haul", state.longHaulTicks() / 20 + 1),
                    x, y + 1, 0xFFCAA903, false);
        }
    }

    private static int profileBar(GuiGraphics graphics, Minecraft minecraft, int x, int y, Component label, float value,
                                  int color, MutableComponent primary, MutableComponent secondary) {
        y = bar(graphics, minecraft, x, y, label, value, color, false);
        MutableComponent profile = Component.literal("  ").append(primary).append(Component.literal("   ")).append(secondary);
        graphics.drawString(minecraft.font, profile, x, y - 2, 0xFFD8D8D8, false);
        return y + 9;
    }

    private static MutableComponent aspect(String glyph, String name, int color) {
        return Component.literal(glyph + " " + name).withStyle(style -> style.withColor(color));
    }

    private static int alcoholBar(GuiGraphics graphics, Minecraft minecraft, int x, int y, float alcohol) {
        y = bar(graphics, minecraft, x, y, Component.translatable("systemic_salience.ui.alcohol"), alcohol, 0xFF8A6CB2, true);
        y = bar(graphics, minecraft, x, y, Component.translatable("systemic_salience.ui.alcohol_benefit"),
                (float) MetabolicMath.alcoholPositive(alcohol), 0xFF35BBD0, false);
        return bar(graphics, minecraft, x, y, Component.translatable("systemic_salience.ui.alcohol_impairment"),
                (float) MetabolicMath.alcoholImpairment(alcohol), 0xFFE4717D, false);
    }

    private static int bar(GuiGraphics graphics, Minecraft minecraft, int x, int y, Component label, float value, int color,
                           boolean centerMarker) {
        int width = 52;
        float clamped = Math.max(0.0f, Math.min(1.0f, value));
        graphics.drawString(minecraft.font, label, x, y, 0xFFD8D8D8, false);
        int barX = x + PANEL_WIDTH - width;
        graphics.fill(barX, y + 1, barX + width, y + 8, 0xFF30343B);
        graphics.fill(barX + 1, y + 2, barX + 1 + Math.round((width - 2) * clamped), y + 7, color);
        if (centerMarker) graphics.fill(barX + width / 2, y, barX + width / 2 + 1, y + 9, 0xFFFFFFFF);
        return y + 13;
    }
}
