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
package de.jvstvshd.necrify.velocity.internal;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.velocitypowered.api.command.CommandSource;
import de.jvstvshd.necrify.api.duration.PunishmentDuration;
import de.jvstvshd.necrify.api.message.MessageProvider;
import de.jvstvshd.necrify.api.punishment.Punishment;
import de.jvstvshd.necrify.api.punishment.TemporalPunishment;
import de.jvstvshd.necrify.api.user.NecrifyUser;
import de.jvstvshd.necrify.velocity.NecrifyVelocityPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.jetbrains.annotations.ApiStatus;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class PunishmentHelper {

    private PunishmentHelper() {
        throw new IllegalStateException("Utility class");
    }

    public static Component buildPunishmentData(Punishment punishment, MessageProvider provider) {
        return Component.text()
                .append(provider.provide("helper.type").color(NamedTextColor.AQUA),
                        Component.text(punishment.getType().getName()).color(NamedTextColor.YELLOW),
                        Component.newline(),
                        provider.provide("helper.reason").color(NamedTextColor.AQUA),
                        punishment.getReason(),
                        Component.newline(),
                        punishment instanceof TemporalPunishment temporalPunishment ?
                                buildPunishmentDataTemporal(temporalPunishment, provider) : Component.text(""),
                        Component.newline()
                )
                .build();
    }

    public static Component buildPunishmentDataTemporal(TemporalPunishment punishment, MessageProvider provider) {
        return punishment.isPermanent() ? Component.text("permanent").color(NamedTextColor.RED) : Component.text()
                .append(provider.provide("helper.temporal.duration").color(NamedTextColor.AQUA),
                        Component.text(punishment.getDuration().remainingDuration()).color(NamedTextColor.YELLOW),
                        Component.newline(),
                        provider.provide("helper.temporal.end").color(NamedTextColor.AQUA),
                        Component.text(punishment.getDuration().expirationAsString()).color(NamedTextColor.YELLOW))
                .build();
    }

    public static Optional<PunishmentDuration> parseDuration(CommandContext<CommandSource> context, MessageProvider provider) {
        if (!context.getArguments().containsKey("duration"))
            return Optional.empty();
        var duration = context.getArgument("duration", String.class);
        try {
            return Optional.ofNullable(PunishmentDuration.parse(duration));
        } catch (IllegalArgumentException e) {
            context.getSource().sendMessage(Component.text().append(Component.text("Cannot parse duration: ").color(NamedTextColor.RED),
                    Component.text(e.getMessage()).color(NamedTextColor.YELLOW)));
            return Optional.empty();
        } catch (Exception e) {
            context.getSource().sendMessage(provider.internalError());
            throw new RuntimeException(e);
        }
    }

    /**
     * @deprecated in favor of the new User API, use {@link #getUser(CommandContext, NecrifyVelocityPlugin)} instead.
     */
    @Deprecated(since = "1.2.0", forRemoval = true)
    @ApiStatus.ScheduledForRemoval(inVersion = "1.3.0")
    public static CompletableFuture<UUID> getPlayerUuid(CommandContext<CommandSource> context, NecrifyVelocityPlugin plugin) {
        var argument = context.getArgument("player", String.class);
        if (argument.length() <= 16) {
            return plugin.getPlayerResolver().getOrQueryPlayerUuid(argument, plugin.getService());
        } else if (argument.length() <= 36) {
            try {
                return CompletableFuture.completedFuture(Util.parseUuid(argument));
            } catch (IllegalArgumentException e) {
                return CompletableFuture.completedFuture(null);
            }
        } else {
            return CompletableFuture.completedFuture(null);
        }
    }

    public static CompletableFuture<Optional<NecrifyUser>> getUser(CommandContext<CommandSource> context, NecrifyVelocityPlugin plugin) {
        var argument = context.getArgument("player", String.class);
        if (argument.length() <= 16) {
            return plugin.getUserManager().loadUser(argument);
        } else if (argument.length() <= 36) {
            try {
                return plugin.getUserManager().loadUser(Util.parseUuid(argument));
            } catch (IllegalArgumentException e) {
                return CompletableFuture.completedFuture(null);
            }
        } else {
            return CompletableFuture.completedFuture(null);
        }
    }

    public static Component parseReason(CommandContext<CommandSource> context, TextComponent def) {
        if (!context.getArguments().containsKey("reason")) {
            return def;
        }
        return MiniMessage.miniMessage().deserialize(StringArgumentType.getString(context, "reason"));
    }

    public static Component parseReason(CommandContext<CommandSource> context) {
        return parseReason(context, Component.text("No reason specified", NamedTextColor.RED));
    }
}
