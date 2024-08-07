/*
 * This file is part of Necrify (formerly Velocity Punishment), which is licensed under the MIT license.
 *
 * Copyright (c) 2022-2024 JvstvsHD
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
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
