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
import de.jvstvshd.necrify.api.template.NecrifyTemplate;
import de.jvstvshd.necrify.api.template.NecrifyTemplateStage;
import de.jvstvshd.necrify.api.template.TemplateManager;
import de.jvstvshd.necrify.api.user.NecrifyUser;
import de.jvstvshd.necrify.api.user.UserDeletionReason;
import de.jvstvshd.necrify.common.AbstractNecrifyPlugin;
import de.jvstvshd.necrify.common.BuildParameters;
import de.jvstvshd.necrify.common.config.ConfigData;
import de.jvstvshd.necrify.common.template.MinecraftTemplateStage;
import de.jvstvshd.necrify.common.util.PunishmentHelper;
import de.jvstvshd.necrify.common.util.Util;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.event.HoverEventSource;
import net.kyori.adventure.text.feature.pagination.Pagination;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.incendo.cloud.annotation.specifier.Greedy;
import org.incendo.cloud.annotations.*;
import org.incendo.cloud.annotations.suggestion.Suggestions;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.context.CommandInput;
import org.incendo.cloud.minecraft.extras.suggestion.ComponentTooltipSuggestion;
import org.incendo.cloud.suggestion.Suggestion;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

public class NecrifyCommand {

    private final AbstractNecrifyPlugin plugin;
    private final MiniMessage miniMessage;
    private final Logger logger;
    private final MessageProvider provider;
    private final TemplateManager templateManager;

    private static final List<String> PUNISHMENT_COMMAND_OPTIONS = List.of("cancel", "remove", "info", "change", "log");
    private static final List<String> USER_COMMAND_OPTIONS = List.of("info", "delete", "whitelist");
    private static final List<String> TEMPLATE_COMMAND_OPTIONS = List.of("info", "delete");

    public NecrifyCommand(AbstractNecrifyPlugin plugin) {
        this.plugin = plugin;
        this.miniMessage = MiniMessage.miniMessage();
        this.logger = plugin.getLogger();
        this.provider = plugin.getMessageProvider();
        this.templateManager = plugin.getTemplateManager();
    }

    //COMMANDS
    //Punishing

    @Command("necrify ban <target> [reason]")
    @Command("ban <target> [reason]")
    @Permission(value = {"necrify.command.ban", "necrify.admin"}, mode = Permission.Mode.ANY_OF)
    public void banCommand(
            NecrifyUser sender,
            @Argument(value = "target", description = "Player to ban", suggestions = "suggestOnlinePlayers") NecrifyUser target,
            @Argument(value = "reason", description = "Reason the user should be banned for", suggestions = "suggestMiniMessageAndTemplate") @Greedy String templateOrReason
    ) {
        infinitePunishmentCommand(sender, target, templateOrReason, target::banPermanent, "command.ban.success", StandardPunishmentType.PERMANENT_BAN);
    }

    @Command("necrify mute <target> [reason]")
    @Command("mute <target> [reason]")
    @Permission(value = {"necrify.command.mute", "necrify.admin"}, mode = Permission.Mode.ANY_OF)
    public void muteCommand(
            NecrifyUser sender,
            @Argument(value = "target", description = "Player to mute", suggestions = "suggestOnlinePlayers") NecrifyUser target,
            @Argument(value = "reason", description = "Reason the user should be muted for", suggestions = "suggestMiniMessage") @Greedy String templateOrReason
    ) {
        infinitePunishmentCommand(sender, target, templateOrReason, target::mutePermanent, "command.mute.success", StandardPunishmentType.PERMANENT_MUTE);
    }

    @Command("necrify kick <target> [reason]")
    @Command("kick <target> [reason]")
    @Permission(value = {"necrify.command.kick", "necrify.admin"}, mode = Permission.Mode.ANY_OF)
    public void kickCommand(
            NecrifyUser sender,
            @Argument(value = "target", description = "Player to kick", suggestions = "suggestOnlinePlayers") NecrifyUser target,
            @Argument(value = "reason", description = "Reason the user should be kicked for", suggestions = "suggestMiniMessage") @Greedy String templateOrReason
    ) {
        infinitePunishmentCommand(sender, target, templateOrReason, target::kick, "command.kick.success", StandardPunishmentType.KICK);
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
                    NamedTextColor.GRAY,
                    userReference(target),
                    copyComponent(ban.getUuid().toString()).color(NamedTextColor.YELLOW),
                    finalReason,
                    miniMessage(duration.expirationAsString()).color(NamedTextColor.YELLOW));
            tryChainPunishments(sender, target, ban);
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
                    NamedTextColor.GRAY,
                    userReference(target),
                    copyComponent(mute.getUuid().toString()).color(NamedTextColor.YELLOW),
                    finalReason,
                    miniMessage(duration.expirationAsString()).color(NamedTextColor.YELLOW));
            tryChainPunishments(sender, target, mute);
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
            @Argument(value = "option", description = "Option to manage the punishment", suggestions = "suggestPunishmentCommandOptions") @Default(value = "info") String option,
            @Flag(value = "chain", description = "Another punishment to chain") Punishment otherPunishment,
            @Flag(value = "page", description = "Page to display") Integer pageArgument
    ) {
        switch (option) {
            case "info" -> //TODO paginate punishments
                    sender.sendMessage(buildComponent(PunishmentHelper.buildPunishmentData(punishmentParsed, plugin.getMessageProvider())));
            case "cancel", "remove" -> {
                try {
                    punishmentParsed.cancel().whenCompleteAsync((unused, th) -> {
                        if (th != null) {
                            logException(sender, th);
                            return;
                        }
                        sender.sendMessage(provider.provide("command.punishment.cancel.success").color(NamedTextColor.GREEN));
                    }, plugin.getExecutor());
                } catch (UnsupportedOperationException e) {
                    sender.sendMessage(Component.text("Error: Punishment can not be deleted, maybe because it is not active anymore.", NamedTextColor.RED));
                }
            }
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
            case "log" -> {
                int page = pageArgument == null ? 1 : pageArgument;
                punishmentParsed.loadPunishmentLog().whenComplete((punishmentLog, throwable) -> {
                    if (throwable != null) {
                        logException(sender, throwable);
                        return;
                    }
                    var paginator = Pagination.builder().width(42).resultsPerPage(5).build(Component.text("Necrify Punishment Log"),
                                    new PunishmentLogPaginationRowRenderer(miniMessage, plugin),
                                    functionPage -> "/necrify punishment " + punishmentParsed.getPunishmentUuid() + " log --page " + functionPage)
                            .render(punishmentLog.getEntries(), page);
                    for (Component component : paginator) {
                        sender.sendMessage(component);
                    }
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
                        Component.text(punishments.size())).color(NamedTextColor.GRAY));
                for (Punishment punishment : punishments) {
                    if (punishment.getPredecessor() != null) {
                        continue;
                    }
                    Component component = buildComponent(PunishmentHelper.buildPunishmentData(punishment, plugin.getMessageProvider()));
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

    @Command("necrify reload")
    @Permission(value = {"necrify.command.reload", "necrify.admin"}, mode = Permission.Mode.ANY_OF)
    public void reloadCommand(NecrifyUser sender) {
        try {
            sender.sendMessage(provider.provide("command.reload.start").color(NamedTextColor.GRAY));
            long start = System.currentTimeMillis();
            final ConfigData old = plugin.getConfig().getConfiguration();
            plugin.getConfig().load();
            ConfigData reloaded = plugin.getConfig().getConfiguration();
            if (!old.isWhitelistActivated() && reloaded.isWhitelistActivated()) {
                Util.executeAsync(() -> {
                    logger.info("Whitelist activated. Kicking all players who aren't allowed here anymore. This may take some time...");
                    var startKick = System.currentTimeMillis();
                    var onlinePlayers = plugin.getOnlinePlayers();
                    onlinePlayers.forEach(pair -> {
                        try {
                            //noinspection OptionalGetWithoutIsPresent (we know that the user must exist/can be created)
                            var user = plugin.getUserManager().loadOrCreateUser(pair.second()).join().get();
                            if (!user.isWhitelisted()) {
                                user.kick(provider.provide("whitelist.removed").color(NamedTextColor.RED)).join();
                            }
                        } catch (Exception e) {
                            logException(e);
                        }
                    });
                    logger.info("Kicked all non-whitelisted players. Took {} seconds.", (System.currentTimeMillis() - startKick) / 1000.0);
                    return null;
                }, plugin.getExecutor());
            }
            String took = String.format("%.2f", (System.currentTimeMillis() - start) / 1000.0);
            sender.sendMessage(provider.provide("command.reload.success", Component.text(took).color(NamedTextColor.YELLOW)).color(NamedTextColor.GREEN));
        } catch (IOException e) {
            logException(sender, e);
            sender.sendMessage(provider.provide("command.reload.failure").color(NamedTextColor.RED));
        }
    }

    @Command("necrify info")
    public void infoCommand(NecrifyUser sender) {
        Component version = Component.text(BuildParameters.VERSION, NamedTextColor.GOLD);
        Component gitCommit = Component.text(BuildParameters.GIT_COMMIT, NamedTextColor.YELLOW);
        //noinspection ConstantValue
        if (BuildParameters.VERSION.contains("SNAPSHOT")) {
            sender.sendMessage("command.info.version.snapshot", NamedTextColor.GRAY, version,
                    Component.text(BuildParameters.BUILD_NUMBER, NamedTextColor.YELLOW), gitCommit);
        } else {
            sender.sendMessage("command.info.version.release", NamedTextColor.GRAY, version, gitCommit);
        }
        var hangar = Component.text("Hangar", NamedTextColor.AQUA)
                .clickEvent(ClickEvent.openUrl("https://hangar.papermc.io/JvstvsHD/Necrify"));
        var modrinth = Component.text("Modrinth", NamedTextColor.AQUA)
                .clickEvent(ClickEvent.openUrl("https://modrinth.com/plugin/necrify"));
        var jenkins = Component.text("Jenkins", NamedTextColor.AQUA)
                .clickEvent(ClickEvent.openUrl("https://ci.jvstvshd.de/job/Necrify/"));
        sender.sendMessage("command.info.download-updates", NamedTextColor.GRAY, hangar, modrinth, jenkins);
        sender.sendMessage(provider.provide("command.info.view-source").color(NamedTextColor.LIGHT_PURPLE)
                .clickEvent(ClickEvent.openUrl("https://github.com/JvstvsHD/necrify")));
        sender.sendMessage(provider.provide("command.info.documentation").color(NamedTextColor.LIGHT_PURPLE)
                .clickEvent(ClickEvent.openUrl("https://docs.jvstvshd.de/necrify/")));
    }

    @Command("necrify whitelist [option]")
    @Permission(value = {"necrify.command.whitelist", "necrify.admin"}, mode = Permission.Mode.ANY_OF)
    public void whitelistCommand(
            NecrifyUser sender,
            @Argument(value = "option", description = "Option to retrieve the whitelist's status") @Default("status") String option
    ) {
        var whitelist = plugin.isWhitelistActive();
        var activeState = whitelist ? "active" : "inactive";
        sender.sendMessage(provider.provide("command.whitelist." + activeState).color(NamedTextColor.GRAY));
        sender.sendMessage(provider.provide("whitelist.change-in-config"));
    }

    //template management

    @Command("necrify createtemplate <name>")
    @Permission(value = {"necrify.command.template.create", "necrify.admin"}, mode = Permission.Mode.ANY_OF)
    public void createTemplateCommand(
            NecrifyUser sender,
            @Argument(value = "name", description = "Name of the template") String name
    ) {
        if (templateManager.getTemplate(name).isPresent()) {
            sender.sendMessage("command.template.create.already-exists", NamedTextColor.RED);
            return;
        }
        templateManager.createTemplate(name).whenComplete((necrifyTemplate, throwable) -> {
            if (throwable != null) {
                logException(sender, throwable);
                return;
            }
            sender.sendMessage("command.template.create.success", NamedTextColor.GREEN, Component.text(name, NamedTextColor.YELLOW));
        });
    }

    @Command("necrify template <name>")
    @Permission(value = {"necrify.command.template.manage", "necrify.admin"}, mode = Permission.Mode.ANY_OF)
    public void templateInfoCommand(NecrifyUser sender, @Argument(value = "name", description = "Description") NecrifyTemplate template, @Flag("page") Integer pageArgument) {
        int page = pageArgument == null ? 1 : pageArgument;
        sender.sendMessage("command.template.manage.info", NamedTextColor.GRAY, Component.text(template.name(), NamedTextColor.YELLOW),
                Component.text(template.stages().size(), NamedTextColor.YELLOW));
        Pagination.Renderer.RowRenderer<NecrifyTemplateStage> rowRenderer = (stage, index) ->
                List.of(PunishmentHelper.buildTemplateStageInformation(Objects.requireNonNull(stage), provider));
        var components = Pagination.builder().width(42).resultsPerPage(5).build(Component.text(template.name(), NamedTextColor.YELLOW), rowRenderer,
                functionPage -> "/necrify template " + template.name() + " --page " + functionPage).render(template.stages(), page);
        for (Component component : components) {
            sender.sendMessage(component);
        }
    }

    @Command("necrify template <name> delete")
    @Permission(value = {"necrify.command.template.delete", "necrify.admin"}, mode = Permission.Mode.ANY_OF)
    public void templateDeleteCommand(
            NecrifyUser sender,
            @Argument(value = "name", description = "Name of the template") NecrifyTemplate template
    ) {
        //TODO add confirmation
        template.delete().whenComplete((integer, throwable) -> {
            if (throwable != null) {
                logException(sender, throwable);
                return;
            }
            sender.sendMessage("command.template.delete.success", NamedTextColor.GREEN, Component.text(template.name(), NamedTextColor.YELLOW), Component.text(integer, NamedTextColor.YELLOW));
        });
    }

    @Command("necrify template <name> addstage <duration> <type> <reason>")
    @Permission(value = {"necrify.command.template.stage.add", "necrify.admin"}, mode = Permission.Mode.ANY_OF)
    public void templateAddStageCommand(
            NecrifyUser sender,
            @Argument(value = "name") NecrifyTemplate template,
            @Argument(value = "duration") PunishmentDuration duration,
            @Argument(value = "type") PunishmentType punishmentType,
            @Argument(value = "reason", suggestions = "suggestMiniMessage") @Greedy String reasonString
    ) {
        Component reason = miniMessage(reasonString);
        //TODO add creation wizard, duration optional (omit if permanent punishment)
        if (punishmentType instanceof StandardPunishmentType standardPunishmentType && standardPunishmentType.isPermanent()) {
            duration = PunishmentDuration.PERMANENT;
        }
        template.addStage(new MinecraftTemplateStage(template, punishmentType, duration, reason, template.stages().size(), plugin)).whenComplete((stage, throwable) -> {
            if (throwable != null) {
                logException(sender, throwable);
                return;
            }
            sender.sendMessage("command.template.stage.add.success", NamedTextColor.GREEN);
        });
    }

    @Command("necrify template <name> removestage <index>")
    @Permission(value = {"necrify.command.template.stage.remove", "necrify.admin"}, mode = Permission.Mode.ANY_OF)
    public void templateRemoveStageCommand(
            NecrifyUser sender,
            @Argument(value = "name") NecrifyTemplate template,
            @Argument(value = "index") int index
    ) {
        //TODO add confirmation
        template.getStage(index - 1).delete().whenCompleteAsync((integer, throwable) -> {
            if (throwable != null) {
                logException(sender, throwable);
                return;
            }
            sender.sendMessage("command.template.stage.remove.success", NamedTextColor.GREEN);
        });
    }

    @Command("necrify template <name> apply <user>")
    @Permission(value = {"necrify.command.template.apply", "necrify.admin"}, mode = Permission.Mode.ANY_OF)
    public void templateApplyCommand(
            NecrifyUser sender,
            @Argument(value = "name") NecrifyTemplate template,
            @Argument(value = "user", suggestions = "suggestOnlinePlayers") NecrifyUser user
    ) {
        user.punishModelled(template).whenComplete((punishment, throwable) -> {
            if (throwable != null) {
                logException(sender, throwable);
                return;
            }
            sender.sendMessage("command.template.apply.success", NamedTextColor.GREEN, userReference(user),
                    PunishmentHelper.buildPunishmentTypeInformation(punishment.getType(), provider),
                    punishment.getReason(), Component.text(PunishmentDuration.ofPunishment(punishment).remainingDuration()),
                    Component.text(template.name(), NamedTextColor.YELLOW),
                    copyComponent(punishment.getUuid().toString()).color(NamedTextColor.YELLOW));
        });
    }

    @Command("necrify template <name> amnesty <target>")
    @Permission(value = {"necrify.command.template.amnesty", "necrify.admin"}, mode = Permission.Mode.ANY_OF)
    public void templateAmnestyCommand(
            NecrifyUser sender,
            @Argument(value = "name") NecrifyTemplate template,
            @Argument(value = "target", suggestions = "suggestOnlinePlayers") NecrifyUser target,
            @Flag("to") @Default("0") Integer toStage
    ) {
        int stageIndex = toStage == null ? 0 : Math.min(Math.max(toStage, 1), template.stages().size()) - 1;
        target.amnesty(template, stageIndex).whenComplete((unused, throwable) -> {
            sender.sendMessage("command.template.amnesty.success", NamedTextColor.GREEN);
        });
    }

    @Command("necrify template <name> state <target>")
    @Permission(value = {"necrify.command.template.state", "necrify.admin"}, mode = Permission.Mode.ANY_OF)
    public void templateStateCommand(
            NecrifyUser sender,
            @Argument(value = "name") NecrifyTemplate template,
            @Argument(value = "target", suggestions = "suggestOnlinePlayers") NecrifyUser target
    ) {
        var stageOptional = target.getCurrentTemplateStage(template);
        if (stageOptional.isEmpty()) {
            sender.sendMessage("command.template.state.no-stage", NamedTextColor.RED);
            return;
        }
        NecrifyTemplateStage currentStage = stageOptional.get();
        NecrifyTemplateStage nextStage = currentStage.nextOrThis();
        sender.sendMessage("command.template.state.text", NamedTextColor.GRAY, Component.text(template.name(), NamedTextColor.YELLOW), userReference(target),
                Component.text(currentStage.index() + 1, NamedTextColor.YELLOW)
                        .hoverEvent(HoverEvent.showText(PunishmentHelper.buildTemplateStageInformation(currentStage, provider))),
                Component.text(nextStage.index() + 1, NamedTextColor.YELLOW)
                        .hoverEvent(HoverEvent.showText(PunishmentHelper.buildTemplateStageInformation(nextStage, provider))));
        //TODO show active punishment (if exists)
    }

    //SUGGESTIONS

    @Suggestions("suggestOnlinePlayers")
    public List<? extends Suggestion> suggestNames(CommandContext<NecrifyUser> context, CommandInput input) {
        return plugin
                .getOnlinePlayers()
                .stream()
                .filter(pair -> pair.first().toLowerCase(Locale.ROOT).startsWith(input.peekString().toLowerCase(Locale.ROOT)))
                .map(pair -> {
                    var select = provider.provide("suggestion.select-player", context.sender().getLocale(), false, miniMessage(
                            "<yellow>(<name>/<uuid>)</yellow>",
                            Placeholder.parsed("name", pair.first()),
                            Placeholder.parsed("uuid", pair.second().toString()))).color(NamedTextColor.RED);
                    return ComponentTooltipSuggestion.suggestion(pair.first(), select);
                }).toList();
    }

    @Suggestions("suggestMiniMessage")
    public List<? extends Suggestion> suggestMiniMessage(CommandContext<NecrifyUser> context, CommandInput input) {
        return Collections.singletonList(ComponentTooltipSuggestion.suggestion(input.remainingInput()
                        + " (" + provider.provideString("suggestion.hover-over-me", context.sender().getLocale(), false) + ")",
                miniMessage(input.remainingInput())));
    }

    @Suggestions("suggestMiniMessageAndTemplate")
    public List<? extends Suggestion> suggestMiniMessageAndTemplate(CommandContext<NecrifyUser> context, CommandInput input) {
        List<Suggestion> suggestions = new ArrayList<>(suggestMiniMessage(context, input));
        suggestions.addAll(0, templateManager.getTemplates()
                .stream()
                .filter(template -> template.name().toLowerCase().startsWith(input.peekString().toLowerCase()))
                .map(template -> ComponentTooltipSuggestion.suggestion(template.name(), MiniMessage.miniMessage().deserialize(template.name())))
                .toList());
        return suggestions;
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

    @Suggestions("suggestTemplateCommandOptions")
    public List<? extends Suggestion> suggestTemplateCommandOptions(CommandContext<NecrifyUser> context, CommandInput input) {
        return TEMPLATE_COMMAND_OPTIONS
                .stream()
                .filter(option -> option.toLowerCase().startsWith(input.peekString().toLowerCase()))
                .map(option -> ComponentTooltipSuggestion.suggestion(option, miniMessage(option)))
                .toList();
    }

    //HELPER METHODS

    public void infinitePunishmentCommand(NecrifyUser sender, NecrifyUser target, String templateOrReasonString,
                                          Function<Component, CompletableFuture<? extends Punishment>> reasonExecution,
                                          String successString, StandardPunishmentType defaultReasonType) {
        var templateOrReason = StringOrTemplate.fromString(templateOrReasonString, templateManager);
        CompletableFuture<? extends Punishment> punishmentFuture;
        if (templateOrReason.template().isPresent()) {
            punishmentFuture = target.punishModelled(templateOrReason.template().get());
        } else {
            var finalReason = reasonOrDefaultTo(templateOrReason.component().orElse(plugin.getDefaultReason(defaultReasonType)), defaultReasonType);
            punishmentFuture = reasonExecution.apply(finalReason);
        }
        punishmentFuture.whenComplete((punishment, throwable) -> {
            if (throwable != null) {
                logException(sender, throwable);
                return;
            }
            sender.sendMessage(successString,
                    NamedTextColor.GRAY,
                    userReference(target),
                    copyComponent(punishment.getUuid().toString()).color(NamedTextColor.YELLOW),
                    punishment.getReason());
            //TODO chaining permanent punishments does not make sense
            //TODO offer option to merge reasons of both punishments
            //TODO this also causes errors when chaining
            //tryChainPunishments(sender, target, ban);
        });
    }


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
                if (punishment.getPredecessor() != null) {
                    continue;
                }
                source.sendMessage(buildComponent(PunishmentHelper.buildPunishmentData(punishment, plugin.getMessageProvider())));
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
                }, plugin.getExecutor());
            } catch (Exception e) {
                logException(source, e);
            }
        }
    }

    private void tryChainPunishments(NecrifyUser sender, NecrifyUser target, Punishment newPunishment) {
        var types = newPunishment.getType().getRelatedTypes();
        var matchingPunishments = target.getPunishments(types.toArray(new PunishmentType[0])).stream().filter(punishment -> !punishment.equals(newPunishment)).toList();
        if (matchingPunishments.isEmpty()) {
            return;
        }
        var unchainedPunishments = matchingPunishments.stream().filter(punishment -> !punishment.hasSuccessor()).toList();
        if (unchainedPunishments.isEmpty()) {
            //This should not happen since the chained punishment only gets activated after is predecessor runs out
            throw new IllegalStateException("No unchained punishments found. Did you forget to remove a reference?");
        }
        sender.sendMessage(provider.provide("command.punishment.chain.info").color(NamedTextColor.GRAY));
        for (Punishment unchainedPunishment : unchainedPunishments) {
            sender.sendMessage(provider.provide("command.punishment.chain",
                            Component.text(unchainedPunishment.getPunishmentUuid().toString()).color(NamedTextColor.YELLOW)).color(NamedTextColor.GRAY)
                    .clickEvent(ClickEvent.runCommand("/necrify punishment " + unchainedPunishment.getPunishmentUuid().toString().toLowerCase(Locale.ROOT)
                            + " chain --chain " + newPunishment.getPunishmentUuid().toString().toLowerCase(Locale.ROOT)))
                    .hoverEvent((HoverEventSource<Component>) op -> HoverEvent.showText(provider.provide("command.punishment.chain").color(NamedTextColor.GREEN))));
        }
    }

    //Communication, messaging, logging

    private Component buildComponent(Component dataComponent) {
        return dataComponent;
    }

    private Component reasonOrDefaultTo(String reason, StandardPunishmentType type) {
        return miniMessage(reason == null ? plugin.getDefaultReason(type) : reason);
    }

    private Component userReference(NecrifyUser user) {
        return Component.text(getUsername(user), NamedTextColor.YELLOW)
                .clickEvent(ClickEvent.runCommand("/necrify user " + user.getUuid() + " info"))
                .hoverEvent((HoverEventSource<Component>) op -> HoverEvent
                        .showText(provider.provide("commands.general.view-user-info", false,
                                Component.text(getUsername(user), NamedTextColor.YELLOW)).color(NamedTextColor.GREEN)));
    }

    private String getUsername(NecrifyUser user) {
        return Objects.requireNonNullElse(user.getUsername(), user.getUuid().toString());
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
                        Component.text(Objects.requireNonNullElse(user.getUsername(), provider.provideString("user.unknown", user.getLocale()))).color(NamedTextColor.YELLOW),
                        provider
                                .provide("whitelist.status." + (whitelisted ? "whitelisted" : "disallowed"), false)
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