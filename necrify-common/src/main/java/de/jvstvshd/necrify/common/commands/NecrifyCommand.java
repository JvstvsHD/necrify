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
import de.jvstvshd.necrify.api.message.MessageProvider;
import de.jvstvshd.necrify.api.punishment.Punishment;
import de.jvstvshd.necrify.api.punishment.StandardPunishmentType;
import de.jvstvshd.necrify.api.user.NecrifyUser;
import de.jvstvshd.necrify.api.user.UserDeletionReason;
import de.jvstvshd.necrify.api.user.UserManager;
import de.jvstvshd.necrify.common.AbstractNecrifyPlugin;
import de.jvstvshd.necrify.common.util.PunishmentHelper;
import de.jvstvshd.necrify.common.util.Util;
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
import org.incendo.cloud.annotations.Default;
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
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

public class NecrifyCommand {

    private final AbstractNecrifyPlugin plugin;
    private final MiniMessage miniMessage;
    private final UserManager userManager;
    private final Logger logger;
    private final ExecutorService executor;
    private final MessageProvider provider;

    private static final List<String> PUNISHMENT_COMMAND_OPTIONS = List.of("cancel", "remove", "info", "change");
    private static final List<String> USER_COMMAND_OPTIONS = List.of("info", "delete");

    public NecrifyCommand(AbstractNecrifyPlugin plugin) {
        this.plugin = plugin;
        this.userManager = plugin.getUserManager();
        this.miniMessage = MiniMessage.miniMessage();
        this.logger = plugin.getLogger();
        this.executor = plugin.getExecutor();
        this.provider = plugin.getMessageProvider();
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
            removePunishments(sender, punishments, "unban", "ban", "banned");
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
        removePunishments(sender, punishments, "unmute", "mute", "muted");
    }

    //Informational/other

    @Command("necrify punishment <punishmentId> [option]")
    @Permission(value = {"necrify.command.punishment", "necrify.admin"}, mode = Permission.Mode.ANY_OF)
    public void punishmentCommand(
            NecrifyUser sender,
            @Argument(value = "punishmentId", description = "Punishment to manage") Punishment punishmentParsed,
            @Argument(value = "option", description = "Option to manage the punishment", suggestions = "suggestPunishmentCommandOptions") @Default(value = "info") String option
    ) {
        switch (option) {
            case "info" ->
                    sender.sendMessage(buildComponent(PunishmentHelper.buildPunishmentData(punishmentParsed, plugin.getMessageProvider()), punishmentParsed));
            case "cancel", "remove" -> {
                punishmentParsed.cancel().whenCompleteAsync((unused, th) -> {
                    if (th != null) {
                        logException(sender, th);
                        return;
                    }
                    sender.sendMessage(provider.provide("command.punishment.cancel.success").color(NamedTextColor.GREEN));
                }, plugin.getService());
            }
            case "change" -> {
                sender.sendMessage(miniMessage("Soon (TM)").color(NamedTextColor.LIGHT_PURPLE));
            }
        }
    }

    @Command("necrify user <target> [option]")
    @Permission(value = {"necrify.command.user", "necrify.admin"}, mode = Permission.Mode.ANY_OF)
    public void userCommand(
            NecrifyUser sender,
            @Argument(value = "target", description = "Player to manage", suggestions = "suggestOnlinePlayers") NecrifyUser target,
            @Argument(value = "option", description = "Option to manage the player", suggestions = "suggestUserCommandOptions") @Default("info") String option
    ) {
        switch (option) {
            case "info" -> {
                var punishments = target.getPunishments();
                sender.sendMessage(provider.provide("command.user.overview",
                        Util.copyComponent(Objects.requireNonNullElse(target.getUsername(), "null"), provider).color(NamedTextColor.YELLOW),
                        Util.copyComponent(target.getUuid().toString(), provider).color(NamedTextColor.YELLOW),
                        Component.text(punishments.size())));
                for (Punishment punishment : punishments) {
                    Component component = buildComponent(PunishmentHelper.buildPunishmentData(punishment, plugin.getMessageProvider()), punishment);
                    sender.sendMessage(component);
                }
            }
            case "delete" -> {
                //TODO add confirmation
                target.delete(UserDeletionReason.USER_DELETED);
                sender.sendMessage("command.user.delete.success",
                        miniMessage(target.getUsername()).color(NamedTextColor.YELLOW),
                        copyComponent(target.getUuid().toString()).color(NamedTextColor.YELLOW));
            }
        }
    }

    //SUGGESTIONS

    @Suggestions("suggestOnlinePlayers")
    public List<? extends Suggestion> suggestNames(CommandContext<NecrifyUser> context, CommandInput input) {
        return plugin
                .getOnlinePlayers()
                .stream()
                .filter(pair -> pair.first().toLowerCase(Locale.ROOT).startsWith(input.peekString().toLowerCase(Locale.ROOT)))
                .map(pair -> {
                    var select = provider.unprefixedProvider().provide("suggestion.select-player", context.sender().getLocale(), miniMessage(
                            "<yellow>(<name>/<uuid>)</yellow>",
                            Placeholder.parsed("name", pair.first()),
                            Placeholder.parsed("uuid", pair.second().toString()))).color(NamedTextColor.RED);
                    return ComponentTooltipSuggestion.suggestion(pair.first(), select);
                }).toList();
    }

    @Suggestions("suggestMiniMessage")
    public List<? extends Suggestion> suggestMiniMessage(CommandContext<NecrifyUser> context, CommandInput input) {
        return Collections.singletonList(ComponentTooltipSuggestion.suggestion(input.remainingInput()
                        + " (" + provider.unprefixedProvider() .provideString("suggestion.hover-over-me", context.sender().getLocale()) + ")",
                miniMessage(input.remainingInput())));
    }

    @Suggestions("suggestPunishmentCommandOptions")
    public List<? extends Suggestion> suggestPunishmentCommandOptions(CommandContext<NecrifyUser> context, CommandInput input) {
        return PUNISHMENT_COMMAND_OPTIONS
                .stream()
                .filter(option -> option.toLowerCase().startsWith(input.peekString().toLowerCase()))
                .map(option -> ComponentTooltipSuggestion.suggestion(option, miniMessage(option)))
                .toList();
    }

    @Suggestions("suggestUserCommandOptions")
    public List<? extends Suggestion> suggestUserCommandOptions(CommandContext<NecrifyUser> context, CommandInput input) {
        return USER_COMMAND_OPTIONS
                .stream()
                .filter(option -> option.toLowerCase().startsWith(input.peekString().toLowerCase()))
                .map(option -> ComponentTooltipSuggestion.suggestion(option, miniMessage(option)))
                .toList();
    }

    //HELPER METHODS

    /**
     * This will execute the punishment removal process for the given punishments. If more than one punishment is found,
     * the user will be informed about it and can then decide which punishment to remove.
     *
     * @param source      the user who executed the command.
     * @param punishments the list of punishments the target has.
     * @param values      some strings for the message provider. Format: [0] = command name, [1] = punishment type, [2] = past
     *                    of the verb form to indicate the process of punishing the target; for example: "unban", "ban", "banned"
     */
    private void removePunishments(NecrifyUser source, List<Punishment> punishments, String... values) {
        if (punishments.isEmpty()) {
            source.sendMessage(plugin.getMessageProvider().provide("command.punishment.not-" + values[2]).color(NamedTextColor.RED));
            return;
        }
        if (punishments.size() > 1) {
            source.sendMessage(plugin.getMessageProvider().provide("command." + values[0] + ".multiple-" + values[1] + "s").color(NamedTextColor.YELLOW));
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
                    source.sendMessage(plugin.getMessageProvider().provide("command." + values[0] + ".success").color(NamedTextColor.GREEN));
                }, plugin.getService());
            } catch (Exception e) {
                logException(source, e);
            }
        }
    }

    //Communication, messaging, logging

    private Component buildComponent(Component dataComponent, Punishment punishment) {
        var clickToRemove = provider.provide("command.punishment.click-to-remove");
        return dataComponent.append(
                clickToRemove
                        .color(NamedTextColor.RED)
                        .clickEvent(ClickEvent.runCommand("/necrify punishment " + punishment.getPunishmentUuid().toString().toLowerCase(Locale.ROOT) + " remove"))
                        .hoverEvent((HoverEventSource<Component>) op -> HoverEvent.showText(clickToRemove.color(NamedTextColor.GREEN))));
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
