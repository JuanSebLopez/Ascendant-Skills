package com.harmfy.ascendantskills;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public final class BossTracker {
    private static final Map<UUID, BossFightLog> FIGHTS = new HashMap<>();

    private BossTracker() {
    }

    public static void onLivingDamage(LivingDamageEvent.Post event) {
        String bossId = entityId(event.getEntity());
        if (!AscendantConfig.bossIds().contains(bossId)) {
            return;
        }
        Entity attacker = event.getSource().getEntity();
        if (!(attacker instanceof ServerPlayer player)) {
            return;
        }
        BossFightLog fight = FIGHTS.computeIfAbsent(event.getEntity().getUUID(), ignored -> new BossFightLog(bossId));
        fight.entries.add(new DamageEntry(player.getUUID(), event.getEntity().level().getGameTime(), event.getNewDamage()));
    }

    public static void onLivingDeath(LivingDeathEvent event) {
        String bossId = entityId(event.getEntity());
        if (!AscendantConfig.bossIds().contains(bossId)) {
            return;
        }

        BossFightLog fight = FIGHTS.remove(event.getEntity().getUUID());
        if (fight == null) {
            Entity killer = event.getSource().getEntity();
            if (killer instanceof ServerPlayer player) {
                unlock(event.getEntity().getServer(), List.of(player.getUUID()), bossId);
            }
            return;
        }

        long minTick = event.getEntity().level().getGameTime() - AscendantConfig.bossCreditWindowTicks();
        List<DamageEntry> recent = fight.entries.stream()
                .filter(entry -> entry.tick >= minTick && entry.amount > 0)
                .toList();
        if (recent.isEmpty()) {
            return;
        }

        MinecraftServer server = event.getEntity().getServer();
        AscendantData data = AscendantData.get(server);
        Map<UUID, Double> playerDamage = new HashMap<>();
        for (DamageEntry entry : recent) {
            playerDamage.merge(entry.player, (double) entry.amount, Double::sum);
        }

        Map<UUID, Double> partyDamage = new HashMap<>();
        for (var entry : playerDamage.entrySet()) {
            data.partyOf(entry.getKey())
                    .filter(AscendantData.Party::isValidForBossCredit)
                    .ifPresent(party -> partyDamage.merge(party.id, entry.getValue(), Double::sum));
        }

        if (!partyDamage.isEmpty()) {
            UUID winningPartyId = partyDamage.entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .map(Map.Entry::getKey)
                    .orElseThrow();
            AscendantData.Party party = data.partyById(winningPartyId).orElse(null);
            if (party != null) {
                List<UUID> winners = party.members.stream()
                        .filter(playerDamage::containsKey)
                        .toList();
                unlock(server, winners, bossId);
            }
            return;
        }

        List<UUID> topSoloPlayers = playerDamage.entrySet().stream()
                .sorted(Map.Entry.<UUID, Double>comparingByValue(Comparator.reverseOrder()))
                .limit(AscendantConfig.maxSoloBossCreditPlayers())
                .map(Map.Entry::getKey)
                .collect(Collectors.toCollection(ArrayList::new));
        unlock(server, topSoloPlayers, bossId);
    }

    private static void unlock(MinecraftServer server, List<UUID> players, String bossId) {
        AscendantData data = AscendantData.get(server);
        for (UUID uuid : players) {
            boolean changed = data.unlockBoss(uuid, bossId);
            ServerPlayer player = server.getPlayerList().getPlayer(uuid);
            if (player != null && changed) {
                player.sendSystemMessage(Component.literal("Requisito desbloqueado: " + bossId));
            }
        }
    }

    private static String entityId(Entity entity) {
        ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        return id.toString();
    }

    private static final class BossFightLog {
        private final String bossId;
        private final List<DamageEntry> entries = new ArrayList<>();

        private BossFightLog(String bossId) {
            this.bossId = bossId;
        }
    }

    private record DamageEntry(UUID player, long tick, float amount) {
    }
}
