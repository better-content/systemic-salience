package com.bettercontent.systemicsalience;

import com.bettercontent.systemicsalience.config.SalienceConfig;
import com.bettercontent.systemicsalience.network.SalienceNetwork;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;

@Mod(SystemicSalienceMod.MOD_ID)
public final class SystemicSalienceMod {
    public static final String MOD_ID = "systemic_salience";

    public SystemicSalienceMod() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, SalienceConfig.SPEC);
        SalienceNetwork.init();
    }
}
