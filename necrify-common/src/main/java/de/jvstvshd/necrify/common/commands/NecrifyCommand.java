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

package de.jvstvshd.necrify.common.commands;

import de.jvstvshd.necrify.api.duration.PunishmentDuration;
import de.jvstvshd.necrify.api.punishment.Punishment;
import de.jvstvshd.necrify.api.punishment.StandardPunishmentType;
import de.jvstvshd.necrify.api.user.NecrifyUser;
import de.jvstvshd.necrify.api.user.UserManager;
import de.jvstvshd.necrify.common.AbstractNecrifyPlugin;
import de.jvstvshd.necrify.common.util.PunishmentHelper;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.event.HoverEventSource;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.incendo.cloud.annotation.specifier.Greedy;
import org.incendo.cloud.annotations.Argument;
import org.incendo.cloud.annotations.Command;
import org.incendo.cloud.annotations.Permission;
import org.incendo.cloud.annotations.suggestion.Suggestions;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.context.CommandInput;
import org.incendo.cloud.minecraft.extras.suggestion.ComponentTooltipSuggestion;
import org.incendo.cloud.suggestion.Suggestion;
import org.slf4j.Logger;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

public class NecrifyCommand {

    private final AbstractNecrifyPlugin plugin;
    private final MiniMessage miniMessage;
    private final UserManager userManager;
    private final Logger logger;
    private final ExecutorService executor;

    public NecrifyCommand(AbstractNecrifyPlugin plugin) {
        this.plugin = plugin;
        this.userManager = plugin.getUserManager();
        this.miniMessage = MiniMessage.miniMessage();
        this.logger = plugin.getLogger();
        this.executor = plugin.getExecutor();
    }

    //COMMANDS
    //Punishing

    @Command("necrify ban <target> [reason]")
    @Command("ban <target> [reason]")
    @Permission(value = {"necrify.command.ban", "necrify.admin"}, mode = Permission.Mode.ANY_OF)
    public void banCommand(
            NecrifyUser sender,
            @Argument(value = "target", description = "Player to ban", suggestions = "suggestOnlinePlayers") NecrifyUser target,
            @Argument(value = "reason", description = "Reason the user should be banned for", suggestions = "suggestMiniMessage") @Greedy String reason
    ) {
        var finalReason = reasonOrDefaultTo(reason, StandardPunishmentType.PERMANENT_BAN);
        target.banPermanent(finalReason).whenComplete((ban, throwable) -> {
            if (throwable != null) {
                logException(sender, throwable);
                return;
            }
            sender.sendMessage("command.ban.success",
                    miniMessage(target.getUsername()).color(NamedTextColor.YELLOW),
                    copyComponent(target.getUuid().toString()).color(NamedTextColor.YELLOW),
                    finalReason);
        });
    }

    @Command("necrify mute <target> [reason]")
    @Command("mute <target> [reason]")
    @Permission(value = {"necrify.command.mute", "necrify.admin"}, mode = Permission.Mode.ANY_OF)
    public void muteCommand(
            NecrifyUser sender,
            @Argument(value = "target", description = "Player to mute", suggestions = "suggestOnlinePlayers") NecrifyUser target,
            @Argument(value = "reason", description = "Reason the user should be muted for", suggestions = "suggestMiniMessage") @Greedy String reason
    ) {
        var finalReason = reasonOrDefaultTo(reason, StandardPunishmentType.PERMANENT_MUTE);
        target.mutePermanent(finalReason).whenComplete((mute, throwable) -> {
            if (throwable != null) {
                logException(sender, throwable);
                return;
            }
            sender.sendMessage("command.mute.success",
                    miniMessage(target.getUsername()).color(NamedTextColor.YELLOW),
                    copyComponent(target.getUuid().toString()).color(NamedTextColor.YELLOW),
                    finalReason);
        });
    }

    @Command("necrify kick <target> [reason]")
    @Command("kick <target> [reason]")
    @Permission(value = {"necrify.command.kick", "necrify.admin"}, mode = Permission.Mode.ANY_OF)
    public void kickCommand(
            NecrifyUser sender,
            @Argument(value = "target", description = "Player to kick", suggestions = "suggestOnlinePlayers") NecrifyUser target,
            @Argument(value = "reason", description = "Reason the user should be kicked for", suggestions = "suggestMiniMessage") @Greedy String reason
    ) {
        var finalReason = reasonOrDefaultTo(reason, StandardPunishmentType.KICK);
        target.kick(finalReason).whenComplete((unused, throwable) -> {
            if (throwable != null) {
                logException(sender, throwable);
                return;
            }
            sender.sendMessage("command.kick.success",
                    miniMessage(target.getUsername()).color(NamedTextColor.YELLOW),
                    copyComponent(target.getUuid().toString()).color(NamedTextColor.YELLOW),
                    finalReason);
        });
    }

    @Command("necrify tempban <target> <duration> [reason]")
    @Command("tempban <target> <duration> [reason]")
    @Permission(value = {"necrify.command.tempban", "necrify.admin"}, mode = Permission.Mode.ANY_OF)
    public void tempbanCommand(
            NecrifyUser sender,
            @Argument(value = "target", description = "Player to tempban", suggestions = "suggestOnlinePlayers") NecrifyUser target,
            @Argument(value = "duration", description = "Duration the user should be banned for") PunishmentDuration duration,
            @Argument(value = "reason", description = "Reason the user should be banned for", suggestions = "suggestMiniMessage") @Greedy String reason
    ) {
        var finalReason = reasonOrDefaultTo(reason, StandardPunishmentType.TEMPORARY_BAN);
        target.ban(finalReason, duration).whenComplete((ban, throwable) -> {
            if (throwable != null) {
                logException(sender, throwable);
                return;
            }
            sender.sendMessage("command.tempban.success",
                    miniMessage(target.getUsername()).color(NamedTextColor.YELLOW),
                    copyComponent(target.getUuid().toString()).color(NamedTextColor.YELLOW),
                    finalReason,
                    miniMessage(duration.expirationAsString()).color(NamedTextColor.YELLOW));
        });
    }

    @Command("necrify tempmute <target> <duration> [reason]")
    @Command("tempmute <target> <duration> [reason]")
    @Permission(value = {"necrify.command.tempmute", "necrify.admin"}, mode = Permission.Mode.ANY_OF)
    public void tempmuteCommand(
            NecrifyUser sender,
            @Argument(value = "target", description = "Player to tempmute", suggestions = "suggestOnlinePlayers") NecrifyUser target,
            @Argument(value = "duration", description = "Duration the user should be muted for") PunishmentDuration duration,
            @Argument(value = "reason", description = "Reason the user should be muted for", suggestions = "suggestMiniMessage") @Greedy String reason
    ) {
        var finalReason = reasonOrDefaultTo(reason, StandardPunishmentType.TEMPORARY_MUTE);
        target.mute(finalReason, duration).whenComplete((mute, throwable) -> {
            if (throwable != null) {
                logException(sender, throwable);
                return;
            }
            sender.sendMessage("command.tempmute.success",
                    miniMessage(target.getUsername()).color(NamedTextColor.YELLOW),
                    copyComponent(target.getUuid().toString()).color(NamedTextColor.YELLOW),
                    finalReason,
                    miniMessage(duration.expirationAsString()).color(NamedTextColor.YELLOW));
        });
    }

    //Removal of punishments

    @Command("necrify unban <target>")
    @Command("unban <target>")
    @Permission(value = {"necrify.command.unban", "necrify.admin"}, mode = Permission.Mode.ANY_OF)
    public void unbanCommand(
            NecrifyUser sender,
            @Argument(value = "target", description = "Player to unban") NecrifyUser target
    ) {
        var punishments = target.getPunishments(StandardPunishmentType.TEMPORARY_BAN, StandardPunishmentType.PERMANENT_BAN);
        try {
            removePunishments(sender, "unban", punishments);
        } catch (Exception e) {
            logException(e);
        }
    }

    @Command("necrify unmute <target>")
    @Command("unmute <target>")
    @Permission(value = {"necrify.command.unmute", "necrify.admin"}, mode = Permission.Mode.ANY_OF)
    public void unmuteCommand(
            NecrifyUser sender,
            @Argument(value = "target", description = "Player to unmute", suggestions = "suggestOnlinePlayers") NecrifyUser target
    ) {
        var punishments = target.getPunishments(StandardPunishmentType.TEMPORARY_MUTE, StandardPunishmentType.PERMANENT_MUTE);
        removePunishments(sender, "unmute", punishments);
    }

    //SUGGESTIONS

    @Suggestions("suggestOnlinePlayers")
    public List<? extends Suggestion> suggestNames(CommandContext<NecrifyUser> context, CommandInput input) {
        return plugin
                .getOnlinePlayers()
                .stream()
                .filter(pair -> pair.first().toLowerCase(Locale.ROOT).startsWith(input.peekString().toLowerCase(Locale.ROOT)))
                .map(pair -> ComponentTooltipSuggestion.suggestion(pair.first(),
                        miniMessage("<red>The player <yellow>(<name>/<uuid>)</yellow> you want to select.</red><yellow>",
                                Placeholder.parsed("name", pair.first()),
                                Placeholder.parsed("uuid", pair.second().toString()))
                )).toList();
    }

    @Suggestions("suggestMiniMessage")
    public List<? extends Suggestion> suggestMiniMessage(CommandContext<NecrifyUser> context, CommandInput input) {
        return Collections.singletonList(ComponentTooltipSuggestion.suggestion(input.remainingInput() + " (hover for preview)",
                miniMessage(input.remainingInput())));
    }

    //HELPER METHODS

    private void removePunishments(NecrifyUser source, String commandName, List<Punishment> punishments) {
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
                        logException(source, th);
                        plugin.getLogger().error("An error occurred while removing punishment {} for player {}", punishment.getPunishmentUuid(), punishment.getUser().getUsername(), th);
                        source.sendMessage(plugin.getMessageProvider().internalError());
                        return;
                    }
                    source.sendMessage(plugin.getMessageProvider().provide("command." + commandName + ".success").color(NamedTextColor.GREEN));
                }, plugin.getService());
            } catch (Exception e) {
                logException(source, e);
            }
        }
    }

    //Communication, messaging, logging

    private Component buildComponent(Component dataComponent, Punishment punishment) {
        return dataComponent.clickEvent(ClickEvent.runCommand("/punishment " + punishment.getPunishmentUuid()
                        .toString().toLowerCase(Locale.ROOT) + " remove"))
                .hoverEvent((HoverEventSource<Component>) op -> HoverEvent.showText(Component
                        .text("Click to remove punishment").color(NamedTextColor.GREEN)));
    }

    private Component reasonOrDefaultTo(String reason, StandardPunishmentType type) {
        return miniMessage(reason == null ? plugin.getDefaultReason(type) : reason);
    }

    private Component miniMessage(String message, TagResolver... resolvers) {
        return miniMessage.deserialize(message, resolvers);
    }

    public TextComponent copyComponent(String text) {
        return Component.text(text).clickEvent(ClickEvent.suggestCommand(text))
                .hoverEvent((HoverEventSource<Component>) op -> HoverEvent.showText(plugin
                        .getMessageProvider()
                        .provide("commands.general.copy")
                        .color(NamedTextColor.GREEN)));
    }



    private void logException(Throwable throwable) {
        logger.error("An error occurred while executing a command", throwable);
    }

    private void logException(NecrifyUser sender, Throwable throwable) {
        logger.error("An error occurred while executing a command for player {} ({})", sender.getUsername(), sender.getUuid(), throwable);
        sender.sendErrorMessage();
    }
}
