package com.bettercontent.systemicsalience.presentation;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Locale;

public enum AspectIdentity {
    IMPACT(0, "proteins", "✦", "Impact", 0xE4717D),
    TEMPO(1, "sugar", "»", "Tempo", 0xAA652B),
    WORK(2, "grains", "⚒", "Work", 0xCAA903),
    MOBILITY(3, "fruits", "➜", "Mobility", 0xC0E304),
    ENDURANCE(4, "fats", "∞", "Endurance", 0x35BBD0),
    ROBUSTNESS(5, "vegetables", "◆", "Robustness", 0x1175FC),
    RENEWAL(6, "dairy", "✚", "Renewal", 0x6FEDBA),
    CONTROL(7, "alcohol", "⊕", "Control", 0x8A6CB2);

    public final int icon;
    public final String representative;
    public final String glyph;
    public final String displayName;
    public final int color;

    AspectIdentity(int icon, String representative, String glyph, String displayName, int color) {
        this.icon = icon;
        this.representative = representative;
        this.glyph = glyph;
        this.displayName = displayName;
        this.color = color;
    }

    public static AspectIdentity fromGroupName(String name) {
        String normalized = (name.contains(":") ? name.substring(name.indexOf(':') + 1) : name).toLowerCase(Locale.ROOT);
        return Arrays.stream(values()).filter(value -> value.representative.equals(normalized)).findFirst().orElse(null);
    }

    public static Comparator<AspectIdentity> displayOrder() { return Comparator.comparingInt(value -> value.icon); }
    public String badge() { return Character.toString(0xE100 + icon); }
}
