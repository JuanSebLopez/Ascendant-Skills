package com.harmfy.ascendantskills;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.puffish.skillsmod.api.Category;
import net.puffish.skillsmod.api.Skill;
import net.puffish.skillsmod.api.SkillsAPI;

public final class PuffishBridge {
    private static final ResourceLocation CATEGORY_ID = ResourceLocation.fromNamespaceAndPath(AscendantSkills.MOD_ID, "ascendant");
    private static final ResourceLocation POINT_SOURCE = ResourceLocation.fromNamespaceAndPath("puffish_skills", "commands");

    private PuffishBridge() {
    }

    public static void register() {
        SkillsAPI.registerSkillUnlockEvent(PuffishBridge::onSkillUnlock);
        SkillsAPI.registerSkillLockEvent(PuffishBridge::onSkillLock);
    }

    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            syncPoints(player);
        }
    }

    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player && player.tickCount % 100 == 0) {
            syncPoints(player);
        }
    }

    public static boolean isAvailable() {
        return ModList.get().isLoaded("puffish_skills");
    }

    public static void open(ServerPlayer player) {
        if (isAvailable()) {
            SkillsAPI.openScreen(player);
        } else {
            player.sendSystemMessage(Component.literal("Puffish Skills no esta cargado."));
        }
    }

    public static void syncPoints(ServerPlayer player) {
        if (!isAvailable()) {
            return;
        }
        SkillsAPI.getCategory(CATEGORY_ID).ifPresent(category -> {
            int visibleTotal = Math.max(0, player.experienceLevel) + category.getSpentPoints(player);
            int delta = visibleTotal - category.getPointsTotal(player);
            if (delta != 0) {
                category.addPointsSilently(player, POINT_SOURCE, delta);
            }
        });
    }

    public static String status(ServerPlayer player) {
        if (!isAvailable()) {
            return "Puffish Skills no esta cargado.";
        }
        return SkillsAPI.getCategory(CATEGORY_ID)
                .map(category -> "Puffish OK. XP levels=" + player.experienceLevel +
                        ", spent=" + category.getSpentPoints(player) +
                        ", left=" + category.getPointsLeft(player) +
                        ", total=" + category.getPointsTotal(player))
                .orElse("Puffish esta cargado, pero no existe la categoria " + CATEGORY_ID + ".");
    }

    public static boolean refreshRewards(ServerPlayer player) {
        if (!isAvailable()) {
            return false;
        }
        return SkillsAPI.getCategory(CATEGORY_ID)
                .map(category -> {
                    SkillsAPI.updateRewards(player, CATEGORY_ID);
                    syncPoints(player);
                    return true;
                })
                .orElse(false);
    }

    public static ResetResult resetAscendantTree(ServerPlayer player) {
        if (!isAvailable()) {
            AscendantData data = AscendantData.get(player.server);
            int removedPerks = data.clearPerks(player.getUUID());
            data.clearSpentLevels(player.getUUID());
            CombatPerks.resetRuntime(player);
            int resetAttributes = AscendantAttributes.resetAscendantBaseValues(player);
            return new ResetResult(false, 0, removedPerks, resetAttributes);
        }

        return SkillsAPI.getCategory(CATEGORY_ID)
                .map(category -> {
                    AscendantData data = AscendantData.get(player.server);
                    int refundedLevels = data.refundableSpentLevels(player.getUUID());
                    int removedPerks = data.perks(player.getUUID()).size();
                    category.resetSkills(player);
                    removedPerks = Math.max(removedPerks, data.clearPerks(player.getUUID()));
                    data.clearSpentLevels(player.getUUID());
                    if (refundedLevels > 0 && !player.isCreative()) {
                        player.giveExperienceLevels(refundedLevels);
                    }
                    CombatPerks.resetRuntime(player);
                    int resetAttributes = AscendantAttributes.resetAscendantBaseValues(player);
                    SkillsAPI.updateRewards(player, CATEGORY_ID);
                    syncPoints(player);
                    return new ResetResult(true, refundedLevels, removedPerks, resetAttributes);
                })
                .orElseGet(() -> {
                    AscendantData data = AscendantData.get(player.server);
                    int removedPerks = data.clearPerks(player.getUUID());
                    data.clearSpentLevels(player.getUUID());
                    CombatPerks.resetRuntime(player);
                    int resetAttributes = AscendantAttributes.resetAscendantBaseValues(player);
                    return new ResetResult(false, 0, removedPerks, resetAttributes);
                });
    }

    private static void onSkillUnlock(ServerPlayer player, ResourceLocation categoryId, String skillId) {
        if (!CATEGORY_ID.equals(categoryId)) {
            return;
        }

        SkillRequirement requirement = Requirements.forSkill(skillId);
        AscendantData data = AscendantData.get(player.server);
        String missingBoss = requirement.bossId()
                .filter(bossId -> !data.hasBoss(player.getUUID(), bossId))
                .orElse(null);
        if (missingBoss != null) {
            relock(player, skillId);
            player.sendSystemMessage(Component.literal("Falta requisito de boss para " + skillId + ": " + missingBoss));
            syncPoints(player);
            return;
        }
        if (player.experienceLevel < requirement.levels()) {
            relock(player, skillId);
            player.sendSystemMessage(Component.literal("Faltan niveles para " + skillId + ". Requiere " + requirement.levels() + ", tienes " + player.experienceLevel + "."));
            syncPoints(player);
            return;
        }

        if (!player.isCreative()) {
            player.giveExperienceLevels(-requirement.levels());
            data.recordSpentLevels(player.getUUID(), skillId, requirement.levels());
        }
        data.grantPerk(player.getUUID(), AscendantSkills.MOD_ID + ":" + skillId);
        player.sendSystemMessage(Component.literal("Skill desbloqueada: " + skillId + ". Niveles consumidos: " + requirement.levels() + "."));
        syncPoints(player);
    }

    private static void onSkillLock(ServerPlayer player, ResourceLocation categoryId, String skillId) {
        if (CATEGORY_ID.equals(categoryId)) {
            AscendantData.get(player.server).revokePerk(player.getUUID(), AscendantSkills.MOD_ID + ":" + skillId);
        }
    }

    private static void relock(ServerPlayer player, String skillId) {
        SkillsAPI.getCategory(CATEGORY_ID)
                .flatMap(category -> category.getSkill(skillId))
                .ifPresent(skill -> skill.lock(player));
    }

    public record ResetResult(boolean puffishCategoryFound, int refundedLevels, int removedPerks, int resetAttributes) {
    }
}
