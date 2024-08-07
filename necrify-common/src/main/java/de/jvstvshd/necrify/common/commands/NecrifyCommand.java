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
package de.jvstvshd.necrify.common.commands;

import de.jvstvshd.necrify.api.duration.PunishmentDuration;
import de.jvstvshd.necrify.api.message.MessageProvider;
import de.jvstvshd.necrify.api.punishment.Punishment;
import de.jvstvshd.necrify.api.punishment.PunishmentType;
import de.jvstvshd.necrify.api.punishment.StandardPunishmentType;
import de.jvstvshd.necrify.api.user.NecrifyUser;
import de.jvstvshd.necrify.api.user.UserDeletionReason;
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

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public class NecrifyCommand {

    private final AbstractNecrifyPlugin plugin;
    private final MiniMessage miniMessage;
    private final Logger logger;
    private final MessageProvider provider;

    private static final List<String> PUNISHMENT_COMMAND_OPTIONS = List.of("cancel", "remove", "info", "change");
    private static final List<String> USER_COMMAND_OPTIONS = List.of("info", "delete", "whitelist");
    private static final List<String> WHITELIST_COMMAND_OPTIONS = List.of("enable", "disable", "status");

    public NecrifyCommand(AbstractNecrifyPlugin plugin) {
        this.plugin = plugin;
        this.miniMessage = MiniMessage.miniMessage();
        this.logger = plugin.getLogger();
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

    @Command("necrify punishment <punishmentId> [option] [otherPunishment]")
    @Permission(value = {"necrify.command.punishment", "necrify.admin"}, mode = Permission.Mode.ANY_OF)
    public void punishmentCommand(
            NecrifyUser sender,
            @Argument(value = "punishmentId", description = "Punishment to manage") Punishment punishmentParsed,
            @Argument(value = "option", description = "Option to manage the punishment", suggestions = "suggestPunishmentCommandOptions") @Default(value = "info") String option,
            @Argument(value = "otherPunishment", description = "Another punishment to chain") Punishment otherPunishment
    ) {
        switch (option) {
            case "info" ->
                    sender.sendMessage(buildComponent(PunishmentHelper.buildPunishmentData(punishmentParsed, plugin.getMessageProvider()), punishmentParsed));
            case "cancel", "remove" -> punishmentParsed.cancel().whenCompleteAsync((unused, th) -> {
                if (th != null) {
                    logException(sender, th);
                    return;
                }
                sender.sendMessage(provider.provide("command.punishment.cancel.success").color(NamedTextColor.GREEN));
            }, plugin.getService());
            case "change" -> sender.sendMessage(miniMessage("Soon (TM)").color(NamedTextColor.LIGHT_PURPLE));
            case "chain" -> {
                if (!punishmentParsed.getType().getRelatedTypes().contains(otherPunishment.getType())) {
                    sender.sendMessage(provider.provide("command.punishment.chain.unrelated-types").color(NamedTextColor.RED));
                    return;
                }
                if (!punishmentParsed.getUser().equals(otherPunishment.getUser())) {
                    sender.sendMessage(provider.provide("command.punishment.chain.user-mismatch").color(NamedTextColor.RED));
                    return;
                }
                if (Util.circularSuccessionChain(punishmentParsed, otherPunishment)) {
                    sender.sendMessage(provider.provide("command.punishment.circular-chain").color(NamedTextColor.RED));
                    return;
                }
                punishmentParsed.setSuccessor(otherPunishment).whenComplete((unused, th) -> {
                    if (th != null) {
                        logException(sender, th);
                        return;
                    }
                    sender.sendMessage(provider.provide("command.punishment.chain.success").color(NamedTextColor.GREEN));
                });
            }
            default -> sender.sendMessage(unknownOption(option, PUNISHMENT_COMMAND_OPTIONS));
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
                sender.sendMessage(whitelistStatus(target));
                sender.sendMessage(provider.provide("command.user.overview",
                        Util.copyComponent(Objects.requireNonNullElse(target.getUsername(), "null"), provider).color(NamedTextColor.YELLOW),
                        Util.copyComponent(target.getUuid().toString(), provider).color(NamedTextColor.YELLOW),
                        Component.text(punishments.size())));
                for (Punishment punishment : punishments) {
                    Component component = buildComponent(PunishmentHelper.buildPunishmentData(punishment, plugin.getMessageProvider()), punishment);
                    sender.sendMessage(component);
                }
            }
            case "delete" -> //TODO add confirmation
                    target.delete(UserDeletionReason.USER_DELETED).whenComplete((integer, throwable) -> {
                        if (throwable != null) {
                            logException(sender, throwable);
                            return;
                        }
                        sender.sendMessage(provider.provide("command.user.delete.success", Component.text(integer).color(NamedTextColor.YELLOW)).color(NamedTextColor.RED));
                    });
            case "whitelist" -> {
                var newState = target.isWhitelisted();
                target.setWhitelisted(!newState).whenComplete((unused, throwable) -> {
                    if (throwable != null) {
                        logException(sender, throwable);
                        return;
                    }
                    sender.sendMessage(provider.provide("command.whitelist.success").color(NamedTextColor.GREEN));
                    sender.sendMessage(whitelistStatus(target));
                });
            }
            default -> sender.sendMessage(unknownOption(option, USER_COMMAND_OPTIONS));
        }
    }

    @Command("necrify whitelist [option]")
    @Permission(value = {"necrify.command.whitelist", "necrify.admin"}, mode = Permission.Mode.ANY_OF)
    public void whitelistCommand(
            NecrifyUser sender,
            @Argument(value = "option", description = "Option to manage the whitelist", suggestions = "suggestWhitelistCommandOptions") @Default("status") String option
    ) {
        switch (option) {
            case "status" -> {
                var whitelist = plugin.isWhitelistActive();
                var activeState = whitelist ? "active" : "inactive";
                sender.sendMessage(provider.provide("command.whitelist." + activeState).color(NamedTextColor.GRAY));
            }
            case "enable", "disable" -> {
                var state = "enable".equals(option);
                try {
                    plugin.setWhitelistActive(state);
                } catch (IOException e) {
                    logException(sender, e);
                    return;
                }
                sender.sendMessage(provider.provide("command.whitelist." + (state ? "enabled" : "disabled")).color(NamedTextColor.GREEN));
            }
            default -> sender.sendMessage(unknownOption(option, WHITELIST_COMMAND_OPTIONS));
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
                        + " (" + provider.unprefixedProvider().provideString("suggestion.hover-over-me", context.sender().getLocale()) + ")",
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

    @Suggestions("suggestWhitelistCommandOptions")
    public List<? extends Suggestion> suggestWhitelistCommandOptions(CommandContext<NecrifyUser> context, CommandInput input) {
        return WHITELIST_COMMAND_OPTIONS
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

    private void tryChainPunishments(NecrifyUser sender, NecrifyUser target, Punishment newPunishment) {
        var types = newPunishment.getType().getRelatedTypes();
        var matchingPunishments = target.getPunishments(types.toArray(new PunishmentType[0]));
        if (matchingPunishments.isEmpty()) {
            return;
        }
        var unchainedPunishments = matchingPunishments.stream().filter(punishment -> !punishment.hasSuccessor()).toList();
        if (unchainedPunishments.isEmpty()) {
            //This should not happen since the chained punishment only gets activated after is predecessor runs out
            throw new IllegalStateException("No unchained punishments found. Did you forget to remove a reference?");
        }
        sender.sendMessage(provider.provide("command.punishment.chain.info"));
        for (Punishment unchainedPunishment : unchainedPunishments) {
            sender.sendMessage(provider.provide("command.punishment.chain",
                    Component.text(unchainedPunishment.getPunishmentUuid().toString()).color(NamedTextColor.YELLOW)).color(NamedTextColor.GRAY)
                    .clickEvent(ClickEvent.runCommand("/necrify punishment " + unchainedPunishment.getPunishmentUuid().toString().toLowerCase(Locale.ROOT)
                            + " chain " + newPunishment.getPunishmentUuid().toString().toLowerCase(Locale.ROOT)))
                    .hoverEvent((HoverEventSource<Component>) op -> HoverEvent.showText(provider.provide("command.punishment.chain.hover").color(NamedTextColor.GREEN))));
        }
    }

    //Communication, messaging, logging

    private Component buildComponent(Component dataComponent, Punishment punishment) {
        return dataComponent;
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

    private Component whitelistStatus(NecrifyUser user) {
        var whitelisted = user.isWhitelisted();
        return provider.provide("command.whitelist.status",
                        Component.text(Objects.requireNonNullElse(user.getUsername(), "Unknown Username")).color(NamedTextColor.YELLOW),
                        provider
                                .unprefixedProvider()
                                .provide("whitelist.status." + (whitelisted ? "whitelisted" : "disallowed"))
                                .color(whitelisted ? NamedTextColor.GREEN : NamedTextColor.RED))
                .color(NamedTextColor.GRAY);
    }

    private Component unknownOption(String option, List<String> options) {
        return provider.provide("commands.general.unknown-option",
                Component.text(option).color(NamedTextColor.RED),
                Component.text(String.join(", ", options)).color(NamedTextColor.YELLOW)).color(NamedTextColor.GRAY);
    }

    private void logException(Throwable throwable) {
        logger.error("An error occurred while executing a command", throwable);
    }

    private void logException(NecrifyUser sender, Throwable throwable) {
        logger.error("An error occurred while executing a command for player {} ({})", sender.getUsername(), sender.getUuid(), throwable);
        sender.sendErrorMessage();
    }
}
