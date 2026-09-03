package com.harmfy.ascendantskills;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.neoforged.neoforge.event.brewing.PotionBrewEvent;
import net.neoforged.neoforge.event.brewing.RegisterBrewingRecipesEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class AscendantPotions {
    public static final DeferredRegister<Potion> POTIONS = DeferredRegister.create(BuiltInRegistries.POTION, AscendantSkills.MOD_ID);
    public static final DeferredHolder<Potion, Potion> FORGETFULNESS_ELIXIR = POTIONS.register(
            "forgetfulness_elixir",
            () -> new Potion("forgetfulness_elixir", new MobEffectInstance(AscendantEffects.FORGETFULNESS, 1))
    );

    private AscendantPotions() {
    }

    public static void registerBrewingRecipes(RegisterBrewingRecipesEvent event) {
        event.getBuilder().addMix(Potions.WEAKNESS, Items.SPORE_BLOSSOM, FORGETFULNESS_ELIXIR);
    }

    public static void onUseItemFinish(LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.level().isClientSide || !isForgetfulnessElixir(event.getItem())) {
            return;
        }

        PuffishBridge.ResetResult result = PuffishBridge.resetAscendantTree(player);
        player.sendSystemMessage(Component.literal("[Ascendant Skills] ").withStyle(ChatFormatting.GOLD)
                .append(Component.literal("Elixir del Olvido usado. Niveles reembolsados: " + result.refundedLevels() + ". Perks removidos: " + result.removedPerks() + ". Atributos propios reiniciados: " + result.resetAttributes() + ".").withStyle(ChatFormatting.GRAY)));
    }

    public static void onPotionBrewPost(PotionBrewEvent.Post event) {
        for (int i = 0; i < event.getLength(); i++) {
            ItemStack stack = event.getItem(i);
            if ((stack.is(Items.SPLASH_POTION) || stack.is(Items.LINGERING_POTION)) && hasForgetfulnessPotion(stack)) {
                event.setItem(i, createNormalForgetfulnessElixir());
            }
        }
    }

    private static ItemStack createNormalForgetfulnessElixir() {
        ItemStack stack = new ItemStack(Items.POTION);
        stack.set(DataComponents.POTION_CONTENTS, new PotionContents(FORGETFULNESS_ELIXIR));
        return stack;
    }

    private static boolean isForgetfulnessElixir(ItemStack stack) {
        return stack.is(Items.POTION) && hasForgetfulnessPotion(stack);
    }

    private static boolean hasForgetfulnessPotion(ItemStack stack) {
        PotionContents contents = stack.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY);
        return contents.potion().map(holder -> holder.is(FORGETFULNESS_ELIXIR)).orElse(false);
    }
}
