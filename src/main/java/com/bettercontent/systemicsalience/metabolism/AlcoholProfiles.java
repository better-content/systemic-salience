package com.bettercontent.systemicsalience.metabolism;

import com.bettercontent.systemicsalience.config.SalienceConfig;

import java.util.Map;

public final class AlcoholProfiles {
    private AlcoholProfiles() {}

    public static double potency(String itemId) {
        return all().getOrDefault(itemId, 0.0);
    }

    public static Map<String, Double> all() {
        return SalienceConfig.alcoholPotencies();
    }
}
