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

import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandSource;
import de.jvstvshd.necrify.api.PunishmentException;
import de.jvstvshd.necrify.api.event.punishment.PunishmentCancelledEvent;
import de.jvstvshd.necrify.api.punishment.Punishment;
import de.jvstvshd.necrify.api.punishment.PunishmentType;
import de.jvstvshd.necrify.api.punishment.StandardPunishmentType;
import de.jvstvshd.necrify.velocity.NecrifyVelocityPlugin;
import de.jvstvshd.necrify.velocity.internal.PunishmentHelper;
import de.jvstvshd.necrify.velocity.internal.Util;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.event.HoverEventSource;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.Locale;

public class PunishmentRemovalCommand {

    public static BrigadierCommand unmuteCommand(NecrifyVelocityPlugin plugin) {
        var node = Util.permissibleCommand("unmute", "necrify.command.unmute")
                .then(Util.punishmentRemoveArgument(plugin).executes(context -> execute(context, plugin, "unmute", StandardPunishmentType.TEMPORARY_MUTE, StandardPunishmentType.PERMANENT_MUTE)));
        return new BrigadierCommand(node);
    }

    public static BrigadierCommand unbanCommand(NecrifyVelocityPlugin plugin) {
        var node = Util.permissibleCommand("unban", "necrify.command.unban")
                .then(Util.punishmentRemoveArgument(plugin).executes(context -> execute(context, plugin, "unban", StandardPunishmentType.TEMPORARY_BAN, StandardPunishmentType.PERMANENT_BAN)));
        return new BrigadierCommand(node);
    }

    public static int execute(CommandContext<CommandSource> context, NecrifyVelocityPlugin plugin, String commandName, PunishmentType... types) {
        var source = context.getSource();
        PunishmentHelper.getPlayerUuid(context, plugin).whenCompleteAsync((uuid, throwable) -> {
            if (Util.sendErrorMessageIfErrorOccurred(context, uuid, throwable, plugin)) return;
            plugin.getPunishmentManager().getPunishments(uuid, plugin.getService(), types).whenComplete((punishments, t) -> {
                if (t != null) {
                    plugin.getLogger().error("An error occurred while getting punishments for player {}", uuid, t);
                    source.sendMessage(plugin.getMessageProvider().internalError());
                    return;
                }
                if (punishments.isEmpty()) {
                    source.sendMessage(plugin.getMessageProvider().provide("command.punishment.not-banned").color(NamedTextColor.RED));
                    return;
                }
                if (punishments.size() > 1) {
                    source.sendMessage(plugin.getMessageProvider().provide("command." + commandName + ".multiple-bans").color(NamedTextColor.YELLOW));
                    for (Punishment punishment : punishments) {
                        source.sendMessage(buildComponent(PunishmentHelper.buildPunishmentData(punishment, plugin.getMessageProvider()), punishment));
                    }
                } else {
                    Punishment punishment = punishments.getFirst();
                    try {
                        punishment.cancel().whenCompleteAsync((unused, th) -> {
                            if (th != null) {
                                plugin.getLogger().error("An error occurred while removing punishment {} for player {}", punishment.getPunishmentUuid(), uuid, th);
                                source.sendMessage(plugin.getMessageProvider().internalError());
                                return;
                            }
                            plugin.getEventDispatcher().dispatch(new PunishmentCancelledEvent(punishment));
                            //plugin.getUserManager().getUser(uuid).ifPresentOrElse(user -> user.removePunishment(punishment), () -> plugin.getLogger().warn("User {} not found in cache", uuid));
                            source.sendMessage(plugin.getMessageProvider().provide("command." + commandName + ".success").color(NamedTextColor.GREEN));
                        }, plugin.getService());
                    } catch (PunishmentException e) {
                        plugin.getLogger().error("An error occurred while removing punishment {} for player {}", punishment.getPunishmentUuid(), uuid, e);
                        Util.sendErrorMessage(context, e);
                    }
                }
            });
        }, plugin.getService());
        return Command.SINGLE_SUCCESS;
    }

    private static Component buildComponent(Component dataComponent, Punishment punishment) {
        return dataComponent.clickEvent(ClickEvent.runCommand("/punishment " + punishment.getPunishmentUuid()
                        .toString().toLowerCase(Locale.ROOT) + " remove"))
                .hoverEvent((HoverEventSource<Component>) op -> HoverEvent.showText(Component
                        .text("Click to remove punishment").color(NamedTextColor.GREEN)));
    }
}
