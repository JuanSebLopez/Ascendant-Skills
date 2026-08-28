package com.harmfy.ascendantskills;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;
import java.util.stream.Collectors;

public final class PartyService {
    private PartyService() {
    }

    public static boolean invite(ServerPlayer inviter, ServerPlayer target) {
        AscendantData data = AscendantData.get(inviter.server);
        if (inviter.getUUID().equals(target.getUUID())) {
            send(inviter, "No puedes invitarte a tu propia party.", ChatFormatting.RED);
            return false;
        }
        if (data.partyOf(target.getUUID()).isPresent()) {
            send(inviter, target.getName().getString() + " ya esta en una party.", ChatFormatting.RED);
            return false;
        }
        if (inviter.level().dimension() != target.level().dimension()) {
            send(inviter, "Para crear o invitar a una party, ambos jugadores deben estar en la misma dimension.", ChatFormatting.RED);
            return false;
        }

        AscendantData.Party party = data.createParty(inviter.getUUID());
        if (party.members.size() >= AscendantConfig.maxPartySize()) {
            send(inviter, "La party ya esta llena. Maximo " + AscendantConfig.maxPartySize() + " jugadores.", ChatFormatting.RED);
            return false;
        }

        data.invite(target.getUUID(), party.id);
        send(inviter, "Invitaste a " + target.getName().getString() + " a tu party.", ChatFormatting.GREEN);
        send(target, inviter.getName().getString() + " te invito a su party. Usa /ascendant_skills party accept.", ChatFormatting.YELLOW);
        return true;
    }

    public static boolean accept(ServerPlayer player) {
        AscendantData data = AscendantData.get(player.server);
        if (data.partyOf(player.getUUID()).isPresent()) {
            send(player, "Ya estas en una party.", ChatFormatting.RED);
            return false;
        }

        AscendantData.Party party = data.consumeInvite(player.getUUID()).orElse(null);
        if (party == null) {
            send(player, "No tienes invitaciones pendientes.", ChatFormatting.RED);
            return false;
        }
        if (party.members.size() >= AscendantConfig.maxPartySize()) {
            send(player, "Esa party ya esta llena.", ChatFormatting.RED);
            return false;
        }

        ServerPlayer leader = player.server.getPlayerList().getPlayer(party.leader);
        if (leader != null && leader.level().dimension() != player.level().dimension()) {
            send(player, "Debes estar en la misma dimension que quien te invito para entrar a la party.", ChatFormatting.RED);
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
            send(player, "No estas en una party.", ChatFormatting.RED);
            return false;
        }
        data.leaveParty(player.getUUID());
        send(player, "Saliste de la party.", ChatFormatting.YELLOW);
        for (UUID member : party.members) {
            ServerPlayer online = player.server.getPlayerList().getPlayer(member);
            if (online != null && !online.getUUID().equals(player.getUUID())) {
                send(online, player.getName().getString() + " salio de la party.", ChatFormatting.YELLOW);
            }
        }
        return true;
    }

    public static Component describe(ServerPlayer viewer) {
        AscendantData data = AscendantData.get(viewer.server);
        AscendantData.Party party = data.partyOf(viewer.getUUID()).orElse(null);
        if (party == null) {
            return prefixed("No estas en una party.", ChatFormatting.RED);
        }
        return prefixed("Party [" + party.members.size() + "/" + AscendantConfig.maxPartySize() + "]: " +
                party.members.stream()
                        .map(uuid -> name(viewer, uuid) + (uuid.equals(party.leader) ? " (lider)" : ""))
                        .collect(Collectors.joining(", ")), ChatFormatting.AQUA);
    }

    public static boolean forceInvite(ServerPlayer leader, ServerPlayer target) {
        AscendantData data = AscendantData.get(leader.server);
        if (leader.getUUID().equals(target.getUUID())) {
            send(leader, "No puedes invitarte a tu propia party.", ChatFormatting.RED);
            return false;
        }
        if (data.partyOf(target.getUUID()).isPresent()) {
            send(leader, target.getName().getString() + " ya esta en una party.", ChatFormatting.RED);
            return false;
        }

        AscendantData.Party party = data.createParty(leader.getUUID());
        if (party.members.size() >= AscendantConfig.maxPartySize()) {
            send(leader, "La party ya esta llena. Maximo " + AscendantConfig.maxPartySize() + " jugadores.", ChatFormatting.RED);
            return false;
        }

        data.invite(target.getUUID(), party.id);
        send(leader, "Invite forzado enviado a " + target.getName().getString() + ".", ChatFormatting.GREEN);
        send(target, leader.getName().getString() + " te invito a su party. Usa /ascendant_skills party accept.", ChatFormatting.YELLOW);
        return true;
    }

    public static boolean forceJoin(ServerPlayer leader, ServerPlayer target) {
        if (leader.getUUID().equals(target.getUUID())) {
            send(leader, "El lider ya esta en su propia party.", ChatFormatting.RED);
            return false;
        }

        AscendantData data = AscendantData.get(leader.server);
        AscendantData.Party party = data.createParty(leader.getUUID());
        if (!party.members.contains(target.getUUID()) && party.members.size() >= AscendantConfig.maxPartySize()) {
            send(leader, "La party ya esta llena. Maximo " + AscendantConfig.maxPartySize() + " jugadores.", ChatFormatting.RED);
            return false;
        }
        if (data.partyOf(target.getUUID()).map(existing -> existing.id.equals(party.id)).orElse(false)) {
            send(leader, target.getName().getString() + " ya esta en esa party.", ChatFormatting.YELLOW);
            return false;
        }

        data.clearInvite(target.getUUID());
        data.leaveParty(target.getUUID());
        data.addMember(party, target.getUUID());
        broadcast(leader, party, target.getName().getString() + " fue agregado a la party por debug.");
        return true;
    }

    public static boolean forceKick(ServerPlayer target) {
        AscendantData data = AscendantData.get(target.server);
        if (data.partyOf(target.getUUID()).isEmpty()) {
            send(target, "No estas en una party.", ChatFormatting.RED);
            return false;
        }
        data.leaveParty(target.getUUID());
        send(target, "Fuiste removido de la party por debug.", ChatFormatting.YELLOW);
        return true;
    }

    public static boolean forceDisband(ServerPlayer target) {
        AscendantData data = AscendantData.get(target.server);
        AscendantData.Party party = data.partyOf(target.getUUID()).orElse(null);
        if (party == null) {
            send(target, "No estas en una party.", ChatFormatting.RED);
            return false;
        }
        broadcast(target, party, "La party fue cerrada por debug.");
        data.disbandParty(party);
        return true;
    }

    private static void broadcast(ServerPlayer context, AscendantData.Party party, String message) {
        for (UUID member : party.members) {
            ServerPlayer online = context.server.getPlayerList().getPlayer(member);
            if (online != null) {
                send(online, message, ChatFormatting.YELLOW);
            }
        }
    }

    private static String name(ServerPlayer context, UUID uuid) {
        ServerPlayer online = context.server.getPlayerList().getPlayer(uuid);
        return online == null ? uuid.toString() : online.getName().getString();
    }

    private static void send(ServerPlayer player, String message, ChatFormatting color) {
        player.sendSystemMessage(prefixed(message, color));
    }

    private static Component prefixed(String message, ChatFormatting color) {
        return Component.literal("[Ascendant Skills] ").withStyle(ChatFormatting.GOLD)
                .append(Component.literal(message).withStyle(color));
    }
}
