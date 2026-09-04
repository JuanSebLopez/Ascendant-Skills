package com.harmfy.ascendantskills;

import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import com.harmfy.ascendantskills.mixin.AbstractArrowAccessor;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.neoforge.common.damagesource.DamageContainer;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.living.LivingHealEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingKnockBackEvent;
import net.neoforged.neoforge.event.entity.player.ArrowLooseEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
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
    private static final int SECOND_WIND_COOLDOWN_TICKS = 30 * TICKS_PER_SECOND;
    private static final int LAST_STAND_ACTIVE_TICKS = 5 * TICKS_PER_SECOND;
    private static final int LAST_STAND_COOLDOWN_TICKS = 40 * TICKS_PER_SECOND;
    private static final int ALJABA_DECAY_TICKS = 5 * TICKS_PER_SECOND;
    private static final int ALJABA_MAX_STACKS = 5;
    private static final int DEADEYE_DECAY_TICKS = 20 * TICKS_PER_SECOND;
    private static final int DEADEYE_MAX_STACKS = 25;
    private static final int BARRAGE_REQUIRED_HITS = 5;
    private static final int MAESTRO_CHARGE_TICKS = 5 * TICKS_PER_SECOND;
    private static final int MAESTRO_COOLDOWN_TICKS = 10 * TICKS_PER_SECOND;
    private static final int ABSOLUTE_BASTION_COOLDOWN_TICKS = 15 * TICKS_PER_SECOND;
    private static final int PROJECTILE_STATE_TICKS = 30 * TICKS_PER_SECOND;
    private static final double ESCARAMUZADOR_REQUIRED_DISTANCE = 15.0D;
    private static final double WARLORD_AURA_RADIUS = 10.0D;
    private static final double BODYGUARD_RADIUS = 10.0D;
    private static final double PROVOCADOR_RADIUS = 5.0D;
    private static final double ABSOLUTE_BASTION_RADIUS = 10.0D;
    private static final ResourceLocation GLOBAL_ATTACK_SPEED_ATTACK_SPEED = id("global_attack_speed_attack_speed");
    private static final ResourceLocation STEEL_COMBO_ATTACK_SPEED = id("steel_combo_attack_speed");
    private static final ResourceLocation BERSERKER_ATTACK_SPEED = id("berserker_attack_speed");
    private static final ResourceLocation MURALLA_OFFHAND_MOVE_SPEED = id("muralla_offhand_move_speed");
    private static final ResourceLocation MURALLA_OFFHAND_ATTACK_DAMAGE = id("muralla_offhand_attack_damage");
    private static final ResourceLocation FORTRESS_MELEE_RESISTANCE = id("fortress_melee_resistance");
    private static final ResourceLocation LAST_STAND_GLOBAL_RESISTANCE = id("last_stand_global_resistance");
    private static final ResourceLocation TITAN_GLOBAL_DAMAGE = id("titan_global_damage");
    private static final ResourceLocation TITAN_GLOBAL_ATTACK_SPEED = id("titan_global_attack_speed");
    private static final ResourceLocation TITAN_MOVE_SPEED = id("titan_move_speed");
    private static final ResourceLocation TITAN_GLOBAL_RESISTANCE = id("titan_global_resistance");
    private static final ResourceLocation TITAN_GLOBAL_CRIT_CHANCE = id("titan_global_crit_chance");
    private static final ResourceLocation TITAN_GLOBAL_CRIT_DAMAGE = id("titan_global_crit_damage");
    private static final ResourceLocation TITAN_MAX_HEALTH = id("titan_max_health");
    private static final ResourceLocation ALJABA_RANGED_ATTACK_SPEED = id("aljaba_ranged_attack_speed");
    private static final ResourceLocation DEADEYE_RANGED_CRIT_CHANCE = id("deadeye_ranged_crit_chance");
    private static final Map<UUID, SteelCombo> STEEL_COMBOS = new HashMap<>();
    private static final Map<UUID, Long> CRITICAL_EYE_COOLDOWNS = new HashMap<>();
    private static final Map<UUID, VeteranMark> VETERAN_MARKS = new HashMap<>();
    private static final Map<UUID, JuggernautState> JUGGERNAUTS = new HashMap<>();
    private static final Map<UUID, StackingBuff> CONQUEROR_STACKS = new HashMap<>();
    private static final Map<UUID, StackingBuff> FORTRESS_STACKS = new HashMap<>();
    private static final Map<UUID, StackingBuff> ALJABA_STACKS = new HashMap<>();
    private static final Map<UUID, DeadeyeState> DEADEYE_STACKS = new HashMap<>();
    private static final Map<UUID, BarrageState> BARRAGE_STATES = new HashMap<>();
    private static final Map<UUID, EscaramuzadorState> ESCARAMUZADOR_STATES = new HashMap<>();
    private static final Map<UUID, Long> MAESTRO_COOLDOWNS = new HashMap<>();
    private static final Map<UUID, Long> BODYGUARD_COOLDOWNS = new HashMap<>();
    private static final Map<UUID, Long> SECOND_WIND_COOLDOWNS = new HashMap<>();
    private static final Map<UUID, LastStandState> LAST_STANDS = new HashMap<>();
    private static final Map<UUID, Long> ABSOLUTE_BASTION_COOLDOWNS = new HashMap<>();
    private static final Map<UUID, RangedUseSpeed> RANGED_USE_SPEED = new HashMap<>();
    private static final Map<UUID, ProjectileOrigin> PROJECTILE_ORIGINS = new HashMap<>();
    private static final Map<UUID, Long> MAESTRO_PROJECTILES = new HashMap<>();
    private static final Map<UUID, Long> ESCARAMUZADOR_PROJECTILES = new HashMap<>();
    private static final Map<UUID, Long> BARRAGE_CONSUMED_PROJECTILES = new HashMap<>();
    private static final Map<UUID, Boolean> PROJECTILE_CRITS = new HashMap<>();
    private static final Map<UUID, Long> PROJECTILE_HIT_ENTITIES = new HashMap<>();
    private static final Map<UUID, Long> PROCESSED_RANGED_PROJECTILES = new HashMap<>();
    private static final Map<UUID, Float> DAMAGE_DEBUG_PRE_HEALTH = new HashMap<>();
    private static final Map<UUID, String> ASCENDANT_HEAL_REASONS = new HashMap<>();
    private static final Set<UUID> DAMAGE_DEBUG_PLAYERS = new HashSet<>();

    private CombatPerks() {
    }

    public static void onLivingIncomingDamage(LivingIncomingDamageEvent event) {
        float startingAmount = event.getAmount();
        if (!Float.isFinite(startingAmount) || startingAmount <= 0.0F) {
            if (event.getEntity() instanceof ServerPlayer defender && isDamageDebugEnabled(defender)) {
                debugDamage(defender, "incoming/skip", event.getSource(), event.getOriginalAmount(), startingAmount, startingAmount, false, false, false, "amount <= 0 or invalid");
            }
            event.setAmount(0.0F);
            return;
        }
        DamageSource source = event.getSource();
        ServerPlayer attacker = playerAttacker(source);
        boolean melee = isMelee(source);
        boolean projectile = isProjectile(source);
        boolean explosion = source.is(DamageTypeTags.IS_EXPLOSION);
        float amount = event.getAmount();
        float multiplier = 1.0F;

        if (attacker != null) {
            amount += outgoingFlatDamage(attacker, melee);
            applyArmorShred(event, attacker, melee, projectile, event.getEntity(), source.getDirectEntity());
            multiplier *= outgoingDamageMultiplier(attacker, melee, projectile, event.getEntity(), source.getDirectEntity());
            if (projectile) {
                boolean rangedCrit = rollRangedCrit(attacker, event.getEntity(), source.getDirectEntity());
                if (rangedCrit) {
                    multiplier *= rangedCritMultiplier(attacker);
                    attacker.crit(event.getEntity());
                }
                recordRangedHit(attacker, rangedCrit, source.getDirectEntity());
            }
        }

        if (event.getEntity() instanceof ServerPlayer defender) {
            activateLastStand(defender, amount * multiplier);
            multiplier *= incomingDamageMultiplier(defender, source, melee, projectile, explosion, amount);
            multiplier *= bodyguardMultiplier(defender, source, melee, projectile);
            activateAbsoluteBastion(defender, source, melee, projectile, explosion);
            activateJuggernaut(defender, melee && source.getEntity() instanceof LivingEntity);
        }

        float newAmount = amount * multiplier;
        if (event.getEntity() instanceof ServerPlayer defender) {
            newAmount = applySecondWind(defender, newAmount);
            if (isDamageDebugEnabled(defender)) {
                debugDamage(defender, "incoming", source, event.getOriginalAmount(), event.getAmount(), newAmount, melee, projectile, explosion, "before armor/shield reductions");
            }
        }
        if (newAmount != event.getAmount()) {
            event.setAmount(Math.max(0.0F, newAmount));
        }
    }

    public static void onLivingDamagePre(LivingDamageEvent.Pre event) {
        if (!(event.getEntity() instanceof ServerPlayer defender)) {
            return;
        }
        float damage = event.getNewDamage();
        boolean invalid = !Float.isFinite(damage) || damage < 0.0F;
        if (isDamageDebugEnabled(defender)) {
            DAMAGE_DEBUG_PRE_HEALTH.put(defender.getUUID(), defender.getHealth());
            DamageSource source = event.getSource();
            debugDamage(defender, "pre", source, event.getOriginalDamage(), event.getOriginalDamage(), damage,
                    isMelee(source), isProjectile(source), source.is(DamageTypeTags.IS_EXPLOSION),
                    invalid ? "clamped final damage to 0" : "after armor/shield reductions");
        }
        if (invalid) {
            event.setNewDamage(0.0F);
        }
    }

    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        Entity entity = event.getEntity();
        if (entity.level().isClientSide || !(entity instanceof Projectile projectile) || !(projectile.getOwner() instanceof ServerPlayer owner)) {
            return;
        }

        long now = owner.level().getGameTime();
        UUID projectileId = projectile.getUUID();
        PROJECTILE_ORIGINS.put(projectileId, new ProjectileOrigin(owner.getUUID(), owner.level().dimension().location().toString(), projectile.position(), now + PROJECTILE_STATE_TICKS));

        if (projectile instanceof net.minecraft.world.entity.projectile.AbstractArrow arrow) {
            double velocity = projectileVelocityMultiplier(owner);
            if (Math.abs(velocity - 1.0D) > 0.0001D) {
                arrow.setDeltaMovement(arrow.getDeltaMovement().scale(velocity));
            }
            if (has(owner, "perforador") && canApplyVanillaPiercing(arrow)) {
                ((AbstractArrowAccessor) arrow).ascendantSkills$setPierceLevel((byte) Math.min(Byte.MAX_VALUE, arrow.getPierceLevel() + 1));
            }
        }

        RangedUseSpeed useState = RANGED_USE_SPEED.get(owner.getUUID());
        if (has(owner, "maestro_tirador") && useState != null && useState.useTicks >= MAESTRO_CHARGE_TICKS && remainingTicks(owner, MAESTRO_COOLDOWNS.getOrDefault(owner.getUUID(), 0L)) == 0) {
            MAESTRO_PROJECTILES.put(projectileId, now + PROJECTILE_STATE_TICKS);
            MAESTRO_COOLDOWNS.put(owner.getUUID(), now + MAESTRO_COOLDOWN_TICKS);
            useState.useTicks = 0;
        }

        EscaramuzadorState escaramuzador = ESCARAMUZADOR_STATES.get(owner.getUUID());
        if (has(owner, "escaramuzador") && escaramuzador != null && escaramuzador.ready) {
            ESCARAMUZADOR_PROJECTILES.put(projectileId, now + PROJECTILE_STATE_TICKS);
            escaramuzador.distance = 0.0D;
            escaramuzador.ready = false;
        }
    }

    private static boolean canApplyVanillaPiercing(net.minecraft.world.entity.projectile.AbstractArrow arrow) {
        ResourceLocation entityTypeId = BuiltInRegistries.ENTITY_TYPE.getKey(arrow.getType());
        if ("minecraft".equals(entityTypeId.getNamespace()) || AscendantSkills.MOD_ID.equals(entityTypeId.getNamespace())) {
            return true;
        }
        return !"cataclysm".equals(entityTypeId.getNamespace()) && !arrow.getClass().getName().contains("cataclysm");
    }

    public static void onProjectileImpact(ProjectileImpactEvent event) {
        Projectile projectile = event.getProjectile();
        if (projectile.level().isClientSide || !(projectile.getOwner() instanceof ServerPlayer owner)) {
            return;
        }
        if (event.getRayTraceResult() instanceof EntityHitResult) {
            PROJECTILE_HIT_ENTITIES.put(projectile.getUUID(), projectile.level().getGameTime() + PROJECTILE_STATE_TICKS);
            return;
        }
        recordRangedMiss(owner, projectile);
    }

    public static void onLivingDamagePost(LivingDamageEvent.Post event) {
        DamageSource source = event.getSource();
        boolean melee = isMelee(source);
        boolean projectile = isProjectile(source);

        if (event.getEntity() instanceof ServerPlayer defender && isDamageDebugEnabled(defender)) {
            debugDamagePost(defender, event);
        }

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
                heal(attacker, event.getNewDamage() * lifesteal, "berserker_lifesteal");
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

    public static void onLivingHeal(LivingHealEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        float amount = event.getAmount();
        if (!Float.isFinite(amount) || amount < 0.0F) {
            event.setAmount(0.0F);
            amount = 0.0F;
        }

        if (isDamageDebugEnabled(player)) {
            String reason = ASCENDANT_HEAL_REASONS.getOrDefault(player.getUUID(), "unknown/vanilla/other_mod");
            player.sendSystemMessage(Component.literal(String.format(Locale.ROOT,
                    "[AS Heal] reason=%s amount=%.3f hp=%.2f/%s",
                    reason,
                    amount,
                    player.getHealth(),
                    format(player.getMaxHealth())
            )));
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
        updateBarrage(player);
        updateEscaramuzador(player);
        updateMaestroCharge(player);
        updateProvocadorAggro(player);
        updateLastStand(player);
        updateTitan(player);
        applyPassiveRegen(player);
        syncPerkHud(player);
        if (!player.isUsingItem() && !isChargedCrossbowInHand(player)) {
            RANGED_USE_SPEED.remove(player.getUUID());
        }
        pruneExpired(player);
    }

    public static void onLivingUseItemTick(LivingEntityUseItemEvent.Tick event) {
        if (!(event.getEntity() instanceof Player player) || !isRangedUseItem(event.getItem())) {
            return;
        }

        RangedUseSpeed state = RANGED_USE_SPEED.computeIfAbsent(player.getUUID(), ignored -> new RangedUseSpeed());
        float speed = rangedAttackSpeedMultiplier(player);
        if (speed <= 1.0F) {
            return;
        }

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
        return BASE_CRIT_MULTIPLIER
                + (float) attributeValue(player, AscendantAttributes.MELEE_CRIT_DAMAGE)
                + (float) attributeValue(player, AscendantAttributes.GLOBAL_CRIT_DAMAGE);
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
            if (tauntedTargetDamageBonus(attacker, target)) {
                multiplier *= 1.05F;
            }
        }
        if (projectile) {
            multiplier *= rangedDamageMultiplier(attacker);
            if (has(attacker, "francotirador")) {
                double distance = rangedDistance(attacker, target, directEntity);
                if (distance > 15.0D) {
                    float bonus = (float) Math.min(0.40D, (distance - 15.0D) * 0.016D);
                    multiplier *= 1.0F + bonus;
                }
            }
            if (isMarkedProjectile(directEntity, MAESTRO_PROJECTILES)) {
                multiplier *= 1.30F;
            }
            if (isMarkedProjectile(directEntity, ESCARAMUZADOR_PROJECTILES)) {
                multiplier *= 1.15F;
            }
            if (tauntedTargetDamageBonus(attacker, target)) {
                multiplier *= 1.05F;
            }
        }
        return multiplier;
    }

    private static float incomingDamageMultiplier(ServerPlayer defender, DamageSource source, boolean melee, boolean projectile, boolean explosion, float originalDamage) {
        float multiplier = globalResistanceMultiplier(defender, melee || projectile || explosion, originalDamage);
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
        if (directEntity instanceof Projectile projectile) {
            UUID projectileId = projectile.getUUID();
            Boolean cached = PROJECTILE_CRITS.get(projectileId);
            if (cached != null) {
                return cached;
            }
            boolean crit = rollFreshRangedCrit(player, directEntity);
            PROJECTILE_CRITS.put(projectileId, crit);
            return crit;
        }
        return rollFreshRangedCrit(player, directEntity);
    }

    private static boolean rollFreshRangedCrit(ServerPlayer player, Entity directEntity) {
        if (has(player, "barrage")) {
            BarrageState barrage = BARRAGE_STATES.get(player.getUUID());
            if (barrage != null && barrage.chargedCrit) {
                barrage.chargedCrit = false;
                barrage.hits = 0;
                if (directEntity != null) {
                    BARRAGE_CONSUMED_PROJECTILES.put(directEntity.getUUID(), player.level().getGameTime() + PROJECTILE_STATE_TICKS);
                }
                return true;
            }
        }
        float chance = rangedCritChance(player);
        return chance > 0.0F && player.getRandom().nextFloat() < chance;
    }

    private static float meleeCritChance(ServerPlayer player) {
        return (float) clamp(attributeValue(player, AscendantAttributes.MELEE_CRIT_CHANCE)
                + attributeValue(player, AscendantAttributes.GLOBAL_CRIT_CHANCE), 0.0D, 1.0D);
    }

    private static float rangedCritChance(ServerPlayer player) {
        return (float) clamp(attributeValue(player, AscendantAttributes.RANGED_CRIT_CHANCE)
                + attributeValue(player, AscendantAttributes.GLOBAL_CRIT_CHANCE), 0.0D, 1.0D);
    }

    private static float rangedCritMultiplier(ServerPlayer player) {
        return BASE_CRIT_MULTIPLIER
                + (float) attributeValue(player, AscendantAttributes.RANGED_CRIT_DAMAGE)
                + (float) attributeValue(player, AscendantAttributes.GLOBAL_CRIT_DAMAGE);
    }

    private static void recordRangedHit(ServerPlayer player, boolean crit, Entity directEntity) {
        long now = player.level().getGameTime();
        if (directEntity instanceof Projectile projectile) {
            UUID projectileId = projectile.getUUID();
            PROJECTILE_HIT_ENTITIES.put(projectileId, now + PROJECTILE_STATE_TICKS);
            if (PROCESSED_RANGED_PROJECTILES.getOrDefault(projectileId, 0L) >= now) {
                return;
            }
            PROCESSED_RANGED_PROJECTILES.put(projectileId, now + PROJECTILE_STATE_TICKS);
        }
        if (has(player, "deadeye")) {
            DeadeyeState state = DEADEYE_STACKS.computeIfAbsent(player.getUUID(), ignored -> new DeadeyeState());
            if (crit) {
                state.stacks = Math.min(DEADEYE_MAX_STACKS, state.stacks + 1);
                state.lastCritTick = now;
                state.lastDecayTick = now;
            } else if (state.stacks > 0) {
                state.stacks--;
                state.lastDecayTick = now;
            }
            updateDeadeyeModifier(player);
        }
        if (has(player, "barrage")) {
            if (directEntity != null && BARRAGE_CONSUMED_PROJECTILES.remove(directEntity.getUUID()) != null) {
                return;
            }
            BarrageState barrage = BARRAGE_STATES.computeIfAbsent(player.getUUID(), ignored -> new BarrageState());
            if (!barrage.chargedCrit) {
                barrage.hits++;
                if (barrage.hits >= BARRAGE_REQUIRED_HITS) {
                    barrage.hits = 0;
                    barrage.chargedCrit = true;
                }
            }
        }
    }

    private static void recordRangedMiss(ServerPlayer player, Projectile projectile) {
        long now = player.level().getGameTime();
        UUID projectileId = projectile.getUUID();
        if (PROJECTILE_HIT_ENTITIES.getOrDefault(projectileId, 0L) >= now || PROCESSED_RANGED_PROJECTILES.getOrDefault(projectileId, 0L) >= now) {
            return;
        }
        PROCESSED_RANGED_PROJECTILES.put(projectileId, now + PROJECTILE_STATE_TICKS);
        if (has(player, "deadeye")) {
            DeadeyeState state = DEADEYE_STACKS.get(player.getUUID());
            if (state != null && state.stacks > 0) {
                state.stacks--;
                state.lastDecayTick = now;
                updateDeadeyeModifier(player);
            }
        }
        if (has(player, "barrage")) {
            BarrageState barrage = BARRAGE_STATES.get(player.getUUID());
            if (barrage != null) {
                barrage.hits = 0;
                barrage.chargedCrit = false;
            }
        }
    }

    private static double rangedDistance(ServerPlayer player, LivingEntity target, Entity directEntity) {
        if (directEntity instanceof Projectile projectile) {
            ProjectileOrigin origin = PROJECTILE_ORIGINS.get(projectile.getUUID());
            if (origin != null && origin.dimension.equals(target.level().dimension().location().toString())) {
                return origin.position.distanceTo(target.position());
            }
        }
        return player.distanceTo(target);
    }

    private static float globalDamageMultiplier(ServerPlayer player) {
        return (float) attributeValue(player, AscendantAttributes.GLOBAL_DAMAGE);
    }

    private static float rangedDamageMultiplier(ServerPlayer player) {
        return (float) attributeValue(player, AscendantAttributes.RANGED_DAMAGE);
    }

    private static float globalResistanceMultiplier(ServerPlayer player, boolean combatDamage, float originalDamage) {
        if (!combatDamage) {
            return 1.0F;
        }
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
        double effectiveResistance = Math.min(AscendantConfig.globalResistanceHardCap(), softenedResistance * damageScale);
        return (float) (1.0D - effectiveResistance);
    }

    private static float resistanceMultiplier(ServerPlayer player, Holder<Attribute> attribute) {
        double resistance = clamp(attributeValue(player, attribute), 0.0D, AscendantConfig.specificResistanceHardCap());
        return (float) (1.0D - resistance);
    }

    private static void applyArmorShred(LivingIncomingDamageEvent event, ServerPlayer attacker, boolean melee, boolean projectile, LivingEntity target, Entity directEntity) {
        if (target.getArmorValue() <= 0) {
            return;
        }
        double shred = 0.0D;
        if (melee) {
            shred += attributeValue(attacker, AscendantAttributes.MELEE_ARMOR_SHRED);
        }
        if (projectile) {
            shred += attributeValue(attacker, AscendantAttributes.RANGED_ARMOR_SHRED);
            if (isMarkedProjectile(directEntity, MAESTRO_PROJECTILES)) {
                shred += 0.15D;
            }
        }
        double effectiveShred = clamp(shred, 0.0D, 1.0D);
        if (effectiveShred <= 0.0D) {
            return;
        }
        event.addReductionModifier(DamageContainer.Reduction.ARMOR, (container, reduction) -> (float) (reduction * (1.0D - effectiveShred)));
    }

    private static boolean isMarkedProjectile(Entity directEntity, Map<UUID, Long> projectiles) {
        return directEntity != null && projectiles.getOrDefault(directEntity.getUUID(), 0L) >= directEntity.level().getGameTime();
    }

    private static float rangedAttackSpeedMultiplier(LivingEntity player) {
        return (float) (attributeValue(player, AscendantAttributes.GLOBAL_ATTACK_SPEED)
                * attributeValue(player, AscendantAttributes.RANGED_ATTACK_SPEED));
    }

    private static double projectileVelocityMultiplier(LivingEntity player) {
        return attributeValue(player, AscendantAttributes.PROJECTILE_VELOCITY);
    }

    private static float globalAttackSpeedMultiplier(LivingEntity player) {
        return (float) attributeValue(player, AscendantAttributes.GLOBAL_ATTACK_SPEED);
    }

    private static boolean isRangedUseItem(ItemStack stack) {
        return stack.getItem() instanceof BowItem || stack.getItem() instanceof CrossbowItem;
    }

    private static boolean isChargedCrossbowInHand(ServerPlayer player) {
        return isChargedCrossbow(player.getMainHandItem()) || isChargedCrossbow(player.getOffhandItem());
    }

    private static boolean isChargedCrossbow(ItemStack stack) {
        return stack.getItem() instanceof CrossbowItem && CrossbowItem.isCharged(stack);
    }

    private static boolean isBlockingWithShield(ServerPlayer player) {
        return player.isUsingItem() && player.getUseItem().getItem() instanceof ShieldItem;
    }

    private static void updateMaestroCharge(ServerPlayer player) {
        UUID playerId = player.getUUID();
        RangedUseSpeed state = RANGED_USE_SPEED.get(playerId);
        if (!has(player, "maestro_tirador")) {
            if (state != null) {
                state.useTicks = 0;
            }
            return;
        }

        if (remainingTicks(player, MAESTRO_COOLDOWNS.getOrDefault(playerId, 0L)) > 0) {
            if (state != null) {
                state.useTicks = 0;
            }
            return;
        }

        boolean chargingBow = player.isUsingItem() && player.getUseItem().getItem() instanceof BowItem;
        if (chargingBow) {
            RANGED_USE_SPEED.computeIfAbsent(playerId, ignored -> new RangedUseSpeed()).useTicks++;
            return;
        }

        boolean holdingReadyCrossbow = !player.isUsingItem() && isChargedCrossbowInHand(player);
        if (holdingReadyCrossbow) {
            RANGED_USE_SPEED.computeIfAbsent(playerId, ignored -> new RangedUseSpeed()).useTicks++;
            return;
        }

        if (state != null) {
            state.useTicks = 0;
        }
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

    private static void activateAbsoluteBastion(ServerPlayer defender, DamageSource source, boolean melee, boolean projectile, boolean explosion) {
        if (!has(defender, "bastion_absoluto") || !(melee || projectile || explosion) || !isBlockingWithShield(defender)) {
            return;
        }
        long now = defender.level().getGameTime();
        UUID defenderId = defender.getUUID();
        if (ABSOLUTE_BASTION_COOLDOWNS.getOrDefault(defenderId, 0L) > now) {
            return;
        }
        ABSOLUTE_BASTION_COOLDOWNS.put(defenderId, now + ABSOLUTE_BASTION_COOLDOWN_TICKS);
        for (LivingEntity target : defender.level().getEntitiesOfClass(LivingEntity.class, defender.getBoundingBox().inflate(ABSOLUTE_BASTION_RADIUS))) {
            if (target == defender || target instanceof Player) {
                continue;
            }
            Vec3 direction = target.position().subtract(defender.position());
            double horizontalLength = Math.sqrt(direction.x * direction.x + direction.z * direction.z);
            if (horizontalLength <= 0.001D) {
                continue;
            }
            Vec3 push = new Vec3(direction.x / horizontalLength * 1.1D, 0.35D, direction.z / horizontalLength * 1.1D);
            target.setDeltaMovement(target.getDeltaMovement().add(push));
            target.hasImpulse = true;
        }
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
        float heal = defender.getMaxHealth() * 0.20F;
        float healthFloor = predictedHealth <= 0.0F ? 1.0F : predictedHealth;
        float desiredHealth = Math.min(defender.getMaxHealth(), healthFloor + heal);
        return Math.max(0.0F, defender.getHealth() - desiredHealth);
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
            updateDeadeyeModifier(player);
            return;
        }
        DeadeyeState state = DEADEYE_STACKS.get(player.getUUID());
        if (state == null || state.stacks <= 0) {
            updateDeadeyeModifier(player);
            return;
        }
        long now = player.level().getGameTime();
        if (now - state.lastCritTick >= DEADEYE_DECAY_TICKS && now - state.lastDecayTick >= DEADEYE_DECAY_TICKS) {
            state.stacks--;
            state.lastDecayTick = now;
        }
        if (state.stacks <= 0) {
            DEADEYE_STACKS.remove(player.getUUID());
            updateDeadeyeModifier(player);
        } else {
            updateDeadeyeModifier(player);
        }
    }

    private static void updateDeadeyeModifier(ServerPlayer player) {
        int stacks = deadeyeStacks(player);
        if (!has(player, "deadeye") || stacks <= 0) {
            removeModifier(player, AscendantAttributes.RANGED_CRIT_CHANCE, DEADEYE_RANGED_CRIT_CHANCE);
            return;
        }
        applyModifier(player, AscendantAttributes.RANGED_CRIT_CHANCE, DEADEYE_RANGED_CRIT_CHANCE, stacks * 0.01D, AttributeModifier.Operation.ADD_VALUE);
    }

    private static void updateBarrage(ServerPlayer player) {
        if (!has(player, "barrage")) {
            BARRAGE_STATES.remove(player.getUUID());
        }
    }

    private static void updateEscaramuzador(ServerPlayer player) {
        UUID playerId = player.getUUID();
        if (!has(player, "escaramuzador")) {
            ESCARAMUZADOR_STATES.remove(playerId);
            return;
        }

        EscaramuzadorState state = ESCARAMUZADOR_STATES.computeIfAbsent(playerId, ignored -> new EscaramuzadorState(player.position(), player.level().dimension().location().toString()));
        String dimension = player.level().dimension().location().toString();
        if (!state.dimension.equals(dimension)) {
            state.dimension = dimension;
            state.lastPosition = player.position();
            state.distance = 0.0D;
            state.ready = false;
            return;
        }

        Vec3 current = player.position();
        Vec3 delta = current.subtract(state.lastPosition);
        state.lastPosition = current;
        if (state.ready || player.isPassenger() || player.isFallFlying()) {
            return;
        }

        double horizontalDistance = Math.sqrt(delta.x * delta.x + delta.z * delta.z);
        if (horizontalDistance <= 1.5D) {
            state.distance = Math.min(ESCARAMUZADOR_REQUIRED_DISTANCE, state.distance + horizontalDistance);
        }
        if (state.distance >= ESCARAMUZADOR_REQUIRED_DISTANCE) {
            state.ready = true;
        }
    }

    private static void updateLastStand(ServerPlayer player) {
        LastStandState state = LAST_STANDS.get(player.getUUID());
        if (state == null || state.activeUntilTick > player.level().getGameTime()) {
            return;
        }
        removeModifier(player, AscendantAttributes.GLOBAL_RESISTANCE, LAST_STAND_GLOBAL_RESISTANCE);
    }

    private static void updateProvocadorAggro(ServerPlayer player) {
        if (!has(player, "provocador")) {
            return;
        }
        double radius = AscendantConfig.provocadorBaseAggroRange() * (1.0D + attributeValue(player, AscendantAttributes.AGGRO_REACH));
        for (Mob mob : player.level().getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(radius))) {
            if (!(mob instanceof Enemy) || !mob.isAlive() || mob.isNoAi() || mob.getTarget() != null) {
                continue;
            }
            if (mob.distanceToSqr(player) <= radius * radius && mob.hasLineOfSight(player)) {
                mob.setTarget(player);
            }
        }
    }

    private static void updateTitan(ServerPlayer player) {
        int stacks = titanStacks(player);
        if (!has(player, "titan") || stacks <= 0) {
            removeTitanModifiers(player);
            return;
        }
        applyModifier(player, AscendantAttributes.GLOBAL_DAMAGE, TITAN_GLOBAL_DAMAGE,
                stacks * AscendantConfig.titanGlobalDamagePerStack(), AttributeModifier.Operation.ADD_VALUE);
        applyModifier(player, AscendantAttributes.GLOBAL_ATTACK_SPEED, TITAN_GLOBAL_ATTACK_SPEED,
                stacks * AscendantConfig.titanGlobalAttackSpeedPerStack(), AttributeModifier.Operation.ADD_VALUE);
        applyModifier(player, Attributes.MOVEMENT_SPEED, TITAN_MOVE_SPEED,
                stacks * AscendantConfig.titanMoveSpeedPerStack(), AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        applyModifier(player, AscendantAttributes.GLOBAL_RESISTANCE, TITAN_GLOBAL_RESISTANCE,
                stacks * AscendantConfig.titanGlobalResistancePerStack(), AttributeModifier.Operation.ADD_VALUE);
        applyModifier(player, AscendantAttributes.GLOBAL_CRIT_CHANCE, TITAN_GLOBAL_CRIT_CHANCE,
                stacks * AscendantConfig.titanGlobalCritChancePerStack(), AttributeModifier.Operation.ADD_VALUE);
        applyModifier(player, AscendantAttributes.GLOBAL_CRIT_DAMAGE, TITAN_GLOBAL_CRIT_DAMAGE,
                stacks * AscendantConfig.titanGlobalCritDamagePerStack(), AttributeModifier.Operation.ADD_VALUE);
        applyModifier(player, Attributes.MAX_HEALTH, TITAN_MAX_HEALTH,
                stacks * AscendantConfig.titanHealthPerStack(), AttributeModifier.Operation.ADD_VALUE);
    }

    private static void removeTitanModifiers(ServerPlayer player) {
        removeModifier(player, AscendantAttributes.GLOBAL_DAMAGE, TITAN_GLOBAL_DAMAGE);
        removeModifier(player, AscendantAttributes.GLOBAL_ATTACK_SPEED, TITAN_GLOBAL_ATTACK_SPEED);
        removeModifier(player, Attributes.MOVEMENT_SPEED, TITAN_MOVE_SPEED);
        removeModifier(player, AscendantAttributes.GLOBAL_RESISTANCE, TITAN_GLOBAL_RESISTANCE);
        removeModifier(player, AscendantAttributes.GLOBAL_CRIT_CHANCE, TITAN_GLOBAL_CRIT_CHANCE);
        removeModifier(player, AscendantAttributes.GLOBAL_CRIT_DAMAGE, TITAN_GLOBAL_CRIT_DAMAGE);
        removeModifier(player, Attributes.MAX_HEALTH, TITAN_MAX_HEALTH);
    }

    private static int titanStacks(ServerPlayer player) {
        if (!has(player, "titan")) {
            return 0;
        }
        int stacks = 1;
        AscendantData.Party party = AscendantData.get(player.server).partyOf(player.getUUID()).orElse(null);
        if (party != null) {
            stacks = 0;
            for (UUID memberId : party.members) {
                if (player.server.getPlayerList().getPlayer(memberId) != null) {
                    stacks++;
                }
            }
        }
        return Math.min(AscendantConfig.titanMaxStacks(), Math.max(1, stacks));
    }

    private static void applyPassiveRegen(ServerPlayer player) {
        double heal = attributeValue(player, AscendantAttributes.PASSIVE_REGEN_HEALTH);
        if (heal <= 0.0D || player.getHealth() >= player.getMaxHealth()) {
            return;
        }
        int intervalTicks = Math.max(TICKS_PER_SECOND, (int) Math.round(attributeValue(player, AscendantAttributes.PASSIVE_REGEN_INTERVAL) * TICKS_PER_SECOND));
        if (player.tickCount % intervalTicks == 0) {
            heal(player, (float) heal, "passive_regen");
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
                has(player, "berserker") && isLowHealth(player),
                has(player, "aljaba_ligera") ? aljabaStacks(player) : -1,
                ALJABA_MAX_STACKS,
                has(player, "deadeye") ? deadeyeStacks(player) : -1,
                DEADEYE_MAX_STACKS,
                has(player, "escaramuzador") ? escaramuzadorProgress(player) : -1,
                has(player, "escaramuzador") && escaramuzadorReady(player),
                has(player, "maestro_tirador") ? remainingTicks(player, MAESTRO_COOLDOWNS.getOrDefault(player.getUUID(), 0L)) : -1,
                has(player, "maestro_tirador") ? maestroChargeSeconds(player) : 0,
                has(player, "barrage") ? barrageHits(player) : -1,
                BARRAGE_REQUIRED_HITS,
                has(player, "barrage") && barrageReady(player),
                has(player, "muralla") ? murallaPosture(player) : -1,
                has(player, "fortaleza") ? fortressStacks(player) : -1,
                FORTRESS_MAX_STACKS,
                has(player, "guardaespaldas") ? remainingTicks(player, BODYGUARD_COOLDOWNS.getOrDefault(player.getUUID(), 0L)) : -1,
                has(player, "regenerador") ? remainingTicks(player, SECOND_WIND_COOLDOWNS.getOrDefault(player.getUUID(), 0L)) : -1,
                has(player, "invencible") ? remainingLastStandActiveTicks(player) : -1,
                has(player, "invencible") ? remainingLastStandCooldownTicks(player) : -1,
                has(player, "bastion_absoluto") ? remainingTicks(player, ABSOLUTE_BASTION_COOLDOWNS.getOrDefault(player.getUUID(), 0L)) : -1,
                has(player, "provocador"),
                has(player, "titan") ? titanStacks(player) : -1,
                AscendantConfig.titanMaxStacks()
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

    private static int fortressStacks(ServerPlayer player) {
        StackingBuff stacks = FORTRESS_STACKS.get(player.getUUID());
        return stacks == null ? 0 : Math.max(0, stacks.stacks);
    }

    private static int murallaPosture(ServerPlayer player) {
        return player.getOffhandItem().getItem() instanceof ShieldItem ? 1 : 0;
    }

    private static int aljabaStacks(ServerPlayer player) {
        StackingBuff stacks = ALJABA_STACKS.get(player.getUUID());
        return stacks == null ? 0 : Math.max(0, stacks.stacks);
    }

    private static int deadeyeStacks(ServerPlayer player) {
        DeadeyeState state = DEADEYE_STACKS.get(player.getUUID());
        return state == null ? 0 : Math.max(0, state.stacks);
    }

    private static int escaramuzadorProgress(ServerPlayer player) {
        EscaramuzadorState state = ESCARAMUZADOR_STATES.get(player.getUUID());
        return state == null ? 0 : (int) Math.floor(Math.min(ESCARAMUZADOR_REQUIRED_DISTANCE, state.distance));
    }

    private static boolean escaramuzadorReady(ServerPlayer player) {
        EscaramuzadorState state = ESCARAMUZADOR_STATES.get(player.getUUID());
        return state != null && state.ready;
    }

    private static int barrageHits(ServerPlayer player) {
        BarrageState state = BARRAGE_STATES.get(player.getUUID());
        return state == null ? 0 : Math.max(0, state.hits);
    }

    private static boolean barrageReady(ServerPlayer player) {
        BarrageState state = BARRAGE_STATES.get(player.getUUID());
        return state != null && state.chargedCrit;
    }

    private static int maestroChargeSeconds(ServerPlayer player) {
        if (remainingTicks(player, MAESTRO_COOLDOWNS.getOrDefault(player.getUUID(), 0L)) > 0) {
            return 0;
        }
        boolean chargingBow = player.isUsingItem() && player.getUseItem().getItem() instanceof BowItem;
        if (!chargingBow && !isChargedCrossbowInHand(player)) {
            return 0;
        }
        RangedUseSpeed state = RANGED_USE_SPEED.get(player.getUUID());
        if (state == null || state.useTicks <= 0) {
            return 0;
        }
        return Math.min(5, (state.useTicks + TICKS_PER_SECOND - 1) / TICKS_PER_SECOND);
    }

    private static int remainingJuggernautActiveTicks(ServerPlayer player) {
        JuggernautState state = JUGGERNAUTS.get(player.getUUID());
        return state == null ? 0 : remainingTicks(player, state.immunityUntilTick);
    }

    private static int remainingJuggernautCooldownTicks(ServerPlayer player) {
        JuggernautState state = JUGGERNAUTS.get(player.getUUID());
        return state == null ? 0 : remainingTicks(player, state.cooldownUntilTick);
    }

    private static int remainingLastStandActiveTicks(ServerPlayer player) {
        LastStandState state = LAST_STANDS.get(player.getUUID());
        return state == null ? 0 : remainingTicks(player, state.activeUntilTick);
    }

    private static int remainingLastStandCooldownTicks(ServerPlayer player) {
        LastStandState state = LAST_STANDS.get(player.getUUID());
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

    public static void resetRuntime(ServerPlayer player) {
        UUID playerId = player.getUUID();
        STEEL_COMBOS.remove(playerId);
        CRITICAL_EYE_COOLDOWNS.remove(playerId);
        VETERAN_MARKS.entrySet().removeIf(entry -> entry.getValue().targetPlayer.equals(playerId) || entry.getKey().equals(playerId));
        JUGGERNAUTS.remove(playerId);
        CONQUEROR_STACKS.remove(playerId);
        FORTRESS_STACKS.remove(playerId);
        ALJABA_STACKS.remove(playerId);
        DEADEYE_STACKS.remove(playerId);
        BARRAGE_STATES.remove(playerId);
        ESCARAMUZADOR_STATES.remove(playerId);
        MAESTRO_COOLDOWNS.remove(playerId);
        BODYGUARD_COOLDOWNS.remove(playerId);
        SECOND_WIND_COOLDOWNS.remove(playerId);
        LAST_STANDS.remove(playerId);
        ABSOLUTE_BASTION_COOLDOWNS.remove(playerId);
        RANGED_USE_SPEED.remove(playerId);
        PROJECTILE_ORIGINS.entrySet().removeIf(entry -> entry.getValue().owner.equals(playerId));
        MAESTRO_PROJECTILES.clear();
        ESCARAMUZADOR_PROJECTILES.clear();
        BARRAGE_CONSUMED_PROJECTILES.clear();
        PROJECTILE_CRITS.clear();
        PROJECTILE_HIT_ENTITIES.clear();
        PROCESSED_RANGED_PROJECTILES.clear();

        removeModifier(player, Attributes.ATTACK_SPEED, GLOBAL_ATTACK_SPEED_ATTACK_SPEED);
        removeModifier(player, Attributes.ATTACK_SPEED, STEEL_COMBO_ATTACK_SPEED);
        removeModifier(player, Attributes.ATTACK_SPEED, BERSERKER_ATTACK_SPEED);
        removeModifier(player, Attributes.MOVEMENT_SPEED, MURALLA_OFFHAND_MOVE_SPEED);
        removeModifier(player, Attributes.ATTACK_DAMAGE, MURALLA_OFFHAND_ATTACK_DAMAGE);
        removeModifier(player, AscendantAttributes.MELEE_RESISTANCE, FORTRESS_MELEE_RESISTANCE);
        removeModifier(player, AscendantAttributes.GLOBAL_RESISTANCE, LAST_STAND_GLOBAL_RESISTANCE);
        removeModifier(player, AscendantAttributes.RANGED_ATTACK_SPEED, ALJABA_RANGED_ATTACK_SPEED);
        removeModifier(player, AscendantAttributes.RANGED_CRIT_CHANCE, DEADEYE_RANGED_CRIT_CHANCE);
        removeTitanModifiers(player);
    }

    private static void pruneExpired(ServerPlayer player) {
        long now = player.level().getGameTime();
        VETERAN_MARKS.entrySet().removeIf(entry -> entry.getValue().expiresAtTick < now);
        JUGGERNAUTS.entrySet().removeIf(entry -> entry.getValue().cooldownUntilTick < now && entry.getValue().immunityUntilTick < now);
        BODYGUARD_COOLDOWNS.entrySet().removeIf(entry -> entry.getValue() < now);
        SECOND_WIND_COOLDOWNS.entrySet().removeIf(entry -> entry.getValue() < now);
        ABSOLUTE_BASTION_COOLDOWNS.entrySet().removeIf(entry -> entry.getValue() < now);
        MAESTRO_COOLDOWNS.entrySet().removeIf(entry -> entry.getValue() < now);
        PROJECTILE_ORIGINS.entrySet().removeIf(entry -> entry.getValue().expiresAtTick < now);
        MAESTRO_PROJECTILES.entrySet().removeIf(entry -> entry.getValue() < now);
        ESCARAMUZADOR_PROJECTILES.entrySet().removeIf(entry -> entry.getValue() < now);
        BARRAGE_CONSUMED_PROJECTILES.entrySet().removeIf(entry -> entry.getValue() < now);
        PROJECTILE_CRITS.keySet().removeIf(projectileId -> PROJECTILE_ORIGINS.get(projectileId) == null);
        PROJECTILE_HIT_ENTITIES.entrySet().removeIf(entry -> entry.getValue() < now);
        PROCESSED_RANGED_PROJECTILES.entrySet().removeIf(entry -> entry.getValue() < now);
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

    private static void heal(ServerPlayer player, float amount, String reason) {
        if (amount <= 0.0F || !Float.isFinite(amount)) {
            return;
        }
        ASCENDANT_HEAL_REASONS.put(player.getUUID(), reason);
        try {
            player.heal(amount);
        } finally {
            ASCENDANT_HEAL_REASONS.remove(player.getUUID());
        }
    }

    public static boolean setDamageDebug(ServerPlayer player, boolean enabled) {
        return enabled ? DAMAGE_DEBUG_PLAYERS.add(player.getUUID()) : DAMAGE_DEBUG_PLAYERS.remove(player.getUUID());
    }

    public static boolean isDamageDebugEnabled(ServerPlayer player) {
        return DAMAGE_DEBUG_PLAYERS.contains(player.getUUID());
    }

    private static void debugDamage(ServerPlayer player, String stage, DamageSource source, float original, float input, float output,
                                    boolean melee, boolean projectile, boolean explosion, String note) {
        player.sendSystemMessage(Component.literal(String.format(Locale.ROOT,
                "[AS Damage] %s src=%s in=%.3f original=%.3f out=%.3f flags=%s/%s/%s armor=%.2f tough=%.2f gr=%.3f mr=%.3f pr=%.3f er=%.3f hp=%.2f/%s %s",
                stage,
                source.getMsgId(),
                input,
                original,
                output,
                melee ? "melee" : "-",
                projectile ? "projectile" : "-",
                explosion ? "explosion" : "-",
                attributeValue(player, Attributes.ARMOR),
                attributeValue(player, Attributes.ARMOR_TOUGHNESS),
                attributeValue(player, AscendantAttributes.GLOBAL_RESISTANCE),
                attributeValue(player, AscendantAttributes.MELEE_RESISTANCE),
                attributeValue(player, AscendantAttributes.PROJECTILE_RESISTANCE),
                attributeValue(player, AscendantAttributes.EXPLOSION_RESISTANCE),
                player.getHealth(),
                format(player.getMaxHealth()),
                note
        )));
    }

    private static void debugDamagePost(ServerPlayer player, LivingDamageEvent.Post event) {
        Float preHealth = DAMAGE_DEBUG_PRE_HEALTH.remove(player.getUUID());
        float healthDelta = preHealth == null ? 0.0F : player.getHealth() - preHealth;
        player.sendSystemMessage(Component.literal(String.format(Locale.ROOT,
                "[AS Damage] post src=%s original=%.3f final=%.3f blocked=%.3f shield=%.3f armorRed=%.3f enchRed=%.3f effectsRed=%.3f absorption=%.3f hp=%.2f/%s hpDelta=%.3f",
                event.getSource().getMsgId(),
                event.getOriginalDamage(),
                event.getNewDamage(),
                event.getBlockedDamage(),
                event.getShieldDamage(),
                event.getReduction(DamageContainer.Reduction.ARMOR),
                event.getReduction(DamageContainer.Reduction.ENCHANTMENTS),
                event.getReduction(DamageContainer.Reduction.MOB_EFFECTS),
                event.getReduction(DamageContainer.Reduction.ABSORPTION),
                player.getHealth(),
                format(player.getMaxHealth()),
                healthDelta
        )));
    }

    private static String format(float value) {
        return String.format(Locale.ROOT, "%.2f", value);
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

    private static final class BarrageState {
        private int hits;
        private boolean chargedCrit;
    }

    private static final class EscaramuzadorState {
        private Vec3 lastPosition;
        private String dimension;
        private double distance;
        private boolean ready;

        private EscaramuzadorState(Vec3 lastPosition, String dimension) {
            this.lastPosition = lastPosition;
            this.dimension = dimension;
        }
    }

    private static final class LastStandState {
        private long activeUntilTick;
        private long cooldownUntilTick;
    }

    private static final class RangedUseSpeed {
        private float extraTicks;
        private int useTicks;
    }

    private record ProjectileOrigin(UUID owner, String dimension, Vec3 position, long expiresAtTick) {
    }
}
