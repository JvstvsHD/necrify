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

import de.jvstvshd.necrify.api.message.MessageProvider;
import de.jvstvshd.necrify.api.punishment.log.PunishmentLogEntry;
import de.jvstvshd.necrify.common.AbstractNecrifyPlugin;
import de.jvstvshd.necrify.common.util.Util;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.feature.pagination.Pagination;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.function.Function;

import static de.jvstvshd.necrify.api.punishment.log.PunishmentLogAction.*;

public class PunishmentLogPaginationRowRenderer implements Pagination.Renderer.RowRenderer<PunishmentLogEntry> {

    private final MiniMessage miniMessage;
    private final MessageProvider messageProvider;
    private final AbstractNecrifyPlugin plugin;

    public PunishmentLogPaginationRowRenderer(MiniMessage miniMessage, AbstractNecrifyPlugin plugin) {
        this.miniMessage = miniMessage;
        this.messageProvider = plugin.getMessageProvider().autoPrefixed(false);
        this.plugin = plugin;
    }

    @Override
    public @NotNull Collection<Component> renderRow(@Nullable PunishmentLogEntry value, int index) {
        try {
            if (value == null) {
                return List.of(Component.text("null"));
            }
            var punishment = value.punishment();
            var action = value.action();
            if (action == INFORMATION) {
                return List.of(Component.text().append(
                        messageProvider.prefix(),
                        Component.text(value.index() + 1 + ". ", NamedTextColor.GOLD),
                        miniMessage.deserialize("<gray>Information logged internally.")
                ).build());
            }
            Component change;
            if (action == CREATED) {
                change = messageProvider.provide("log.punishment.created", false, punishment.getReason()).color(NamedTextColor.GRAY);
            } else if (action == REMOVED) {
                change = messageProvider.provide("log.punishment.removed", false, punishment.getReason()).color(NamedTextColor.GRAY);
            } else {
                Function<PunishmentLogEntry, String> acquire;
                String actionName;
                if (action == CHANGE_REASON) {
                    acquire = entry -> miniMessage.serialize(entry.reason());
                    actionName = "log.change-reason";
                } else if (action == CHANGE_DURATION) {
                    acquire = entry -> entry.duration().toString();
                    actionName = "log.change-duration";
                } else if (action == CHANGE_PREDECESSOR) {
                    //TODO acquire 'none' over message provider
                    acquire = entry -> entry.predecessor() == null ? "none" : entry.predecessor().getUuid().toString();
                    actionName = "log.change-predecessor";
                } else if (action == CHANGE_SUCCESSOR) {
                    acquire = entry -> entry.successor() == null ? "none" : entry.successor().getUuid().toString();
                    actionName = "log.change-successor";
                } else if (action == CHANGE_TIME) {
                    acquire = entry -> Util.dtf.format(entry.beginsAt()) + " - " + Util.dtf.format(entry.duration().expiration());
                    actionName = "log.change-time";
                } else {
                    return List.of();
                }
                var actionComponent = messageProvider.provide(actionName, false).color(NamedTextColor.LIGHT_PURPLE);
                change = Component.text().append(
                        actionComponent,
                        Component.space(),
                        Component.text(acquire.apply(value.previousOrThis()), NamedTextColor.RED, TextDecoration.STRIKETHROUGH),
                        Component.text(" -> ", NamedTextColor.YELLOW),
                        Component.text(acquire.apply(value), NamedTextColor.GREEN)
                ).build();
            }
            var username = value.actor() == null ? messageProvider.provide("user.unknown", false) :
                    Component.text(value.actor().getUsername() == null ? value.actor().getUuid().toString() : value.actor().getUsername());
            var usernameComponent = Util.copyComponent(username.color(NamedTextColor.YELLOW), username, messageProvider);
            var timeComponent = Component.text(Util.dtf.format(value.instant())).color(NamedTextColor.YELLOW);
            return List.of(Component.text().append(
                            messageProvider.prefix(),
                            Component.text(value.index() + 1 + ". ", NamedTextColor.GOLD),
                            messageProvider.provide("log.executed-by-on", usernameComponent, timeComponent).color(NamedTextColor.GRAY),
                            Component.space(),
                            change)
                    .build());
        } catch (Exception e) {
            plugin.getLogger().error("An error occurred while rendering a punishment log entry row.", e);
            throw new RuntimeException(e);
        }
    }
}
