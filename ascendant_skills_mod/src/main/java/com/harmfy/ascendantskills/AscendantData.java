package com.harmfy.ascendantskills;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class AscendantData extends SavedData {
    private static final String DATA_ID = AscendantSkills.MOD_ID;
    private static final int TAG_STRING = 8;
    private static final int TAG_COMPOUND = 10;
    private static final SavedData.Factory<AscendantData> FACTORY = new SavedData.Factory<>(AscendantData::new, AscendantData::load);

    private final Map<UUID, Party> parties = new HashMap<>();
    private final Map<UUID, UUID> playerParty = new HashMap<>();
    private final Map<UUID, UUID> invitations = new HashMap<>();
    private final Map<UUID, PlayerProgress> progress = new HashMap<>();

    public static AscendantData get(MinecraftServer server) {
        return server.getLevel(Level.OVERWORLD).getDataStorage().computeIfAbsent(FACTORY, DATA_ID);
    }

    public static AscendantData load(CompoundTag tag, HolderLookup.Provider registries) {
        var data = new AscendantData();

        ListTag partyTags = tag.getList("parties", TAG_COMPOUND);
        for (int i = 0; i < partyTags.size(); i++) {
            CompoundTag partyTag = partyTags.getCompound(i);
            UUID id = partyTag.getUUID("id");
            UUID leader = partyTag.getUUID("leader");
            Party party = new Party(id, leader);
            ListTag members = partyTag.getList("members", TAG_STRING);
            for (int j = 0; j < members.size(); j++) {
                UUID member = UUID.fromString(members.getString(j));
                party.members.add(member);
                data.playerParty.put(member, id);
            }
            if (!party.members.isEmpty()) {
                data.parties.put(id, party);
            }
        }

        ListTag playerTags = tag.getList("players", TAG_COMPOUND);
        for (int i = 0; i < playerTags.size(); i++) {
            CompoundTag playerTag = playerTags.getCompound(i);
            UUID player = playerTag.getUUID("uuid");
            PlayerProgress playerProgress = new PlayerProgress();
            loadStringSet(playerTag.getList("bosses", TAG_STRING), playerProgress.bosses);
            loadStringSet(playerTag.getList("perks", TAG_STRING), playerProgress.perks);
            migratePerks(playerProgress.perks);
            loadSpentLevels(playerTag.getList("spent_levels", TAG_COMPOUND), playerProgress.spentLevels);
            data.progress.put(player, playerProgress);
        }

        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag partyTags = new ListTag();
        for (Party party : parties.values()) {
            CompoundTag partyTag = new CompoundTag();
            partyTag.putUUID("id", party.id);
            partyTag.putUUID("leader", party.leader);
            ListTag members = new ListTag();
            for (UUID member : party.members) {
                members.add(StringTag.valueOf(member.toString()));
            }
            partyTag.put("members", members);
            partyTags.add(partyTag);
        }
        tag.put("parties", partyTags);

        ListTag playerTags = new ListTag();
        for (var entry : progress.entrySet()) {
            CompoundTag playerTag = new CompoundTag();
            playerTag.putUUID("uuid", entry.getKey());
            playerTag.put("bosses", saveStringSet(entry.getValue().bosses));
            playerTag.put("perks", saveStringSet(entry.getValue().perks));
            playerTag.put("spent_levels", saveSpentLevels(entry.getValue().spentLevels));
            playerTags.add(playerTag);
        }
        tag.put("players", playerTags);
        return tag;
    }

    public Optional<Party> partyOf(UUID player) {
        UUID partyId = playerParty.get(player);
        return partyId == null ? Optional.empty() : Optional.ofNullable(parties.get(partyId));
    }

    public Optional<Party> partyById(UUID partyId) {
        return Optional.ofNullable(parties.get(partyId));
    }

    public Party createParty(UUID leader) {
        Party existing = partyOf(leader).orElse(null);
        if (existing != null) {
            return existing;
        }
        Party party = new Party(UUID.randomUUID(), leader);
        party.members.add(leader);
        parties.put(party.id, party);
        playerParty.put(leader, party.id);
        setDirty();
        return party;
    }

    public void invite(UUID invitee, UUID partyId) {
        invitations.put(invitee, partyId);
        setDirty();
    }

    public void clearInvite(UUID invitee) {
        if (invitations.remove(invitee) != null) {
            setDirty();
        }
    }

    public Optional<Party> consumeInvite(UUID invitee) {
        UUID partyId = invitations.remove(invitee);
        setDirty();
        return partyId == null ? Optional.empty() : partyById(partyId);
    }

    public void addMember(Party party, UUID player) {
        party.members.add(player);
        playerParty.put(player, party.id);
        setDirty();
    }

    public void leaveParty(UUID player) {
        Party party = partyOf(player).orElse(null);
        if (party == null) {
            return;
        }
        party.members.remove(player);
        playerParty.remove(player);
        if (player.equals(party.leader) && !party.members.isEmpty()) {
            party.leader = party.members.iterator().next();
        }
        if (party.members.size() < 2) {
            for (UUID member : new ArrayList<>(party.members)) {
                playerParty.remove(member);
            }
            parties.remove(party.id);
        }
        setDirty();
    }

    public boolean disbandPartyOf(UUID player) {
        Party party = partyOf(player).orElse(null);
        if (party == null) {
            return false;
        }
        disbandParty(party);
        return true;
    }

    public void disbandParty(Party party) {
        for (UUID member : new ArrayList<>(party.members)) {
            playerParty.remove(member);
        }
        parties.remove(party.id);
        invitations.values().removeIf(party.id::equals);
        setDirty();
    }

    public boolean unlockBoss(UUID player, String bossId) {
        boolean changed = progress(player).bosses.add(bossId);
        if (changed) {
            setDirty();
        }
        return changed;
    }

    public boolean clearBoss(UUID player, String bossId) {
        boolean changed = progress(player).bosses.remove(bossId);
        if (changed) {
            setDirty();
        }
        return changed;
    }

    public boolean hasBoss(UUID player, String bossId) {
        return progress(player).bosses.contains(bossId);
    }

    public Set<String> bosses(UUID player) {
        return Set.copyOf(progress(player).bosses);
    }

    public boolean grantPerk(UUID player, String perkId) {
        boolean changed = progress(player).perks.add(canonicalPerkId(perkId));
        if (changed) {
            setDirty();
        }
        return changed;
    }

    public boolean revokePerk(UUID player, String perkId) {
        boolean changed = progress(player).perks.remove(canonicalPerkId(perkId));
        if (changed) {
            setDirty();
        }
        return changed;
    }

    public boolean hasPerk(UUID player, String perkId) {
        return progress(player).perks.contains(canonicalPerkId(perkId));
    }

    public Set<String> perks(UUID player) {
        return Set.copyOf(progress(player).perks);
    }

    public int clearPerks(UUID player) {
        PlayerProgress playerProgress = progress(player);
        int removed = playerProgress.perks.size();
        if (removed > 0) {
            playerProgress.perks.clear();
            setDirty();
        }
        return removed;
    }

    public void recordSpentLevels(UUID player, String skillId, int levels) {
        if (levels <= 0) {
            return;
        }
        PlayerProgress playerProgress = progress(player);
        playerProgress.spentLevels.merge(canonicalSkillId(skillId), levels, Integer::sum);
        setDirty();
    }

    public int refundableSpentLevels(UUID player) {
        PlayerProgress playerProgress = progress(player);
        int recorded = playerProgress.spentLevels.values().stream().mapToInt(Integer::intValue).sum();
        if (recorded > 0) {
            return recorded;
        }

        return playerProgress.perks.stream()
                .map(AscendantData::skillIdFromPerkId)
                .map(Requirements::forSkill)
                .mapToInt(SkillRequirement::levels)
                .sum();
    }

    public void clearSpentLevels(UUID player) {
        PlayerProgress playerProgress = progress(player);
        if (!playerProgress.spentLevels.isEmpty()) {
            playerProgress.spentLevels.clear();
            setDirty();
        }
    }

    public void clearSpentLevels(UUID player, String skillId) {
        PlayerProgress playerProgress = progress(player);
        if (playerProgress.spentLevels.remove(canonicalSkillId(skillId)) != null) {
            setDirty();
        }
    }

    private PlayerProgress progress(UUID player) {
        return progress.computeIfAbsent(player, ignored -> new PlayerProgress());
    }

    private static void loadStringSet(ListTag tag, Collection<String> target) {
        for (int i = 0; i < tag.size(); i++) {
            target.add(tag.getString(i));
        }
    }

    private static void loadSpentLevels(ListTag tag, Map<String, Integer> target) {
        for (int i = 0; i < tag.size(); i++) {
            CompoundTag entry = tag.getCompound(i);
            String skillId = canonicalSkillId(entry.getString("skill"));
            int levels = entry.getInt("levels");
            if (!skillId.isBlank() && levels > 0) {
                target.put(skillId, levels);
            }
        }
    }

    private static void migratePerks(Set<String> perks) {
        boolean hadCombatiente = perks.remove(AscendantSkills.MOD_ID + ":combatiente");
        boolean hadVanguardia = perks.remove(AscendantSkills.MOD_ID + ":vanguardia");
        if (hadCombatiente) {
            perks.add(AscendantSkills.MOD_ID + ":combate");
        }
        if (hadVanguardia) {
            perks.add(AscendantSkills.MOD_ID + ":luchador");
        }
    }

    private static String canonicalPerkId(String perkId) {
        return switch (perkId) {
            case "ascendant_skills:combatiente" -> AscendantSkills.MOD_ID + ":combate";
            case "ascendant_skills:vanguardia" -> AscendantSkills.MOD_ID + ":luchador";
            default -> perkId;
        };
    }

    private static String skillIdFromPerkId(String perkId) {
        String prefix = AscendantSkills.MOD_ID + ":";
        return canonicalSkillId(perkId.startsWith(prefix) ? perkId.substring(prefix.length()) : perkId);
    }

    private static String canonicalSkillId(String skillId) {
        String prefix = AscendantSkills.MOD_ID + ":";
        String id = skillId.startsWith(prefix) ? skillId.substring(prefix.length()) : skillId;
        return switch (id) {
            case "combatiente" -> "combate";
            case "vanguardia" -> "luchador";
            default -> id;
        };
    }

    private static ListTag saveStringSet(Collection<String> values) {
        ListTag tag = new ListTag();
        for (String value : values) {
            tag.add(StringTag.valueOf(value));
        }
        return tag;
    }

    private static ListTag saveSpentLevels(Map<String, Integer> spentLevels) {
        ListTag tag = new ListTag();
        for (var entry : spentLevels.entrySet()) {
            CompoundTag spent = new CompoundTag();
            spent.putString("skill", entry.getKey());
            spent.putInt("levels", entry.getValue());
            tag.add(spent);
        }
        return tag;
    }

    public static final class Party {
        public final UUID id;
        public UUID leader;
        public final LinkedHashSet<UUID> members = new LinkedHashSet<>();

        private Party(UUID id, UUID leader) {
            this.id = id;
            this.leader = leader;
        }

        public boolean isValidForBossCredit() {
            return members.size() >= AscendantConfig.minPartySize() && members.size() <= AscendantConfig.maxPartySize();
        }
    }

    private static final class PlayerProgress {
        private final Set<String> bosses = new LinkedHashSet<>();
        private final Set<String> perks = new LinkedHashSet<>();
        private final Map<String, Integer> spentLevels = new LinkedHashMap<>();
    }
}
