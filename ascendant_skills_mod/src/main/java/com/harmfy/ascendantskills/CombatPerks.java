package com.harmfy.ascendantskills;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

public final class CombatPerks {
    private static final float BASE_CRIT_MULTIPLIER = 1.5F;

    private CombatPerks() {
    }

    public static void onLivingIncomingDamage(LivingIncomingDamageEvent event) {
        float multiplier = 1.0F;
        DamageSource source = event.getSource();
        ServerPlayer attacker = playerAttacker(source);
        boolean melee = isMelee(source);
        boolean projectile = isProjectile(source);

        if (attacker != null) {
            multiplier *= outgoingDamageMultiplier(attacker, melee, projectile, event.getEntity(), source.getDirectEntity());
            if (projectile && rollRangedCrit(attacker, event.getEntity(), source.getDirectEntity())) {
                multiplier *= rangedCritMultiplier(attacker, event.getEntity(), source.getDirectEntity());
                attacker.crit(event.getEntity());
            }
        }

        if (event.getEntity() instanceof ServerPlayer defender) {
            multiplier *= incomingDamageMultiplier(defender, melee, projectile);
        }

        if (multiplier != 1.0F) {
            event.setAmount(event.getAmount() * multiplier);
        }
    }

    public static void onLivingDamagePost(LivingDamageEvent.Post event) {
        ServerPlayer attacker = playerAttacker(event.getSource());
        if (attacker == null || !isMelee(event.getSource()) || !has(attacker, "berserker")) {
            return;
        }
        if (event.getNewDamage() > 0.0F) {
            attacker.heal(event.getNewDamage() * 0.04F);
        }
    }

    public static boolean rollMeleeCrit(ServerPlayer player, Entity target) {
        float chance = meleeCritChance(player, target);
        return chance > 0.0F && player.getRandom().nextFloat() < chance;
    }

    public static float meleeCritMultiplier(ServerPlayer player, Entity target) {
        float multiplier = BASE_CRIT_MULTIPLIER;
        if (has(player, "verdugo")) {
            multiplier += 0.10F;
        }
        return multiplier;
    }

    private static float outgoingDamageMultiplier(ServerPlayer attacker, boolean melee, boolean projectile, LivingEntity target, Entity directEntity) {
        float multiplier = 1.0F;
        if (has(attacker, "combatiente")) {
            multiplier *= 1.05F;
        }
        if (melee) {
            if (has(attacker, "vanguardia")) {
                multiplier *= 1.05F;
            }
            if (has(attacker, "berserker")) {
                multiplier *= attacker.getHealth() <= attacker.getMaxHealth() * 0.20F ? 1.15F : 1.0F;
            }
            if (has(attacker, "juggernaut")) {
                multiplier *= 0.95F;
            }
        }
        if (projectile) {
            if (has(attacker, "cazador")) {
                multiplier *= 1.05F;
            }
            if (has(attacker, "punteria")) {
                multiplier *= 1.05F;
            }
            if (has(attacker, "perforador")) {
                multiplier *= 1.05F;
            }
            if (has(attacker, "maestro_tirador")) {
                multiplier *= 1.10F;
            }
            if (has(attacker, "francotirador")) {
                multiplier *= rangedDistance(attacker, target, directEntity) >= 30.0D ? 1.20F : 0.95F;
            }
        }
        return multiplier;
    }

    private static float incomingDamageMultiplier(ServerPlayer defender, boolean melee, boolean projectile) {
        float multiplier = 1.0F;
        if (has(defender, "bastion")) {
            multiplier *= 0.95F;
        }
        if (has(defender, "titan")) {
            multiplier *= 0.95F;
        }
        if (has(defender, "invencible")) {
            multiplier *= 0.95F;
        }
        if (melee) {
            if (has(defender, "fortaleza")) {
                multiplier *= 0.90F;
            }
            if (has(defender, "juggernaut")) {
                multiplier *= 0.95F;
            }
        }
        if (projectile && has(defender, "muralla")) {
            multiplier *= 0.95F;
        }
        return multiplier;
    }

    private static boolean rollRangedCrit(ServerPlayer player, LivingEntity target, Entity directEntity) {
        float chance = rangedCritChance(player, target, directEntity);
        return chance > 0.0F && player.getRandom().nextFloat() < chance;
    }

    private static float meleeCritChance(ServerPlayer player, Entity target) {
        float chance = 0.0F;
        if (has(player, "verdugo")) {
            chance += 0.05F;
        }
        if (target instanceof LivingEntity living && has(player, "verdugo") && living.getHealth() <= living.getMaxHealth() * 0.25F) {
            chance += 0.05F;
        }
        return chance;
    }

    private static float rangedCritChance(ServerPlayer player, LivingEntity target, Entity directEntity) {
        float chance = 0.0F;
        if (has(player, "cazador")) {
            chance += 0.03F;
        }
        if (has(player, "ojo_certero")) {
            chance += 0.05F;
        }
        if (has(player, "deadeye") && rangedDistance(player, target, directEntity) >= 30.0D) {
            chance += 0.10F;
        }
        return chance;
    }

    private static float rangedCritMultiplier(ServerPlayer player, LivingEntity target, Entity directEntity) {
        float multiplier = BASE_CRIT_MULTIPLIER;
        if (has(player, "ojo_certero")) {
            multiplier += 0.10F;
        }
        if (has(player, "deadeye") && rangedDistance(player, target, directEntity) >= 30.0D) {
            multiplier += 0.20F;
        }
        if (has(player, "maestro_tirador")) {
            multiplier += 0.20F;
        }
        return multiplier;
    }

    private static double rangedDistance(ServerPlayer player, LivingEntity target, Entity directEntity) {
        Entity origin = directEntity instanceof Projectile ? directEntity : player;
        return origin.distanceTo(target);
    }

    private static ServerPlayer playerAttacker(DamageSource source) {
        if (source.getEntity() instanceof ServerPlayer player) {
            return player;
        }
        return null;
    }

    private static boolean isMelee(DamageSource source) {
        return source.getEntity() instanceof ServerPlayer && source.getDirectEntity() == source.getEntity();
    }

    private static boolean isProjectile(DamageSource source) {
        Entity direct = source.getDirectEntity();
        return direct instanceof Projectile projectile && projectile.getOwner() instanceof ServerPlayer;
    }

    private static boolean has(ServerPlayer player, String perkId) {
        return AscendantData.get(player.server).hasPerk(player.getUUID(), AscendantSkills.MOD_ID + ":" + perkId);
    }
}
