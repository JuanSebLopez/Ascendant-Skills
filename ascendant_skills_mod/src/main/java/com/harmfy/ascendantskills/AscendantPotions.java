package com.harmfy.ascendantskills;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.neoforged.neoforge.common.brewing.IBrewingRecipe;
import net.neoforged.neoforge.event.brewing.RegisterBrewingRecipesEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.List;
import java.util.Optional;

public final class AscendantPotions {
    public static final DeferredRegister<Potion> POTIONS = DeferredRegister.create(BuiltInRegistries.POTION, AscendantSkills.MOD_ID);
    public static final DeferredHolder<Potion, Potion> FORGETFULNESS_ELIXIR = POTIONS.register(
            "forgetfulness_elixir",
            () -> new Potion("forgetfulness_elixir")
    );
    private static final int FORGETFULNESS_COLOR = 0x77777D;

    private AscendantPotions() {
    }

    public static void registerBrewingRecipes(RegisterBrewingRecipesEvent event) {
        event.getBuilder().addRecipe(new ForgetfulnessBrewingRecipe());
    }

    public static void onUseItemFinish(LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.level().isClientSide || !isForgetfulnessElixir(event.getItem())) {
            return;
        }

        PuffishBridge.ResetResult result = PuffishBridge.resetAscendantTree(player);
        player.sendSystemMessage(Component.literal("[Ascendant Skills] ").withStyle(ChatFormatting.GOLD)
                .append(Component.literal("Elixir del Olvido usado. Niveles reembolsados: " + result.refundedLevels() + ". Perks removidos: " + result.removedPerks() + ".").withStyle(ChatFormatting.GRAY)));
    }

    private static ItemStack createForgetfulnessPotion(ItemStack input) {
        ItemStack output = new ItemStack(input.getItem());
        output.set(DataComponents.POTION_CONTENTS, new PotionContents(Optional.of(FORGETFULNESS_ELIXIR), Optional.of(FORGETFULNESS_COLOR), List.of()));
        return output;
    }

    private static boolean isForgetfulnessElixir(ItemStack stack) {
        PotionContents contents = stack.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY);
        return contents.potion().map(holder -> holder.is(FORGETFULNESS_ELIXIR)).orElse(false);
    }

    private static final class ForgetfulnessBrewingRecipe implements IBrewingRecipe {
        @Override
        public boolean isInput(ItemStack input) {
            PotionContents contents = input.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY);
            return input.is(Items.POTION) && contents.is(Potions.WEAKNESS);
        }

        @Override
        public boolean isIngredient(ItemStack ingredient) {
            return ingredient.is(Items.SPORE_BLOSSOM);
        }

        @Override
        public ItemStack getOutput(ItemStack input, ItemStack ingredient) {
            return isInput(input) && isIngredient(ingredient) ? createForgetfulnessPotion(input) : ItemStack.EMPTY;
        }
    }
}
