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
        boolean berserkerActive,
        int aljabaStacks,
        int aljabaMaxStacks,
        int deadeyeStacks,
        int deadeyeMaxStacks,
        int escaramuzadorProgress,
        boolean escaramuzadorReady,
        int maestroCooldownTicks,
        int maestroChargeSeconds,
        int barrageHits,
        int barrageRequiredHits,
        boolean barrageReady,
        int murallaPosture,
        int fortressStacks,
        int fortressMaxStacks,
        int bodyguardCooldownTicks,
        int secondWindCooldownTicks,
        int lastStandActiveTicks,
        int lastStandCooldownTicks,
        int absoluteBastionCooldownTicks,
        boolean provocadorActive,
        int titanStacks,
        int titanMaxStacks
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
                buffer.readBoolean(),
                buffer.readVarInt(),
                buffer.readVarInt(),
                buffer.readVarInt(),
                buffer.readVarInt(),
                buffer.readVarInt(),
                buffer.readBoolean(),
                buffer.readVarInt(),
                buffer.readVarInt(),
                buffer.readVarInt(),
                buffer.readVarInt(),
                buffer.readBoolean(),
                buffer.readVarInt(),
                buffer.readVarInt(),
                buffer.readVarInt(),
                buffer.readVarInt(),
                buffer.readVarInt(),
                buffer.readVarInt(),
                buffer.readVarInt(),
                buffer.readVarInt(),
                buffer.readBoolean(),
                buffer.readVarInt(),
                buffer.readVarInt()
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
        buffer.writeVarInt(payload.aljabaStacks());
        buffer.writeVarInt(payload.aljabaMaxStacks());
        buffer.writeVarInt(payload.deadeyeStacks());
        buffer.writeVarInt(payload.deadeyeMaxStacks());
        buffer.writeVarInt(payload.escaramuzadorProgress());
        buffer.writeBoolean(payload.escaramuzadorReady());
        buffer.writeVarInt(payload.maestroCooldownTicks());
        buffer.writeVarInt(payload.maestroChargeSeconds());
        buffer.writeVarInt(payload.barrageHits());
        buffer.writeVarInt(payload.barrageRequiredHits());
        buffer.writeBoolean(payload.barrageReady());
        buffer.writeVarInt(payload.murallaPosture());
        buffer.writeVarInt(payload.fortressStacks());
        buffer.writeVarInt(payload.fortressMaxStacks());
        buffer.writeVarInt(payload.bodyguardCooldownTicks());
        buffer.writeVarInt(payload.secondWindCooldownTicks());
        buffer.writeVarInt(payload.lastStandActiveTicks());
        buffer.writeVarInt(payload.lastStandCooldownTicks());
        buffer.writeVarInt(payload.absoluteBastionCooldownTicks());
        buffer.writeBoolean(payload.provocadorActive());
        buffer.writeVarInt(payload.titanStacks());
        buffer.writeVarInt(payload.titanMaxStacks());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
