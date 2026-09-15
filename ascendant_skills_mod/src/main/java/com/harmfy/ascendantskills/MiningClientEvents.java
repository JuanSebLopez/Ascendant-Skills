package com.harmfy.ascendantskills;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = AscendantSkills.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class MiningClientEvents {
    private static final KeyMapping CYCLE_ORE = new KeyMapping(
            "key.ascendant_skills.cycle_ore",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_C,
            "key.categories.ascendant_skills"
    );

    private MiningClientEvents() {
    }

    @SubscribeEvent
    public static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(CYCLE_ORE);
    }

    @EventBusSubscriber(modid = AscendantSkills.MOD_ID, value = Dist.CLIENT)
    public static final class GameBus {
        private GameBus() {
        }

        @SubscribeEvent
        public static void onClientTick(ClientTickEvent.Post event) {
            while (CYCLE_ORE.consumeClick()) {
                PacketDistributor.sendToServer(MinerCyclePayload.CYCLE_ORE_PAYLOAD);
            }
        }
    }
}
