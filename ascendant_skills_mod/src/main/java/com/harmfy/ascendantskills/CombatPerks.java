package com.harmfy.ascendantskills;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.living.LivingKnockBackEvent;
import net.neoforged.neoforge.event.entity.player.ArrowLooseEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class CombatPerks {
    private static final float BASE_CRIT_MULTIPLIER = 1.5F;
    private static final int TICKS_PER_SECOND = 20;
    private static final int STEEL_COMBO_MAX_STACKS = 5;
    private static final int STEEL_COMBO_DECAY_TICKS = 3 * TICKS_PER_SECOND;
    private static final int CRITICAL_EYE_COOLDOWN_TICKS = 10 * TICKS_PER_SECOND;
    private static final int VETERAN_MARK_TICKS = 3 * TICKS_PER_SECOND;
    private static final int JUGGERNAUT_IMMUNITY_TICKS = 5 * TICKS_PER_SECOND;
    private static final int JUGGERNAUT_COOLDOWN_TICKS = 15 * TICKS_PER_SECOND;
    private static final int CONQUEROR_STACK_TICKS = 5 * TICKS_PER_SECOND;
    private static final int CONQUEROR_MAX_STACKS = 5;
    private static final double WARLORD_AURA_RADIUS = 10.0D;
    private static final ResourceLocation GLOBAL_ATTACK_SPEED_ATTACK_SPEED = ResourceLocation.fromNamespaceAndPath(AscendantSkills.MOD_ID, "global_attack_speed_attack_speed");
    private static final ResourceLocation STEEL_COMBO_ATTACK_SPEED = ResourceLocation.fromNamespaceAndPath(AscendantSkills.MOD_ID, "steel_combo_attack_speed");
    private static final ResourceLocation BERSERKER_ATTACK_SPEED = ResourceLocation.fromNamespaceAndPath(AscendantSkills.MOD_ID, "berserker_attack_speed");
    private static final Map<UUID, SteelCombo> STEEL_COMBOS = new HashMap<>();
    private static final Map<UUID, Long> CRITICAL_EYE_COOLDOWNS = new HashMap<>();
    private static final Map<UUID, VeteranMark> VETERAN_MARKS = new HashMap<>();
    private static final Map<UUID, JuggernautState> JUGGERNAUTS = new HashMap<>();
    private static final Map<UUID, ConquerorStacks> CONQUEROR_STACKS = new HashMap<>();
    private static final Map<UUID, RangedUseSpeed> RANGED_USE_SPEED = new HashMap<>();

    private CombatPerks() {
    }

    public static void onLivingIncomingDamage(LivingIncomingDamageEvent event) {
        DamageSource source = event.getSource();
        ServerPlayer attacker = playerAttacker(source);
        boolean melee = isMelee(source);
        boolean projectile = isProjectile(source);
        float amount = event.getAmount();
        float multiplier = 1.0F;

        if (attacker != null) {
            amount += outgoingFlatDamage(attacker, melee);
            multiplier *= outgoingDamageMultiplier(attacker, melee, projectile, event.getEntity(), source.getDirectEntity());
            if (projectile && rollRangedCrit(attacker, event.getEntity(), source.getDirectEntity())) {
                multiplier *= rangedCritMultiplier(attacker, event.getEntity(), source.getDirectEntity());
                attacker.crit(event.getEntity());
            }
        }

        if (event.getEntity() instanceof ServerPlayer defender) {
            multiplier *= incomingDamageMultiplier(defender, source, melee, projectile);
            activateJuggernaut(defender, melee);
        }

        float newAmount = amount * multiplier;
        if (newAmount != event.getAmount()) {
            event.setAmount(newAmount);
        }
    }

    public static void onLivingDamagePost(LivingDamageEvent.Post event) {
        ServerPlayer attacker = playerAttacker(event.getSource());
        boolean melee = isMelee(event.getSource());
        if (attacker == null || !melee || event.getNewDamage() <= 0.0F) {
            return;
        }

        long now = attacker.level().getGameTime();
        if (has(attacker, "danzante_de_acero")) {
            SteelCombo combo = STEEL_COMBOS.computeIfAbsent(attacker.getUUID(), ignored -> new SteelCombo());
            combo.stacks = Math.min(STEEL_COMBO_MAX_STACKS, combo.stacks + 1);
            combo.lastHitTick = now;
            combo.lastDecayTick = now;
            applyAttackSpeedModifier(attacker, STEEL_COMBO_ATTACK_SPEED, combo.stacks * 0.02D);
        }

        if (has(attacker, "veterano_de_guerra")) {
            VETERAN_MARKS.put(event.getEntity().getUUID(), new VeteranMark(attacker.getUUID(), now + VETERAN_MARK_TICKS));
        }

        if (has(attacker, "berserker")) {
            float lifesteal = isLowHealth(attacker) ? 0.10F : 0.04F;
            attacker.heal(event.getNewDamage() * lifesteal);
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
            ConquerorStacks stacks = CONQUEROR_STACKS.computeIfAbsent(attacker.getUUID(), ignored -> new ConquerorStacks());
            stacks.stacks = Math.min(CONQUEROR_MAX_STACKS, stacks.stacks + 1);
            stacks.expiresAtTick = attacker.level().getGameTime() + CONQUEROR_STACK_TICKS;
        }
    }

    public static void onLivingKnockBack(LivingKnockBackEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            long now = player.level().getGameTime();
            JuggernautState state = JUGGERNAUTS.get(player.getUUID());
            if (state != null && state.immunityUntilTick >= now && has(player, "juggernaut")) {
                event.setCanceled(true);
            }
        }
    }

    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        updateSteelCombo(player);
        updateBerserkerAttackSpeed(player);
        updateGlobalAttackSpeed(player);
        updatePerkActionBar(player);
        if (!player.isUsingItem() || !isRangedUseItem(player.getUseItem())) {
            RANGED_USE_SPEED.remove(player.getUUID());
        }
        pruneExpired(player);
    }

    public static void onLivingUseItemTick(LivingEntityUseItemEvent.Tick event) {
        if (!(event.getEntity() instanceof Player player) || !isRangedUseItem(event.getItem())) {
            return;
        }

        float speed = globalAttackSpeedMultiplier(player);
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

        float speed = globalAttackSpeedMultiplier(event.getEntity());
        if (speed > 1.0F) {
            event.setCharge(Math.round(event.getCharge() * speed));
        }
    }

    public static boolean rollMeleeCrit(ServerPlayer player, Entity target) {
        float chance = meleeCritChance(player, target);
        return chance > 0.0F && player.getRandom().nextFloat() < chance;
    }

    public static float meleeCritMultiplier(ServerPlayer player, Entity target) {
        return BASE_CRIT_MULTIPLIER;
    }

    public static boolean consumeCriticalEye(ServerPlayer attacker, Entity target) {
        if (!has(attacker, "verdugo") || !(target instanceof LivingEntity livingTarget)) {
            return false;
        }
        if (livingTarget.getHealth() > livingTarget.getMaxHealth() * 0.25F) {
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

    public static float criticalEyeDamageMultiplier() {
        return BASE_CRIT_MULTIPLIER * 1.15F;
    }

    private static float outgoingFlatDamage(ServerPlayer attacker, boolean melee) {
        if (!melee || !has(attacker, "berserker")) {
            return 0.0F;
        }
        ItemStack weapon = attacker.getMainHandItem();
        return weapon.getItem() instanceof SwordItem || weapon.getItem() instanceof AxeItem ? 1.0F : 0.0F;
    }

    private static float outgoingDamageMultiplier(ServerPlayer attacker, boolean melee, boolean projectile, LivingEntity target, Entity directEntity) {
        float multiplier = 1.0F;
        multiplier *= globalDamageMultiplier(attacker);
        if (melee) {
            if (has(attacker, "berserker")) {
                multiplier *= attacker.getHealth() <= attacker.getMaxHealth() * 0.20F ? 1.15F : 1.0F;
            }
            if (has(attacker, "conquistador")) {
                multiplier *= 1.0F + conquerorStacks(attacker) * 0.02F;
                if (target.getArmorValue() > 0) {
                    multiplier *= 1.05F;
                }
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

    private static float incomingDamageMultiplier(ServerPlayer defender, DamageSource source, boolean melee, boolean projectile) {
        float multiplier = 1.0F;
        multiplier *= globalResistanceMultiplier(defender);
        if (melee) {
            if (has(defender, "fortaleza")) {
                multiplier *= 0.90F;
            }
            if (has(defender, "juggernaut")) {
                multiplier *= 0.95F;
            }
            if (has(defender, "conquistador")) {
                multiplier *= 0.97F;
            }
        }
        if (projectile && has(defender, "muralla")) {
            multiplier *= 0.95F;
        }
        if (veteranMarkApplies(defender, source)) {
            multiplier *= 0.95F;
        }
        if (warlordAuraApplies(defender, source)) {
            multiplier *= 0.95F;
        }
        return multiplier;
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

    private static boolean rollRangedCrit(ServerPlayer player, LivingEntity target, Entity directEntity) {
        float chance = rangedCritChance(player, target, directEntity);
        return chance > 0.0F && player.getRandom().nextFloat() < chance;
    }

    private static float meleeCritChance(ServerPlayer player, Entity target) {
        return 0.0F;
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

    private static float globalDamageMultiplier(ServerPlayer player) {
        AttributeInstance attribute = player.getAttribute(AscendantAttributes.GLOBAL_DAMAGE);
        return attribute == null ? 1.0F : (float) attribute.getValue();
    }

    private static float globalResistanceMultiplier(ServerPlayer player) {
        AttributeInstance attribute = player.getAttribute(AscendantAttributes.GLOBAL_RESISTANCE);
        double resistance = attribute == null ? 0.0D : attribute.getValue();
        if (has(player, "bastion")) {
            resistance += 0.05D;
        }
        if (has(player, "inamovible")) {
            resistance += 0.05D;
        }
        if (has(player, "invencible")) {
            resistance += 0.05D;
        }
        if (has(player, "titan")) {
            resistance += 0.05D;
        }
        resistance = Math.max(-1.0D, Math.min(0.95D, resistance));
        return (float) Math.max(0.05D, 1.0D - resistance);
    }

    private static float globalAttackSpeedMultiplier(LivingEntity player) {
        AttributeInstance attribute = player.getAttribute(AscendantAttributes.GLOBAL_ATTACK_SPEED);
        return attribute == null ? 1.0F : (float) attribute.getValue();
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

    private static void updateSteelCombo(ServerPlayer player) {
        UUID playerId = player.getUUID();
        SteelCombo combo = STEEL_COMBOS.get(playerId);
        if (!has(player, "danzante_de_acero")) {
            STEEL_COMBOS.remove(playerId);
            removeAttackSpeedModifier(player, STEEL_COMBO_ATTACK_SPEED);
            return;
        }
        if (combo == null || combo.stacks <= 0) {
            removeAttackSpeedModifier(player, STEEL_COMBO_ATTACK_SPEED);
            return;
        }

        long now = player.level().getGameTime();
        if (now - combo.lastHitTick >= STEEL_COMBO_DECAY_TICKS && now - combo.lastDecayTick >= STEEL_COMBO_DECAY_TICKS) {
            combo.stacks--;
            combo.lastDecayTick = now;
        }
        if (combo.stacks <= 0) {
            STEEL_COMBOS.remove(playerId);
            removeAttackSpeedModifier(player, STEEL_COMBO_ATTACK_SPEED);
        } else {
            applyAttackSpeedModifier(player, STEEL_COMBO_ATTACK_SPEED, combo.stacks * 0.02D);
        }
    }

    private static void updateBerserkerAttackSpeed(ServerPlayer player) {
        if (has(player, "berserker") && isLowHealth(player)) {
            applyAttackSpeedModifier(player, BERSERKER_ATTACK_SPEED, 0.12D);
        } else {
            removeAttackSpeedModifier(player, BERSERKER_ATTACK_SPEED);
        }
    }

    private static void updateGlobalAttackSpeed(ServerPlayer player) {
        double amount = globalAttackSpeedMultiplier(player) - 1.0D;
        if (Math.abs(amount) > 0.0001D) {
            applyAttackSpeedModifier(player, GLOBAL_ATTACK_SPEED_ATTACK_SPEED, amount);
        } else {
            removeAttackSpeedModifier(player, GLOBAL_ATTACK_SPEED_ATTACK_SPEED);
        }
    }

    private static void updatePerkActionBar(ServerPlayer player) {
        if (player.tickCount % 10 != 0) {
            return;
        }

        StringBuilder status = new StringBuilder();
        if (has(player, "danzante_de_acero")) {
            SteelCombo combo = STEEL_COMBOS.get(player.getUUID());
            int stacks = combo == null ? 0 : Math.max(0, combo.stacks);
            status.append("Combo de acero: ").append(stacks).append("/").append(STEEL_COMBO_MAX_STACKS);
        }
        if (has(player, "verdugo")) {
            if (!status.isEmpty()) {
                status.append(" | ");
            }
            long now = player.level().getGameTime();
            long readyAt = CRITICAL_EYE_COOLDOWNS.getOrDefault(player.getUUID(), 0L);
            if (readyAt <= now) {
                status.append("Ojo Critico: listo");
            } else {
                long seconds = Math.max(1L, (readyAt - now + TICKS_PER_SECOND - 1L) / TICKS_PER_SECOND);
                status.append("Ojo Critico: ").append(seconds).append("s");
            }
        }

        if (!status.isEmpty()) {
            player.displayClientMessage(Component.literal(status.toString()), true);
        }
    }

    private static void applyAttackSpeedModifier(ServerPlayer player, ResourceLocation id, double amount) {
        AttributeInstance attackSpeed = player.getAttribute(Attributes.ATTACK_SPEED);
        if (attackSpeed != null) {
            attackSpeed.addOrUpdateTransientModifier(new AttributeModifier(id, amount, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        }
    }

    private static void removeAttackSpeedModifier(ServerPlayer player, ResourceLocation id) {
        AttributeInstance attackSpeed = player.getAttribute(Attributes.ATTACK_SPEED);
        if (attackSpeed != null) {
            attackSpeed.removeModifier(id);
        }
    }

    private static int conquerorStacks(ServerPlayer player) {
        ConquerorStacks stacks = CONQUEROR_STACKS.get(player.getUUID());
        if (stacks == null) {
            return 0;
        }
        long now = player.level().getGameTime();
        if (stacks.expiresAtTick < now) {
            CONQUEROR_STACKS.remove(player.getUUID());
            return 0;
        }
        return stacks.stacks;
    }

    private static void pruneExpired(ServerPlayer player) {
        long now = player.level().getGameTime();
        CONQUEROR_STACKS.entrySet().removeIf(entry -> entry.getValue().expiresAtTick < now);
        VETERAN_MARKS.entrySet().removeIf(entry -> entry.getValue().expiresAtTick < now);
        JUGGERNAUTS.entrySet().removeIf(entry -> entry.getValue().cooldownUntilTick < now && entry.getValue().immunityUntilTick < now);
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
        return source.getEntity() instanceof ServerPlayer && source.getDirectEntity() == source.getEntity();
    }

    private static boolean isProjectile(DamageSource source) {
        Entity direct = source.getDirectEntity();
        return direct instanceof Projectile projectile && projectile.getOwner() instanceof ServerPlayer;
    }

    private static boolean has(ServerPlayer player, String perkId) {
        return AscendantData.get(player.server).hasPerk(player.getUUID(), AscendantSkills.MOD_ID + ":" + perkId);
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

    private static final class ConquerorStacks {
        private int stacks;
        private long expiresAtTick;
    }

    private static final class RangedUseSpeed {
        private float extraTicks;
    }
}
