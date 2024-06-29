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

package de.jvstvshd.necrify.velocity.internal;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.velocitypowered.api.command.CommandSource;
import de.jvstvshd.necrify.api.duration.PunishmentDuration;
import de.jvstvshd.necrify.api.message.MessageProvider;
import de.jvstvshd.necrify.api.punishment.Punishment;
import de.jvstvshd.necrify.api.punishment.TemporalPunishment;
import de.jvstvshd.necrify.api.user.NecrifyUser;
import de.jvstvshd.necrify.velocity.NecrifyPlugin;
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
     * @deprecated in favor of the new User API, use {@link #getUser(CommandContext, NecrifyPlugin)} instead.
     */
    @Deprecated(since = "1.2.0", forRemoval = true)
    @ApiStatus.ScheduledForRemoval(inVersion = "2.0.0")
    public static CompletableFuture<UUID> getPlayerUuid(CommandContext<CommandSource> context, NecrifyPlugin plugin) {
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

    public static CompletableFuture<Optional<NecrifyUser>> getUser(CommandContext<CommandSource> context, NecrifyPlugin plugin) {
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
