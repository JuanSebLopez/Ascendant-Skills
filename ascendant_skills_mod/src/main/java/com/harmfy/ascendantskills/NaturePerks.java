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
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.neoforged.neoforge.common.IShearable;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class NaturePerks {
    private static final int TICKS_PER_SECOND = 20;
    private static final ResourceLocation FOREST_SOUL_SPEED = id("forest_soul_speed");
    private static final ResourceLocation DRUID_LEAF_SPEED = id("druid_leaf_speed");
    private static final ResourceLocation DRUID_LEAF_JUMP = id("druid_leaf_jump");
    private static final ResourceLocation AVATAR_CROUCH_SPEED = id("avatar_crouch_speed");
    private static final Set<Holder<MobEffect>> RATEL_BLOCKED_EFFECTS = Set.of(MobEffects.POISON, MobEffects.HUNGER);
    private static final Map<UUID, Map<UUID, Long>> NATURE_RETALIATION = new HashMap<>();
    private static final Map<String, Double> CROP_GROWTH_PROGRESS = new HashMap<>();
    private static final Set<UUID> PLAYERS_UNDERWATER = new java.util.HashSet<>();

    private NaturePerks() {
    }

    public static void onFarmlandTrample(BlockEvent.FarmlandTrampleEvent event) {
        Entity entity = event.getEntity();
        if (entity instanceof ServerPlayer player && has(player, "herbolario")) {
            event.setCanceled(true);
        }
    }

    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.level().isClientSide()) {
            return;
        }

        Entity target = event.getTarget();
        ItemStack stack = event.getItemStack();
        int extraProducts = productBonus(player, target);
        if (extraProducts <= 0) {
            return;
        }

        if (stack.is(Items.SHEARS) && target instanceof IShearable shearable) {
            handleShearingBonus(event, player, target, stack, shearable, extraProducts);
            return;
        }

        if (stack.is(Items.BUCKET) && isMilkable(target)) {
            for (int i = 0; i < extraProducts; i++) {
                giveOrDrop(player, new ItemStack(Items.MILK_BUCKET));
            }
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
            int foodLevel = player.getFoodData().getFoodLevel();
            float saturation = player.getFoodData().getSaturationLevel();
            player.getFoodData().setSaturation(Math.min(foodLevel, saturation + extraSaturation));
            player.getFoodData().setFoodLevel(foodLevel);
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
        if (!(event.getNewAboutToBeSetTarget() instanceof ServerPlayer player)) {
            return;
        }
        if (event.getEntity() instanceof LivingEntity attacker && isProtectedByNature(player, attacker) && !canRetaliateAgainst(player, attacker)) {
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

        if (isProtectedByNature(attacker, event.getEntity())) {
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
        updateNatureMobTargets(player);
        updateUnderwaterBreath(player);
        accelerateNearbyCrops(player);
        accelerateNearbyAnimals(player);
        pruneRetaliation(player);
    }

    private static void updateForestSoul(ServerPlayer player) {
        if (has(player, "pacto_natural") && isNaturalGround(player)) {
            applyModifier(player, Attributes.MOVEMENT_SPEED, FOREST_SOUL_SPEED, AscendantConfig.natureForestSoulSpeed(), AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        } else {
            removeModifier(player, Attributes.MOVEMENT_SPEED, FOREST_SOUL_SPEED);
        }
    }

    private static void updateDruidLeafPerk(ServerPlayer player) {
        if (has(player, "druida") && isStandingOnLeaves(player)) {
            applyModifier(player, Attributes.MOVEMENT_SPEED, DRUID_LEAF_SPEED, AscendantConfig.natureDruidLeafSpeed(), AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
            applyModifier(player, Attributes.JUMP_STRENGTH, DRUID_LEAF_JUMP, AscendantConfig.natureDruidLeafJumpStrength(), AttributeModifier.Operation.ADD_VALUE);
        } else {
            removeModifier(player, Attributes.MOVEMENT_SPEED, DRUID_LEAF_SPEED);
            removeModifier(player, Attributes.JUMP_STRENGTH, DRUID_LEAF_JUMP);
        }
    }

    private static void updateAvatarCrouchSpeed(ServerPlayer player) {
        if (has(player, "avatar_de_la_naturaleza") && player.isCrouching()) {
            applyModifier(player, Attributes.MOVEMENT_SPEED, AVATAR_CROUCH_SPEED, AscendantConfig.natureAvatarCrouchSpeed(), AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        } else {
            removeModifier(player, Attributes.MOVEMENT_SPEED, AVATAR_CROUCH_SPEED);
        }
    }

    private static void applyLongNatureEffects(ServerPlayer player) {
        if (player.tickCount % AscendantConfig.natureLongEffectRefreshTicks() != 0) {
            return;
        }

        if (has(player, "el_don_del_ratel")) {
            for (Holder<MobEffect> effect : RATEL_BLOCKED_EFFECTS) {
                player.removeEffect(effect);
            }
        }

        if (has(player, "apex_predator")) {
            player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, MobEffectInstance.INFINITE_DURATION, 0, true, false, true));
        }
    }

    private static void accelerateNearbyAnimals(ServerPlayer player) {
        int scanTicks = AscendantConfig.natureAnimalScanIntervalTicks();
        if (player.tickCount % scanTicks != 0) {
            return;
        }

        double radius = attributeValue(player, AscendantAttributes.BREEDING_RADIUS);
        double speed = attributeValue(player, AscendantAttributes.BREEDING_SPEED);
        if (radius <= 0.0D || speed <= 0.0D) {
            return;
        }

        int bonusAgeTicks = Math.max(1, (int) Math.round(scanTicks * speed * AscendantConfig.natureBreedingSpeedScale()));
        int accelerated = 0;
        for (AgeableMob mob : player.level().getEntitiesOfClass(AgeableMob.class, player.getBoundingBox().inflate(radius))) {
            if (!(mob instanceof Animal) || !mob.isAlive() || mob.distanceToSqr(player) > radius * radius) {
                continue;
            }

            int age = mob.getAge();
            if (age < 0) {
                mob.setAge(Math.min(0, age + bonusAgeTicks));
                accelerated++;
            } else if (age > 0) {
                mob.setAge(Math.max(0, age - bonusAgeTicks));
                accelerated++;
            }

            if (accelerated >= AscendantConfig.natureMaxAnimalAccelerationsPerScan()) {
                break;
            }
        }
    }

    private static void accelerateNearbyCrops(ServerPlayer player) {
        int scanTicks = AscendantConfig.natureCropScanIntervalTicks();
        if (player.tickCount % scanTicks != 0 || !(player.level() instanceof ServerLevel level)) {
            return;
        }

        double radius = attributeValue(player, AscendantAttributes.CROP_GROWTH_RADIUS);
        double speed = attributeValue(player, AscendantAttributes.CROP_GROWTH_SPEED);
        if (radius <= 0.0D || speed <= 0.0D) {
            return;
        }

        int wholeRadius = Math.max(1, (int) Math.ceil(radius));
        BlockPos center = player.blockPosition();
        int advanced = 0;
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-wholeRadius, -2, -wholeRadius), center.offset(wholeRadius, 2, wholeRadius))) {
            if (pos.distSqr(center) <= radius * radius && advanceCropGradually(level, pos.immutable(), speed, scanTicks)) {
                advanced++;
                if (advanced >= AscendantConfig.natureMaxCropsAdvancedPerScan()) {
                    return;
                }
            }
        }
    }

    private static boolean advanceCropGradually(ServerLevel level, BlockPos pos, double speed, int scanTicks) {
        BlockState state = level.getBlockState(pos);
        if (!isGrowableCropLike(state)) {
            CROP_GROWTH_PROGRESS.remove(cropProgressKey(level, pos));
            return false;
        }

        String key = cropProgressKey(level, pos);
        double progress = CROP_GROWTH_PROGRESS.getOrDefault(key, 0.0D);
        progress += speed * (scanTicks / (double) TICKS_PER_SECOND) / AscendantConfig.natureCropSecondsPerStageAtSpeedOne();
        if (progress < 1.0D) {
            CROP_GROWTH_PROGRESS.put(key, progress);
            return false;
        }

        BlockState nextState = nextGrowthStage(state);
        if (nextState == state) {
            CROP_GROWTH_PROGRESS.remove(key);
            return false;
        }

        CROP_GROWTH_PROGRESS.put(key, progress - 1.0D);
        level.setBlock(pos, nextState, Block.UPDATE_CLIENTS);
        return true;
    }

    private static void handleShearingBonus(PlayerInteractEvent.EntityInteract event, ServerPlayer player, Entity target, ItemStack stack, IShearable shearable, int extraProducts) {
        if (!(player.level() instanceof ServerLevel level) || !shearable.isShearable(player, stack, level, target.blockPosition())) {
            return;
        }

        List<ItemStack> drops = shearable.onSheared(player, stack, level, target.blockPosition());
        if (drops.isEmpty()) {
            return;
        }

        for (ItemStack drop : drops) {
            shearable.spawnShearedDrop(level, target.blockPosition(), drop.copy());
            for (int i = 0; i < extraProducts; i++) {
                shearable.spawnShearedDrop(level, target.blockPosition(), drop.copy());
            }
        }

        EquipmentSlot slot = LivingEntity.getSlotForHand(event.getHand());
        stack.hurtAndBreak(1, player, slot);
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
    }

    private static int productBonus(ServerPlayer player, Entity target) {
        double radius = attributeValue(player, AscendantAttributes.BREEDING_RADIUS);
        double bonus = attributeValue(player, AscendantAttributes.ANIMAL_PRODUCT_BONUS);
        if (radius <= 0.0D || bonus < 1.0D || target.distanceToSqr(player) > radius * radius) {
            return 0;
        }
        return Math.min(16, (int) Math.floor(bonus));
    }

    private static boolean isMilkable(Entity entity) {
        ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        return AscendantConfig.natureMilkableMobs().contains(id.toString());
    }

    private static void giveOrDrop(ServerPlayer player, ItemStack stack) {
        if (!player.getInventory().add(stack)) {
            ItemEntity itemEntity = player.drop(stack, false);
            if (itemEntity != null) {
                itemEntity.setNoPickUpDelay();
                itemEntity.setTarget(player.getUUID());
            }
        }
    }

    private static boolean isRawNatureFood(ItemStack stack) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return AscendantConfig.natureRawFoodItems().contains(id.toString());
    }

    private static boolean isNatureNeutral(LivingEntity entity) {
        ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        return AscendantConfig.natureNeutralMobs().contains(id.toString());
    }

    private static boolean isApexAvoidMob(LivingEntity entity) {
        ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        return AscendantConfig.natureCreeperAvoidMobs().contains(id.toString());
    }

    private static boolean isProtectedByNature(ServerPlayer player, LivingEntity attacker) {
        return has(player, "ganadero") && isNatureNeutral(attacker)
                || has(player, "apex_predator") && isApexAvoidMob(attacker);
    }

    private static void rememberRetaliation(ServerPlayer player, LivingEntity entity) {
        long expiresAt = player.level().getGameTime() + AscendantConfig.natureRatelRetaliationTicks();
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
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        return AscendantConfig.natureNaturalGroundBlocks().contains(id.toString());
    }

    private static boolean isStandingOnLeaves(ServerPlayer player) {
        BlockState state = player.level().getBlockState(player.blockPosition().below());
        return state.is(BlockTags.LEAVES);
    }

    private static boolean isMatureCrop(BlockState state) {
        return state.getBlock() instanceof CropBlock crop && crop.isMaxAge(state);
    }

    private static boolean isGrowableCropLike(BlockState state) {
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        return AscendantConfig.natureGrowableCropBlocks().contains(id.toString()) && nextGrowthStage(state) != state;
    }

    private static BlockState nextGrowthStage(BlockState state) {
        if (state.getBlock() instanceof CropBlock crop) {
            int age = crop.getAge(state);
            return age >= crop.getMaxAge() ? state : crop.getStateForAge(age + 1);
        }

        for (Property<?> property : state.getProperties()) {
            if (property instanceof IntegerProperty integerProperty && "age".equals(integerProperty.getName())) {
                int current = state.getValue(integerProperty);
                int max = integerProperty.getPossibleValues().stream().mapToInt(Integer::intValue).max().orElse(current);
                return current >= max ? state : state.setValue(integerProperty, current + 1);
            }
        }
        return state;
    }

    private static boolean isConfiguredCropDrop(ItemStack stack) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return AscendantConfig.natureCropBonusDrops().contains(id.toString());
    }

    private static void updateNatureMobTargets(ServerPlayer player) {
        if (player.tickCount % AscendantConfig.natureAnimalScanIntervalTicks() != 0) {
            return;
        }

        double radius = Math.max(8.0D, Math.max(
                attributeValue(player, AscendantAttributes.BREEDING_RADIUS),
                attributeValue(player, AscendantAttributes.CROP_GROWTH_RADIUS)
        ) + 8.0D);
        for (Mob mob : player.level().getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(radius))) {
            if (mob.getTarget() != player || !isProtectedByNature(player, mob) || canRetaliateAgainst(player, mob)) {
                continue;
            }
            mob.setTarget(null);
            if (mob instanceof Creeper creeper) {
                creeper.setSwellDir(-1);
            }
        }
    }

    private static void updateUnderwaterBreath(ServerPlayer player) {
        double extraSeconds = attributeValue(player, AscendantAttributes.UNDERWATER_BREATH_SECONDS);
        if (extraSeconds <= 0.0D || !player.isUnderWater()) {
            PLAYERS_UNDERWATER.remove(player.getUUID());
            return;
        }

        if (!PLAYERS_UNDERWATER.add(player.getUUID())) {
            return;
        }
        int extraAirTicks = (int) Math.round(extraSeconds * TICKS_PER_SECOND);
        int boostedAir = Math.max(player.getAirSupply(), player.getMaxAirSupply()) + extraAirTicks;
        if (player.getAirSupply() < boostedAir) {
            player.setAirSupply(boostedAir);
        }
    }

    private static String cropProgressKey(ServerLevel level, BlockPos pos) {
        return level.dimension().location() + ":" + pos.asLong();
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
