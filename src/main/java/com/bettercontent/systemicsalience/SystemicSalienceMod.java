package com.bettercontent.systemicsalience;

import com.bettercontent.systemicsalience.config.SalienceConfig;
import com.bettercontent.systemicsalience.network.SalienceNetwork;
import com.bettercontent.systemicsalience.presentation.ModSounds;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(SystemicSalienceMod.MOD_ID)
public final class SystemicSalienceMod {
    public static final String MOD_ID = "systemic_salience";

    public SystemicSalienceMod() {
        ModSounds.register(FMLJavaModLoadingContext.get().getModEventBus());
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, SalienceConfig.SPEC);
        SalienceNetwork.init();
    }
}
