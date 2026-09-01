package com.harmfy.ascendantskills;

import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public final class AscendantNetworking {
    private AscendantNetworking() {
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1")
                .playToClient(PerkHudPayload.TYPE, PerkHudPayload.STREAM_CODEC, AscendantNetworking::handlePerkHud);
    }

    private static void handlePerkHud(PerkHudPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> AscendantClientHud.accept(payload));
    }
}
