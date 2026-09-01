package com.harmfy.ascendantskills;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attribute;
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
            () -> new RangedAttribute("attribute.ascendant_skills.melee_armor_shred", 0.0D, 0.0D, 1.0D).setSyncable(true)
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
            () -> new RangedAttribute("attribute.ascendant_skills.ranged_armor_shred", 0.0D, 0.0D, 1.0D).setSyncable(true)
    );
    public static final DeferredHolder<Attribute, Attribute> PASSIVE_REGEN_HEALTH = ATTRIBUTES.register(
            "passive_regen_health",
            () -> new RangedAttribute("attribute.ascendant_skills.passive_regen_health", 0.0D, 0.0D, 100.0D).setSyncable(true)
    );
    public static final DeferredHolder<Attribute, Attribute> PASSIVE_REGEN_INTERVAL = ATTRIBUTES.register(
            "passive_regen_interval",
            () -> new RangedAttribute("attribute.ascendant_skills.passive_regen_interval", 10.0D, 1.0D, 120.0D).setSyncable(true)
    );

    private AscendantAttributes() {
    }

    public static void addPlayerAttributes(EntityAttributeModificationEvent event) {
        event.add(EntityType.PLAYER, GLOBAL_DAMAGE);
        event.add(EntityType.PLAYER, GLOBAL_ATTACK_SPEED);
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
        event.add(EntityType.PLAYER, PASSIVE_REGEN_HEALTH);
        event.add(EntityType.PLAYER, PASSIVE_REGEN_INTERVAL);
    }
}
