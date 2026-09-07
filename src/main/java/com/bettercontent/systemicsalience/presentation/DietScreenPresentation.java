package com.bettercontent.systemicsalience.presentation;

public final class DietScreenPresentation {
    public static final int MIN_TWO_COLUMN_WIDTH = 300;
    public static final int SECTION_HEADER_HEIGHT = 12;

    private DietScreenPresentation() {}

    public static Layout layout(int screenWidth, int screenHeight) {
        int margin = screenHeight < 220 ? 4 : 8;
        int panelWidth = Math.min(480, Math.max(1, screenWidth - margin * 2));
        int panelHeight = Math.min(232, Math.max(1, screenHeight - margin * 2));
        int panelX = (screenWidth - panelWidth) / 2;
        int panelY = (screenHeight - panelHeight) / 2;
        int padding = panelWidth < 360 ? 8 : 12;
        int gap = panelWidth < 360 ? 8 : 12;
        int columnWidth = Math.max(1, (panelWidth - padding * 2 - gap) / 2);
        int leftX = panelX + padding;
        int rightX = leftX + columnWidth + gap;
        int contentTop = panelY + 27;
        int footerTop = panelY + panelHeight - 24;
        int contentHeight = Math.max(1, footerTop - contentTop - 2);
        int nutrientRowHeight = Math.max(18, Math.min(25,
                (contentHeight - SECTION_HEADER_HEIGHT) / 6));
        return new Layout(panelX, panelY, panelWidth, panelHeight, leftX, rightX,
                columnWidth, contentTop, footerTop, contentHeight, nutrientRowHeight,
                panelWidth < MIN_TWO_COLUMN_WIDTH);
    }

    public static String tierKey(NutritionTier tier) {
        return switch (tier) {
            case BUILDING -> "systemic_salience.ui.tier.undernourished";
            case SUPPORTED -> "systemic_salience.ui.tier.supported";
            case PREPARED -> "systemic_salience.ui.tier.prepared";
            case FEAST -> "systemic_salience.ui.tier.feast";
        };
    }

    public static String sugarStateKey(double sugar, double debt) {
        if (sugar >= .60) return "systemic_salience.ui.sugar.tempo_two";
        if (sugar >= .25) return "systemic_salience.ui.sugar.tempo_one";
        if (debt >= .25) return "systemic_salience.ui.sugar.crash";
        return "systemic_salience.ui.sugar.baseline";
    }

    public static String alcoholStateKey(double alcohol) {
        if (alcohol > .65) return "systemic_salience.ui.alcohol.impaired";
        if (alcohol >= .35) return "systemic_salience.ui.alcohol.composed";
        return "systemic_salience.ui.alcohol.low";
    }

    public record Layout(
            int panelX,
            int panelY,
            int panelWidth,
            int panelHeight,
            int leftX,
            int rightX,
            int columnWidth,
            int contentTop,
            int footerTop,
            int contentHeight,
            int nutrientRowHeight,
            boolean tooNarrow
    ) {}
}
