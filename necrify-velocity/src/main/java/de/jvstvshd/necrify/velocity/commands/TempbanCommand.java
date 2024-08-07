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
import de.jvstvshd.necrify.api.duration.PunishmentDuration;
import de.jvstvshd.necrify.velocity.NecrifyVelocityPlugin;
import de.jvstvshd.necrify.velocity.internal.PunishmentHelper;
import de.jvstvshd.necrify.velocity.internal.Util;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

import java.time.format.DateTimeFormatter;
import java.util.Optional;

public class TempbanCommand {


    public static BrigadierCommand tempbanCommand(NecrifyVelocityPlugin plugin) {
        var node = Util.permissibleCommand("tempban", "necrify.command.tempban")
                .then(Util.playerArgument(plugin.getServer())
                        .then(Util.durationArgument.executes(context -> execute(context, plugin))
                                .then(Util.reasonArgument.executes(context -> execute(context, plugin)))));
        return new BrigadierCommand(node);
    }

    private static int execute(CommandContext<CommandSource> context, NecrifyVelocityPlugin plugin) {
        CommandSource source = context.getSource();
        var player = context.getArgument("player", String.class);
        plugin.getPlayerResolver().getOrQueryPlayerUuid(player, plugin.getService()).whenCompleteAsync((uuid, throwable) -> {
            if (Util.sendErrorMessageIfErrorOccurred(context, uuid, throwable, plugin)) return;
            Optional<PunishmentDuration> optDuration = PunishmentHelper.parseDuration(context, plugin.getMessageProvider());
            if (optDuration.isEmpty()) {
                return;
            }
            PunishmentDuration duration = optDuration.get();
            Component reason = PunishmentHelper.parseReason(context);
            try {
                plugin.getPunishmentManager().createBan(uuid, reason, duration).punish().whenComplete((ban, t) -> {
                    if (t != null) {
                        source.sendMessage(plugin.getMessageProvider().internalError());
                        plugin.getLogger().error("An error occurred while punishing {}/{}.", uuid.toString(), plugin, t);
                        return;
                    }
                    String until = duration.expiration().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss"));
                    String uuidString = uuid.toString().toLowerCase();
                    source.sendMessage(plugin.getMessageProvider().provide("command.tempban.success",
                            Util.copyComponent(player, plugin.getMessageProvider()).color(NamedTextColor.YELLOW).decorate(TextDecoration.BOLD),
                            Util.copyComponent(uuidString, plugin.getMessageProvider()).color(NamedTextColor.RED).decorate(TextDecoration.BOLD),
                            reason,
                            Component.text(until).color(NamedTextColor.GREEN)).color(NamedTextColor.GREEN));
                    source.sendMessage(plugin.getMessageProvider().provide("commands.general.punishment.id", Component.text(ban.getPunishmentUuid().toString().toLowerCase()).color(NamedTextColor.YELLOW)));
                });
            } catch (Exception e) {
                plugin.getLogger().error("An error occurred while punishing {}/{}.", uuid.toString(), plugin, e);
                Util.sendErrorMessage(context, e);
            }
        }, plugin.getService());
        return Command.SINGLE_SUCCESS;
    }
}
