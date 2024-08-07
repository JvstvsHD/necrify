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
package de.jvstvshd.necrify.common.util;

import de.jvstvshd.necrify.api.message.MessageProvider;
import de.jvstvshd.necrify.api.punishment.Punishment;
import de.jvstvshd.necrify.api.punishment.TemporalPunishment;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentBuilder;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.event.HoverEventSource;
import net.kyori.adventure.text.flattener.ComponentFlattener;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class PunishmentHelper {

    private PunishmentHelper() {
        throw new IllegalStateException("Utility class");
    }

    public static Component buildPunishmentData(Punishment punishment, MessageProvider provider) {
        return buildPunishmentData(punishment, provider, 0);
    }

    public static Component buildPunishmentData(Punishment punishment, MessageProvider messageProvider, int indents) {
        var provider = indents == 0 ? messageProvider : messageProvider.unprefixedProvider();
        var ic = indentComponent(indents);
        var clickToRemove = provider.provide("command.punishment.click-to-remove");

        var builder = Component.text()
                .append(ic,
                        provider.provide("helper.type").color(NamedTextColor.AQUA),
                        copyable("%s (%d)".formatted(punishment.getType().getName(), punishment.getType().getId()), NamedTextColor.YELLOW, provider),
                        Component.newline(),
                        ic,
                        provider.provide("helper.reason").color(NamedTextColor.AQUA),
                        Util.copyComponent(punishment.getReason(), PlainTextComponentSerializer.plainText().serialize(punishment.getReason()), provider),
                        Component.newline(),
                        ic,
                        provider.prefixed(Component.text("ID: ")).color(NamedTextColor.AQUA),
                        copyable(punishment.getPunishmentUuid().toString(), NamedTextColor.YELLOW, provider),
                        Component.newline(),
                        punishment instanceof TemporalPunishment temporalPunishment ?
                                buildPunishmentDataTemporal(temporalPunishment, provider, ic) : Component.text(""),
                        Component.newline(),
                        ic,
                        clickToRemove
                                .color(NamedTextColor.RED)
                                .clickEvent(ClickEvent.runCommand("/necrify punishment " + punishment.getPunishmentUuid().toString().toLowerCase(Locale.ROOT) + " remove"))
                                .hoverEvent((HoverEventSource<Component>) op -> HoverEvent.showText(clickToRemove.color(NamedTextColor.GREEN))),
                        Component.newline()
                );
        if (punishment.hasSuccessor()) {
            int newIndenting;
            if (indents == 0) {
                newIndenting = 10;
            } else {
                newIndenting = indents + 3;
            }
            var child = buildPunishmentData(punishment.getSuccessor(), provider, newIndenting);
            builder.append(ic, provider.provide("helper.successor").color(NamedTextColor.AQUA), Component.newline())
                    .append(child);
        }
        return builder.build();
    }

    public static Component indentComponent(int n) {
        if (n == 0) {
            return Component.empty();
        }
        return Component.text(" ".repeat(n) + "> ").color(NamedTextColor.GRAY);
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

    private static Component copyable(String s, NamedTextColor color, MessageProvider provider) {
        return Util.copyComponent(s, provider).color(color);
    }
}
