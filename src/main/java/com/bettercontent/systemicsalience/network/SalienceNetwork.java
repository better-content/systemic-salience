package com.bettercontent.systemicsalience.network;

import com.bettercontent.systemicsalience.SystemicSalienceMod;
import com.bettercontent.systemicsalience.metabolism.MetabolicState;
import com.bettercontent.systemicsalience.nutrition.NutritionSnapshot;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public final class SalienceNetwork {
    private static final String PROTOCOL = "2";
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(SystemicSalienceMod.MOD_ID, "main"),
            () -> PROTOCOL,
            PROTOCOL::equals,
            PROTOCOL::equals
    );

    private SalienceNetwork() {}

    public static void init() {
        CHANNEL.registerMessage(0, MetabolicSyncPacket.class,
                MetabolicSyncPacket::encode, MetabolicSyncPacket::decode, MetabolicSyncPacket::handle);
    }

    public static void sync(ServerPlayer player, NutritionSnapshot nutrition, MetabolicState state) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new MetabolicSyncPacket(
                nutrition.proteins(), nutrition.grains(), nutrition.fruits(), nutrition.fats(), nutrition.vegetables(), nutrition.dairy(),
                (float) state.sugar, (float) state.debt, (float) state.alcohol, state.workSequence
        ));
    }
}
