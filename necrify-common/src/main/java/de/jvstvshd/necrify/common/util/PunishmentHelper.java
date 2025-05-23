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

package de.jvstvshd.necrify.common.util;

import de.jvstvshd.necrify.api.duration.PunishmentDuration;
import de.jvstvshd.necrify.api.message.MessageProvider;
import de.jvstvshd.necrify.api.punishment.Punishment;
import de.jvstvshd.necrify.api.punishment.PunishmentType;
import de.jvstvshd.necrify.api.punishment.TemporalPunishment;
import de.jvstvshd.necrify.api.template.NecrifyTemplateStage;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.event.HoverEventSource;
import net.kyori.adventure.text.feature.pagination.Pagination;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

public class PunishmentHelper {

    private PunishmentHelper() {
        throw new IllegalStateException("Utility class");
    }

    private static final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    public static Component buildPunishmentData(Punishment punishment, MessageProvider provider) {
        return buildPunishmentData(punishment, provider, 0);
    }

    public static Component buildPunishmentData(Punishment punishment, MessageProvider messageProvider, int indents) {
        var provider = indents == 0 ? messageProvider : messageProvider.autoPrefixed(false);
        var ic = indentComponent(indents);
        var clickToRemove = provider.provide("command.punishment.click-to-remove");

        var builder = Component.text()
                .append(ic,
                        provider.provide("helper.type").color(NamedTextColor.AQUA),
                        buildPunishmentTypeInformation(punishment.getType(), provider),
                        Component.newline(),
                        ic,
                        provider.provide("helper.reason").color(NamedTextColor.AQUA),
                        Util.copyComponent(punishment.getReason(), PlainTextComponentSerializer.plainText().serialize(punishment.getReason()), provider),
                        Component.newline(),
                        ic,
                        provider.provide("ID: ").color(NamedTextColor.AQUA),
                        copyable(punishment.getPunishmentUuid().toString(), NamedTextColor.YELLOW, provider),
                        Component.newline(),
                        punishment instanceof TemporalPunishment temporalPunishment ?
                                buildPunishmentDataTemporal(temporalPunishment, provider, ic) : Component.text("")
                );
        if (punishment.isOngoing()) {
            builder.append(Component.newline(),
                    ic,
                    clickToRemove
                            .color(NamedTextColor.RED)
                            .clickEvent(ClickEvent.runCommand("/necrify punishment " + punishment.getPunishmentUuid().toString().toLowerCase(Locale.ROOT) + " remove"))
                            .hoverEvent((HoverEventSource<Component>) op -> HoverEvent.showText(clickToRemove.color(NamedTextColor.GREEN))),
                    Component.text(" | ", NamedTextColor.GRAY));
        } else {
            builder.append(Component.newline());
        }
        builder.append(provider
                        .provide("helper.view-log", false)
                        .color(NamedTextColor.GRAY)
                        .clickEvent(ClickEvent.runCommand("/necrify punishment " + punishment.getPunishmentUuid().toString().toLowerCase(Locale.ROOT) + " log")),
                Component.newline());
        if (punishment.hasSuccessor()) {
            int newIndenting;
            if (indents == 0) {
                newIndenting = 6;
            } else {
                newIndenting = indents + 3;
            }
            var child = buildPunishmentData(punishment.getSuccessor(), provider, newIndenting);
            builder.append(ic, provider.provide("helper.successor").color(NamedTextColor.AQUA), Component.newline())
                    .append(child);
        }
        return builder.build();
    }

    public static Component buildPunishmentDataTemporal(TemporalPunishment punishment, MessageProvider provider, Component linePrefix) {
        if (punishment.isPermanent()) {
            return Component.text()
                    .append(linePrefix,
                            provider.provide("helper.temporal.duration").color(NamedTextColor.AQUA),
                            Component.text("PERMANENT").color(NamedTextColor.RED))
                    .build();
        }
        return Component.text()
                .append(linePrefix,
                        provider.provide("helper.temporal.duration").color(NamedTextColor.AQUA),
                        Component.text(punishment.getDuration().remainingDuration()).color(NamedTextColor.YELLOW),
                        Component.newline(),
                        linePrefix,
                        provider.provide("helper.temporal.end").color(NamedTextColor.AQUA),
                        Component.text(punishment.getDuration().expirationAsString()).color(NamedTextColor.YELLOW))
                .build();
    }

    public static Component indentComponent(int n) {
        if (n == 0) {
            return Component.empty();
        }
        return Component.text(" ".repeat(n) + "> ").color(NamedTextColor.GRAY);
    }

    //TODO add information on hover/click
    public static Component buildPunishmentTypeInformation(PunishmentType type, MessageProvider provider) {
        return copyable("%s (%d)".formatted(type.getName(), type.getId()), NamedTextColor.YELLOW, provider);
    }

    public static Component buildTemplateStageInformation(NecrifyTemplateStage stage, MessageProvider provider) {
        return provider.provide("command.template.stage.info", Component.text(stage.index() + 1, NamedTextColor.YELLOW),
                stage.reason(), Component.text(stage.duration().remainingDuration(PunishmentDuration.StringRepresentation.SHORT), NamedTextColor.YELLOW),
                PunishmentHelper.buildPunishmentTypeInformation(stage.punishmentType(), provider)).color(NamedTextColor.GRAY);
    }

    private static Component copyable(String s, NamedTextColor color, MessageProvider provider) {
        return Util.copyComponent(s, provider).color(color);
    }
}
