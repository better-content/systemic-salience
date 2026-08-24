package com.bettercontent.systemicsalience.client;

import com.bettercontent.systemicsalience.SystemicSalienceMod;
import com.bettercontent.systemicsalience.network.MetabolicSyncPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = SystemicSalienceMod.MOD_ID, value = Dist.CLIENT)
public final class ClientSalienceEvents {
    private static final int PANEL_WIDTH = 150;

    private ClientSalienceEvents() {}

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) return;
        MetabolicSyncPacket state = ClientMetabolicState.snapshot();
        if (state.alcohol() >= 0.85f) minecraft.player.setSprinting(false);
        else if (state.longHaulTicks() > 0 && minecraft.player.input.hasForwardImpulse()) minecraft.player.setSprinting(true);
    }

    @SubscribeEvent
    public static void onScreenRender(ScreenEvent.Render.Post event) {
        if (!event.getScreen().getClass().getName().equals("com.illusivesoulworks.diet.client.screen.DietScreen")) return;
        Minecraft minecraft = Minecraft.getInstance();
        GuiGraphics graphics = event.getGuiGraphics();
        MetabolicSyncPacket state = ClientMetabolicState.snapshot();
        int x = event.getScreen().width - PANEL_WIDTH - 12;
        int y = 24;
        graphics.fill(x - 6, y - 8, x + PANEL_WIDTH + 6, y + 132, 0xD0101216);
        graphics.drawString(minecraft.font, "Bodily State", x, y, 0xFFF1F1F1, false);
        y += 15;
        y = bar(graphics, minecraft, x, y, "Fruits · Renewal", state.fruits(), 0xFF24966A);
        y = bar(graphics, minecraft, x, y, "Grains · Work", state.grains(), 0xFFC5A529);
        y = bar(graphics, minecraft, x, y, "Proteins · Impact", state.proteins(), 0xFFD94B4B);
        y = bar(graphics, minecraft, x, y, "Vegetables · Robustness", state.vegetables(), 0xFF496CC3);
        y += 3;
        y = bar(graphics, minecraft, x, y, "Sugar load", state.sugar(), 0xFFF28E2B);
        y = bar(graphics, minecraft, x, y, "Metabolic debt", state.debt(), 0xFF956641);
        y = bar(graphics, minecraft, x, y, "Alcohol load", state.alcohol(), 0xFF9B58B5);
        if (state.longHaulTicks() > 0) {
            graphics.drawString(minecraft.font, "Long Haul: " + (state.longHaulTicks() / 20 + 1) + "s", x, y + 1, 0xFFC5A529, false);
        }
    }

    private static int bar(GuiGraphics graphics, Minecraft minecraft, int x, int y, String label, float value, int color) {
        int width = 52;
        float clamped = Math.max(0.0f, Math.min(1.0f, value));
        graphics.drawString(minecraft.font, label, x, y, 0xFFD8D8D8, false);
        int barX = x + PANEL_WIDTH - width;
        graphics.fill(barX, y + 1, barX + width, y + 8, 0xFF30343B);
        graphics.fill(barX + 1, y + 2, barX + 1 + Math.round((width - 2) * clamped), y + 7, color);
        return y + 13;
    }
}
