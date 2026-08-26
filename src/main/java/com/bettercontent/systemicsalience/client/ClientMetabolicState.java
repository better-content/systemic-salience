package com.bettercontent.systemicsalience.client;

import com.bettercontent.systemicsalience.network.MetabolicSyncPacket;

public final class ClientMetabolicState {
    private static volatile MetabolicSyncPacket snapshot = MetabolicSyncPacket.EMPTY;

    private ClientMetabolicState() {}

    public static void accept(MetabolicSyncPacket packet) {
        snapshot = packet;
    }

    public static MetabolicSyncPacket snapshot() {
        return snapshot;
    }
}
