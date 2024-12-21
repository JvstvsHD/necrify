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

package de.jvstvshd.necrify.common.punishment.log;

import de.chojo.sadu.queries.api.call.Call;
import de.chojo.sadu.queries.api.query.Query;
import de.jvstvshd.necrify.api.duration.PunishmentDuration;
import de.jvstvshd.necrify.api.event.punishment.PunishmentLogEvent;
import de.jvstvshd.necrify.api.punishment.Punishment;
import de.jvstvshd.necrify.api.punishment.log.PunishmentLog;
import de.jvstvshd.necrify.api.punishment.log.PunishmentLogAction;
import de.jvstvshd.necrify.api.punishment.log.PunishmentLogActionRegistry;
import de.jvstvshd.necrify.api.punishment.log.PunishmentLogEntry;
import de.jvstvshd.necrify.api.user.NecrifyUser;
import de.jvstvshd.necrify.api.user.UserManager;
import de.jvstvshd.necrify.common.AbstractNecrifyPlugin;
import de.jvstvshd.necrify.common.io.Adapters;
import de.jvstvshd.necrify.common.util.Util;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;

public class NecrifyPunishmentLog implements PunishmentLog {

    private final Punishment punishment;
    private final List<PunishmentLogEntry> entries = Collections.synchronizedList(new ArrayList<>());
    private final AbstractNecrifyPlugin plugin;
    private final UserManager userManager;
    private final Logger logger;

    public NecrifyPunishmentLog(Punishment punishment, AbstractNecrifyPlugin plugin) {
        this.punishment = punishment;
        this.plugin = plugin;
        this.userManager = plugin.getUserManager();
        this.logger = plugin.getLogger();
    }

    /**
     * Loads the log from the database. This method should be called right after instantiation of this object, otherwise
     * there will be no data contained in this object.
     * <p>This method is ran synchronously and should not be called on the main thread. Use an asynchronous context yourself.
     *
     * @param loadIfAlreadyLoaded whether to load the log if it is already loaded
     */
    public synchronized void load(boolean loadIfAlreadyLoaded) {
        if (!loadIfAlreadyLoaded && !entries.isEmpty()) {
            return;
        }
        var entries = Query.query("SELECT id, player_id, message, expiration, reason, predecessor, successor, action, " +
                        "beginsAt, created_at FROM punishment_log WHERE punishment_id = ? ORDER BY id ASC")
                .single(Call.of().bind(punishment.getUuid(), Adapters.UUID_ADAPTER))
                .map(row -> {
                    var id = row.getInt(1);
                    var actorUuid = row.getObject(2, UUID.class);
                    NecrifyUser actor;
                    if (actorUuid == null) {
                        actor = null;
                    } else {
                        actor = userManager.loadUser(actorUuid).join().orElseThrow(() -> new IllegalStateException("Actor not found."));
                    }
                    var message = row.getString(3);
                    var duration = PunishmentDuration.fromTimestamp(row.getTimestamp(4));
                    var reason = MiniMessage.miniMessage().deserialize(row.getString(5));
                    var predecessor = plugin.getPunishment(row.getObject(6, UUID.class)).join().orElse(null);
                    var successor = plugin.getPunishment(row.getObject(7, UUID.class)).join().orElse(null);
                    var action = PunishmentLogActionRegistry.getAction(row.getString(8)).orElse(PunishmentLogAction.UNKNOWN);
                    var beginsAt = row.getTimestamp(9).toLocalDateTime();
                    var instant = row.getTimestamp(10).toLocalDateTime();
                    return new PunishmentLogEntry(actor, message, duration, reason, predecessor, punishment, successor,
                            beginsAt, action, this, instant, id);
                }).all();
        Collections.sort(entries);
        var completedEntries = entries.stream().map(this::completeEntry).toList();
        this.entries.clear();
        this.entries.addAll(completedEntries);
    }

    /**
     * Retrieves a value from freshly created log entries. This method is only meant to be applied to such entries that
     * have been filled with values provided by the database as they only contain the changed value, whereas all other values
     * are null. This method will retrieve the value from the previous entry or punishment object if the value is null.
     *
     * @param startAt  the entry to start at (last entry; the method will look upon all previous entries, only if needed)
     * @param function the function to retrieve the value from the entry
     * @param <T>      the type of the value
     * @return the value
     */
    private <T> T retrieveValue(@NotNull PunishmentLogEntry startAt, @NotNull Function<PunishmentLogEntry, T> function) {
        T value;
        do {
            value = function.apply(startAt);
            startAt = startAt.previous().orElse(null);
        } while (value == null && startAt != null);
        return value != null ? value : function.apply(punishment.createCurrentLogEntry());
    }

    private PunishmentLogEntry completeEntry(PunishmentLogEntry entry) {
        return new PunishmentLogEntry(entry.actor(), entry.message(), retrieveValue(entry, PunishmentLogEntry::duration),
                retrieveValue(entry, PunishmentLogEntry::reason), retrieveValue(entry, PunishmentLogEntry::predecessor),
                entry.punishment(), retrieveValue(entry, PunishmentLogEntry::successor),
                retrieveValue(entry, PunishmentLogEntry::beginsAt), entry.action(), entry.log(), entry.instant(), entry.index());
    }

    @Override
    public @NotNull Punishment getPunishment() {
        return punishment;
    }

    @Override
    public @NotNull List<PunishmentLogEntry> getEntries() {
        return entries;
    }

    @Override
    public @NotNull List<PunishmentLogEntry> getEntries(@NotNull PunishmentLogAction action) {
        return entries.stream().filter(entry -> entry.action().equals(action)).toList();
    }

    @Override
    public @Nullable PunishmentLogEntry getEntry(@NotNull PunishmentLogAction action) {
        return getEntries(action).getFirst();
    }

    @Override
    public @NotNull PunishmentLogEntry getEntry(int index) {
        return entries.get(index);
    }

    @Override
    public void log(@NotNull PunishmentLogAction action, @NotNull String message, @NotNull NecrifyUser actor) {
        if (action.onlyOnce() && getEntry(action) != null) {
            throw new IllegalArgumentException("This action can only be logged once.");
        }
        var entry = new PunishmentLogEntry(actor, message, PunishmentDuration.ofPunishment(punishment), punishment.getReason(),
                punishment.getPredecessor(), punishment, punishment.getSuccessor(),
                punishment.getCreationTime(), action, this, LocalDateTime.now(),
                entries.size());
        entries.add(entry);
        Util.executeAsync(() -> Query.query("INSERT INTO necrify_schema.punishment_log (punishment_id, player_id, message, expiration, reason, predecessor, successor, action, begins_at created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")
                .single(Call.of().bind(punishment.getUuid(), Adapters.UUID_ADAPTER)
                        .bind(actor.getUuid(), Adapters.UUID_ADAPTER)
                        .bind(message)
                        .bind(PunishmentDuration.ofPunishment(punishment).expirationAsTimestamp())
                        .bind(MiniMessage.miniMessage().serialize(punishment.getReason()))
                        .bind(punishment.getPredecessor() == null ? null : punishment.getPredecessor().getUuid(), Adapters.UUID_ADAPTER)
                        .bind(punishment.getSuccessorOrNull() == null ? null : punishment.getSuccessor().getUuid(), Adapters.UUID_ADAPTER)
                        .bind(action.name())
                        .bind(Timestamp.valueOf(entry.beginsAt()))
                        .bind(Timestamp.from(Instant.now())))
                .insert().changed(), plugin.getExecutor()).whenComplete((aBoolean, throwable) -> {
            if (throwable != null) {
                logger.error("Could not log punishment action.", throwable);
                return;
            }
            plugin.getEventDispatcher().dispatch(new PunishmentLogEvent(entry));
        });
    }
}