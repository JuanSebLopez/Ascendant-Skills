package com.harmfy.ascendantskills;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.neoforged.neoforge.event.entity.EntityAttributeModificationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class AscendantAttributes {
    public static final DeferredRegister<Attribute> ATTRIBUTES = DeferredRegister.create(BuiltInRegistries.ATTRIBUTE, AscendantSkills.MOD_ID);
    public static final DeferredHolder<Attribute, Attribute> GLOBAL_DAMAGE = ATTRIBUTES.register(
            "global_damage",
            () -> new RangedAttribute("attribute.ascendant_skills.global_damage", 1.0D, 0.0D, 100.0D).setSyncable(true)
    );
    public static final DeferredHolder<Attribute, Attribute> GLOBAL_ATTACK_SPEED = ATTRIBUTES.register(
            "global_attack_speed",
            () -> new RangedAttribute("attribute.ascendant_skills.global_attack_speed", 1.0D, 0.0D, 100.0D).setSyncable(true)
    );
    public static final DeferredHolder<Attribute, Attribute> GLOBAL_CRIT_CHANCE = ATTRIBUTES.register(
            "global_crit_chance",
            () -> new RangedAttribute("attribute.ascendant_skills.global_crit_chance", 0.0D, 0.0D, 1.0D).setSyncable(true)
    );
    public static final DeferredHolder<Attribute, Attribute> GLOBAL_CRIT_DAMAGE = ATTRIBUTES.register(
            "global_crit_damage",
            () -> new RangedAttribute("attribute.ascendant_skills.global_crit_damage", 0.0D, -10.0D, 10.0D).setSyncable(true)
    );
    public static final DeferredHolder<Attribute, Attribute> GLOBAL_RESISTANCE = ATTRIBUTES.register(
            "global_resistance",
            () -> new RangedAttribute("attribute.ascendant_skills.global_resistance", 0.0D, -1.0D, 0.95D).setSyncable(true)
    );
    public static final DeferredHolder<Attribute, Attribute> MELEE_RESISTANCE = ATTRIBUTES.register(
            "melee_resistance",
            () -> new RangedAttribute("attribute.ascendant_skills.melee_resistance", 0.0D, -1.0D, 0.95D).setSyncable(true)
    );
    public static final DeferredHolder<Attribute, Attribute> PROJECTILE_RESISTANCE = ATTRIBUTES.register(
            "projectile_resistance",
            () -> new RangedAttribute("attribute.ascendant_skills.projectile_resistance", 0.0D, -1.0D, 0.95D).setSyncable(true)
    );
    public static final DeferredHolder<Attribute, Attribute> EXPLOSION_RESISTANCE = ATTRIBUTES.register(
            "explosion_resistance",
            () -> new RangedAttribute("attribute.ascendant_skills.explosion_resistance", 0.0D, -1.0D, 0.95D).setSyncable(true)
    );
    public static final DeferredHolder<Attribute, Attribute> MELEE_ARMOR_SHRED = ATTRIBUTES.register(
            "melee_armor_shred",
            () -> new RangedAttribute("attribute.ascendant_skills.melee_armor_shred", 0.0D, 0.0D, 10.0D).setSyncable(true)
    );
    public static final DeferredHolder<Attribute, Attribute> MELEE_CRIT_CHANCE = ATTRIBUTES.register(
            "melee_crit_chance",
            () -> new RangedAttribute("attribute.ascendant_skills.melee_crit_chance", 0.0D, 0.0D, 1.0D).setSyncable(true)
    );
    public static final DeferredHolder<Attribute, Attribute> MELEE_CRIT_DAMAGE = ATTRIBUTES.register(
            "melee_crit_damage",
            () -> new RangedAttribute("attribute.ascendant_skills.melee_crit_damage", 0.0D, -10.0D, 10.0D).setSyncable(true)
    );
    public static final DeferredHolder<Attribute, Attribute> RANGED_DAMAGE = ATTRIBUTES.register(
            "ranged_damage",
            () -> new RangedAttribute("attribute.ascendant_skills.ranged_damage", 1.0D, 0.0D, 100.0D).setSyncable(true)
    );
    public static final DeferredHolder<Attribute, Attribute> RANGED_ATTACK_SPEED = ATTRIBUTES.register(
            "ranged_attack_speed",
            () -> new RangedAttribute("attribute.ascendant_skills.ranged_attack_speed", 1.0D, 0.0D, 100.0D).setSyncable(true)
    );
    public static final DeferredHolder<Attribute, Attribute> RANGED_CRIT_CHANCE = ATTRIBUTES.register(
            "ranged_crit_chance",
            () -> new RangedAttribute("attribute.ascendant_skills.ranged_crit_chance", 0.0D, 0.0D, 1.0D).setSyncable(true)
    );
    public static final DeferredHolder<Attribute, Attribute> RANGED_CRIT_DAMAGE = ATTRIBUTES.register(
            "ranged_crit_damage",
            () -> new RangedAttribute("attribute.ascendant_skills.ranged_crit_damage", 0.0D, -10.0D, 10.0D).setSyncable(true)
    );
    public static final DeferredHolder<Attribute, Attribute> RANGED_ARMOR_SHRED = ATTRIBUTES.register(
            "ranged_armor_shred",
            () -> new RangedAttribute("attribute.ascendant_skills.ranged_armor_shred", 0.0D, 0.0D, 10.0D).setSyncable(true)
    );
    public static final DeferredHolder<Attribute, Attribute> PROJECTILE_VELOCITY = ATTRIBUTES.register(
            "projectile_velocity",
            () -> new RangedAttribute("attribute.ascendant_skills.projectile_velocity", 1.0D, 0.0D, 10.0D).setSyncable(true)
    );
    public static final DeferredHolder<Attribute, Attribute> PASSIVE_REGEN_HEALTH = ATTRIBUTES.register(
            "passive_regen_health",
            () -> new RangedAttribute("attribute.ascendant_skills.passive_regen_health", 0.0D, 0.0D, 100.0D).setSyncable(true)
    );
    public static final DeferredHolder<Attribute, Attribute> PASSIVE_REGEN_INTERVAL = ATTRIBUTES.register(
            "passive_regen_interval",
            () -> new RangedAttribute("attribute.ascendant_skills.passive_regen_interval", 20.0D, 1.0D, 120.0D).setSyncable(true)
    );
    public static final DeferredHolder<Attribute, Attribute> AGGRO_REACH = ATTRIBUTES.register(
            "aggro_reach",
            () -> new RangedAttribute("attribute.ascendant_skills.aggro_reach", 0.0D, 0.0D, 10.0D).setSyncable(true)
    );
    public static final DeferredHolder<Attribute, Attribute> CROP_GROWTH_SPEED = ATTRIBUTES.register(
            "crop_growth_speed",
            () -> new RangedAttribute("attribute.ascendant_skills.crop_growth_speed", 0.0D, 0.0D, 100.0D).setSyncable(true)
    );
    public static final DeferredHolder<Attribute, Attribute> CROP_GROWTH_RADIUS = ATTRIBUTES.register(
            "crop_growth_radius",
            () -> new RangedAttribute("attribute.ascendant_skills.crop_growth_radius", 0.0D, 0.0D, 64.0D).setSyncable(true)
    );
    public static final DeferredHolder<Attribute, Attribute> BREEDING_SPEED = ATTRIBUTES.register(
            "breeding_speed",
            () -> new RangedAttribute("attribute.ascendant_skills.breeding_speed", 0.0D, 0.0D, 10.0D).setSyncable(true)
    );
    public static final DeferredHolder<Attribute, Attribute> BREEDING_RADIUS = ATTRIBUTES.register(
            "breeding_radius",
            () -> new RangedAttribute("attribute.ascendant_skills.breeding_radius", 0.0D, 0.0D, 64.0D).setSyncable(true)
    );
    public static final DeferredHolder<Attribute, Attribute> ANIMAL_PRODUCT_BONUS = ATTRIBUTES.register(
            "animal_product_bonus",
            () -> new RangedAttribute("attribute.ascendant_skills.animal_product_bonus", 0.0D, 0.0D, 100.0D).setSyncable(true)
    );
    public static final DeferredHolder<Attribute, Attribute> CROP_HARVEST_BONUS = ATTRIBUTES.register(
            "crop_harvest_bonus",
            () -> new RangedAttribute("attribute.ascendant_skills.crop_harvest_bonus", 0.0D, 0.0D, 100.0D).setSyncable(true)
    );
    public static final DeferredHolder<Attribute, Attribute> RAW_FOOD_SATURATION = ATTRIBUTES.register(
            "raw_food_saturation",
            () -> new RangedAttribute("attribute.ascendant_skills.raw_food_saturation", 0.0D, 0.0D, 10.0D).setSyncable(true)
    );
    public static final DeferredHolder<Attribute, Attribute> HOE_DAMAGE = ATTRIBUTES.register(
            "hoe_damage",
            () -> new RangedAttribute("attribute.ascendant_skills.hoe_damage", 0.0D, 0.0D, 100.0D).setSyncable(true)
    );
    public static final DeferredHolder<Attribute, Attribute> GLOBAL_LIFE_STEAL = ATTRIBUTES.register(
            "global_life_steal",
            () -> new RangedAttribute("attribute.ascendant_skills.global_life_steal", 0.0D, 0.0D, 10.0D).setSyncable(true)
    );
    public static final DeferredHolder<Attribute, Attribute> UNDERWATER_BREATH_SECONDS = ATTRIBUTES.register(
            "underwater_breath_seconds",
            () -> new RangedAttribute("attribute.ascendant_skills.underwater_breath_seconds", 0.0D, 0.0D, 300.0D).setSyncable(true)
    );
    public static final DeferredHolder<Attribute, Attribute> MINING_SPEED = ATTRIBUTES.register(
            "mining_speed",
            () -> new RangedAttribute("attribute.ascendant_skills.mining_speed", 0.0D, 0.0D, 100.0D).setSyncable(true)
    );
    public static final DeferredHolder<Attribute, Attribute> PICKAXE_MINING_SPEED = ATTRIBUTES.register(
            "pickaxe_mining_speed",
            () -> new RangedAttribute("attribute.ascendant_skills.pickaxe_mining_speed", 0.0D, 0.0D, 100.0D).setSyncable(true)
    );
    public static final DeferredHolder<Attribute, Attribute> PICKAXE_INTERACTION_RANGE = ATTRIBUTES.register(
            "pickaxe_interaction_range",
            () -> new RangedAttribute("attribute.ascendant_skills.pickaxe_interaction_range", 0.0D, 0.0D, 64.0D).setSyncable(true)
    );
    public static final DeferredHolder<Attribute, Attribute> VIRTUAL_FORTUNE = ATTRIBUTES.register(
            "virtual_fortune",
            () -> new RangedAttribute("attribute.ascendant_skills.virtual_fortune", 0.0D, 0.0D, 16.0D).setSyncable(true)
    );
    public static final DeferredHolder<Attribute, Attribute> OBSIDIAN_MINING_SPEED = ATTRIBUTES.register(
            "obsidian_mining_speed",
            () -> new RangedAttribute("attribute.ascendant_skills.obsidian_mining_speed", 0.0D, 0.0D, 100.0D).setSyncable(true)
    );
    public static final DeferredHolder<Attribute, Attribute> PICKAXE_DURABILITY_PRESERVATION = ATTRIBUTES.register(
            "pickaxe_durability_preservation",
            () -> new RangedAttribute("attribute.ascendant_skills.pickaxe_durability_preservation", 0.0D, 0.0D, 1.0D).setSyncable(true)
    );

    private AscendantAttributes() {
    }

    public static void addPlayerAttributes(EntityAttributeModificationEvent event) {
        event.add(EntityType.PLAYER, GLOBAL_DAMAGE);
        event.add(EntityType.PLAYER, GLOBAL_ATTACK_SPEED);
        event.add(EntityType.PLAYER, GLOBAL_CRIT_CHANCE);
        event.add(EntityType.PLAYER, GLOBAL_CRIT_DAMAGE);
        event.add(EntityType.PLAYER, GLOBAL_RESISTANCE);
        event.add(EntityType.PLAYER, MELEE_RESISTANCE);
        event.add(EntityType.PLAYER, PROJECTILE_RESISTANCE);
        event.add(EntityType.PLAYER, EXPLOSION_RESISTANCE);
        event.add(EntityType.PLAYER, MELEE_ARMOR_SHRED);
        event.add(EntityType.PLAYER, MELEE_CRIT_CHANCE);
        event.add(EntityType.PLAYER, MELEE_CRIT_DAMAGE);
        event.add(EntityType.PLAYER, RANGED_DAMAGE);
        event.add(EntityType.PLAYER, RANGED_ATTACK_SPEED);
        event.add(EntityType.PLAYER, RANGED_CRIT_CHANCE);
        event.add(EntityType.PLAYER, RANGED_CRIT_DAMAGE);
        event.add(EntityType.PLAYER, RANGED_ARMOR_SHRED);
        event.add(EntityType.PLAYER, PROJECTILE_VELOCITY);
        event.add(EntityType.PLAYER, PASSIVE_REGEN_HEALTH);
        event.add(EntityType.PLAYER, PASSIVE_REGEN_INTERVAL);
        event.add(EntityType.PLAYER, AGGRO_REACH);
        event.add(EntityType.PLAYER, CROP_GROWTH_SPEED);
        event.add(EntityType.PLAYER, CROP_GROWTH_RADIUS);
        event.add(EntityType.PLAYER, BREEDING_SPEED);
        event.add(EntityType.PLAYER, BREEDING_RADIUS);
        event.add(EntityType.PLAYER, ANIMAL_PRODUCT_BONUS);
        event.add(EntityType.PLAYER, CROP_HARVEST_BONUS);
        event.add(EntityType.PLAYER, RAW_FOOD_SATURATION);
        event.add(EntityType.PLAYER, HOE_DAMAGE);
        event.add(EntityType.PLAYER, GLOBAL_LIFE_STEAL);
        event.add(EntityType.PLAYER, UNDERWATER_BREATH_SECONDS);
        event.add(EntityType.PLAYER, MINING_SPEED);
        event.add(EntityType.PLAYER, PICKAXE_MINING_SPEED);
        event.add(EntityType.PLAYER, PICKAXE_INTERACTION_RANGE);
        event.add(EntityType.PLAYER, VIRTUAL_FORTUNE);
        event.add(EntityType.PLAYER, OBSIDIAN_MINING_SPEED);
        event.add(EntityType.PLAYER, PICKAXE_DURABILITY_PRESERVATION);
    }

    public static int resetAscendantBaseValues(ServerPlayer player) {
        int changed = 0;
        changed += reset(player, GLOBAL_DAMAGE);
        changed += reset(player, GLOBAL_ATTACK_SPEED);
        changed += reset(player, GLOBAL_CRIT_CHANCE);
        changed += reset(player, GLOBAL_CRIT_DAMAGE);
        changed += reset(player, GLOBAL_RESISTANCE);
        changed += reset(player, MELEE_RESISTANCE);
        changed += reset(player, PROJECTILE_RESISTANCE);
        changed += reset(player, EXPLOSION_RESISTANCE);
        changed += reset(player, MELEE_ARMOR_SHRED);
        changed += reset(player, MELEE_CRIT_CHANCE);
        changed += reset(player, MELEE_CRIT_DAMAGE);
        changed += reset(player, RANGED_DAMAGE);
        changed += reset(player, RANGED_ATTACK_SPEED);
        changed += reset(player, RANGED_CRIT_CHANCE);
        changed += reset(player, RANGED_CRIT_DAMAGE);
        changed += reset(player, RANGED_ARMOR_SHRED);
        changed += reset(player, PROJECTILE_VELOCITY);
        changed += reset(player, PASSIVE_REGEN_HEALTH);
        changed += reset(player, PASSIVE_REGEN_INTERVAL);
        changed += reset(player, AGGRO_REACH);
        changed += reset(player, CROP_GROWTH_SPEED);
        changed += reset(player, CROP_GROWTH_RADIUS);
        changed += reset(player, BREEDING_SPEED);
        changed += reset(player, BREEDING_RADIUS);
        changed += reset(player, ANIMAL_PRODUCT_BONUS);
        changed += reset(player, CROP_HARVEST_BONUS);
        changed += reset(player, RAW_FOOD_SATURATION);
        changed += reset(player, HOE_DAMAGE);
        changed += reset(player, GLOBAL_LIFE_STEAL);
        changed += reset(player, UNDERWATER_BREATH_SECONDS);
        changed += reset(player, MINING_SPEED);
        changed += reset(player, PICKAXE_MINING_SPEED);
        changed += reset(player, PICKAXE_INTERACTION_RANGE);
        changed += reset(player, VIRTUAL_FORTUNE);
        changed += reset(player, OBSIDIAN_MINING_SPEED);
        changed += reset(player, PICKAXE_DURABILITY_PRESERVATION);
        return changed;
    }

    private static int reset(ServerPlayer player, DeferredHolder<Attribute, Attribute> attribute) {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance == null) {
            return 0;
        }
        double defaultValue = attribute.value().getDefaultValue();
        if (Math.abs(instance.getBaseValue() - defaultValue) < 0.00001D) {
            return 0;
        }
        instance.setBaseValue(defaultValue);
        return 1;
    }
}
