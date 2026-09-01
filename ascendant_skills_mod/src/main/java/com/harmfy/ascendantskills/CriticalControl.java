package com.harmfy.ascendantskills;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.event.entity.player.CriticalHitEvent;

public final class CriticalControl {
    private CriticalControl() {
    }

    public static void onCriticalHit(CriticalHitEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        if (AscendantConfig.disableVanillaJumpCriticals() && event.isVanillaCritical()) {
            event.setCriticalHit(false);
            event.setDamageMultiplier(1.0F);
        }

        if (CombatPerks.consumeCriticalEye(player, event.getTarget())) {
            event.setCriticalHit(true);
            event.setDamageMultiplier(CombatPerks.criticalEyeDamageMultiplier());
            player.crit(event.getTarget());
            return;
        }

        if (CombatPerks.rollMeleeCrit(player, event.getTarget())) {
            event.setCriticalHit(true);
            event.setDamageMultiplier(CombatPerks.meleeCritMultiplier(player, event.getTarget()));
        }
    }

    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        removeVanillaCriticalArrow(event.getEntity());
    }

    public static void onProjectileImpact(ProjectileImpactEvent event) {
        removeVanillaCriticalArrow(event.getProjectile());
    }

    private static void removeVanillaCriticalArrow(Entity entity) {
        if (!AscendantConfig.disableVanillaFullyChargedBowCriticals()) {
            return;
        }
        if (entity instanceof AbstractArrow arrow && arrow.isCritArrow()) {
            arrow.setCritArrow(false);
        }
    }
}
