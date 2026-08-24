package com.bettercontent.systemicsalience.network;

import com.bettercontent.systemicsalience.client.ClientMetabolicState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record MetabolicSyncPacket(
        float fruits,
        float grains,
        float proteins,
        float vegetables,
        float sugar,
        float debt,
        float alcohol,
        int longHaulTicks
) {
    public static void encode(MetabolicSyncPacket packet, FriendlyByteBuf buffer) {
        buffer.writeFloat(packet.fruits);
        buffer.writeFloat(packet.grains);
        buffer.writeFloat(packet.proteins);
        buffer.writeFloat(packet.vegetables);
        buffer.writeFloat(packet.sugar);
        buffer.writeFloat(packet.debt);
        buffer.writeFloat(packet.alcohol);
        buffer.writeVarInt(packet.longHaulTicks);
    }

    public static MetabolicSyncPacket decode(FriendlyByteBuf buffer) {
        return new MetabolicSyncPacket(
                buffer.readFloat(), buffer.readFloat(), buffer.readFloat(), buffer.readFloat(),
                buffer.readFloat(), buffer.readFloat(), buffer.readFloat(), buffer.readVarInt()
        );
    }

    public static void handle(MetabolicSyncPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> ClientMetabolicState.accept(packet));
        context.setPacketHandled(true);
    }
}
