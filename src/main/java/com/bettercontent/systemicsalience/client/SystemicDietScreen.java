package com.bettercontent.systemicsalience.client;

import com.bettercontent.systemicsalience.SystemicSalienceMod;
import com.bettercontent.systemicsalience.metabolism.MetabolicMath;
import com.bettercontent.systemicsalience.network.MetabolicSyncPacket;
import com.bettercontent.systemicsalience.presentation.AspectIdentity;
import com.bettercontent.systemicsalience.presentation.DietScreenPresentation;
import com.bettercontent.systemicsalience.presentation.NutritionTier;
import com.illusivesoulworks.diet.api.DietApi;
import com.illusivesoulworks.diet.api.type.IDietGroup;
import com.illusivesoulworks.diet.api.type.IDietTracker;
import com.illusivesoulworks.diet.client.DietKeys;
import com.illusivesoulworks.diet.platform.Services;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public final class SystemicDietScreen extends Screen {
    private static final ResourceLocation BADGES =
            new ResourceLocation(SystemicSalienceMod.MOD_ID, "textures/gui/aspect_badges.png");
    private static final int PANEL_BORDER = 0xFF69717C;
    private static final int PANEL_BACKGROUND = 0xEC101318;
    private static final int SECTION_BACKGROUND = 0xE51B2027;
    private static final int ROW_BACKGROUND = 0xD9181C22;
    private static final int ROW_ALTERNATE = 0xD91D2229;
    private static final int PRIMARY_TEXT = 0xFFF4F5F7;
    private static final int SECONDARY_TEXT = 0xFFB8BEC7;
    private static final int BAR_BACKGROUND = 0xFF30343B;
    private static final int RULE_COLOR = 0xFF3E4650;

    private final Screen inventoryOrigin;
    private double nutritionScroll;
    private int nutritionContentHeight;
    private int nutritionViewportHeight;

    public SystemicDietScreen(Screen inventoryOrigin) {
        super(Component.translatable("systemic_salience.ui.diet_title"));
        this.inventoryOrigin = inventoryOrigin;
    }

    @Override
    protected void init() {
        DietScreenPresentation.Layout layout = DietScreenPresentation.layout(width, height);
        addRenderableWidget(Button.builder(Component.translatable("gui.diet.close"), button -> closeToOrigin())
                .pos(layout.panelX() + layout.panelWidth() / 2 - 40, layout.footerTop() + 3)
                .size(80, 18)
                .build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        DietScreenPresentation.Layout layout = DietScreenPresentation.layout(width, height);
        drawPanel(graphics, layout);

        IDietTracker tracker = minecraft == null || minecraft.player == null
                ? null : Services.CAPABILITY.get(minecraft.player).orElse(null);
        List<NutrientRow> rows = nutrientRows(tracker);
        drawNutrition(graphics, layout, rows, mouseX, mouseY);
        drawMetabolism(graphics, layout, ClientMetabolicState.snapshot());

        graphics.fill(layout.panelX() + 1, layout.footerTop(),
                layout.panelX() + layout.panelWidth() - 1, layout.footerTop() + 1, RULE_COLOR);
        super.render(graphics, mouseX, mouseY, partialTick);
        drawNutrientTooltip(graphics, layout, rows, mouseX, mouseY);
    }

    private void drawPanel(GuiGraphics graphics, DietScreenPresentation.Layout layout) {
        graphics.fill(layout.panelX(), layout.panelY(), layout.panelX() + layout.panelWidth(),
                layout.panelY() + layout.panelHeight(), PANEL_BORDER);
        graphics.fill(layout.panelX() + 1, layout.panelY() + 1,
                layout.panelX() + layout.panelWidth() - 1, layout.panelY() + layout.panelHeight() - 1,
                PANEL_BACKGROUND);
        String titleText = title.getString();
        graphics.drawString(font, titleText, width / 2 - font.width(titleText) / 2,
                layout.panelY() + 8, PRIMARY_TEXT, false);
    }

    private void drawNutrition(GuiGraphics graphics, DietScreenPresentation.Layout layout,
                               List<NutrientRow> rows, int mouseX, int mouseY) {
        int headerY = layout.contentTop();
        graphics.fill(layout.leftX(), headerY, layout.leftX() + layout.columnWidth(),
                headerY + DietScreenPresentation.SECTION_HEADER_HEIGHT, SECTION_BACKGROUND);
        graphics.drawString(font, Component.translatable("systemic_salience.ui.nutrition"),
                layout.leftX() + 4, headerY + 2, PRIMARY_TEXT, false);

        int rowsTop = headerY + DietScreenPresentation.SECTION_HEADER_HEIGHT;
        nutritionViewportHeight = layout.contentHeight() - DietScreenPresentation.SECTION_HEADER_HEIGHT;
        nutritionContentHeight = rows.size() * layout.nutrientRowHeight();
        nutritionScroll = Mth.clamp(nutritionScroll, 0.0,
                Math.max(0, nutritionContentHeight - nutritionViewportHeight));
        graphics.enableScissor(layout.leftX(), rowsTop,
                layout.leftX() + layout.columnWidth(), rowsTop + nutritionViewportHeight);
        if (rows.isEmpty()) {
            graphics.drawString(font, Component.translatable("systemic_salience.ui.nutrition_unavailable"),
                    layout.leftX() + 6, rowsTop + 8, SECONDARY_TEXT, false);
        }
        for (int index = 0; index < rows.size(); index++) {
            int rowY = rowsTop + index * layout.nutrientRowHeight() - (int) nutritionScroll;
            if (rowY + layout.nutrientRowHeight() < rowsTop || rowY > rowsTop + nutritionViewportHeight) continue;
            drawNutrientRow(graphics, layout, rows.get(index), index, rowY);
        }
        graphics.disableScissor();
        drawScrollbar(graphics, layout.leftX() + layout.columnWidth() - 2, rowsTop,
                nutritionViewportHeight, nutritionContentHeight, nutritionScroll);
    }

    private void drawNutrientRow(GuiGraphics graphics, DietScreenPresentation.Layout layout,
                                 NutrientRow row, int index, int rowY) {
        int rowHeight = layout.nutrientRowHeight();
        int x = layout.leftX();
        int right = x + layout.columnWidth();
        graphics.fill(x, rowY, right, rowY + rowHeight - 1,
                index % 2 == 0 ? ROW_BACKGROUND : ROW_ALTERNATE);
        int color = row.aspect == null ? row.group.getColor().getRGB() : row.aspect.color;
        graphics.fill(x, rowY, x + 3, rowY + rowHeight - 1, 0xFF000000 | color);

        int textX;
        if (rowHeight >= 22) {
            graphics.renderItem(new ItemStack(row.group.getIcon()), x + 4, rowY + 3);
            if (row.aspect != null && layout.columnWidth() >= 165) {
                graphics.blit(BADGES, x + 22, rowY + 2, row.aspect.icon * 18f, 0,
                        18, 18, 144, 18);
                textX = x + 42;
            } else textX = x + 23;
        } else textX = x + 5;

        int percent = Math.round(row.value * 100);
        String percentText = percent + "%";
        Component name = Component.translatable("groups." + row.group.getName() + ".name");
        graphics.drawString(font, ellipsize(name.getString(), right - textX - font.width(percentText) - 8),
                textX, rowY + 2, PRIMARY_TEXT, false);
        graphics.drawString(font, percentText, right - font.width(percentText) - 5, rowY + 2,
                PRIMARY_TEXT, false);

        if (rowHeight >= 22) {
            int barWidth = Math.max(34, Math.min(52, layout.columnWidth() / 3));
            int barX = right - barWidth - 5;
            String tierText = ClientMetabolicState.isReady()
                    ? nutrientStateText(row)
                    : Component.translatable("systemic_salience.ui.syncing").getString();
            graphics.drawString(font, ellipsize(tierText, barX - textX - 4), textX, rowY + 13,
                    SECONDARY_TEXT, false);
            if (ClientMetabolicState.isReady()) {
                drawThresholdBar(graphics, barX, rowY + 14, barWidth, row.value,
                        color, ClientMetabolicState.snapshot());
            } else {
                drawSimpleBar(graphics, barX, rowY + 14, barWidth, row.value, color, false);
            }
        }
    }

    private void drawMetabolism(GuiGraphics graphics, DietScreenPresentation.Layout layout,
                                MetabolicSyncPacket state) {
        int x = layout.rightX();
        int width = layout.columnWidth();
        int y = layout.contentTop();
        graphics.fill(x, y, x + width, y + DietScreenPresentation.SECTION_HEADER_HEIGHT, SECTION_BACKGROUND);
        graphics.drawString(font, Component.translatable("systemic_salience.ui.metabolic_load"),
                x + 4, y + 2, PRIMARY_TEXT, false);
        y += DietScreenPresentation.SECTION_HEADER_HEIGHT;
        if (!ClientMetabolicState.isReady()) {
            graphics.drawString(font, Component.translatable("systemic_salience.ui.metabolic_syncing"),
                    x + 6, y + 8, SECONDARY_TEXT, false);
            return;
        }
        int available = layout.contentHeight() - DietScreenPresentation.SECTION_HEADER_HEIGHT;
        int simpleHeight = Math.max(30, Math.min(39, available / 4));
        drawLoadCard(graphics, x, y, width, simpleHeight,
                Component.translatable("systemic_salience.ui.sugar"),
                Component.translatable(DietScreenPresentation.sugarStateKey(state.sugar(), state.debt())),
                state.sugar(), AspectIdentity.TEMPO.color, false);
        y += simpleHeight;
        drawLoadCard(graphics, x, y, width, simpleHeight,
                Component.translatable("systemic_salience.ui.debt"),
                Component.translatable(state.debt() >= .25
                        ? "systemic_salience.ui.debt.active" : "systemic_salience.ui.debt.clear"),
                state.debt(), 0x956641, false);
        y += simpleHeight;
        int alcoholHeight = Math.max(simpleHeight, layout.footerTop() - y - 2);
        drawLoadCard(graphics, x, y, width, alcoholHeight,
                Component.translatable("systemic_salience.ui.alcohol_load"),
                Component.translatable(DietScreenPresentation.alcoholStateKey(state.alcohol())),
                state.alcohol(), AspectIdentity.CONTROL.color, true);
        if (alcoholHeight >= 48) {
            int barX = x + Math.max(62, width / 2);
            int barWidth = x + width - 6 - barX;
            graphics.drawString(font, Component.translatable("systemic_salience.ui.alcohol_benefit"),
                    x + 7, y + 31, SECONDARY_TEXT, false);
            drawSimpleBar(graphics, barX, y + 32, barWidth,
                    (float) MetabolicMath.alcoholPositive(state.alcohol()), AspectIdentity.CONTROL.color, false);
            graphics.drawString(font, Component.translatable("systemic_salience.ui.alcohol_impairment"),
                    x + 7, y + 42, SECONDARY_TEXT, false);
            drawSimpleBar(graphics, barX, y + 43, barWidth,
                    (float) MetabolicMath.alcoholImpairment(state.alcohol()), AspectIdentity.IMPACT.color, false);
        }
    }

    private void drawLoadCard(GuiGraphics graphics, int x, int y, int width, int height,
                              Component label, Component state, float value, int color, boolean centerMarker) {
        graphics.fill(x, y, x + width, y + height - 1, ROW_BACKGROUND);
        graphics.fill(x, y, x + 3, y + height - 1, 0xFF000000 | color);
        String percent = Math.round(value * 100) + "%";
        graphics.drawString(font, ellipsize(label.getString(), width - font.width(percent) - 18),
                x + 7, y + 4, PRIMARY_TEXT, false);
        graphics.drawString(font, percent, x + width - font.width(percent) - 6, y + 4, PRIMARY_TEXT, false);
        int barWidth = Math.max(34, Math.min(58, width / 3));
        int barX = x + width - barWidth - 6;
        graphics.drawString(font, ellipsize(state.getString(), barX - x - 13),
                x + 7, y + 17, SECONDARY_TEXT, false);
        drawSimpleBar(graphics, barX, y + 18, barWidth, value, color, centerMarker);
    }

    private void drawThresholdBar(GuiGraphics graphics, int x, int y, int width, float value,
                                  int color, MetabolicSyncPacket state) {
        drawSimpleBar(graphics, x, y, width, value, color, false);
        for (float threshold : new float[]{state.ordinary(), state.prepared(), state.feast()}) {
            int marker = x + 1 + Math.round((width - 2) * Mth.clamp(threshold, 0, 1));
            graphics.fill(marker, y - 1, marker + 1, y + 7, 0xFFFFFFFF);
        }
    }

    private void drawSimpleBar(GuiGraphics graphics, int x, int y, int width, float value,
                               int color, boolean centerMarker) {
        float clamped = Mth.clamp(value, 0, 1);
        graphics.fill(x, y, x + width, y + 6, BAR_BACKGROUND);
        graphics.fill(x + 1, y + 1, x + 1 + Math.round((width - 2) * clamped), y + 5,
                0xFF000000 | color);
        if (centerMarker) graphics.fill(x + width / 2, y - 1, x + width / 2 + 1, y + 7, 0xFFFFFFFF);
    }

    private void drawScrollbar(GuiGraphics graphics, int x, int y, int viewportHeight,
                               int contentHeight, double offset) {
        if (contentHeight <= viewportHeight) return;
        graphics.fill(x, y, x + 2, y + viewportHeight, 0xAA303741);
        int thumbHeight = Math.max(16, viewportHeight * viewportHeight / contentHeight);
        int maxScroll = contentHeight - viewportHeight;
        int thumbY = y + (int) (offset / maxScroll * (viewportHeight - thumbHeight));
        graphics.fill(x, thumbY, x + 2, thumbY + thumbHeight, 0xFFD4D8DE);
    }

    private void drawNutrientTooltip(GuiGraphics graphics, DietScreenPresentation.Layout layout,
                                     List<NutrientRow> rows, int mouseX, int mouseY) {
        int rowsTop = layout.contentTop() + DietScreenPresentation.SECTION_HEADER_HEIGHT;
        if (mouseX < layout.leftX() || mouseX >= layout.leftX() + layout.columnWidth()
                || mouseY < rowsTop || mouseY >= rowsTop + nutritionViewportHeight) return;
        int index = (mouseY - rowsTop + (int) nutritionScroll) / layout.nutrientRowHeight();
        NutrientRow row = index >= 0 && index < rows.size() ? rows.get(index) : null;
        if (row == null) return;
        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable("groups." + row.group.getName() + ".name"));
        if (!ClientMetabolicState.isReady()) {
            lines.add(Component.translatable("systemic_salience.ui.metabolic_syncing"));
            graphics.renderComponentTooltip(font, lines, mouseX, mouseY);
            return;
        }
        NutritionTier tier = tier(row.value);
        lines.add(Component.translatable(DietScreenPresentation.tierKey(tier))
                .append(Component.literal(" · " + Math.round(row.value * 100) + "%")));
        if (row.packetIndex >= 0) {
            String duration = MealRecap.duration(ClientMetabolicState.snapshot().nutrientSeconds(row.packetIndex));
            if (!duration.isEmpty()) {
                lines.add(Component.translatable("systemic_salience.ui.transition", duration));
            }
        }
        if (row.aspect != null) {
            lines.add(Component.literal(row.aspect.glyph + " " + row.aspect.displayName)
                    .withStyle(style -> style.withColor(row.aspect.color)));
            lines.add(Component.translatable("systemic_salience.detail." + row.aspect.representative
                    + "." + tier.name().toLowerCase(Locale.ROOT)));
        }
        graphics.renderComponentTooltip(font, lines, mouseX, mouseY);
    }

    private List<NutrientRow> nutrientRows(IDietTracker tracker) {
        if (minecraft == null || minecraft.player == null || tracker == null) return List.of();
        try {
            return DietApi.getInstance().getSuite(minecraft.player).getGroups().stream()
                    .sorted(Comparator.comparingInt(IDietGroup::getOrder).thenComparing(IDietGroup::getName))
                    .map(group -> new NutrientRow(group, tracker.getValue(group.getName()),
                            AspectIdentity.fromGroupName(group.getName()), nutrientPacketIndex(group.getName())))
                    .toList();
        } catch (RuntimeException exception) {
            return List.of();
        }
    }

    private NutritionTier tier(float value) {
        MetabolicSyncPacket state = ClientMetabolicState.snapshot();
        return NutritionTier.of(value, state.ordinary(), state.prepared(), state.feast());
    }

    private String nutrientStateText(NutrientRow row) {
        String tier = Component.translatable(DietScreenPresentation.tierKey(tier(row.value))).getString();
        if (row.packetIndex < 0) return tier;
        String duration = MealRecap.duration(ClientMetabolicState.snapshot().nutrientSeconds(row.packetIndex));
        return duration.isEmpty() ? tier : tier + " · " + duration;
    }

    private static int nutrientPacketIndex(String groupName) {
        String normalized = groupName.contains(":")
                ? groupName.substring(groupName.indexOf(':') + 1) : groupName;
        return switch (normalized.toLowerCase(Locale.ROOT)) {
            case "proteins" -> 0;
            case "grains" -> 1;
            case "fruits" -> 2;
            case "fats" -> 3;
            case "vegetables" -> 4;
            case "dairy" -> 5;
            default -> -1;
        };
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        DietScreenPresentation.Layout layout = DietScreenPresentation.layout(width, height);
        int rowsTop = layout.contentTop() + DietScreenPresentation.SECTION_HEADER_HEIGHT;
        if (mouseX >= layout.leftX() && mouseX < layout.leftX() + layout.columnWidth()
                && mouseY >= rowsTop && mouseY < rowsTop + nutritionViewportHeight) {
            nutritionScroll = Mth.clamp(nutritionScroll - delta * layout.nutrientRowHeight(),
                    0.0, Math.max(0, nutritionContentHeight - nutritionViewportHeight));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        Minecraft client = minecraft;
        if (client != null && client.player != null && client.options.keyInventory.matches(keyCode, scanCode)) {
            client.setScreen(new InventoryScreen(client.player));
            return true;
        }
        if (DietKeys.OPEN_GUI.matches(keyCode, scanCode)) {
            closeToOrigin();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void closeToOrigin() {
        if (minecraft == null) return;
        if (inventoryOrigin != null) minecraft.setScreen(inventoryOrigin);
        else onClose();
    }

    private String ellipsize(String text, int maxWidth) {
        if (maxWidth <= 0) return "";
        if (font.width(text) <= maxWidth) return text;
        String ellipsis = "…";
        return font.plainSubstrByWidth(text, Math.max(0, maxWidth - font.width(ellipsis))) + ellipsis;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private record NutrientRow(IDietGroup group, float value, AspectIdentity aspect, int packetIndex) {}
}
