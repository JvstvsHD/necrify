/*
 * This file is part of Necrify (formerly Velocity Punishment), a plugin designed to manage player's punishments for the platforms Velocity and partly Paper.
 * Copyright (C) 2022-2024 JvstvsHD
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package de.jvstvshd.necrify.velocity.commands;

import com.google.common.collect.ImmutableList;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandSource;
import de.jvstvshd.necrify.api.PunishmentException;
import de.jvstvshd.necrify.api.punishment.Punishment;
import de.jvstvshd.necrify.velocity.NecrifyVelocityPlugin;
import de.jvstvshd.necrify.velocity.internal.PunishmentHelper;
import de.jvstvshd.necrify.velocity.internal.Util;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.event.HoverEventSource;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class PunishmentCommand {

    private final static List<String> PUNISHMENT_OPTIONS = ImmutableList.of("cancel", "remove", "info", "change");

    public static BrigadierCommand punishmentCommand(NecrifyVelocityPlugin plugin) {
        var node = Util.permissibleCommand("punishment", "necrify.command.punishment")
                .then(LiteralArgumentBuilder.<CommandSource>literal("playerinfo")
                        .then(Util.punishmentRemoveArgument(plugin).executes(context -> executePlayerInfo(context, plugin))))
                .then(RequiredArgumentBuilder.<CommandSource, String>argument("punishment ID", StringArgumentType.word())
                        .then(RequiredArgumentBuilder.<CommandSource, String>argument("option", StringArgumentType.word()).suggests((context, builder) -> {
                            PUNISHMENT_OPTIONS.forEach(builder::suggest);
                            return builder.buildFuture();
                        }).executes(context -> execute(context, plugin))));
        return new BrigadierCommand(node);
    }

    private static int executePlayerInfo(CommandContext<CommandSource> context, NecrifyVelocityPlugin plugin) {
        CommandSource source = context.getSource();
        var punishmentManager = plugin.getPunishmentManager();
        PunishmentHelper.getPlayerUuid(context, plugin).whenCompleteAsync((uuid, throwable) -> {
            if (Util.sendErrorMessageIfErrorOccurred(context, uuid, throwable, plugin)) return;
            punishmentManager.getPunishments(uuid, plugin.getService()).whenComplete((punishments, t) -> {
                if (t != null) {
                    source.sendMessage(plugin.getMessageProvider().internalError());
                    plugin.getLogger().error("An error occurred while getting punishments for player {}", uuid, t);
                    return;
                }
                source.sendMessage(plugin.getMessageProvider().provide("command.punishment.punishments", Component.text(punishments.size())).color(NamedTextColor.AQUA));
                for (Punishment punishment : punishments) {
                    Component component = PunishmentHelper.buildPunishmentData(punishment, plugin.getMessageProvider())
                            .clickEvent(ClickEvent.suggestCommand(punishment.getPunishmentUuid().toString().toLowerCase(Locale.ROOT)))
                            .hoverEvent((HoverEventSource<Component>) op -> HoverEvent.showText(plugin.getMessageProvider().provide("commands.general.copy")
                                    .color(NamedTextColor.GREEN)));
                    source.sendMessage(component);
                }
            });
        }, plugin.getService());
        return Command.SINGLE_SUCCESS;
    }

    private static int execute(CommandContext<CommandSource> context, NecrifyVelocityPlugin plugin) {
        var source = context.getSource();
        var uuidString = context.getArgument("punishment ID", String.class);
        UUID uuid;
        try {
            uuid = Util.parseUuid(uuidString);
        } catch (IllegalArgumentException e) {
            source.sendMessage(plugin.getMessageProvider().provide("command.punishment.uuid-parse-error", Component.text(uuidString).color(NamedTextColor.YELLOW)).color(NamedTextColor.RED));
            return 0;
        }
        String option = context.getArgument("option", String.class);
        if (!PUNISHMENT_OPTIONS.contains(option)) {
            source.sendMessage(plugin.getMessageProvider().provide("command.punishment.unknown-option", Component.text(option).color(NamedTextColor.YELLOW)).color(NamedTextColor.RED));
            return 0;
        }
        plugin.getPunishmentManager().getPunishment(uuid, plugin.getService()).whenCompleteAsync((optional, throwable) -> {
            if (throwable != null) {
                plugin.getLogger().error("An error occurred while getting punishment {}", uuid, throwable);
                source.sendMessage(plugin.getMessageProvider().internalError());
                return;
            }
            Punishment punishment;
            if (optional.isEmpty()) {
                source.sendMessage(plugin.getMessageProvider().provide("command.punishment.unknown-punishment-id",
                        Component.text(uuid.toString().toLowerCase(Locale.ROOT)).color(NamedTextColor.YELLOW)).color(NamedTextColor.RED));
                return;
            }
            punishment = optional.get();
            switch (option) {
                case "cancel", "remove" -> {
                    try {
                        punishment.cancel().whenCompleteAsync((unused, t) -> {
                            if (t != null) {
                                plugin.getLogger().error("An error occurred while cancelling punishment {}", uuid, t);
                                source.sendMessage(plugin.getMessageProvider().internalError());
                                return;
                            }
                            source.sendMessage(plugin.getMessageProvider().provide("punishment.remove").color(NamedTextColor.GREEN));
                        });
                    } catch (PunishmentException e) {
                        plugin.getLogger().error("An error occurred while cancelling punishment {}", uuid, e);
                        Util.sendErrorMessage(context, e);
                    }
                }
                case "info" ->
                        source.sendMessage(PunishmentHelper.buildPunishmentData(punishment, plugin.getMessageProvider()));
                case "change" -> source.sendMessage(Component.text("Soon (TM)"));
            }
        });
        return Command.SINGLE_SUCCESS;
    }
}
