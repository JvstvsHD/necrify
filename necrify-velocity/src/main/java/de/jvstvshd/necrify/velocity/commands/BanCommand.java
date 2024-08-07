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
import de.jvstvshd.necrify.velocity.NecrifyVelocityPlugin;
import de.jvstvshd.necrify.velocity.internal.PunishmentHelper;
import de.jvstvshd.necrify.velocity.internal.Util;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

public class BanCommand {

    public static BrigadierCommand banCommand(NecrifyVelocityPlugin plugin) {
        var node = Util.permissibleCommand("ban", "necrify.command.ban")
                .then(Util.playerArgument(plugin.getServer()).executes(context -> execute(context, plugin))
                        .then(Util.reasonArgument.executes(context -> execute(context, plugin))));
        return new BrigadierCommand(node);
    }

    private static int execute(CommandContext<CommandSource> context, NecrifyVelocityPlugin plugin) {
        var source = context.getSource();
        var player = context.getArgument("player", String.class);
        var reason = PunishmentHelper.parseReason(context);
        var playerResolver = plugin.getPlayerResolver();
        var punishmentManager = plugin.getPunishmentManager();
        playerResolver.getOrQueryPlayerUuid(player, plugin.getService()).whenCompleteAsync((uuid, throwable) -> {
            if (Util.sendErrorMessageIfErrorOccurred(context, uuid, throwable, plugin)) return;
            try {
                punishmentManager.createPermanentBan(uuid, reason).punish().whenComplete((ban, t) -> {
                    if (t != null) {
                        plugin.getLogger().error("An error occurred while creating a ban for player {} ({})", player, uuid, t);
                        source.sendMessage(plugin.getMessageProvider().internalError());
                    } else {
                        String uuidString = uuid.toString().toLowerCase();
                        source.sendMessage(plugin.getMessageProvider().provide("command.ban.success", Util.copyComponent(player, plugin.getMessageProvider()).color(NamedTextColor.YELLOW).decorate(TextDecoration.BOLD),
                                Util.copyComponent(uuidString, plugin.getMessageProvider()).color(NamedTextColor.YELLOW).decorate(TextDecoration.BOLD),
                                reason).color(NamedTextColor.GREEN));
                        source.sendMessage(plugin.getMessageProvider().provide("commands.general.punishment.id", Util.copyComponent(ban.getPunishmentUuid().toString().toLowerCase(), plugin.getMessageProvider()).color(NamedTextColor.YELLOW)));
                    }
                });
            } catch (PunishmentException e) {
                plugin.getLogger().error("An error occurred while creating a ban for player {} ({})", player, uuid, e);
                Util.sendErrorMessage(context, e);
            }
        });
        return Command.SINGLE_SUCCESS;
    }
}
