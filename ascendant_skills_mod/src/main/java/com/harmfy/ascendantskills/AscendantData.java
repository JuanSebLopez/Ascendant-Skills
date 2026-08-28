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
        boolean changed = progress(player).perks.add(perkId);
        if (changed) {
            setDirty();
        }
        return changed;
    }

    public boolean revokePerk(UUID player, String perkId) {
        boolean changed = progress(player).perks.remove(perkId);
        if (changed) {
            setDirty();
        }
        return changed;
    }

    public boolean hasPerk(UUID player, String perkId) {
        return progress(player).perks.contains(perkId);
    }

    public Set<String> perks(UUID player) {
        return Set.copyOf(progress(player).perks);
    }

    private PlayerProgress progress(UUID player) {
        return progress.computeIfAbsent(player, ignored -> new PlayerProgress());
    }

    private static void loadStringSet(ListTag tag, Collection<String> target) {
        for (int i = 0; i < tag.size(); i++) {
            target.add(tag.getString(i));
        }
    }

    private static ListTag saveStringSet(Collection<String> values) {
        ListTag tag = new ListTag();
        for (String value : values) {
            tag.add(StringTag.valueOf(value));
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
    }
}
