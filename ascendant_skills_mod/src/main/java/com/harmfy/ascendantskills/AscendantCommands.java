package com.harmfy.ascendantskills;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
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

public final class AscendantCommands {
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
                                .executes(ctx -> partyInfo(ctx.getSource()))))
                .then(Commands.literal("boss")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.literal("unlock")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .then(Commands.argument("boss", ResourceLocationArgument.id())
                                                .executes(ctx -> bossUnlock(ctx.getSource(), EntityArgument.getPlayer(ctx, "player"), ResourceLocationArgument.getId(ctx, "boss"))))))
                        .then(Commands.literal("clear")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .then(Commands.argument("boss", ResourceLocationArgument.id())
                                                .executes(ctx -> bossClear(ctx.getSource(), EntityArgument.getPlayer(ctx, "player"), ResourceLocationArgument.getId(ctx, "boss"))))))
                        .then(Commands.literal("list")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(ctx -> bossList(ctx.getSource(), EntityArgument.getPlayer(ctx, "player"))))))
                .then(Commands.literal("perk")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("perk", ResourceLocationArgument.id())
                                        .executes(ctx -> perkGrant(ctx.getSource(), EntityArgument.getPlayer(ctx, "player"), ResourceLocationArgument.getId(ctx, "perk")))))
                        .then(Commands.literal("grant")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .then(Commands.argument("perk", ResourceLocationArgument.id())
                                                .executes(ctx -> perkGrant(ctx.getSource(), EntityArgument.getPlayer(ctx, "player"), ResourceLocationArgument.getId(ctx, "perk"))))))
                        .then(Commands.literal("revoke")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .then(Commands.argument("perk", ResourceLocationArgument.id())
                                                .executes(ctx -> perkRevoke(ctx.getSource(), EntityArgument.getPlayer(ctx, "player"), ResourceLocationArgument.getId(ctx, "perk"))))))
                        .then(Commands.literal("list")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(ctx -> perkList(ctx.getSource(), EntityArgument.getPlayer(ctx, "player"))))))
                .then(Commands.literal("attribute")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("attribute", ResourceLocationArgument.id())
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
                                .executes(ctx -> puffishStatus(ctx.getSource())))));
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
        source.sendSuccess(() -> Component.literal(PartyService.describe(player)), false);
        return 1;
    }

    private static int bossUnlock(CommandSourceStack source, ServerPlayer target, ResourceLocation bossId) {
        boolean changed = AscendantData.get(source.getServer()).unlockBoss(target.getUUID(), bossId.toString());
        source.sendSuccess(() -> Component.literal((changed ? "Desbloqueado " : "Ya estaba desbloqueado ") + bossId + " para " + target.getName().getString()), true);
        return changed ? 1 : 0;
    }

    private static int bossClear(CommandSourceStack source, ServerPlayer target, ResourceLocation bossId) {
        boolean changed = AscendantData.get(source.getServer()).clearBoss(target.getUUID(), bossId.toString());
        source.sendSuccess(() -> Component.literal((changed ? "Removido " : "No tenia ") + bossId + " para " + target.getName().getString()), true);
        return changed ? 1 : 0;
    }

    private static int bossList(CommandSourceStack source, ServerPlayer target) {
        Set<String> bosses = AscendantData.get(source.getServer()).bosses(target.getUUID());
        source.sendSuccess(() -> Component.literal("Bosses de " + target.getName().getString() + ": " + (bosses.isEmpty() ? "(ninguno)" : String.join(", ", bosses))), false);
        return bosses.size();
    }

    private static int perkGrant(CommandSourceStack source, ServerPlayer target, ResourceLocation perkId) {
        boolean changed = AscendantData.get(source.getServer()).grantPerk(target.getUUID(), perkId.toString());
        source.sendSuccess(() -> Component.literal((changed ? "Perk agregado " : "Ya tenia perk ") + perkId + " para " + target.getName().getString()), true);
        return changed ? 1 : 0;
    }

    private static int perkRevoke(CommandSourceStack source, ServerPlayer target, ResourceLocation perkId) {
        boolean changed = AscendantData.get(source.getServer()).revokePerk(target.getUUID(), perkId.toString());
        source.sendSuccess(() -> Component.literal((changed ? "Perk removido " : "No tenia perk ") + perkId + " para " + target.getName().getString()), true);
        return changed ? 1 : 0;
    }

    private static int perkList(CommandSourceStack source, ServerPlayer target) {
        Set<String> perks = AscendantData.get(source.getServer()).perks(target.getUUID());
        source.sendSuccess(() -> Component.literal("Perks de " + target.getName().getString() + ": " + (perks.isEmpty() ? "(ninguno)" : String.join(", ", perks))), false);
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
        source.sendSuccess(() -> Component.literal("Base value de " + attributeId + " para " + target.getName().getString() + " = " + value), true);
        return 1;
    }

    private static int puffishOpen(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        PuffishBridge.open(source.getPlayerOrException());
        return 1;
    }

    private static int puffishSync(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        PuffishBridge.syncPoints(player);
        source.sendSuccess(() -> Component.literal("Puntos Puffish sincronizados con niveles XP."), false);
        return 1;
    }

    private static int puffishStatus(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        source.sendSuccess(() -> Component.literal(PuffishBridge.status(player)), false);
        return 1;
    }
}
