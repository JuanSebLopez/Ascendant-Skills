package com.harmfy.ascendantskills;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.Set;
import java.util.stream.Stream;

public final class AscendantCommands {
    private static final SuggestionProvider<CommandSourceStack> BOSS_SUGGESTIONS = (ctx, builder) ->
            SharedSuggestionProvider.suggestResource(
                    AscendantConfig.bossIds().stream().map(ResourceLocation::parse),
                    builder);
    private static final SuggestionProvider<CommandSourceStack> PERK_SUGGESTIONS = (ctx, builder) ->
            SharedSuggestionProvider.suggestResource(
                    AscendantConfig.perkIds().stream().map(ResourceLocation::parse),
                    builder);
    private static final SuggestionProvider<CommandSourceStack> ATTRIBUTE_SUGGESTIONS = (ctx, builder) ->
            SharedSuggestionProvider.suggestResource(BuiltInRegistries.ATTRIBUTE.keySet(), builder);

    private AscendantCommands() {
    }

    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("ascendant_skills")
                .then(Commands.literal("party")
                        .then(Commands.literal("invite")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(ctx -> partyInvite(ctx.getSource(), EntityArgument.getPlayer(ctx, "player")))))
                        .then(Commands.literal("accept")
                                .executes(ctx -> partyAccept(ctx.getSource())))
                        .then(Commands.literal("leave")
                                .executes(ctx -> partyLeave(ctx.getSource())))
                        .then(Commands.literal("info")
                                .executes(ctx -> partyInfo(ctx.getSource())))
                        .then(Commands.literal("admin")
                                .requires(source -> source.hasPermission(2))
                                .then(Commands.literal("invite")
                                        .then(Commands.argument("leader", EntityArgument.player())
                                                .then(Commands.argument("target", EntityArgument.player())
                                                        .executes(ctx -> partyAdminInvite(ctx.getSource(), EntityArgument.getPlayer(ctx, "leader"), EntityArgument.getPlayer(ctx, "target"))))))
                                .then(Commands.literal("join")
                                        .then(Commands.argument("leader", EntityArgument.player())
                                                .then(Commands.argument("target", EntityArgument.player())
                                                        .executes(ctx -> partyAdminJoin(ctx.getSource(), EntityArgument.getPlayer(ctx, "leader"), EntityArgument.getPlayer(ctx, "target"))))))
                                .then(Commands.literal("kick")
                                        .then(Commands.argument("player", EntityArgument.player())
                                                .executes(ctx -> partyAdminKick(ctx.getSource(), EntityArgument.getPlayer(ctx, "player")))))
                                .then(Commands.literal("disband")
                                        .then(Commands.argument("player", EntityArgument.player())
                                                .executes(ctx -> partyAdminDisband(ctx.getSource(), EntityArgument.getPlayer(ctx, "player")))))))
                .then(Commands.literal("boss")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.literal("unlock")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .then(Commands.argument("boss", ResourceLocationArgument.id()).suggests(BOSS_SUGGESTIONS)
                                                .executes(ctx -> bossUnlock(ctx.getSource(), EntityArgument.getPlayer(ctx, "player"), ResourceLocationArgument.getId(ctx, "boss"))))))
                        .then(Commands.literal("clear")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .then(Commands.argument("boss", ResourceLocationArgument.id()).suggests(BOSS_SUGGESTIONS)
                                                .executes(ctx -> bossClear(ctx.getSource(), EntityArgument.getPlayer(ctx, "player"), ResourceLocationArgument.getId(ctx, "boss"))))))
                        .then(Commands.literal("list")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(ctx -> bossList(ctx.getSource(), EntityArgument.getPlayer(ctx, "player"))))))
                .then(Commands.literal("perk")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("perk", ResourceLocationArgument.id()).suggests(PERK_SUGGESTIONS)
                                        .executes(ctx -> perkGrant(ctx.getSource(), EntityArgument.getPlayer(ctx, "player"), ResourceLocationArgument.getId(ctx, "perk")))))
                        .then(Commands.literal("grant")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .then(Commands.argument("perk", ResourceLocationArgument.id()).suggests(PERK_SUGGESTIONS)
                                                .executes(ctx -> perkGrant(ctx.getSource(), EntityArgument.getPlayer(ctx, "player"), ResourceLocationArgument.getId(ctx, "perk"))))))
                        .then(Commands.literal("revoke")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .then(Commands.argument("perk", ResourceLocationArgument.id()).suggests(PERK_SUGGESTIONS)
                                                .executes(ctx -> perkRevoke(ctx.getSource(), EntityArgument.getPlayer(ctx, "player"), ResourceLocationArgument.getId(ctx, "perk"))))))
                        .then(Commands.literal("list")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(ctx -> perkList(ctx.getSource(), EntityArgument.getPlayer(ctx, "player"))))))
                .then(Commands.literal("attribute")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("attribute", ResourceLocationArgument.id()).suggests(ATTRIBUTE_SUGGESTIONS)
                                        .then(Commands.argument("value", DoubleArgumentType.doubleArg())
                                                .executes(ctx -> attributeSet(
                                                        ctx.getSource(),
                                                        EntityArgument.getPlayer(ctx, "player"),
                                                        ResourceLocationArgument.getId(ctx, "attribute"),
                                                        DoubleArgumentType.getDouble(ctx, "value")))))))
                .then(Commands.literal("puffish")
                        .then(Commands.literal("open")
                                .executes(ctx -> puffishOpen(ctx.getSource())))
                        .then(Commands.literal("sync")
                                .executes(ctx -> puffishSync(ctx.getSource())))
                        .then(Commands.literal("status")
                                .executes(ctx -> puffishStatus(ctx.getSource()))))
                .then(Commands.literal("config")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.literal("reload")
                                .executes(ctx -> configReload(ctx.getSource())))
                        .then(Commands.literal("path")
                                .executes(ctx -> configPath(ctx.getSource())))));
    }

    private static int partyInvite(CommandSourceStack source, ServerPlayer target) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer inviter = source.getPlayerOrException();
        return PartyService.invite(inviter, target) ? 1 : 0;
    }

    private static int partyAccept(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        return PartyService.accept(source.getPlayerOrException()) ? 1 : 0;
    }

    private static int partyLeave(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        return PartyService.leave(source.getPlayerOrException()) ? 1 : 0;
    }

    private static int partyInfo(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        source.sendSuccess(() -> PartyService.describe(player), false);
        return 1;
    }

    private static int partyAdminInvite(CommandSourceStack source, ServerPlayer leader, ServerPlayer target) {
        boolean changed = PartyService.forceInvite(leader, target);
        send(source, changed ? "Invite forzado creado: " + leader.getName().getString() + " -> " + target.getName().getString() + "." : "No se pudo crear el invite forzado.", changed);
        return changed ? 1 : 0;
    }

    private static int partyAdminJoin(CommandSourceStack source, ServerPlayer leader, ServerPlayer target) {
        boolean changed = PartyService.forceJoin(leader, target);
        send(source, changed ? target.getName().getString() + " fue agregado a la party de " + leader.getName().getString() + "." : "No se pudo agregar a la party.", changed);
        return changed ? 1 : 0;
    }

    private static int partyAdminKick(CommandSourceStack source, ServerPlayer target) {
        boolean changed = PartyService.forceKick(target);
        send(source, changed ? target.getName().getString() + " fue removido de su party." : target.getName().getString() + " no esta en una party.", changed);
        return changed ? 1 : 0;
    }

    private static int partyAdminDisband(CommandSourceStack source, ServerPlayer target) {
        boolean changed = PartyService.forceDisband(target);
        send(source, changed ? "Party cerrada usando a " + target.getName().getString() + " como referencia." : target.getName().getString() + " no esta en una party.", changed);
        return changed ? 1 : 0;
    }

    private static int bossUnlock(CommandSourceStack source, ServerPlayer target, ResourceLocation bossId) {
        boolean changed = AscendantData.get(source.getServer()).unlockBoss(target.getUUID(), bossId.toString());
        send(source, (changed ? "Boss desbloqueado: " : "Boss ya desbloqueado: ") + bossId + " para " + target.getName().getString() + ".", true);
        return changed ? 1 : 0;
    }

    private static int bossClear(CommandSourceStack source, ServerPlayer target, ResourceLocation bossId) {
        boolean changed = AscendantData.get(source.getServer()).clearBoss(target.getUUID(), bossId.toString());
        send(source, (changed ? "Boss removido: " : "Boss no estaba desbloqueado: ") + bossId + " para " + target.getName().getString() + ".", changed);
        return changed ? 1 : 0;
    }

    private static int bossList(CommandSourceStack source, ServerPlayer target) {
        Set<String> bosses = AscendantData.get(source.getServer()).bosses(target.getUUID());
        send(source, "Bosses de " + target.getName().getString() + ": " + (bosses.isEmpty() ? "(ninguno)" : String.join(", ", bosses)), true);
        return bosses.size();
    }

    private static int perkGrant(CommandSourceStack source, ServerPlayer target, ResourceLocation perkId) {
        boolean changed = AscendantData.get(source.getServer()).grantPerk(target.getUUID(), perkId.toString());
        send(source, (changed ? "Perk agregado: " : "Perk ya activo: ") + perkId + " para " + target.getName().getString() + ".", true);
        return changed ? 1 : 0;
    }

    private static int perkRevoke(CommandSourceStack source, ServerPlayer target, ResourceLocation perkId) {
        boolean changed = AscendantData.get(source.getServer()).revokePerk(target.getUUID(), perkId.toString());
        send(source, (changed ? "Perk removido: " : "Perk no activo: ") + perkId + " para " + target.getName().getString() + ".", changed);
        return changed ? 1 : 0;
    }

    private static int perkList(CommandSourceStack source, ServerPlayer target) {
        Set<String> perks = AscendantData.get(source.getServer()).perks(target.getUUID());
        send(source, "Perks de " + target.getName().getString() + ": " + (perks.isEmpty() ? "(ninguno)" : String.join(", ", perks)), true);
        return perks.size();
    }

    private static int attributeSet(CommandSourceStack source, ServerPlayer target, ResourceLocation attributeId, double value) {
        Holder.Reference<Attribute> holder = BuiltInRegistries.ATTRIBUTE.getHolder(attributeId).orElse(null);
        if (holder == null) {
            source.sendFailure(Component.literal("Atributo desconocido: " + attributeId));
            return 0;
        }

        AttributeInstance instance = target.getAttribute(holder);
        if (instance == null) {
            source.sendFailure(Component.literal(target.getName().getString() + " no tiene atributo " + attributeId));
            return 0;
        }

        instance.setBaseValue(value);
        send(source, "Atributo actualizado: " + attributeId + " para " + target.getName().getString() + " = " + value + ".", true);
        return 1;
    }

    private static int puffishOpen(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        PuffishBridge.open(source.getPlayerOrException());
        return 1;
    }

    private static int puffishSync(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        PuffishBridge.syncPoints(player);
        send(source, "Puntos Puffish sincronizados con niveles XP.", true);
        return 1;
    }

    private static int puffishStatus(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        source.sendSuccess(() -> Component.literal(PuffishBridge.status(player)), false);
        return 1;
    }

    private static int configReload(CommandSourceStack source) {
        AscendantConfig.loadOrCreate();
        source.getServer().getPlayerList().getPlayers().forEach(PuffishBridge::syncPoints);
        send(source, "Config recargada.", true);
        return 1;
    }

    private static int configPath(CommandSourceStack source) {
        send(source, "Config: " + AscendantConfig.configDirForDisplay(), true);
        return 1;
    }

    private static void send(CommandSourceStack source, String message, boolean success) {
        ChatFormatting color = success ? ChatFormatting.GREEN : ChatFormatting.RED;
        source.sendSuccess(() -> Component.literal("[Ascendant Skills] ").withStyle(ChatFormatting.GOLD)
                .append(Component.literal(message).withStyle(color)), false);
    }
}
