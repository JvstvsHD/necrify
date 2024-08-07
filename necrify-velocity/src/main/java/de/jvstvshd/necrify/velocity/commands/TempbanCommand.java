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
