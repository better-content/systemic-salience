package com.bettercontent.systemicsalience.client;

import com.bettercontent.systemicsalience.SystemicSalienceMod;
import com.bettercontent.systemicsalience.network.MetabolicSyncPacket;
import com.bettercontent.systemicsalience.mixin.DietScreenAccessor;
import com.bettercontent.systemicsalience.presentation.AspectIdentity;
import com.bettercontent.systemicsalience.presentation.DietBenefits;
import com.illusivesoulworks.diet.client.screen.DietScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

@Mod.EventBusSubscriber(modid = SystemicSalienceMod.MOD_ID, value = Dist.CLIENT)
public final class DietBenefitsOverlay {
    private DietBenefitsOverlay() {}

    @SubscribeEvent
    public static void render(ScreenEvent.Render.Post event) {
        if (!(event.getScreen() instanceof DietScreen screen)) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) return;
        List<AspectIdentity> rows = ((DietScreenAccessor) screen).systemicSalience$groups().stream()
                .map(group -> AspectIdentity.fromGroupName(group.getName())).filter(java.util.Objects::nonNull).toList();
        if (rows.isEmpty()) return;
        MetabolicSyncPacket state = ClientMetabolicState.snapshot();
        int left = (screen.width - 248) / 2;
        int top = (screen.height - (rows.size() * 20 + 60)) / 2;
        int rowTop = top + 25;
        int hover = event.getMouseX() >= left + 8 && event.getMouseX() <= left + 240
                && event.getMouseY() >= rowTop && event.getMouseY() < rowTop + rows.size() * 20
                ? (event.getMouseY() - rowTop) / 20 : -1;
        var graphics = event.getGuiGraphics();
        int panelX = left + 256;
        int panelWidth = Math.min(218, screen.width - panelX - 8);
        if (panelWidth >= 160) {
            graphics.fill(panelX, top + 13, panelX + panelWidth, top + 207, 0xE010141A);
            graphics.drawString(minecraft.font, "DIET BENEFITS", panelX + 7, top + 19, 0xFFF0E8D8, false);
            for (int index = 0; index < rows.size(); index++) {
                AspectIdentity aspect = rows.get(index);
                int y = top + 36 + index * 20;
                if (index == hover) graphics.fill(panelX + 3, y - 2, panelX + panelWidth - 3, y + 17, 0x66455A67);
                graphics.drawString(minecraft.font, aspect.glyph + " " + title(aspect) + " " + Math.round(DietBenefits.value(state, aspect) * 100) + "%",
                        panelX + 7, y, 0xFF000000 | aspect.color, false);
                graphics.drawString(minecraft.font, fit(minecraft, DietBenefits.current(aspect, state), panelWidth - 15),
                        panelX + 7, y + 9, 0xFFECE8E1, false);
            }
        } else {
            if (top >= 14) graphics.drawCenteredString(minecraft.font,
                    "Hover a food group for its benefits", screen.width / 2, top - 11, 0xFFECE8E1);
        }
        if (hover < 0) return;
        AspectIdentity aspect = rows.get(hover);
        String duration = switch (aspect) {
            case IMPACT -> MealRecap.duration(state.proteinSeconds());
            case WORK -> MealRecap.duration(state.grainSeconds());
            case MOBILITY -> MealRecap.duration(state.fruitSeconds());
            case ENDURANCE -> MealRecap.duration(state.fatSeconds());
            case ROBUSTNESS -> MealRecap.duration(state.vegetableSeconds());
            case RENEWAL -> MealRecap.duration(state.dairySeconds());
            case TEMPO -> MealRecap.duration(state.sugarSeconds());
            case CONTROL -> MealRecap.duration(state.alcoholSeconds());
        };
        graphics.renderTooltip(minecraft.font, List.of(
                Component.literal(title(aspect) + " — " + aspect.displayName).withStyle(style -> style.withColor(aspect.color)),
                Component.literal(DietBenefits.current(aspect, state)),
                Component.literal(aspect == AspectIdentity.TEMPO ? "Sugar builds debt; crash slows recovery" :
                        aspect == AspectIdentity.CONTROL ? "35–65% helps; above 65% impairs" :
                        "50% supported · 75% powerful · 90% signature ability"),
                Component.literal(duration.isEmpty() ? "" : "Tier remaining, at most: " + duration))
                .stream().filter(component -> !component.getString().isEmpty())
                .flatMap(component -> minecraft.font.split(component, Math.max(100, screen.width - 32)).stream()).toList(),
                event.getMouseX(), event.getMouseY());
    }

    private static String title(AspectIdentity aspect) {
        if (aspect == AspectIdentity.TEMPO) return "Sugar";
        return aspect.representative.substring(0, 1).toUpperCase() + aspect.representative.substring(1);
    }

    private static String fit(Minecraft minecraft, String text, int width) {
        if (minecraft.font.width(text) <= width) return text;
        while (!text.isEmpty() && minecraft.font.width(text + "…") > width) text = text.substring(0, text.length() - 1);
        return text + "…";
    }
}
