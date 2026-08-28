package com.harmfy.ascendantskills;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

public final class CriticalControl {
    private CriticalControl() {
    }

    public static void onLivingIncomingDamage(LivingIncomingDamageEvent event) {
        if (!AscendantConfig.disableVanillaJumpCriticals()) {
            return;
        }
        if (!(event.getSource().getEntity() instanceof ServerPlayer attacker)) {
            return;
        }
        if (!(event.getEntity() instanceof LivingEntity)) {
            return;
        }
        if (!looksLikeVanillaJumpCritical(attacker)) {
            return;
        }

        event.setAmount(event.getAmount() / 1.5F);
    }

    public static void onProjectileImpact(ProjectileImpactEvent event) {
        if (!AscendantConfig.disableVanillaFullyChargedBowCriticals()) {
            return;
        }
        if (event.getProjectile() instanceof AbstractArrow arrow && arrow.isCritArrow()) {
            arrow.setCritArrow(false);
        }
    }

    private static boolean looksLikeVanillaJumpCritical(ServerPlayer player) {
        return player.fallDistance > 0.0F
                && !player.onGround()
                && !player.onClimbable()
                && !player.isInWater()
                && !player.isPassenger()
                && !player.isSprinting();
    }
}
