package com.harmfy.ascendantskills;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class MiningPerks {
    private static final ResourceLocation TUNNELERS_SPEED = id("tunnelers_speed");
    private static final ResourceLocation PICKAXE_RANGE = id("pickaxe_range");
    private static final ResourceLocation QUARRY_RHYTHM = id("quarry_rhythm");
    private static final ResourceLocation ROCK_HEART_KNOCKBACK = id("rock_heart_knockback");
    private static final ResourceLocation ROCK_HEART_ARMOR = id("rock_heart_armor");
    private static final ResourceLocation ROCK_HEART_TOUGHNESS = id("rock_heart_toughness");
    private static final ResourceLocation ROCK_HEART_STEP = id("rock_heart_step");
    private static final Set<UUID> UNDERGROUND_NIGHT_VISION = new HashSet<>();
    private static final Set<UUID> INFERNAL_FIRE_RESISTANCE = new HashSet<>();
    private static final Map<UUID, QuarryRhythm> QUARRY_RHYTHMS = new HashMap<>();
    private static final Map<UUID, Long> ROCK_HEART_LAST_VALID = new HashMap<>();
    private static final Map<UUID, Integer> SELECTED_MINERALS = new HashMap<>();
    private static final Map<UUID, PendingDurabilitySave> PENDING_DURABILITY_SAVES = new HashMap<>();

    private MiningPerks() {
    }

    public static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        Player player = event.getEntity();
        float speed = event.getNewSpeed();
        if (player == null) {
            return;
        }

        double multiplier = 1.0D + attributeValue(player, AscendantAttributes.MINING_SPEED);
        ItemStack tool = player.getMainHandItem();
        if (isPickaxe(tool)) {
            multiplier += attributeValue(player, AscendantAttributes.PICKAXE_MINING_SPEED);
            if (player.level().isClientSide()) {
                multiplier += clientQuarryRhythmBonus(player);
            }
            ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(event.getState().getBlock());
            if (AscendantConfig.miningObsidianLikeBlocks().contains(blockId.toString())) {
                multiplier += attributeValue(player, AscendantAttributes.OBSIDIAN_MINING_SPEED);
            }
        }

        if (speed > 0.0F && multiplier > 1.0D) {
            event.setNewSpeed((float) (speed * multiplier));
        }
    }

    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer player) || event.isCanceled()) {
            return;
        }

        if (isPickaxe(player.getMainHandItem())) {
            addQuarryRhythmStack(player);
            applyMiningBonusDrops(player, event.getState(), event.getPos());
            dropUniversalBlock(player, event.getState(), event.getPos());
            schedulePickaxeDurabilitySave(player);
        }
    }

    public static void onHarvestCheck(PlayerEvent.HarvestCheck event) {
        if (event.getEntity() instanceof ServerPlayer player && canUniversalMine(player, event.getTargetBlock())) {
            event.setCanHarvest(true);
        }
    }

    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        updatePickaxeRange(player);
        updateTunnelers(player);
        updateUndergroundAdaptation(player);
        updateRockHeart(player);
        updateInfernalMiner(player);
        updateQuarryRhythm(player);
        updateOreSense(player);
        restorePreservedDurability(player);
    }

    public static void cycleSelectedMineral(ServerPlayer player) {
        if (!has(player, "ojo_del_minero")) {
            return;
        }
        List<AscendantConfig.MineralEntry> minerals = AscendantConfig.miningDetectableMinerals();
        if (minerals.isEmpty()) {
            SELECTED_MINERALS.remove(player.getUUID());
            player.displayClientMessage(Component.literal("Ojo del Minero: sin minerales").withStyle(ChatFormatting.RED), true);
            return;
        }

        int current = SELECTED_MINERALS.getOrDefault(player.getUUID(), -1);
        int next = current + 1;
        if (next >= minerals.size()) {
            SELECTED_MINERALS.put(player.getUUID(), -1);
            player.displayClientMessage(Component.literal("Ojo del Minero: apagado").withStyle(ChatFormatting.GRAY), true);
            return;
        }

        SELECTED_MINERALS.put(player.getUUID(), next);
        String name = minerals.get(next).name == null || minerals.get(next).name.isBlank() ? "mineral " + (next + 1) : minerals.get(next).name;
        player.displayClientMessage(Component.literal("Ojo del Minero: " + name).withStyle(ChatFormatting.AQUA), true);
    }

    public static int quarryRhythmStacks(ServerPlayer player) {
        QuarryRhythm state = QUARRY_RHYTHMS.get(player.getUUID());
        return state == null ? 0 : Math.max(0, state.stacks);
    }

    public static int quarryRhythmMaxStacks() {
        return AscendantConfig.miningQuarryRhythmMaxStacks();
    }

    public static boolean tunnelersActive(ServerPlayer player) {
        return has(player, "instinto_minero") && isOverworld(player) && player.getBlockY() <= AscendantConfig.miningTunnelersY();
    }

    public static boolean undergroundAdaptationActive(ServerPlayer player) {
        return has(player, "adaptacion_subterranea") && isOverworld(player) && player.getBlockY() <= AscendantConfig.miningUndergroundAdaptationY();
    }

    public static boolean rockHeartActive(ServerPlayer player) {
        if (!has(player, "corazon_de_piedra")) {
            return false;
        }
        Long validUntil = ROCK_HEART_LAST_VALID.get(player.getUUID());
        return validUntil != null && validUntil >= player.level().getGameTime();
    }

    public static boolean infernalMinerActive(ServerPlayer player) {
        return has(player, "minero_infernal") && isNether(player) && player.getBlockY() <= AscendantConfig.miningInfernalY();
    }

    private static void updatePickaxeRange(ServerPlayer player) {
        double range = attributeValue(player, AscendantAttributes.PICKAXE_INTERACTION_RANGE);
        if (range > 0.0D && isPickaxe(player.getMainHandItem())) {
            applyModifier(player, Attributes.BLOCK_INTERACTION_RANGE, PICKAXE_RANGE, range, AttributeModifier.Operation.ADD_VALUE);
        } else {
            removeModifier(player, Attributes.BLOCK_INTERACTION_RANGE, PICKAXE_RANGE);
        }
    }

    private static void updateTunnelers(ServerPlayer player) {
        if (tunnelersActive(player)) {
            applyModifier(player, Attributes.MOVEMENT_SPEED, TUNNELERS_SPEED, AscendantConfig.miningTunnelersMoveSpeed(), AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        } else {
            removeModifier(player, Attributes.MOVEMENT_SPEED, TUNNELERS_SPEED);
        }
    }

    private static void updateUndergroundAdaptation(ServerPlayer player) {
        if (player.tickCount % AscendantConfig.miningLongEffectRefreshTicks() != 0) {
            return;
        }
        if (undergroundAdaptationActive(player)) {
            player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, MobEffectInstance.INFINITE_DURATION, 0, true, false, true));
            UNDERGROUND_NIGHT_VISION.add(player.getUUID());
        } else if (UNDERGROUND_NIGHT_VISION.remove(player.getUUID())) {
            player.removeEffect(MobEffects.NIGHT_VISION);
        }
    }

    private static void updateRockHeart(ServerPlayer player) {
        if (!has(player, "corazon_de_piedra")) {
            removeRockHeart(player);
            ROCK_HEART_LAST_VALID.remove(player.getUUID());
            return;
        }

        long now = player.level().getGameTime();
        if (isStandingOnRockyBlock(player)) {
            ROCK_HEART_LAST_VALID.put(player.getUUID(), now + AscendantConfig.miningRockHeartGraceTicks());
        }

        if (rockHeartActive(player)) {
            applyModifier(player, Attributes.KNOCKBACK_RESISTANCE, ROCK_HEART_KNOCKBACK, AscendantConfig.miningRockHeartKnockbackResistance(), AttributeModifier.Operation.ADD_VALUE);
            applyModifier(player, Attributes.ARMOR, ROCK_HEART_ARMOR, AscendantConfig.miningRockHeartArmor(), AttributeModifier.Operation.ADD_VALUE);
            applyModifier(player, Attributes.ARMOR_TOUGHNESS, ROCK_HEART_TOUGHNESS, AscendantConfig.miningRockHeartToughness(), AttributeModifier.Operation.ADD_VALUE);
            applyModifier(player, Attributes.STEP_HEIGHT, ROCK_HEART_STEP, AscendantConfig.miningRockHeartStepHeight(), AttributeModifier.Operation.ADD_VALUE);
        } else {
            removeRockHeart(player);
        }
    }

    private static void updateInfernalMiner(ServerPlayer player) {
        if (player.tickCount % AscendantConfig.miningLongEffectRefreshTicks() != 0) {
            return;
        }
        if (infernalMinerActive(player)) {
            player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, MobEffectInstance.INFINITE_DURATION, 0, true, false, true));
            INFERNAL_FIRE_RESISTANCE.add(player.getUUID());
        } else if (INFERNAL_FIRE_RESISTANCE.remove(player.getUUID())) {
            player.removeEffect(MobEffects.FIRE_RESISTANCE);
        }
    }

    private static void updateQuarryRhythm(ServerPlayer player) {
        UUID playerId = player.getUUID();
        QuarryRhythm state = QUARRY_RHYTHMS.get(playerId);
        if (!has(player, "maestro_de_cantera") || state == null || state.stacks <= 0) {
            QUARRY_RHYTHMS.remove(playerId);
            removeModifier(player, AscendantAttributes.MINING_SPEED, QUARRY_RHYTHM);
            return;
        }

        long now = player.level().getGameTime();
        if (now - state.lastBreakTick >= AscendantConfig.miningQuarryRhythmDecayTicks()) {
            QUARRY_RHYTHMS.remove(playerId);
            removeModifier(player, AscendantAttributes.MINING_SPEED, QUARRY_RHYTHM);
            return;
        }

        applyModifier(player, AscendantAttributes.MINING_SPEED, QUARRY_RHYTHM,
                state.stacks * AscendantConfig.miningQuarryRhythmPerStack(), AttributeModifier.Operation.ADD_VALUE);
    }

    private static void updateOreSense(ServerPlayer player) {
        if (player.tickCount % AscendantConfig.miningDetectionIntervalTicks() != 0 || !(player.level() instanceof ServerLevel level)) {
            return;
        }
        int selected = SELECTED_MINERALS.getOrDefault(player.getUUID(), -1);
        List<AscendantConfig.MineralEntry> minerals = AscendantConfig.miningDetectableMinerals();
        if (!has(player, "ojo_del_minero") || selected < 0 || selected >= minerals.size()) {
            return;
        }

        AscendantConfig.MineralEntry mineral = minerals.get(selected);
        Set<String> blocks = Set.copyOf(mineral.blocks == null ? List.of() : mineral.blocks);
        if (blocks.isEmpty()) {
            return;
        }

        int radius = (int) Math.ceil(AscendantConfig.miningOreSenseRadius());
        BlockPos center = player.blockPosition();
        DustParticleOptions particle = particle(mineral.color);
        int shown = 0;
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-radius, -radius, -radius), center.offset(radius, radius, radius))) {
            if (pos.distSqr(center) > radius * radius) {
                continue;
            }
            ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(level.getBlockState(pos).getBlock());
            if (!blocks.contains(blockId.toString())) {
                continue;
            }
            Vec3 origin = player.getEyePosition();
            Vec3 target = Vec3.atCenterOf(pos);
            Vec3 direction = target.subtract(origin).normalize();
            Vec3 visiblePing = origin.add(player.getLookAngle().scale(0.25D)).add(direction.scale(0.55D));
            level.sendParticles(player, particle, true, visiblePing.x, visiblePing.y, visiblePing.z, 8, 0.04D, 0.04D, 0.04D, 0.0D);
            shown++;
            if (shown >= 48) {
                return;
            }
        }
    }

    private static void addQuarryRhythmStack(ServerPlayer player) {
        if (!has(player, "maestro_de_cantera")) {
            return;
        }
        long now = player.level().getGameTime();
        QuarryRhythm state = QUARRY_RHYTHMS.computeIfAbsent(player.getUUID(), ignored -> new QuarryRhythm());
        state.stacks = Math.min(AscendantConfig.miningQuarryRhythmMaxStacks(), state.stacks + 1);
        state.lastBreakTick = now;
        applyModifier(player, AscendantAttributes.MINING_SPEED, QUARRY_RHYTHM,
                state.stacks * AscendantConfig.miningQuarryRhythmPerStack(), AttributeModifier.Operation.ADD_VALUE);
    }

    private static void applyMiningBonusDrops(ServerPlayer player, BlockState state, BlockPos pos) {
        ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        if (!AscendantConfig.miningFortuneOres().contains(blockId.toString())) {
            return;
        }

        double virtualFortune = attributeValue(player, AscendantAttributes.VIRTUAL_FORTUNE);
        int rolls = (int) Math.floor(virtualFortune);
        if (player.getRandom().nextDouble() < virtualFortune - rolls) {
            rolls++;
        }
        if (rolls <= 0 || !(player.level() instanceof ServerLevel level)) {
            return;
        }

        List<ItemStack> drops = Block.getDrops(state, level, pos, null, player, player.getMainHandItem());
        for (int i = 0; i < rolls; i++) {
            for (ItemStack drop : drops) {
                if (!drop.isEmpty()) {
                    Block.popResource(level, pos, drop.copy());
                }
            }
        }
    }

    public static float universalDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
        if (!canUniversalMineAnySide(player, state)) {
            return -1.0F;
        }
        float hardness = Math.max(1.0F, (float) AscendantConfig.miningBedrockHardness());
        float speed = Math.max(1.0F, player.getDestroySpeed(Blocks.OBSIDIAN.defaultBlockState()));
        double multiplier = 1.0D + attributeValue(player, AscendantAttributes.MINING_SPEED)
                + attributeValue(player, AscendantAttributes.PICKAXE_MINING_SPEED)
                + attributeValue(player, AscendantAttributes.OBSIDIAN_MINING_SPEED);
        if (player.level().isClientSide()) {
            multiplier += clientQuarryRhythmBonus(player);
        }
        return (float) (speed * Math.max(1.0D, multiplier) / hardness / 30.0D);
    }

    private static boolean canUniversalMine(ServerPlayer player, BlockState state) {
        return canUniversalMineAnySide(player, state);
    }

    private static boolean canUniversalMineAnySide(Player player, BlockState state) {
        ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        boolean hasPerk = player instanceof ServerPlayer serverPlayer ? has(serverPlayer, "pico_universal") : ClientPerkCache.hasUniversalPickaxeHint();
        return hasPerk
                && isPickaxe(player.getMainHandItem())
                && AscendantConfig.miningUniversalBreakableBlocks().contains(blockId.toString());
    }

    private static void dropUniversalBlock(ServerPlayer player, BlockState state, BlockPos pos) {
        if (!canUniversalMine(player, state) || !(player.level() instanceof ServerLevel level)) {
            return;
        }
        Block.popResource(level, pos, new ItemStack(state.getBlock().asItem()));
        level.levelEvent(2001, pos, Block.getId(state));
    }

    private static void schedulePickaxeDurabilitySave(ServerPlayer player) {
        ItemStack tool = player.getMainHandItem();
        double chance = attributeValue(player, AscendantAttributes.PICKAXE_DURABILITY_PRESERVATION);
        if (chance <= 0.0D || !tool.isDamageableItem() || player.getRandom().nextDouble() >= Math.min(1.0D, chance)) {
            return;
        }
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(tool.getItem());
        PENDING_DURABILITY_SAVES.put(player.getUUID(), new PendingDurabilitySave(itemId, tool.getDamageValue()));
    }

    private static void restorePreservedDurability(ServerPlayer player) {
        PendingDurabilitySave pending = PENDING_DURABILITY_SAVES.remove(player.getUUID());
        if (pending == null) {
            return;
        }
        ItemStack tool = player.getMainHandItem();
        ResourceLocation currentId = BuiltInRegistries.ITEM.getKey(tool.getItem());
        if (!tool.isDamageableItem() || !pending.itemId.equals(currentId)) {
            return;
        }
        int damage = tool.getDamageValue();
        if (damage > pending.damageBefore) {
            tool.setDamageValue(Math.max(pending.damageBefore, damage - 1));
        }
    }

    private static double clientQuarryRhythmBonus(Player player) {
        AttributeInstance instance = player.getAttribute(AscendantAttributes.MINING_SPEED);
        if (instance != null && instance.getModifier(QUARRY_RHYTHM) != null) {
            return 0.0D;
        }
        return ClientPerkCache.minerQuarryRhythmBonus();
    }

    private static boolean isStandingOnRockyBlock(ServerPlayer player) {
        BlockPos pos = player.blockPosition().below();
        ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(player.level().getBlockState(pos).getBlock());
        return AscendantConfig.miningRockyBlocks().contains(blockId.toString());
    }

    private static void removeRockHeart(ServerPlayer player) {
        removeModifier(player, Attributes.KNOCKBACK_RESISTANCE, ROCK_HEART_KNOCKBACK);
        removeModifier(player, Attributes.ARMOR, ROCK_HEART_ARMOR);
        removeModifier(player, Attributes.ARMOR_TOUGHNESS, ROCK_HEART_TOUGHNESS);
        removeModifier(player, Attributes.STEP_HEIGHT, ROCK_HEART_STEP);
    }

    private static boolean isPickaxe(ItemStack stack) {
        return !stack.isEmpty() && stack.is(ItemTags.PICKAXES);
    }

    private static boolean isOverworld(ServerPlayer player) {
        return player.level().dimension().equals(Level.OVERWORLD);
    }

    private static boolean isNether(ServerPlayer player) {
        return player.level().dimension().equals(Level.NETHER);
    }

    private static boolean has(ServerPlayer player, String perkId) {
        return AscendantData.get(player.server).hasPerk(player.getUUID(), AscendantSkills.MOD_ID + ":" + perkId);
    }

    private static double attributeValue(Player player, Holder<Attribute> attribute) {
        AttributeInstance instance = player.getAttribute(attribute);
        return instance == null ? attribute.value().getDefaultValue() : instance.getValue();
    }

    private static void applyModifier(Player player, Holder<Attribute> attribute, ResourceLocation id, double amount, AttributeModifier.Operation operation) {
        AttributeInstance instance = player.getAttribute(attribute);
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

    private static void removeModifier(Player player, Holder<Attribute> attribute, ResourceLocation id) {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance != null) {
            instance.removeModifier(id);
        }
    }

    private static DustParticleOptions particle(int color) {
        float r = ((color >> 16) & 255) / 255.0F;
        float g = ((color >> 8) & 255) / 255.0F;
        float b = (color & 255) / 255.0F;
        return new DustParticleOptions(new Vector3f(r, g, b), 1.0F);
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(AscendantSkills.MOD_ID, path);
    }

    private static final class QuarryRhythm {
        private int stacks;
        private long lastBreakTick;
    }

    private record PendingDurabilitySave(ResourceLocation itemId, int damageBefore) {
    }
}
