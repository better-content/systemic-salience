package com.bettercontent.systemicsalience.client;

import com.bettercontent.systemicsalience.network.MealFeedbackPacket;
import com.bettercontent.systemicsalience.presentation.AspectIdentity;
import com.bettercontent.systemicsalience.presentation.NutritionEstimates;
import com.bettercontent.systemicsalience.presentation.NutritionTier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.LinkedHashMap;
import java.util.Map;

public final class MealRecap {
    private static final int SETTLE_TICKS = 25;
    private static final int DISPLAY_TICKS = 80;
    private static final Map<AspectIdentity, Entry> ENTRIES = new LinkedHashMap<>();
    private static int settle;
    private static int display;

    private MealRecap() {}

    public static void accept(MealFeedbackPacket packet) {
        if (settle == 0 && display == 0) ENTRIES.clear();
        MetabolicSyncPacketView thresholds = new MetabolicSyncPacketView(ClientMetabolicState.snapshot());
        AspectIdentity[] nutrientAspects = {AspectIdentity.IMPACT, AspectIdentity.WORK, AspectIdentity.MOBILITY,
                AspectIdentity.ENDURANCE, AspectIdentity.ROBUSTNESS, AspectIdentity.RENEWAL};
        for (int index = 0; index < 6; index++) if ((packet.changedMask() & (1 << index)) != 0) {
            NutritionTier tier = NutritionTier.of(packet.nutrients()[index], thresholds.ordinary, thresholds.prepared, thresholds.feast);
            ENTRIES.put(nutrientAspects[index], new Entry(nutrientAspects[index], tier, packet.nutrients()[index], packet.seconds()[index], State.NUTRIENT));
        }
        if (packet.sugarChanged()) {
            State state = packet.sugar() >= .60f ? State.SUGAR_TWO : packet.sugar() >= .25f ? State.SUGAR_ONE : State.SUGAR_CRASH;
            ENTRIES.put(AspectIdentity.TEMPO, new Entry(AspectIdentity.TEMPO, NutritionTier.BUILDING,
                    packet.sugar(), state == State.SUGAR_CRASH ? packet.debtSeconds() : packet.sugarSeconds(), state));
        }
        if (packet.alcoholChanged()) {
            State state = packet.alcohol() > .65f ? State.ALCOHOL_IMPAIRED
                    : packet.alcohol() >= .35f ? State.ALCOHOL_COMPOSED : State.ALCOHOL_LOW;
            ENTRIES.put(AspectIdentity.CONTROL, new Entry(AspectIdentity.CONTROL, NutritionTier.BUILDING,
                    packet.alcohol(), packet.alcoholSeconds(), state));
        }
        settle = SETTLE_TICKS;
        display = 0;
    }

    public static void tick() {
        if (settle > 0) {
            if (--settle == 0) display = DISPLAY_TICKS;
        } else if (display > 0 && --display == 0) ENTRIES.clear();
    }

    public static void render(GuiGraphics graphics, Minecraft minecraft, int anchorY) {
        if (settle > 0 || display <= 0 || ENTRIES.isEmpty()) return;
        int fadeTicks = 30;
        float opacity = display < fadeTicks ? display / (float) fadeTicks : 1.0f;
        int rise = display < fadeTicks ? Math.round((1.0f - opacity) * 10.0f) : 0;
        int lineHeight = 10;
        int width = ENTRIES.values().stream().map(Entry::component).mapToInt(component -> minecraft.font.width(component)).max().orElse(80) + 12;
        int height = ENTRIES.size() * lineHeight + 8;
        int x = (graphics.guiWidth() - width) / 2;
        int y = anchorY - height - 7 - rise;
        int alpha = Math.max(4, Math.round(176 * opacity));
        graphics.fill(x, y, x + width, y + height, alpha << 24 | 0x101216);
        int textAlpha = Math.max(4, Math.round(255 * opacity)) << 24;
        int rowY = y + 4;
        for (Entry entry : ENTRIES.values()) {
            graphics.drawString(minecraft.font, entry.component(), x + 6, rowY, textAlpha | 0xffffff, false);
            rowY += lineHeight;
        }
    }

    public static String duration(int seconds) {
        if (seconds == NutritionEstimates.OVER_THIRTY_MINUTES) return ">30m";
        if (seconds < 0) return "";
        if (seconds < 60) return "~" + Math.max(5, Math.round(seconds / 5.0f) * 5) + "s";
        return "~" + Math.max(1, Math.round(seconds / 60.0f)) + "m";
    }

    private enum State { NUTRIENT, SUGAR_ONE, SUGAR_TWO, SUGAR_CRASH, ALCOHOL_LOW, ALCOHOL_COMPOSED, ALCOHOL_IMPAIRED }

    private record Entry(AspectIdentity aspect, NutritionTier tier, float value, int seconds, State state) {
        MutableComponent component() {
            MutableComponent result = Component.literal(aspect.glyph + " " + label()).withStyle(style -> style.withColor(aspect.color));
            String time = duration(seconds);
            if (!time.isEmpty()) result.append(Component.literal(" · " + time).withStyle(style -> style.withColor(0xaaaaaa)));
            return result;
        }

        String label() {
            return switch (state) {
                case NUTRIENT -> aspect.representative.substring(0, 1).toUpperCase() + aspect.representative.substring(1)
                        + " — " + (tier == NutritionTier.BUILDING ? "Building · " + Math.round(value * 100) + "%"
                        : tier.name().substring(0, 1) + tier.name().substring(1).toLowerCase());
                case SUGAR_ONE -> "Sugar — Tempo I · nutrition burns 2×";
                case SUGAR_TWO -> "Sugar — Tempo II · nutrition burns 5×";
                case SUGAR_CRASH -> "Sugar crash";
                case ALCOHOL_LOW -> "Alcohol — Low";
                case ALCOHOL_COMPOSED -> "Alcohol — Composed";
                case ALCOHOL_IMPAIRED -> "Alcohol — Impaired";
            };
        }
    }

    private static final class MetabolicSyncPacketView {
        final float ordinary, prepared, feast;
        MetabolicSyncPacketView(com.bettercontent.systemicsalience.network.MetabolicSyncPacket packet) {
            ordinary = packet.ordinary(); prepared = packet.prepared(); feast = packet.feast();
        }
    }
}
