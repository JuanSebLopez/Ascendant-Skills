package com.harmfy.ascendantskills;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

import java.util.ArrayList;
import java.util.List;

@EventBusSubscriber(modid = AscendantSkills.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class AscendantClientHud {
    private static final ResourceLocation LAYER_ID = ResourceLocation.fromNamespaceAndPath(AscendantSkills.MOD_ID, "perk_hud");
    private static final int BOX_SIZE = 12;
    private static final int BOX_GAP = 3;
    private static final long STALE_AFTER_MILLIS = 2_000L;
    private static PerkHudPayload payload;
    private static long lastUpdateMillis;

    private AscendantClientHud() {
    }

    @SubscribeEvent
    public static void registerGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.PLAYER_HEALTH, LAYER_ID, AscendantClientHud::render);
    }

    public static void accept(PerkHudPayload newPayload) {
        payload = newPayload;
        lastUpdateMillis = System.currentTimeMillis();
    }

    private static void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.options.hideGui || payload == null) {
            return;
        }
        if (System.currentTimeMillis() - lastUpdateMillis > STALE_AFTER_MILLIS) {
            return;
        }

        List<HudBox> boxes = boxes(payload);
        if (boxes.isEmpty()) {
            return;
        }

        int x = graphics.guiWidth() / 2 - 142;
        int y = graphics.guiHeight() - 59;
        for (int i = 0; i < boxes.size(); i++) {
            drawBox(graphics, minecraft, boxes.get(i), x + i * (BOX_SIZE + BOX_GAP), y);
        }
    }

    private static List<HudBox> boxes(PerkHudPayload state) {
        List<HudBox> boxes = new ArrayList<>();
        if (state.steelComboStacks() >= 0) {
            boxes.add(new HudBox("C", stackColor(state.steelComboStacks(), state.steelComboMaxStacks()),
                    state.steelComboStacks() > 0 ? Integer.toString(state.steelComboStacks()) : ""));
        }
        if (state.criticalEyeCooldownTicks() >= 0) {
            boolean ready = state.criticalEyeCooldownTicks() == 0;
            boxes.add(new HudBox("E", ready ? 0xFF31E6FF : 0xFF37404A,
                    ready ? "" : seconds(state.criticalEyeCooldownTicks())));
        }
        if (state.juggernautCooldownTicks() >= 0) {
            if (state.juggernautActiveTicks() > 0) {
                boxes.add(new HudBox("J", 0xFF77A7FF, seconds(state.juggernautActiveTicks())));
            } else {
                boxes.add(new HudBox("J", state.juggernautCooldownTicks() == 0 ? 0xFF87919E : 0xFF37404A,
                        state.juggernautCooldownTicks() == 0 ? "" : seconds(state.juggernautCooldownTicks())));
            }
        }
        if (state.berserkerActive()) {
            boxes.add(new HudBox("B", 0xFFE03A3A, ""));
        }
        if (state.conquerorStacks() >= 0) {
            boxes.add(new HudBox("Q", stackColor(state.conquerorStacks(), state.conquerorMaxStacks()),
                    state.conquerorStacks() > 0 ? Integer.toString(state.conquerorStacks()) : ""));
        }
        if (state.aljabaStacks() >= 0) {
            boxes.add(new HudBox("A", stackColor(state.aljabaStacks(), state.aljabaMaxStacks()),
                    state.aljabaStacks() > 0 ? Integer.toString(state.aljabaStacks()) : ""));
        }
        if (state.deadeyeStacks() >= 0) {
            boxes.add(new HudBox("D", stackColor(state.deadeyeStacks(), state.deadeyeMaxStacks()),
                    state.deadeyeStacks() > 0 ? compact(state.deadeyeStacks()) : ""));
        }
        if (state.escaramuzadorProgress() >= 0) {
            int remaining = Math.max(0, 10 - state.escaramuzadorProgress());
            boxes.add(new HudBox("S", state.escaramuzadorReady() ? 0xFF31E6FF : 0xFF37404A,
                    state.escaramuzadorReady() ? "" : Integer.toString(Math.min(9, remaining))));
        }
        if (state.maestroCooldownTicks() >= 0) {
            boolean ready = state.maestroCooldownTicks() == 0;
            boxes.add(new HudBox("M", ready ? 0xFF31E6FF : 0xFF37404A,
                    ready ? "" : seconds(state.maestroCooldownTicks())));
        }
        if (state.barrageHits() >= 0) {
            boxes.add(new HudBox("R", state.barrageReady() ? 0xFF31E6FF : stackColor(state.barrageHits(), state.barrageRequiredHits()),
                    state.barrageReady() ? "" : Integer.toString(Math.min(9, state.barrageHits()))));
        }
        return boxes;
    }

    private static void drawBox(GuiGraphics graphics, Minecraft minecraft, HudBox box, int x, int y) {
        graphics.fill(x - 1, y - 1, x + BOX_SIZE + 1, y + BOX_SIZE + 1, 0xCC101015);
        graphics.fill(x, y, x + BOX_SIZE, y + BOX_SIZE, box.color());
        graphics.fill(x + 1, y + 1, x + BOX_SIZE - 1, y + BOX_SIZE - 1, 0xCC17171C);
        graphics.drawString(minecraft.font, box.label(), x + 3, y + 2, 0xFFFFFFFF, false);
        if (!box.value().isEmpty()) {
            graphics.drawString(minecraft.font, box.value(), x + 7, y + 7, 0xFFFFFFFF, false);
        }
    }

    private static int stackColor(int stacks, int maxStacks) {
        if (stacks <= 0) {
            return 0xFF37404A;
        }
        return stacks >= maxStacks ? 0xFFFFC857 : 0xFFFF7A36;
    }

    private static String seconds(int ticks) {
        int seconds = Math.max(1, (ticks + 19) / 20);
        return seconds > 9 ? "9" : Integer.toString(seconds);
    }

    private static String compact(int value) {
        return value > 9 ? "9" : Integer.toString(value);
    }

    private record HudBox(String label, int color, String value) {
    }
}
