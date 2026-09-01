package com.harmfy.ascendantskills;

import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.SwordItem;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingKnockBackEvent;
import net.neoforged.neoforge.event.entity.player.ArrowLooseEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class CombatPerks {
    private static final float BASE_CRIT_MULTIPLIER = 1.5F;
    private static final int TICKS_PER_SECOND = 20;
    private static final int STEEL_COMBO_MAX_STACKS = 5;
    private static final int STEEL_COMBO_DECAY_TICKS = 3 * TICKS_PER_SECOND;
    private static final int CRITICAL_EYE_COOLDOWN_TICKS = 10 * TICKS_PER_SECOND;
    private static final float CRITICAL_EYE_HEALTH_THRESHOLD = 0.30F;
    private static final int VETERAN_MARK_TICKS = 5 * TICKS_PER_SECOND;
    private static final int JUGGERNAUT_IMMUNITY_TICKS = 5 * TICKS_PER_SECOND;
    private static final int JUGGERNAUT_COOLDOWN_TICKS = 15 * TICKS_PER_SECOND;
    private static final int CONQUEROR_DECAY_TICKS = 5 * TICKS_PER_SECOND;
    private static final int CONQUEROR_MAX_STACKS = 5;
    private static final int FORTRESS_DECAY_TICKS = 5 * TICKS_PER_SECOND;
    private static final int FORTRESS_MAX_STACKS = 5;
    private static final int BODYGUARD_COOLDOWN_TICKS = 10 * TICKS_PER_SECOND;
    private static final int SECOND_WIND_COOLDOWN_TICKS = 20 * TICKS_PER_SECOND;
    private static final int LAST_STAND_ACTIVE_TICKS = 5 * TICKS_PER_SECOND;
    private static final int LAST_STAND_COOLDOWN_TICKS = 40 * TICKS_PER_SECOND;
    private static final int ALJABA_DECAY_TICKS = 5 * TICKS_PER_SECOND;
    private static final int ALJABA_MAX_STACKS = 5;
    private static final int DEADEYE_DECAY_TICKS = 20 * TICKS_PER_SECOND;
    private static final int DEADEYE_MAX_STACKS = 25;
    private static final double WARLORD_AURA_RADIUS = 10.0D;
    private static final double BODYGUARD_RADIUS = 10.0D;
    private static final double PROVOCADOR_RADIUS = 5.0D;
    private static final double TITAN_AURA_RADIUS = 10.0D;
    private static final ResourceLocation GLOBAL_ATTACK_SPEED_ATTACK_SPEED = id("global_attack_speed_attack_speed");
    private static final ResourceLocation STEEL_COMBO_ATTACK_SPEED = id("steel_combo_attack_speed");
    private static final ResourceLocation BERSERKER_ATTACK_SPEED = id("berserker_attack_speed");
    private static final ResourceLocation MURALLA_OFFHAND_MOVE_SPEED = id("muralla_offhand_move_speed");
    private static final ResourceLocation MURALLA_OFFHAND_ATTACK_DAMAGE = id("muralla_offhand_attack_damage");
    private static final ResourceLocation FORTRESS_MELEE_RESISTANCE = id("fortress_melee_resistance");
    private static final ResourceLocation LAST_STAND_GLOBAL_RESISTANCE = id("last_stand_global_resistance");
    private static final ResourceLocation TITAN_AURA_GLOBAL_RESISTANCE = id("titan_aura_global_resistance");
    private static final ResourceLocation ALJABA_RANGED_ATTACK_SPEED = id("aljaba_ranged_attack_speed");
    private static final Map<UUID, SteelCombo> STEEL_COMBOS = new HashMap<>();
    private static final Map<UUID, Long> CRITICAL_EYE_COOLDOWNS = new HashMap<>();
    private static final Map<UUID, VeteranMark> VETERAN_MARKS = new HashMap<>();
    private static final Map<UUID, JuggernautState> JUGGERNAUTS = new HashMap<>();
    private static final Map<UUID, StackingBuff> CONQUEROR_STACKS = new HashMap<>();
    private static final Map<UUID, StackingBuff> FORTRESS_STACKS = new HashMap<>();
    private static final Map<UUID, StackingBuff> ALJABA_STACKS = new HashMap<>();
    private static final Map<UUID, DeadeyeState> DEADEYE_STACKS = new HashMap<>();
    private static final Map<UUID, Long> BODYGUARD_COOLDOWNS = new HashMap<>();
    private static final Map<UUID, Long> SECOND_WIND_COOLDOWNS = new HashMap<>();
    private static final Map<UUID, LastStandState> LAST_STANDS = new HashMap<>();
    private static final Map<UUID, RangedUseSpeed> RANGED_USE_SPEED = new HashMap<>();

    private CombatPerks() {
    }

    public static void onLivingIncomingDamage(LivingIncomingDamageEvent event) {
        DamageSource source = event.getSource();
        ServerPlayer attacker = playerAttacker(source);
        boolean melee = isMelee(source);
        boolean projectile = isProjectile(source);
        boolean explosion = source.is(DamageTypeTags.IS_EXPLOSION);
        float amount = event.getAmount();
        float multiplier = 1.0F;

        if (attacker != null) {
            amount += outgoingFlatDamage(attacker, melee);
            multiplier *= outgoingDamageMultiplier(attacker, melee, projectile, event.getEntity(), source.getDirectEntity());
            if (projectile) {
                boolean rangedCrit = rollRangedCrit(attacker, event.getEntity(), source.getDirectEntity());
                if (rangedCrit) {
                    multiplier *= rangedCritMultiplier(attacker);
                    attacker.crit(event.getEntity());
                }
                recordRangedHit(attacker, rangedCrit);
            }
        }

        if (event.getEntity() instanceof ServerPlayer defender) {
            activateLastStand(defender, amount * multiplier);
            multiplier *= incomingDamageMultiplier(defender, source, melee, projectile, explosion, amount);
            multiplier *= bodyguardMultiplier(defender, source, melee, projectile);
            activateJuggernaut(defender, melee && source.getEntity() instanceof LivingEntity);
        }

        float newAmount = amount * multiplier;
        if (event.getEntity() instanceof ServerPlayer defender) {
            newAmount = applySecondWind(defender, newAmount);
        }
        if (newAmount != event.getAmount()) {
            event.setAmount(Math.max(0.0F, newAmount));
        }
    }

    public static void onLivingDamagePost(LivingDamageEvent.Post event) {
        DamageSource source = event.getSource();
        boolean melee = isMelee(source);
        boolean projectile = isProjectile(source);

        if (event.getEntity() instanceof ServerPlayer defender && melee && event.getNewDamage() > 0.0F && source.getEntity() instanceof LivingEntity attacker && !(attacker instanceof Player)) {
            addFortressStack(defender);
        }

        ServerPlayer attacker = playerAttacker(source);
        if (attacker == null || event.getNewDamage() <= 0.0F) {
            return;
        }

        if (melee) {
            long now = attacker.level().getGameTime();
            if (has(attacker, "danzante_de_acero")) {
                SteelCombo combo = STEEL_COMBOS.computeIfAbsent(attacker.getUUID(), ignored -> new SteelCombo());
                combo.stacks = Math.min(STEEL_COMBO_MAX_STACKS, combo.stacks + 1);
                combo.lastHitTick = now;
                combo.lastDecayTick = now;
                applyModifier(attacker, Attributes.ATTACK_SPEED, STEEL_COMBO_ATTACK_SPEED, combo.stacks * 0.02D, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
            }

            if (has(attacker, "veterano_de_guerra")) {
                VETERAN_MARKS.put(event.getEntity().getUUID(), new VeteranMark(attacker.getUUID(), now + VETERAN_MARK_TICKS));
            }

            if (has(attacker, "berserker")) {
                float lifesteal = isLowHealth(attacker) ? 0.11F : 0.05F;
                attacker.heal(event.getNewDamage() * lifesteal);
            }
        }

        if (projectile && has(attacker, "aljaba_ligera")) {
            addAljabaStack(attacker);
        }
    }

    public static void onLivingDeath(LivingDeathEvent event) {
        ServerPlayer attacker = playerAttacker(event.getSource());
        if (attacker == null) {
            return;
        }

        if (has(attacker, "verdugo")) {
            CRITICAL_EYE_COOLDOWNS.remove(attacker.getUUID());
        }

        if (has(attacker, "conquistador") && event.getEntity().getType().getCategory() == MobCategory.MONSTER) {
            StackingBuff stacks = CONQUEROR_STACKS.computeIfAbsent(attacker.getUUID(), ignored -> new StackingBuff());
            long now = attacker.level().getGameTime();
            stacks.stacks = Math.min(CONQUEROR_MAX_STACKS, stacks.stacks + 1);
            stacks.lastActivityTick = now;
            stacks.lastDecayTick = now;
        }
    }

    public static void onLivingKnockBack(LivingKnockBackEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        long now = player.level().getGameTime();
        JuggernautState state = JUGGERNAUTS.get(player.getUUID());
        if (state != null && state.immunityUntilTick >= now && has(player, "juggernaut")) {
            event.setCanceled(true);
            return;
        }

        if (has(player, "coloso")) {
            event.setStrength(event.getStrength() * 0.4F);
        }
    }

    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        updateSteelCombo(player);
        updateBerserkerAttackSpeed(player);
        updateGlobalAttackSpeed(player);
        updateMurallaPosture(player);
        updateFortressStacks(player);
        updateConquerorStacks(player);
        updateAljabaStacks(player);
        updateDeadeyeStacks(player);
        updateLastStand(player);
        updateTitanAura(player);
        applyPassiveRegen(player);
        syncPerkHud(player);
        if (!player.isUsingItem() || !isRangedUseItem(player.getUseItem())) {
            RANGED_USE_SPEED.remove(player.getUUID());
        }
        pruneExpired(player);
    }

    public static void onLivingUseItemTick(LivingEntityUseItemEvent.Tick event) {
        if (!(event.getEntity() instanceof Player player) || !isRangedUseItem(event.getItem())) {
            return;
        }

        float speed = rangedAttackSpeedMultiplier(player);
        if (speed <= 1.0F) {
            return;
        }

        RangedUseSpeed state = RANGED_USE_SPEED.computeIfAbsent(player.getUUID(), ignored -> new RangedUseSpeed());
        state.extraTicks += speed - 1.0F;
        int ticksToSkip = (int) state.extraTicks;
        if (ticksToSkip > 0) {
            state.extraTicks -= ticksToSkip;
            event.setDuration(Math.max(1, event.getDuration() - ticksToSkip));
        }
    }

    public static void onArrowLoose(ArrowLooseEvent event) {
        if (!(event.getBow().getItem() instanceof BowItem)) {
            return;
        }

        float speed = rangedAttackSpeedMultiplier(event.getEntity());
        if (speed > 1.0F) {
            event.setCharge(Math.round(event.getCharge() * speed));
        }
    }

    public static boolean rollMeleeCrit(ServerPlayer player, Entity target) {
        float chance = meleeCritChance(player);
        return chance > 0.0F && player.getRandom().nextFloat() < chance;
    }

    public static float meleeCritMultiplier(ServerPlayer player, Entity target) {
        return BASE_CRIT_MULTIPLIER + (float) attributeValue(player, AscendantAttributes.MELEE_CRIT_DAMAGE);
    }

    public static boolean consumeCriticalEye(ServerPlayer attacker, Entity target) {
        if (!has(attacker, "verdugo") || !(target instanceof LivingEntity livingTarget)) {
            return false;
        }
        if (livingTarget.getHealth() > livingTarget.getMaxHealth() * CRITICAL_EYE_HEALTH_THRESHOLD) {
            return false;
        }

        long now = attacker.level().getGameTime();
        UUID attackerId = attacker.getUUID();
        if (CRITICAL_EYE_COOLDOWNS.getOrDefault(attackerId, 0L) > now) {
            return false;
        }
        CRITICAL_EYE_COOLDOWNS.put(attackerId, now + CRITICAL_EYE_COOLDOWN_TICKS);
        return true;
    }

    public static float criticalEyeDamageMultiplier(ServerPlayer player, Entity target) {
        return meleeCritMultiplier(player, target) * 1.15F;
    }

    private static float outgoingFlatDamage(ServerPlayer attacker, boolean melee) {
        if (!melee || !has(attacker, "berserker")) {
            return 0.0F;
        }
        ItemStack weapon = attacker.getMainHandItem();
        return weapon.getItem() instanceof SwordItem || weapon.getItem() instanceof AxeItem ? 2.0F : 0.0F;
    }

    private static float outgoingDamageMultiplier(ServerPlayer attacker, boolean melee, boolean projectile, LivingEntity target, Entity directEntity) {
        float multiplier = globalDamageMultiplier(attacker);
        if (melee) {
            if (has(attacker, "berserker") && isLowHealth(attacker)) {
                multiplier *= 1.15F;
            }
            multiplier *= 1.0F + conquerorStacks(attacker) * 0.03F;
            multiplier *= armorShredDamageBonus(attacker, target, AscendantAttributes.MELEE_ARMOR_SHRED);
            if (tauntedTargetDamageBonus(attacker, target)) {
                multiplier *= 1.05F;
            }
        }
        if (projectile) {
            multiplier *= rangedDamageMultiplier(attacker);
            multiplier *= armorShredDamageBonus(attacker, target, AscendantAttributes.RANGED_ARMOR_SHRED);
            if (has(attacker, "francotirador")) {
                double distance = rangedDistance(attacker, target, directEntity);
                if (distance >= 10.0D) {
                    float bonus = (float) Math.min(0.20D, (distance - 10.0D) / 20.0D * 0.20D);
                    multiplier *= 1.0F + bonus;
                }
            }
            if (has(attacker, "maestro_tirador")) {
                multiplier *= 1.30F;
                multiplier *= 1.15F;
            }
            if (tauntedTargetDamageBonus(attacker, target)) {
                multiplier *= 1.05F;
            }
        }
        return multiplier;
    }

    private static float incomingDamageMultiplier(ServerPlayer defender, DamageSource source, boolean melee, boolean projectile, boolean explosion, float originalDamage) {
        float multiplier = globalResistanceMultiplier(defender, source, originalDamage);
        if (melee) {
            multiplier *= resistanceMultiplier(defender, AscendantAttributes.MELEE_RESISTANCE);
        }
        if (projectile) {
            multiplier *= resistanceMultiplier(defender, AscendantAttributes.PROJECTILE_RESISTANCE);
        }
        if (explosion) {
            multiplier *= resistanceMultiplier(defender, AscendantAttributes.EXPLOSION_RESISTANCE);
        }
        if (has(defender, "muralla") && defender.getOffhandItem().getItem() instanceof ShieldItem) {
            if (melee || projectile || explosion) {
                multiplier *= 0.90F;
            }
        }
        if (veteranMarkApplies(defender, source)) {
            multiplier *= 0.90F;
        }
        if (warlordAuraApplies(defender, source)) {
            multiplier *= 0.90F;
        }
        if (tauntedAllyProtectionApplies(defender, source)) {
            multiplier *= 0.90F;
        }
        return multiplier;
    }

    private static float bodyguardMultiplier(ServerPlayer defender, DamageSource source, boolean melee, boolean projectile) {
        if (!(melee || projectile) || source.getEntity() == null) {
            return 1.0F;
        }

        AscendantData.Party party = AscendantData.get(defender.server).partyOf(defender.getUUID()).orElse(null);
        if (party == null) {
            return 1.0F;
        }

        long now = defender.level().getGameTime();
        ServerPlayer closestGuard = null;
        double closestDistance = Double.MAX_VALUE;
        for (UUID memberId : party.members) {
            if (memberId.equals(defender.getUUID()) || BODYGUARD_COOLDOWNS.getOrDefault(memberId, 0L) > now) {
                continue;
            }
            ServerPlayer candidate = defender.server.getPlayerList().getPlayer(memberId);
            if (candidate == null || !candidate.level().dimension().equals(defender.level().dimension()) || !has(candidate, "guardaespaldas")) {
                continue;
            }
            double distance = candidate.distanceToSqr(defender);
            if (distance <= BODYGUARD_RADIUS * BODYGUARD_RADIUS && distance < closestDistance) {
                closestDistance = distance;
                closestGuard = candidate;
            }
        }

        if (closestGuard == null) {
            return 1.0F;
        }
        BODYGUARD_COOLDOWNS.put(closestGuard.getUUID(), now + BODYGUARD_COOLDOWN_TICKS);
        return 0.70F;
    }

    private static boolean veteranMarkApplies(ServerPlayer defender, DamageSource source) {
        Entity attacker = source.getEntity();
        if (attacker == null) {
            return false;
        }
        VeteranMark mark = VETERAN_MARKS.get(attacker.getUUID());
        if (mark == null) {
            return false;
        }
        long now = defender.level().getGameTime();
        if (mark.expiresAtTick < now) {
            VETERAN_MARKS.remove(attacker.getUUID());
            return false;
        }
        return mark.targetPlayer.equals(defender.getUUID());
    }

    private static boolean warlordAuraApplies(ServerPlayer defender, DamageSource source) {
        Entity attacker = source.getEntity();
        return has(defender, "senor_de_la_guerra")
                && attacker instanceof LivingEntity
                && attacker.distanceTo(defender) <= WARLORD_AURA_RADIUS;
    }

    private static boolean tauntedAllyProtectionApplies(ServerPlayer defender, DamageSource source) {
        Entity attacker = source.getEntity();
        if (!(attacker instanceof LivingEntity)) {
            return false;
        }
        AscendantData.Party party = AscendantData.get(defender.server).partyOf(defender.getUUID()).orElse(null);
        if (party == null) {
            return false;
        }
        for (UUID memberId : party.members) {
            if (memberId.equals(defender.getUUID())) {
                continue;
            }
            ServerPlayer provocador = defender.server.getPlayerList().getPlayer(memberId);
            if (provocador != null && has(provocador, "provocador") && provocador.distanceTo(attacker) <= PROVOCADOR_RADIUS) {
                return true;
            }
        }
        return false;
    }

    private static boolean tauntedTargetDamageBonus(ServerPlayer attacker, LivingEntity target) {
        AscendantData.Party party = AscendantData.get(attacker.server).partyOf(attacker.getUUID()).orElse(null);
        if (party == null) {
            return false;
        }
        for (UUID memberId : party.members) {
            if (memberId.equals(attacker.getUUID())) {
                continue;
            }
            ServerPlayer provocador = attacker.server.getPlayerList().getPlayer(memberId);
            if (provocador != null && has(provocador, "provocador") && provocador.distanceTo(target) <= PROVOCADOR_RADIUS) {
                return true;
            }
        }
        return false;
    }

    private static boolean rollRangedCrit(ServerPlayer player, LivingEntity target, Entity directEntity) {
        float chance = rangedCritChance(player);
        return chance > 0.0F && player.getRandom().nextFloat() < chance;
    }

    private static float meleeCritChance(ServerPlayer player) {
        return (float) attributeValue(player, AscendantAttributes.MELEE_CRIT_CHANCE);
    }

    private static float rangedCritChance(ServerPlayer player) {
        return (float) attributeValue(player, AscendantAttributes.RANGED_CRIT_CHANCE) + deadeyeStacks(player) * 0.01F;
    }

    private static float rangedCritMultiplier(ServerPlayer player) {
        return BASE_CRIT_MULTIPLIER + (float) attributeValue(player, AscendantAttributes.RANGED_CRIT_DAMAGE);
    }

    private static void recordRangedHit(ServerPlayer player, boolean crit) {
        if (!has(player, "deadeye")) {
            return;
        }
        long now = player.level().getGameTime();
        DeadeyeState state = DEADEYE_STACKS.computeIfAbsent(player.getUUID(), ignored -> new DeadeyeState());
        if (crit) {
            state.stacks = Math.min(DEADEYE_MAX_STACKS, state.stacks + 1);
            state.lastCritTick = now;
            state.lastDecayTick = now;
        } else if (state.stacks > 0) {
            state.stacks--;
            state.lastDecayTick = now;
        }
    }

    private static double rangedDistance(ServerPlayer player, LivingEntity target, Entity directEntity) {
        Entity origin = directEntity instanceof Projectile ? directEntity : player;
        return origin.distanceTo(target);
    }

    private static float globalDamageMultiplier(ServerPlayer player) {
        return (float) attributeValue(player, AscendantAttributes.GLOBAL_DAMAGE);
    }

    private static float rangedDamageMultiplier(ServerPlayer player) {
        return (float) attributeValue(player, AscendantAttributes.RANGED_DAMAGE);
    }

    private static float globalResistanceMultiplier(ServerPlayer player, DamageSource source, float originalDamage) {
        double rawResistance = Math.max(0.0D, attributeValue(player, AscendantAttributes.GLOBAL_RESISTANCE));
        if (rawResistance <= 0.0D) {
            return 1.0F;
        }

        double softCap = AscendantConfig.globalResistanceSoftCap();
        double softenedResistance = rawResistance <= softCap
                ? rawResistance
                : softCap + (rawResistance - softCap) * AscendantConfig.globalResistanceOverflowMultiplier();
        double damageScale = clamp(originalDamage / AscendantConfig.globalResistanceFullEffectDamage(),
                AscendantConfig.globalResistanceMinimumDamageScale(),
                1.0D);
        double typeMultiplier = source.getEntity() == null
                ? AscendantConfig.globalResistanceEnvironmentalMultiplier()
                : 1.0D;
        double effectiveResistance = Math.min(AscendantConfig.globalResistanceHardCap(), softenedResistance * damageScale * typeMultiplier);
        return (float) (1.0D - effectiveResistance);
    }

    private static float resistanceMultiplier(ServerPlayer player, Holder<Attribute> attribute) {
        double resistance = clamp(attributeValue(player, attribute), 0.0D, 0.95D);
        return (float) (1.0D - resistance);
    }

    private static float armorShredDamageBonus(ServerPlayer attacker, LivingEntity target, Holder<Attribute> attribute) {
        if (target.getArmorValue() <= 0) {
            return 1.0F;
        }
        double shred = clamp(attributeValue(attacker, attribute), 0.0D, 1.0D);
        return (float) (1.0D + shred);
    }

    private static float rangedAttackSpeedMultiplier(LivingEntity player) {
        return (float) (attributeValue(player, AscendantAttributes.GLOBAL_ATTACK_SPEED)
                * attributeValue(player, AscendantAttributes.RANGED_ATTACK_SPEED));
    }

    private static float globalAttackSpeedMultiplier(LivingEntity player) {
        return (float) attributeValue(player, AscendantAttributes.GLOBAL_ATTACK_SPEED);
    }

    private static boolean isRangedUseItem(ItemStack stack) {
        return stack.getItem() instanceof BowItem || stack.getItem() instanceof CrossbowItem;
    }

    private static void activateJuggernaut(ServerPlayer defender, boolean melee) {
        if (!melee || !has(defender, "juggernaut")) {
            return;
        }

        long now = defender.level().getGameTime();
        JuggernautState state = JUGGERNAUTS.computeIfAbsent(defender.getUUID(), ignored -> new JuggernautState());
        if (state.cooldownUntilTick <= now) {
            state.immunityUntilTick = now + JUGGERNAUT_IMMUNITY_TICKS;
            state.cooldownUntilTick = now + JUGGERNAUT_COOLDOWN_TICKS;
        }
    }

    private static void activateLastStand(ServerPlayer defender, float incomingDamage) {
        if (!has(defender, "invencible")) {
            return;
        }
        float predictedHealth = defender.getHealth() - incomingDamage;
        if (predictedHealth > defender.getMaxHealth() * 0.30F) {
            return;
        }

        long now = defender.level().getGameTime();
        LastStandState state = LAST_STANDS.computeIfAbsent(defender.getUUID(), ignored -> new LastStandState());
        if (state.cooldownUntilTick > now) {
            return;
        }
        state.activeUntilTick = now + LAST_STAND_ACTIVE_TICKS;
        state.cooldownUntilTick = now + LAST_STAND_COOLDOWN_TICKS;
        applyModifier(defender, AscendantAttributes.GLOBAL_RESISTANCE, LAST_STAND_GLOBAL_RESISTANCE, 0.50D, AttributeModifier.Operation.ADD_VALUE);
    }

    private static float applySecondWind(ServerPlayer defender, float damage) {
        if (!has(defender, "regenerador")) {
            return damage;
        }
        float predictedHealth = defender.getHealth() - damage;
        if (predictedHealth > 0.0F && predictedHealth > defender.getMaxHealth() * 0.30F) {
            return damage;
        }

        long now = defender.level().getGameTime();
        UUID defenderId = defender.getUUID();
        if (SECOND_WIND_COOLDOWNS.getOrDefault(defenderId, 0L) > now) {
            return damage;
        }
        SECOND_WIND_COOLDOWNS.put(defenderId, now + SECOND_WIND_COOLDOWN_TICKS);
        defender.heal(defender.getMaxHealth() * 0.15F);
        if (predictedHealth <= 0.0F) {
            return Math.min(damage, Math.max(0.0F, defender.getHealth() - 1.0F));
        }
        return damage;
    }

    private static void addFortressStack(ServerPlayer defender) {
        if (!has(defender, "fortaleza")) {
            return;
        }
        long now = defender.level().getGameTime();
        StackingBuff stacks = FORTRESS_STACKS.computeIfAbsent(defender.getUUID(), ignored -> new StackingBuff());
        stacks.stacks = Math.min(FORTRESS_MAX_STACKS, stacks.stacks + 1);
        stacks.lastActivityTick = now;
        stacks.lastDecayTick = now;
        applyModifier(defender, AscendantAttributes.MELEE_RESISTANCE, FORTRESS_MELEE_RESISTANCE, stacks.stacks * 0.02D, AttributeModifier.Operation.ADD_VALUE);
    }

    private static void addAljabaStack(ServerPlayer attacker) {
        long now = attacker.level().getGameTime();
        StackingBuff stacks = ALJABA_STACKS.computeIfAbsent(attacker.getUUID(), ignored -> new StackingBuff());
        stacks.stacks = Math.min(ALJABA_MAX_STACKS, stacks.stacks + 1);
        stacks.lastActivityTick = now;
        stacks.lastDecayTick = now;
        applyModifier(attacker, AscendantAttributes.RANGED_ATTACK_SPEED, ALJABA_RANGED_ATTACK_SPEED, stacks.stacks * 0.02D, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    }

    private static void updateSteelCombo(ServerPlayer player) {
        UUID playerId = player.getUUID();
        SteelCombo combo = STEEL_COMBOS.get(playerId);
        if (!has(player, "danzante_de_acero")) {
            STEEL_COMBOS.remove(playerId);
            removeModifier(player, Attributes.ATTACK_SPEED, STEEL_COMBO_ATTACK_SPEED);
            return;
        }
        if (combo == null || combo.stacks <= 0) {
            removeModifier(player, Attributes.ATTACK_SPEED, STEEL_COMBO_ATTACK_SPEED);
            return;
        }

        long now = player.level().getGameTime();
        if (now - combo.lastHitTick >= STEEL_COMBO_DECAY_TICKS && now - combo.lastDecayTick >= STEEL_COMBO_DECAY_TICKS) {
            combo.stacks--;
            combo.lastDecayTick = now;
        }
        if (combo.stacks <= 0) {
            STEEL_COMBOS.remove(playerId);
            removeModifier(player, Attributes.ATTACK_SPEED, STEEL_COMBO_ATTACK_SPEED);
        } else {
            applyModifier(player, Attributes.ATTACK_SPEED, STEEL_COMBO_ATTACK_SPEED, combo.stacks * 0.02D, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        }
    }

    private static void updateBerserkerAttackSpeed(ServerPlayer player) {
        if (has(player, "berserker") && isLowHealth(player)) {
            applyModifier(player, Attributes.ATTACK_SPEED, BERSERKER_ATTACK_SPEED, 0.12D, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        } else {
            removeModifier(player, Attributes.ATTACK_SPEED, BERSERKER_ATTACK_SPEED);
        }
    }

    private static void updateGlobalAttackSpeed(ServerPlayer player) {
        double amount = globalAttackSpeedMultiplier(player) - 1.0D;
        if (Math.abs(amount) > 0.0001D) {
            applyModifier(player, Attributes.ATTACK_SPEED, GLOBAL_ATTACK_SPEED_ATTACK_SPEED, amount, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        } else {
            removeModifier(player, Attributes.ATTACK_SPEED, GLOBAL_ATTACK_SPEED_ATTACK_SPEED);
        }
    }

    private static void updateMurallaPosture(ServerPlayer player) {
        if (!has(player, "muralla")) {
            removeModifier(player, Attributes.MOVEMENT_SPEED, MURALLA_OFFHAND_MOVE_SPEED);
            removeModifier(player, Attributes.ATTACK_DAMAGE, MURALLA_OFFHAND_ATTACK_DAMAGE);
            return;
        }
        if (player.getOffhandItem().getItem() instanceof ShieldItem) {
            removeModifier(player, Attributes.MOVEMENT_SPEED, MURALLA_OFFHAND_MOVE_SPEED);
            removeModifier(player, Attributes.ATTACK_DAMAGE, MURALLA_OFFHAND_ATTACK_DAMAGE);
            return;
        }
        applyModifier(player, Attributes.MOVEMENT_SPEED, MURALLA_OFFHAND_MOVE_SPEED, 0.10D, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        applyModifier(player, Attributes.ATTACK_DAMAGE, MURALLA_OFFHAND_ATTACK_DAMAGE, 0.10D, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    }

    private static void updateFortressStacks(ServerPlayer player) {
        updateStackingResistance(player, FORTRESS_STACKS, FORTRESS_MAX_STACKS, FORTRESS_DECAY_TICKS,
                AscendantAttributes.MELEE_RESISTANCE, FORTRESS_MELEE_RESISTANCE, "fortaleza", 0.02D);
    }

    private static void updateConquerorStacks(ServerPlayer player) {
        updateStackingMap(player, CONQUEROR_STACKS, CONQUEROR_MAX_STACKS, CONQUEROR_DECAY_TICKS);
    }

    private static void updateAljabaStacks(ServerPlayer player) {
        if (!has(player, "aljaba_ligera")) {
            ALJABA_STACKS.remove(player.getUUID());
            removeModifier(player, AscendantAttributes.RANGED_ATTACK_SPEED, ALJABA_RANGED_ATTACK_SPEED);
            return;
        }
        updateStackingMap(player, ALJABA_STACKS, ALJABA_MAX_STACKS, ALJABA_DECAY_TICKS);
        StackingBuff stacks = ALJABA_STACKS.get(player.getUUID());
        if (stacks == null || stacks.stacks <= 0) {
            removeModifier(player, AscendantAttributes.RANGED_ATTACK_SPEED, ALJABA_RANGED_ATTACK_SPEED);
        } else {
            applyModifier(player, AscendantAttributes.RANGED_ATTACK_SPEED, ALJABA_RANGED_ATTACK_SPEED, stacks.stacks * 0.02D, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        }
    }

    private static void updateDeadeyeStacks(ServerPlayer player) {
        if (!has(player, "deadeye")) {
            DEADEYE_STACKS.remove(player.getUUID());
            return;
        }
        DeadeyeState state = DEADEYE_STACKS.get(player.getUUID());
        if (state == null || state.stacks <= 0) {
            return;
        }
        long now = player.level().getGameTime();
        if (now - state.lastCritTick >= DEADEYE_DECAY_TICKS && now - state.lastDecayTick >= DEADEYE_DECAY_TICKS) {
            state.stacks--;
            state.lastDecayTick = now;
        }
        if (state.stacks <= 0) {
            DEADEYE_STACKS.remove(player.getUUID());
        }
    }

    private static void updateLastStand(ServerPlayer player) {
        LastStandState state = LAST_STANDS.get(player.getUUID());
        if (state == null || state.activeUntilTick > player.level().getGameTime()) {
            return;
        }
        removeModifier(player, AscendantAttributes.GLOBAL_RESISTANCE, LAST_STAND_GLOBAL_RESISTANCE);
    }

    private static void updateTitanAura(ServerPlayer player) {
        boolean hasAura = false;
        AscendantData.Party party = AscendantData.get(player.server).partyOf(player.getUUID()).orElse(null);
        if (party != null) {
            for (UUID memberId : party.members) {
                if (memberId.equals(player.getUUID())) {
                    continue;
                }
                ServerPlayer titan = player.server.getPlayerList().getPlayer(memberId);
                if (titan != null && titan.level().dimension().equals(player.level().dimension()) && has(titan, "titan") && titan.distanceTo(player) <= TITAN_AURA_RADIUS) {
                    hasAura = true;
                    break;
                }
            }
        }
        if (hasAura) {
            applyModifier(player, AscendantAttributes.GLOBAL_RESISTANCE, TITAN_AURA_GLOBAL_RESISTANCE, 0.05D, AttributeModifier.Operation.ADD_VALUE);
        } else {
            removeModifier(player, AscendantAttributes.GLOBAL_RESISTANCE, TITAN_AURA_GLOBAL_RESISTANCE);
        }
    }

    private static void applyPassiveRegen(ServerPlayer player) {
        double heal = attributeValue(player, AscendantAttributes.PASSIVE_REGEN_HEALTH);
        if (heal <= 0.0D || player.getHealth() >= player.getMaxHealth()) {
            return;
        }
        int intervalTicks = Math.max(TICKS_PER_SECOND, (int) Math.round(attributeValue(player, AscendantAttributes.PASSIVE_REGEN_INTERVAL) * TICKS_PER_SECOND));
        if (player.tickCount % intervalTicks == 0) {
            player.heal((float) heal);
        }
    }

    private static void updateStackingResistance(ServerPlayer player, Map<UUID, StackingBuff> states, int maxStacks, int decayTicks,
                                                 Holder<Attribute> attribute, ResourceLocation modifierId, String perkId, double perStack) {
        if (!has(player, perkId)) {
            states.remove(player.getUUID());
            removeModifier(player, attribute, modifierId);
            return;
        }
        updateStackingMap(player, states, maxStacks, decayTicks);
        StackingBuff stacks = states.get(player.getUUID());
        if (stacks == null || stacks.stacks <= 0) {
            removeModifier(player, attribute, modifierId);
        } else {
            applyModifier(player, attribute, modifierId, stacks.stacks * perStack, AttributeModifier.Operation.ADD_VALUE);
        }
    }

    private static void updateStackingMap(ServerPlayer player, Map<UUID, StackingBuff> states, int maxStacks, int decayTicks) {
        UUID playerId = player.getUUID();
        StackingBuff stacks = states.get(playerId);
        if (stacks == null || stacks.stacks <= 0) {
            states.remove(playerId);
            return;
        }
        stacks.stacks = Math.min(maxStacks, stacks.stacks);
        long now = player.level().getGameTime();
        if (now - stacks.lastActivityTick >= decayTicks && now - stacks.lastDecayTick >= decayTicks) {
            stacks.stacks--;
            stacks.lastDecayTick = now;
        }
        if (stacks.stacks <= 0) {
            states.remove(playerId);
        }
    }

    private static void syncPerkHud(ServerPlayer player) {
        if (player.tickCount % 10 != 0) {
            return;
        }

        PacketDistributor.sendToPlayer(player, new PerkHudPayload(
                has(player, "danzante_de_acero") ? steelComboStacks(player) : -1,
                STEEL_COMBO_MAX_STACKS,
                has(player, "verdugo") ? remainingTicks(player, CRITICAL_EYE_COOLDOWNS.getOrDefault(player.getUUID(), 0L)) : -1,
                remainingJuggernautActiveTicks(player),
                has(player, "juggernaut") ? remainingJuggernautCooldownTicks(player) : -1,
                has(player, "conquistador") ? conquerorStacks(player) : -1,
                CONQUEROR_MAX_STACKS,
                has(player, "berserker") && isLowHealth(player)
        ));
    }

    private static int steelComboStacks(ServerPlayer player) {
        SteelCombo combo = STEEL_COMBOS.get(player.getUUID());
        return combo == null ? 0 : Math.max(0, combo.stacks);
    }

    private static int conquerorStacks(ServerPlayer player) {
        StackingBuff stacks = CONQUEROR_STACKS.get(player.getUUID());
        return stacks == null ? 0 : Math.max(0, stacks.stacks);
    }

    private static int deadeyeStacks(ServerPlayer player) {
        DeadeyeState state = DEADEYE_STACKS.get(player.getUUID());
        return state == null ? 0 : Math.max(0, state.stacks);
    }

    private static int remainingJuggernautActiveTicks(ServerPlayer player) {
        JuggernautState state = JUGGERNAUTS.get(player.getUUID());
        return state == null ? 0 : remainingTicks(player, state.immunityUntilTick);
    }

    private static int remainingJuggernautCooldownTicks(ServerPlayer player) {
        JuggernautState state = JUGGERNAUTS.get(player.getUUID());
        return state == null ? 0 : remainingTicks(player, state.cooldownUntilTick);
    }

    private static int remainingTicks(ServerPlayer player, long readyAtTick) {
        long remaining = readyAtTick - player.level().getGameTime();
        return remaining <= 0L ? 0 : (int) Math.min(Integer.MAX_VALUE, remaining);
    }

    private static void applyModifier(ServerPlayer player, Holder<Attribute> attribute, ResourceLocation id, double amount, AttributeModifier.Operation operation) {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance != null) {
            instance.addOrUpdateTransientModifier(new AttributeModifier(id, amount, operation));
        }
    }

    private static void removeModifier(ServerPlayer player, Holder<Attribute> attribute, ResourceLocation id) {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance != null) {
            instance.removeModifier(id);
        }
    }

    private static void pruneExpired(ServerPlayer player) {
        long now = player.level().getGameTime();
        VETERAN_MARKS.entrySet().removeIf(entry -> entry.getValue().expiresAtTick < now);
        JUGGERNAUTS.entrySet().removeIf(entry -> entry.getValue().cooldownUntilTick < now && entry.getValue().immunityUntilTick < now);
        BODYGUARD_COOLDOWNS.entrySet().removeIf(entry -> entry.getValue() < now);
        SECOND_WIND_COOLDOWNS.entrySet().removeIf(entry -> entry.getValue() < now);
    }

    private static boolean isLowHealth(ServerPlayer player) {
        return player.getHealth() <= player.getMaxHealth() * 0.20F;
    }

    private static ServerPlayer playerAttacker(DamageSource source) {
        if (source.getEntity() instanceof ServerPlayer player) {
            return player;
        }
        return null;
    }

    private static boolean isMelee(DamageSource source) {
        Entity attacker = source.getEntity();
        return attacker instanceof LivingEntity && source.getDirectEntity() == attacker;
    }

    private static boolean isProjectile(DamageSource source) {
        return source.getDirectEntity() instanceof Projectile || source.is(DamageTypeTags.IS_PROJECTILE);
    }

    private static double attributeValue(LivingEntity entity, Holder<Attribute> attribute) {
        AttributeInstance instance = entity.getAttribute(attribute);
        return instance == null ? attribute.value().getDefaultValue() : instance.getValue();
    }

    private static boolean has(ServerPlayer player, String perkId) {
        return AscendantData.get(player.server).hasPerk(player.getUUID(), AscendantSkills.MOD_ID + ":" + perkId);
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(AscendantSkills.MOD_ID, path);
    }

    private static final class SteelCombo {
        private int stacks;
        private long lastHitTick;
        private long lastDecayTick;
    }

    private record VeteranMark(UUID targetPlayer, long expiresAtTick) {
    }

    private static final class JuggernautState {
        private long immunityUntilTick;
        private long cooldownUntilTick;
    }

    private static final class StackingBuff {
        private int stacks;
        private long lastActivityTick;
        private long lastDecayTick;
    }

    private static final class DeadeyeState {
        private int stacks;
        private long lastCritTick;
        private long lastDecayTick;
    }

    private static final class LastStandState {
        private long activeUntilTick;
        private long cooldownUntilTick;
    }

    private static final class RangedUseSpeed {
        private float extraTicks;
    }
}
