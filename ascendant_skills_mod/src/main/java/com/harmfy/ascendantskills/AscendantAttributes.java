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

    private AscendantAttributes() {
    }

    public static void addPlayerAttributes(EntityAttributeModificationEvent event) {
        event.add(EntityType.PLAYER, GLOBAL_DAMAGE);
        event.add(EntityType.PLAYER, GLOBAL_ATTACK_SPEED);
        event.add(EntityType.PLAYER, GLOBAL_RESISTANCE);
    }
}
