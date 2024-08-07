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
import com.velocitypowered.api.proxy.Player;
import de.jvstvshd.necrify.velocity.NecrifyVelocityPlugin;
import de.jvstvshd.necrify.velocity.internal.PunishmentHelper;
import de.jvstvshd.necrify.velocity.internal.Util;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.Optional;

public class KickCommand {

    public static BrigadierCommand kickCommand(NecrifyVelocityPlugin plugin) {
        var node = Util.permissibleCommand("kick", "necrify.command.kick")
                .then(Util.playerArgument(plugin.getServer()).executes(context -> execute(context, plugin))
                        .then(Util.reasonArgument.executes(context -> execute(context, plugin))));
        return new BrigadierCommand(node);
    }

    private static int execute(CommandContext<CommandSource> context, NecrifyVelocityPlugin plugin) {
        var source = context.getSource();
        var playerArgument = context.getArgument("player", String.class);
        Optional<Player> playerOptional = plugin.getServer().getPlayer(playerArgument);
        if (playerOptional.isEmpty()) {
            try {
                playerOptional = plugin.getServer().getPlayer(Util.parseUuid(playerArgument));
            } catch (Exception e) {
                source.sendMessage(plugin.getMessageProvider().internalError());
                return 0;
            }
        }
        if (playerOptional.isEmpty()) {
            source.sendMessage(plugin.getMessageProvider().provide("commands.general.not-found", Component.text(playerArgument).color(NamedTextColor.YELLOW)));
            return 0;
        }
        var player = playerOptional.get();
        Component reason = PunishmentHelper.parseReason(context);
        player.disconnect(reason);
        source.sendMessage(plugin.getMessageProvider().provide("command.kick.success", Component.text(player.getUsername()).color(NamedTextColor.YELLOW), reason));
        return Command.SINGLE_SUCCESS;
    }
}
