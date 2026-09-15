package com.harmfy.ascendantskills;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record MinerCyclePayload(int action) implements CustomPacketPayload {
    public static final int CYCLE_ORE = 0;
    public static final MinerCyclePayload CYCLE_ORE_PAYLOAD = new MinerCyclePayload(CYCLE_ORE);
    public static final CustomPacketPayload.Type<MinerCyclePayload> TYPE = new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath(AscendantSkills.MOD_ID, "miner_cycle")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, MinerCyclePayload> STREAM_CODEC = StreamCodec.of(
            MinerCyclePayload::write,
            MinerCyclePayload::read
    );

    private static MinerCyclePayload read(RegistryFriendlyByteBuf buffer) {
        return new MinerCyclePayload(buffer.readVarInt());
    }

    private static void write(RegistryFriendlyByteBuf buffer, MinerCyclePayload payload) {
        buffer.writeVarInt(payload.action());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
