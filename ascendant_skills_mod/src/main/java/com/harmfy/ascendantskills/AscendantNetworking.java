package com.harmfy.ascendantskills;

import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public final class AscendantNetworking {
    private AscendantNetworking() {
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1")
                .playToClient(PerkHudPayload.TYPE, PerkHudPayload.STREAM_CODEC, AscendantNetworking::handlePerkHud)
                .playToServer(MinerCyclePayload.TYPE, MinerCyclePayload.STREAM_CODEC, AscendantNetworking::handleMinerCycle);
    }

    private static void handlePerkHud(PerkHudPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            ClientPerkCache.accept(payload);
            AscendantClientHud.accept(payload);
        });
    }

    private static void handleMinerCycle(MinerCyclePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof net.minecraft.server.level.ServerPlayer player) {
                MiningPerks.cycleSelectedMineral(player);
            }
        });
    }
}
