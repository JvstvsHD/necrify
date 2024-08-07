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
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandSource;
import de.jvstvshd.necrify.velocity.NecrifyVelocityPlugin;
import de.jvstvshd.necrify.velocity.internal.Util;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class WhitelistCommand {

    public static final List<String> options = ImmutableList.of("add", "remove", "on", "off");


    public static BrigadierCommand whitelistCommand(NecrifyVelocityPlugin plugin) {
        var node = Util.permissibleCommand("whitelist", "necrify.command.whitelist")
                .then(RequiredArgumentBuilder.<CommandSource, String>argument("option", StringArgumentType.word())
                        .executes(context -> execute(context, plugin)).suggests((context, builder) -> {
                            for (String option : options) {
                                if (context.getArguments().containsKey("option") && !context.getArgument("option", String.class).isEmpty() &&
                                        !option.startsWith(context.getArgument("option", String.class).toLowerCase(Locale.ROOT)))
                                    continue;
                                builder.suggest(option);
                            }
                            return builder.buildFuture();
                        }).then(Util.playerArgument(plugin.getServer()).executes(context -> execute(context, plugin))));
        return new BrigadierCommand(node);
    }

    private static int execute(CommandContext<CommandSource> context, NecrifyVelocityPlugin plugin) {
        var source = context.getSource();
        String player;
        if (context.getArguments().containsKey("player")) {
            player = context.getArgument("player", String.class);
        } else {
            player = null;
        }
        if (context.getArguments().containsKey("option")) {
            var option = context.getArgument("option", String.class).toLowerCase();
            switch (option) {
                case "add", "remove" -> {
                    if (player == null) {
                        source.sendMessage(plugin.getMessageProvider().provide("command.whitelist.usage"));
                        return Command.SINGLE_SUCCESS;
                    }
                    plugin.getUserManager().loadOrCreateUser(player).whenCompleteAsync((user, throwable) -> {
                        if (Util.sendErrorMessageIfErrorOccurred(context, throwable, plugin)) return;
                        user.ifPresentOrElse(necrifyUser -> {
                            necrifyUser.setWhitelisted(option.equals("add"));
                            source.sendMessage(plugin.getMessageProvider().provide("command.whitelist.success"));
                        }, () -> source.sendMessage(plugin.getMessageProvider().provide("commands.general.not-found",
                                Component.text(player).color(NamedTextColor.YELLOW))));

                    }, plugin.getService());
                }
                case "on", "off" -> {
                    var config = plugin.getConfig();
                    config.getConfiguration().setWhitelistActivated(option.equals("on"));
                    try {
                        config.save();
                        var state = option.equals("on") ? "enabled" : "disabled";
                        source.sendMessage(plugin.getMessageProvider().provide("command.whitelist." + state));
                    } catch (IOException e) {
                        source.sendMessage(plugin.getMessageProvider().internalError());
                        plugin.getLogger().error("Could not save the configuration.", e);
                    }
                    if (option.equals("on")) {
                        plugin.getServer().getAllPlayers().stream().map(p -> plugin.getUserManager().loadUser(p.getUniqueId())).forEach(cf -> {
                            cf.whenComplete((optionalUser, throwable) -> optionalUser.ifPresent(user -> {
                                if (!user.isWhitelisted())
                                    user.kick(plugin.getMessageProvider().provide("whitelist.removed"));
                            }));
                        });
                    }
                }
                default -> source.sendMessage(plugin.getMessageProvider().provide("command.whitelist.usage"));
            }
            return Command.SINGLE_SUCCESS;
        }
        if (player == null) {
            source.sendMessage(plugin.getMessageProvider().provide("command.whitelist.usage"));
            return Command.SINGLE_SUCCESS;
        }
        plugin.getUserManager().loadOrCreateUser(player).whenCompleteAsync((user, throwable) -> {
            if (Util.sendErrorMessageIfErrorOccurred(context, throwable, plugin)) return;
            user.ifPresentOrElse(necrifyUser -> {
                var whitelisted = necrifyUser.isWhitelisted() ? plugin.getMessageProvider().provide("whitelist.status.whitelisted") : plugin.getMessageProvider().provide("whitelist.status.disallowed");
                source.sendMessage(plugin.getMessageProvider().provide("command.whitelist.status",
                        Component.text(player).color(NamedTextColor.YELLOW), whitelisted.color(NamedTextColor.YELLOW)));
            }, () -> source.sendMessage(plugin.getMessageProvider().provide("commands.general.not-found", Component.text(player).color(NamedTextColor.YELLOW))));
        }, plugin.getService());
        return Command.SINGLE_SUCCESS;
    }
}
