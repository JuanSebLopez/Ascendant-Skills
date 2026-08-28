package com.harmfy.ascendantskills;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;
import java.util.stream.Collectors;

public final class PartyService {
    public static final int MAX_PARTY_SIZE = 4;

    private PartyService() {
    }

    public static boolean invite(ServerPlayer inviter, ServerPlayer target) {
        AscendantData data = AscendantData.get(inviter.server);
        if (inviter.getUUID().equals(target.getUUID())) {
            inviter.sendSystemMessage(Component.literal("No puedes invitarte a tu propia party."));
            return false;
        }
        if (data.partyOf(target.getUUID()).isPresent()) {
            inviter.sendSystemMessage(Component.literal(target.getName().getString() + " ya esta en una party."));
            return false;
        }
        if (inviter.level().dimension() != target.level().dimension()) {
            inviter.sendSystemMessage(Component.literal("Para crear o invitar a una party, ambos jugadores deben estar en la misma dimension."));
            return false;
        }

        AscendantData.Party party = data.createParty(inviter.getUUID());
        if (party.members.size() >= MAX_PARTY_SIZE) {
            inviter.sendSystemMessage(Component.literal("La party ya esta llena. Maximo " + MAX_PARTY_SIZE + " jugadores."));
            return false;
        }

        data.invite(target.getUUID(), party.id);
        inviter.sendSystemMessage(Component.literal("Invitaste a " + target.getName().getString() + " a tu party."));
        target.sendSystemMessage(Component.literal(inviter.getName().getString() + " te invito a su party. Usa /ascendant_skills party accept."));
        return true;
    }

    public static boolean accept(ServerPlayer player) {
        AscendantData data = AscendantData.get(player.server);
        if (data.partyOf(player.getUUID()).isPresent()) {
            player.sendSystemMessage(Component.literal("Ya estas en una party."));
            return false;
        }

        AscendantData.Party party = data.consumeInvite(player.getUUID()).orElse(null);
        if (party == null) {
            player.sendSystemMessage(Component.literal("No tienes invitaciones pendientes."));
            return false;
        }
        if (party.members.size() >= MAX_PARTY_SIZE) {
            player.sendSystemMessage(Component.literal("Esa party ya esta llena."));
            return false;
        }

        ServerPlayer leader = player.server.getPlayerList().getPlayer(party.leader);
        if (leader != null && leader.level().dimension() != player.level().dimension()) {
            player.sendSystemMessage(Component.literal("Debes estar en la misma dimension que quien te invito para entrar a la party."));
            return false;
        }

        data.addMember(party, player.getUUID());
        broadcast(player, party, player.getName().getString() + " entro a la party.");
        return true;
    }

    public static boolean leave(ServerPlayer player) {
        AscendantData data = AscendantData.get(player.server);
        AscendantData.Party party = data.partyOf(player.getUUID()).orElse(null);
        if (party == null) {
            player.sendSystemMessage(Component.literal("No estas en una party."));
            return false;
        }
        data.leaveParty(player.getUUID());
        player.sendSystemMessage(Component.literal("Saliste de la party."));
        for (UUID member : party.members) {
            ServerPlayer online = player.server.getPlayerList().getPlayer(member);
            if (online != null && !online.getUUID().equals(player.getUUID())) {
                online.sendSystemMessage(Component.literal(player.getName().getString() + " salio de la party."));
            }
        }
        return true;
    }

    public static String describe(ServerPlayer viewer) {
        AscendantData data = AscendantData.get(viewer.server);
        AscendantData.Party party = data.partyOf(viewer.getUUID()).orElse(null);
        if (party == null) {
            return "No estas en una party.";
        }
        return "Party " + party.id + " [" + party.members.size() + "/" + MAX_PARTY_SIZE + "]: " +
                party.members.stream()
                        .map(uuid -> name(viewer, uuid) + (uuid.equals(party.leader) ? " (lider)" : ""))
                        .collect(Collectors.joining(", "));
    }

    private static void broadcast(ServerPlayer context, AscendantData.Party party, String message) {
        for (UUID member : party.members) {
            ServerPlayer online = context.server.getPlayerList().getPlayer(member);
            if (online != null) {
                online.sendSystemMessage(Component.literal(message));
            }
        }
    }

    private static String name(ServerPlayer context, UUID uuid) {
        ServerPlayer online = context.server.getPlayerList().getPlayer(uuid);
        return online == null ? uuid.toString() : online.getName().getString();
    }
}
