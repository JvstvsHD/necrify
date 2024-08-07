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
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;

/**
 * @see NecrifyVelocityPlugin#MUTES_DISABLED
 */
public class MuteCommand {

    public static BrigadierCommand muteCommand(NecrifyVelocityPlugin plugin) {
        var node = Util.permissibleCommand("mute", "necrify.command.mute")
                .then(Util.playerArgument(plugin.getServer()).executes(context -> execute(context, plugin))
                        .then(Util.reasonArgument.executes(context -> execute(context, plugin))));
        return new BrigadierCommand(node);
    }

    private static int execute(CommandContext<CommandSource> context, NecrifyVelocityPlugin plugin) {
        CommandSource source = context.getSource();
        source.sendMessage(NecrifyVelocityPlugin.MUTES_DISABLED);
        if (!plugin.communicator().isSupportedEverywhere()) {
            source.sendMessage(plugin.getMessageProvider()
                    .prefixed(MiniMessage.miniMessage().deserialize("<red>It seems that not all servers run the mentioned paper extension. " +
                            "Thus, the persecution of this mute cannot be granted in all cases.")));
        }
        var player = context.getArgument("player", String.class);
        var playerResolver = plugin.getPlayerResolver();
        var punishmentManager = plugin.getPunishmentManager();
        playerResolver.getOrQueryPlayerUuid(player, plugin.getService()).whenCompleteAsync((uuid, throwable) -> {
            if (Util.sendErrorMessageIfErrorOccurred(context, uuid, throwable, plugin)) return;
            Component reason = PunishmentHelper.parseReason(context);
            try {
                punishmentManager.createPermanentMute(uuid, reason).punish().whenComplete((mute, t) -> {
                    if (t != null) {
                        plugin.getLogger().error("An error occurred while creating a mute for player {} ({})", player, uuid, t);
                        source.sendMessage(plugin.getMessageProvider().internalError());
                    } else {
                        String uuidString = uuid.toString().toLowerCase();
                        source.sendMessage(plugin.getMessageProvider().provide("command.mute.success", Util.copyComponent(player, plugin.getMessageProvider()).color(NamedTextColor.YELLOW).decorate(TextDecoration.BOLD),
                                Util.copyComponent(uuidString, plugin.getMessageProvider()).color(NamedTextColor.YELLOW).decorate(TextDecoration.BOLD),
                                reason).color(NamedTextColor.GREEN));
                        source.sendMessage(plugin.getMessageProvider().provide("commands.general.punishment.id", Util.copyComponent(mute.getPunishmentUuid().toString().toLowerCase(), plugin.getMessageProvider()).color(NamedTextColor.YELLOW)));
                    }
                });
            } catch (PunishmentException e) {
                plugin.getLogger().error("An error occurred while creating a mute for player {} ({})", player, uuid, e);
                Util.sendErrorMessage(context, e);
            }
        }, plugin.getService());
        return Command.SINGLE_SUCCESS;
    }
}
