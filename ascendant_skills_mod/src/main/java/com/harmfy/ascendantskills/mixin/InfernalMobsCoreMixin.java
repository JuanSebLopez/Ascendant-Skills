package com.harmfy.ascendantskills.mixin;

import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "atomicstryker.infernalmobs.common.InfernalMobsCore", remap = false)
public abstract class InfernalMobsCoreMixin {
    @Inject(method = "sendVelocityPacket", at = @At("HEAD"), cancellable = true)
    private void ascendantSkills$sendVelocityPacket(ServerPlayer player, float xv, float yv, float zv, CallbackInfo callbackInfo) {
        if (ascendantSkills$isValidTarget(player)) {
            player.push(xv, yv, zv);
            ascendantSkills$syncMotion(player);
        }
        callbackInfo.cancel();
    }

    @Inject(method = "sendKnockBackPacket", at = @At("HEAD"), cancellable = true)
    private void ascendantSkills$sendKnockBackPacket(ServerPlayer player, float xv, float zv, CallbackInfo callbackInfo) {
        if (ascendantSkills$isValidTarget(player)) {
            ascendantSkills$applyGravityKnockback(player, xv, zv);
            ascendantSkills$syncMotion(player);
        }
        callbackInfo.cancel();
    }

    @Inject(method = "sendAirPacket", at = @At("HEAD"), cancellable = true)
    private void ascendantSkills$sendAirPacket(ServerPlayer player, int air, CallbackInfo callbackInfo) {
        if (ascendantSkills$isValidTarget(player)) {
            player.setAirSupply(air);
        }
        callbackInfo.cancel();
    }

    @Unique
    private static boolean ascendantSkills$isValidTarget(ServerPlayer player) {
        return player != null && player.isAttackable() && player.isAlive();
    }

    @Unique
    private static void ascendantSkills$applyGravityKnockback(ServerPlayer player, float xv, float zv) {
        double horizontalLength = Mth.sqrt(xv * xv + zv * zv);
        if (horizontalLength <= 1.0E-4D) {
            return;
        }

        Vec3 currentMotion = player.getDeltaMovement();
        double x = currentMotion.x / 2.0D - (double) xv / horizontalLength * 0.8D;
        double y = Math.min(currentMotion.y / 2.0D + 0.8D, 0.4000000059604645D);
        double z = currentMotion.z / 2.0D - (double) zv / horizontalLength * 0.8D;
        player.setDeltaMovement(x, y, z);
        player.hasImpulse = true;
    }

    @Unique
    private static void ascendantSkills$syncMotion(ServerPlayer player) {
        player.hurtMarked = true;
        player.connection.send(new ClientboundSetEntityMotionPacket(player));
    }
}
