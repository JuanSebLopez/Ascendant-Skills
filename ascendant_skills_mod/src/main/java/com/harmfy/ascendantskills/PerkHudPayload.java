package com.harmfy.ascendantskills;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record PerkHudPayload(
        int steelComboStacks,
        int steelComboMaxStacks,
        int criticalEyeCooldownTicks,
        int juggernautActiveTicks,
        int juggernautCooldownTicks,
        int conquerorStacks,
        int conquerorMaxStacks,
        boolean berserkerActive
) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<PerkHudPayload> TYPE = new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath(AscendantSkills.MOD_ID, "perk_hud")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, PerkHudPayload> STREAM_CODEC = StreamCodec.of(
            PerkHudPayload::write,
            PerkHudPayload::read
    );

    private static PerkHudPayload read(RegistryFriendlyByteBuf buffer) {
        return new PerkHudPayload(
                buffer.readVarInt(),
                buffer.readVarInt(),
                buffer.readVarInt(),
                buffer.readVarInt(),
                buffer.readVarInt(),
                buffer.readVarInt(),
                buffer.readVarInt(),
                buffer.readBoolean()
        );
    }

    private static void write(RegistryFriendlyByteBuf buffer, PerkHudPayload payload) {
        buffer.writeVarInt(payload.steelComboStacks());
        buffer.writeVarInt(payload.steelComboMaxStacks());
        buffer.writeVarInt(payload.criticalEyeCooldownTicks());
        buffer.writeVarInt(payload.juggernautActiveTicks());
        buffer.writeVarInt(payload.juggernautCooldownTicks());
        buffer.writeVarInt(payload.conquerorStacks());
        buffer.writeVarInt(payload.conquerorMaxStacks());
        buffer.writeBoolean(payload.berserkerActive());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
