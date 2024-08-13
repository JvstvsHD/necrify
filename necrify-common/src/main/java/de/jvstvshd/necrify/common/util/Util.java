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

import de.chojo.sadu.core.conversion.UUIDConverter;
import de.chojo.sadu.mapper.wrapper.Row;
import de.jvstvshd.necrify.api.message.MessageProvider;
import de.jvstvshd.necrify.api.punishment.Punishment;
import de.jvstvshd.necrify.api.punishment.TemporalPunishment;
import de.jvstvshd.necrify.common.io.NecrifyDatabase;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.event.HoverEventSource;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.Nullable;

import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

public class Util {

    private Util() {
    }

    public static UUID getUuid(Row row, int index) throws SQLException {
        return switch (NecrifyDatabase.SQL_TYPE.toLowerCase(Locale.ROOT)) {
            case "postgres", "postgresql", "mariadb" -> row.getObject(index, UUID.class);
            default -> UUIDConverter.convert(row.getBytes(index));
        };
    }

    public static UUID parseUuid(String uuidString) {
        try {
            return UUID.fromString(uuidString);
        } catch (IllegalArgumentException e) {
            try {
                return UUID.fromString(uuidString.replaceAll(
                        "(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})",
                        "$1-$2-$3-$4-$5"));
            } catch (Exception ex) {
                return null;
            }
        }
    }

    public static <T> CompletableFuture<T> executeAsync(Callable<T> task, Executor service) {
        CompletableFuture<T> cf = new CompletableFuture<>();
        service.execute(() -> {
            try {
                cf.complete(task.call());
            } catch (Throwable e) {
                cf.completeExceptionally(e);
            }
        });
        return cf;
    }

    public static TextComponent copyComponent(String text, MessageProvider provider) {
        return Component.text(text).clickEvent(ClickEvent.suggestCommand(text))
                .hoverEvent((HoverEventSource<Component>) op -> HoverEvent.showText(provider.provide("commands.general.copy").color(NamedTextColor.GREEN)));
    }

    public static Component copyComponent(Component base, String copy, MessageProvider provider) {
        return base.clickEvent(ClickEvent.suggestCommand(copy))
                .hoverEvent((HoverEventSource<Component>) op -> HoverEvent.showText(provider.provide("commands.general.copy").color(NamedTextColor.GREEN)));
    }

    public static Optional<UUID> fromString(String uuidString) {
        UUID uuid;
        try {
            uuid = UUID.fromString(uuidString);
        } catch (IllegalArgumentException e) {
            try {
                uuid = UUID.fromString(uuidString.replaceAll(
                        "(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})",
                        "$1-$2-$3-$4-$5"));
            } catch (Exception ex) {
                return Optional.empty();
            }
        }
        return Optional.of(uuid);
    }

    public static boolean circularSuccessionChain(Punishment base, Punishment successor) {
        Punishment successorsSuccessor = successor;
        do {
            if (successorsSuccessor.equals(base)) {
                return true;
            }
        } while ((successorsSuccessor = successorOrNull(successorsSuccessor)) != null);
        return false;
    }

    @Nullable
    public static Punishment successorOrNull(Punishment punishment) {
        if (punishment.hasSuccessor()) {
            return punishment.getSuccessor();
        }
        return null;
    }

    public static <T extends TemporalPunishment> T getLongestPunishment(List<T> list) {
        if (list.isEmpty())
            return null;
        List<T> sorted = sortPunishments(list);
        return sorted.getLast();
    }

    public static <T extends TemporalPunishment> List<T> sortPunishments(List<T> list) {
        return list.stream().sorted(Comparator.comparing(TemporalPunishment::getDuration)).collect(Collectors.toList());
    }
}
