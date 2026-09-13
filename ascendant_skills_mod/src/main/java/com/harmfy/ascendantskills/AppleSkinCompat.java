package com.harmfy.ascendantskills;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;

import java.lang.reflect.Field;
import java.util.function.Consumer;

public final class AppleSkinCompat {
    private AppleSkinCompat() {
    }

    @SuppressWarnings("unchecked")
    public static void register() {
        try {
            Class<?> eventClass = Class.forName("squeek.appleskin.api.event.FoodValuesEvent");
            if (!Event.class.isAssignableFrom(eventClass)) {
                AscendantSkills.LOGGER.warn("AppleSkin FoodValuesEvent is not a NeoForge event. Tooltip integration skipped.");
                return;
            }
            registerFoodValuesListener((Class<? extends Event>) eventClass);
            AscendantSkills.LOGGER.info("Ascendant Skills loaded with AppleSkin tooltip integration.");
        } catch (ClassNotFoundException ex) {
            AscendantSkills.LOGGER.warn("AppleSkin was loaded, but FoodValuesEvent was not found. Tooltip integration skipped.");
        }
    }

    private static void onFoodValues(Event event) {
        Player player = readField(event, "player", Player.class);
        ItemStack stack = readField(event, "itemStack", ItemStack.class);
        FoodProperties food = readField(event, "modifiedFoodProperties", FoodProperties.class);
        if (player == null || stack == null || food == null || !isRawNatureFood(stack)) {
            return;
        }

        AttributeInstance instance = player.getAttribute(AscendantAttributes.RAW_FOOD_SATURATION);
        double bonus = instance == null ? 0.0D : instance.getValue();
        if (bonus <= 0.0D) {
            return;
        }

        writeField(event, "modifiedFoodProperties", new FoodProperties(
                food.nutrition(),
                (float) (food.saturation() * (1.0D + bonus)),
                food.canAlwaysEat(),
                food.eatSeconds(),
                food.usingConvertsTo(),
                food.effects()
        ));
    }

    private static <T extends Event> void registerFoodValuesListener(Class<T> eventClass) {
        NeoForge.EVENT_BUS.addListener(EventPriority.NORMAL, eventClass, (Consumer<T>) AppleSkinCompat::onFoodValues);
    }

    private static boolean isRawNatureFood(ItemStack stack) {
        if (stack.get(DataComponents.FOOD) == null) {
            return false;
        }
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return AscendantConfig.natureRawFoodItems().contains(id.toString());
    }

    private static <T> T readField(Object target, String fieldName, Class<T> type) {
        try {
            Object value = target.getClass().getField(fieldName).get(target);
            return type.isInstance(value) ? type.cast(value) : null;
        } catch (IllegalAccessException | NoSuchFieldException ex) {
            return null;
        }
    }

    private static void writeField(Object target, String fieldName, Object value) {
        try {
            Field field = target.getClass().getField(fieldName);
            field.set(target, value);
        } catch (IllegalAccessException | NoSuchFieldException ignored) {
        }
    }
}
