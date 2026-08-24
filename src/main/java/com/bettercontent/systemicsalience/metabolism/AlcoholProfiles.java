package com.bettercontent.systemicsalience.metabolism;

import java.util.Map;

public final class AlcoholProfiles {
    private static final Map<String, Double> VALUES = Map.ofEntries(
            Map.entry("brewinandchewin:kombucha", 0.08),
            Map.entry("brewinandchewin:beer", 0.14),
            Map.entry("brewinandchewin:pale_jane", 0.16),
            Map.entry("brewinandchewin:strongroot_ale", 0.18),
            Map.entry("brewinandchewin:steel_toe_stout", 0.20),
            Map.entry("brewinandchewin:mead", 0.22),
            Map.entry("brewinandchewin:rice_wine", 0.22),
            Map.entry("brewinandchewin:egg_grog", 0.24),
            Map.entry("brewinandchewin:bloody_mary", 0.24),
            Map.entry("brewinandchewin:salty_folly", 0.25),
            Map.entry("brewinandchewin:glittering_grenadine", 0.26),
            Map.entry("brewinandchewin:saccharine_rum", 0.28),
            Map.entry("brewinandchewin:red_rum", 0.30),
            Map.entry("brewinandchewin:dread_nog", 0.32),
            Map.entry("brewinandchewin:vodka", 0.38),
            Map.entry("brewinandchewin:withering_dross", 0.42)
    );

    private AlcoholProfiles() {}

    public static double potency(String itemId) {
        return VALUES.getOrDefault(itemId, 0.0);
    }

    public static Map<String, Double> all() {
        return VALUES;
    }
}
