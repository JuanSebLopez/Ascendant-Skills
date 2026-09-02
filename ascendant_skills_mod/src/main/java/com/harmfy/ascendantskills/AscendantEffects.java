package com.harmfy.ascendantskills;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class AscendantEffects {
    public static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(BuiltInRegistries.MOB_EFFECT, AscendantSkills.MOD_ID);
    public static final DeferredHolder<MobEffect, MobEffect> FORGETFULNESS = EFFECTS.register(
            "forgetfulness",
            () -> new ForgetfulnessEffect(MobEffectCategory.NEUTRAL, 0x77777D)
    );

    private AscendantEffects() {
    }

    private static final class ForgetfulnessEffect extends MobEffect {
        private ForgetfulnessEffect(MobEffectCategory category, int color) {
            super(category, color);
        }
    }
}
