package com.harmfy.ascendantskills;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.block.CropGrowEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.Optional;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class NaturePerks {
    private static final int TICKS_PER_SECOND = 20;
    private static final int SHORT_EFFECT_TICKS = 12 * TICKS_PER_SECOND;
    private static final int NATURE_SCAN_TICKS = TICKS_PER_SECOND;
    private static final int NATURE_RETALIATION_TICKS = 30 * TICKS_PER_SECOND;
    private static final ResourceLocation FOREST_SOUL_SPEED = id("forest_soul_speed");
    private static final ResourceLocation DRUID_LEAF_SPEED = id("druid_leaf_speed");
    private static final ResourceLocation DRUID_LEAF_JUMP = id("druid_leaf_jump");
    private static final ResourceLocation AVATAR_CROUCH_SPEED = id("avatar_crouch_speed");
    private static final Set<Holder<MobEffect>> RATEL_BLOCKED_EFFECTS = Set.of(MobEffects.POISON, MobEffects.HUNGER);
    private static final Set<String> RAW_FOOD_ITEMS = Set.of(
            "minecraft:apple",
            "minecraft:carrot",
            "minecraft:potato",
            "minecraft:beetroot"
    );
    private static final Set<String> NATURE_NEUTRAL_MOBS = Set.of(
            "minecraft:bee",
            "minecraft:cave_spider",
            "minecraft:dolphin",
            "minecraft:enderman",
            "minecraft:fox",
            "minecraft:goat",
            "minecraft:iron_golem",
            "minecraft:llama",
            "minecraft:panda",
            "minecraft:polar_bear",
            "minecraft:spider",
            "minecraft:wolf",
            "minecraft:zombified_piglin"
    );
    private static final Set<String> BASE_CROP_DROPS = Set.of(
            "minecraft:wheat",
            "minecraft:carrot",
            "minecraft:potato",
            "minecraft:beetroot",
            "minecraft:nether_wart",
            "minecraft:cocoa_beans",
            "minecraft:sweet_berries",
            "minecraft:glow_berries"
    );
    private static final Map<UUID, Map<UUID, Long>> NATURE_RETALIATION = new HashMap<>();

    private NaturePerks() {
    }

    public static void onCropGrowPre(CropGrowEvent.Pre event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }

        ServerPlayer player = strongestNaturePlayer(level, event.getPos(), AscendantAttributes.CROP_GROWTH_RADIUS, AscendantAttributes.CROP_GROWTH_SPEED).orElse(null);
        if (player == null) {
            return;
        }

        double speed = attributeValue(player, AscendantAttributes.CROP_GROWTH_SPEED);
        if (speed > 0.0D && player.getRandom().nextDouble() < Math.min(0.95D, speed)) {
            event.setResult(CropGrowEvent.Pre.Result.GROW);
        }
    }

    public static void onFarmlandTrample(BlockEvent.FarmlandTrampleEvent event) {
        Entity entity = event.getEntity();
        if (entity instanceof ServerPlayer player && has(player, "herbolario")) {
            event.setCanceled(true);
        }
    }

    public static void onLivingUseItemFinish(LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !isRawNatureFood(event.getItem())) {
            return;
        }

        double bonus = attributeValue(player, AscendantAttributes.RAW_FOOD_SATURATION);
        if (bonus <= 0.0D) {
            return;
        }

        FoodProperties food = event.getItem().get(DataComponents.FOOD);
        if (food == null) {
            return;
        }

        float extraSaturation = (float) (food.nutrition() * food.saturation() * 2.0F * bonus);
        if (extraSaturation > 0.0F) {
            player.getFoodData().setSaturation(Math.min(player.getFoodData().getFoodLevel(), player.getFoodData().getSaturationLevel() + extraSaturation));
        }
    }

    public static void onMobEffectApplicable(MobEffectEvent.Applicable event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !has(player, "el_don_del_ratel")) {
            return;
        }

        MobEffectInstance effect = event.getEffectInstance();
        if (effect != null && RATEL_BLOCKED_EFFECTS.contains(effect.getEffect())) {
            event.setResult(MobEffectEvent.Applicable.Result.DO_NOT_APPLY);
        }
    }

    public static void onLivingChangeTarget(LivingChangeTargetEvent event) {
        if (!(event.getNewAboutToBeSetTarget() instanceof ServerPlayer player) || !has(player, "ganadero")) {
            return;
        }
        if (event.getEntity() instanceof LivingEntity attacker && isNatureNeutral(attacker) && !canRetaliateAgainst(player, attacker)) {
            event.setCanceled(true);
            event.setNewAboutToBeSetTarget(null);
        }
    }

    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer player)) {
            return;
        }
        double bonus = attributeValue(player, AscendantAttributes.CROP_HARVEST_BONUS);
        if (bonus < 1.0D || !(event.getLevel() instanceof Level level) || level.isClientSide()) {
            return;
        }

        BlockState state = event.getState();
        if (!isMatureCrop(state)) {
            return;
        }

        int extraCount = (int) Math.floor(bonus);
        for (ItemStack drop : Block.getDrops(state, (ServerLevel) level, event.getPos(), null, player, player.getMainHandItem())) {
            if (isConfiguredCropDrop(drop)) {
                ItemStack extra = drop.copyWithCount(drop.getCount() * extraCount);
                Block.popResource(level, event.getPos(), extra);
            }
        }
    }

    public static void onLivingDamagePre(LivingDamageEvent.Pre event) {
        if (event.getNewDamage() <= 0.0F) {
            return;
        }

        LivingEntity target = event.getEntity();
        DamageSource source = event.getSource();
        if (target instanceof ServerPlayer player && source.is(DamageTypeTags.IS_FALL) && has(player, "druida") && isStandingOnLeaves(player)) {
            event.setNewDamage(0.0F);
            return;
        }

        if (source.getEntity() instanceof ServerPlayer attacker && attacker.getMainHandItem().getItem() instanceof HoeItem) {
            double bonus = attributeValue(attacker, AscendantAttributes.HOE_DAMAGE);
            if (bonus > 0.0D) {
                event.setNewDamage((float) (event.getNewDamage() + bonus));
            }
        }
    }

    public static void onLivingDamagePost(LivingDamageEvent.Post event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer attacker) || event.getNewDamage() <= 0.0F) {
            return;
        }

        if (has(attacker, "ganadero") && isNatureNeutral(event.getEntity())) {
            rememberRetaliation(attacker, event.getEntity());
        }

        double lifeSteal = attributeValue(attacker, AscendantAttributes.GLOBAL_LIFE_STEAL);
        if (lifeSteal > 0.0D && attacker.getHealth() < attacker.getMaxHealth()) {
            attacker.heal((float) (event.getNewDamage() * lifeSteal));
        }
    }

    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        updateForestSoul(player);
        updateDruidLeafPerk(player);
        updateAvatarCrouchSpeed(player);
        applyLongNatureEffects(player);
        accelerateNearbyAnimals(player);
        pruneRetaliation(player);
    }

    private static void updateForestSoul(ServerPlayer player) {
        if (has(player, "pacto_natural") && isNaturalGround(player)) {
            applyModifier(player, Attributes.MOVEMENT_SPEED, FOREST_SOUL_SPEED, 0.05D, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        } else {
            removeModifier(player, Attributes.MOVEMENT_SPEED, FOREST_SOUL_SPEED);
        }
    }

    private static void updateDruidLeafPerk(ServerPlayer player) {
        if (has(player, "druida") && isStandingOnLeaves(player)) {
            applyModifier(player, Attributes.MOVEMENT_SPEED, DRUID_LEAF_SPEED, 0.05D, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
            applyModifier(player, Attributes.JUMP_STRENGTH, DRUID_LEAF_JUMP, 0.42D, AttributeModifier.Operation.ADD_VALUE);
        } else {
            removeModifier(player, Attributes.MOVEMENT_SPEED, DRUID_LEAF_SPEED);
            removeModifier(player, Attributes.JUMP_STRENGTH, DRUID_LEAF_JUMP);
        }
    }

    private static void updateAvatarCrouchSpeed(ServerPlayer player) {
        if (has(player, "avatar_de_la_naturaleza") && player.isCrouching()) {
            applyModifier(player, Attributes.MOVEMENT_SPEED, AVATAR_CROUCH_SPEED, 0.25D, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        } else {
            removeModifier(player, Attributes.MOVEMENT_SPEED, AVATAR_CROUCH_SPEED);
        }
    }

    private static void applyLongNatureEffects(ServerPlayer player) {
        if (player.tickCount % 100 != 0) {
            return;
        }

        if (has(player, "el_don_del_ratel")) {
            for (Holder<MobEffect> effect : RATEL_BLOCKED_EFFECTS) {
                player.removeEffect(effect);
            }
        }

        if (has(player, "apex_predator")) {
            player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, SHORT_EFFECT_TICKS, 0, true, false, true));
        }

        if (has(player, "avatar_de_la_naturaleza")) {
            player.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, SHORT_EFFECT_TICKS, 0, true, false, true));
        }
    }

    private static void accelerateNearbyAnimals(ServerPlayer player) {
        if (player.tickCount % NATURE_SCAN_TICKS != 0) {
            return;
        }

        double radius = attributeValue(player, AscendantAttributes.BREEDING_RADIUS);
        double speed = attributeValue(player, AscendantAttributes.BREEDING_SPEED);
        if (radius <= 0.0D || speed <= 0.0D) {
            return;
        }

        int bonusAgeTicks = Math.max(1, (int) Math.round(NATURE_SCAN_TICKS * speed));
        for (AgeableMob mob : player.level().getEntitiesOfClass(AgeableMob.class, player.getBoundingBox().inflate(radius))) {
            if (!(mob instanceof Animal) || !mob.isAlive() || mob.distanceToSqr(player) > radius * radius) {
                continue;
            }

            int age = mob.getAge();
            if (age < 0) {
                mob.ageUp(bonusAgeTicks, true);
            } else if (age > 0) {
                mob.setAge(Math.max(0, age - bonusAgeTicks));
            }
        }
    }

    private static Optional<ServerPlayer> strongestNaturePlayer(ServerLevel level, BlockPos pos, Holder<Attribute> radiusAttribute, Holder<Attribute> speedAttribute) {
        ServerPlayer bestPlayer = null;
        double bestSpeed = 0.0D;
        for (ServerPlayer player : level.players()) {
            double speed = attributeValue(player, speedAttribute);
            double radius = attributeValue(player, radiusAttribute);
            if (speed <= 0.0D || radius <= 0.0D || player.distanceToSqr(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D) > radius * radius) {
                continue;
            }
            if (speed > bestSpeed) {
                bestSpeed = speed;
                bestPlayer = player;
            }
        }
        return Optional.ofNullable(bestPlayer);
    }

    private static boolean isRawNatureFood(ItemStack stack) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return RAW_FOOD_ITEMS.contains(id.toString());
    }

    private static boolean isNatureNeutral(LivingEntity entity) {
        ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        return NATURE_NEUTRAL_MOBS.contains(id.toString());
    }

    private static void rememberRetaliation(ServerPlayer player, LivingEntity entity) {
        long expiresAt = player.level().getGameTime() + NATURE_RETALIATION_TICKS;
        NATURE_RETALIATION.computeIfAbsent(player.getUUID(), ignored -> new HashMap<>()).put(entity.getUUID(), expiresAt);
    }

    private static boolean canRetaliateAgainst(ServerPlayer player, LivingEntity entity) {
        Map<UUID, Long> entries = NATURE_RETALIATION.get(player.getUUID());
        if (entries == null) {
            return false;
        }
        Long expiresAt = entries.get(entity.getUUID());
        if (expiresAt == null) {
            return false;
        }
        if (expiresAt < player.level().getGameTime()) {
            entries.remove(entity.getUUID());
            return false;
        }
        return true;
    }

    private static void pruneRetaliation(ServerPlayer player) {
        Map<UUID, Long> entries = NATURE_RETALIATION.get(player.getUUID());
        if (entries == null || entries.isEmpty()) {
            return;
        }
        long now = player.level().getGameTime();
        entries.entrySet().removeIf(entry -> entry.getValue() < now);
        if (entries.isEmpty()) {
            NATURE_RETALIATION.remove(player.getUUID());
        }
    }

    private static boolean isNaturalGround(ServerPlayer player) {
        BlockState state = player.level().getBlockState(player.blockPosition().below());
        return state.is(Blocks.GRASS_BLOCK)
                || state.is(Blocks.PODZOL)
                || state.is(Blocks.MYCELIUM)
                || state.is(Blocks.DIRT)
                || state.is(Blocks.COARSE_DIRT)
                || state.is(Blocks.ROOTED_DIRT)
                || state.is(Blocks.FARMLAND)
                || state.is(Blocks.MOSS_BLOCK);
    }

    private static boolean isStandingOnLeaves(ServerPlayer player) {
        BlockState state = player.level().getBlockState(player.blockPosition().below());
        return state.is(BlockTags.LEAVES);
    }

    private static boolean isMatureCrop(BlockState state) {
        return state.getBlock() instanceof CropBlock crop && crop.isMaxAge(state);
    }

    private static boolean isConfiguredCropDrop(ItemStack stack) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return BASE_CROP_DROPS.contains(id.toString());
    }

    private static boolean has(ServerPlayer player, String perkId) {
        return AscendantData.get(player.server).hasPerk(player.getUUID(), AscendantSkills.MOD_ID + ":" + perkId);
    }

    private static double attributeValue(LivingEntity entity, Holder<Attribute> attribute) {
        AttributeInstance instance = entity.getAttribute(attribute);
        return instance == null ? attribute.value().getDefaultValue() : instance.getValue();
    }

    private static void applyModifier(LivingEntity entity, Holder<Attribute> attribute, ResourceLocation id, double amount, AttributeModifier.Operation operation) {
        AttributeInstance instance = entity.getAttribute(attribute);
        if (instance == null) {
            return;
        }
        AttributeModifier current = instance.getModifier(id);
        if (current != null && Math.abs(current.amount() - amount) < 0.00001D && current.operation() == operation) {
            return;
        }
        instance.removeModifier(id);
        instance.addTransientModifier(new AttributeModifier(id, amount, operation));
    }

    private static void removeModifier(LivingEntity entity, Holder<Attribute> attribute, ResourceLocation id) {
        AttributeInstance instance = entity.getAttribute(attribute);
        if (instance != null) {
            instance.removeModifier(id);
        }
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(AscendantSkills.MOD_ID, path);
    }
}
