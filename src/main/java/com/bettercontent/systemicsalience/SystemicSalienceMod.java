package com.bettercontent.systemicsalience;

import com.bettercontent.systemicsalience.network.SalienceNetwork;
import net.minecraftforge.fml.common.Mod;

@Mod(SystemicSalienceMod.MOD_ID)
public final class SystemicSalienceMod {
    public static final String MOD_ID = "systemic_salience";

    public SystemicSalienceMod() {
        SalienceNetwork.init();
    }
}
