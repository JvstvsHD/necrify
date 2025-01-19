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

import de.chojo.sadu.mapper.wrapper.Row;
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
import de.jvstvshd.necrify.common.punishment.HistoricalPunishment;
import de.jvstvshd.necrify.common.util.Util;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Default implementation of {@link PunishmentLog} for minecraft servers.
 *
 * @implNote This class does not
 */
public class NecrifyPunishmentLog implements PunishmentLog {

    private Punishment punishment;
    private final List<PunishmentLogEntry> entries = Collections.synchronizedList(new ArrayList<>());
    private final AbstractNecrifyPlugin plugin;
    private final UserManager userManager;
    private final Logger logger;
    private final UUID uuid;

    /**
     * @param plugin
     * @param uuid
     */
    public NecrifyPunishmentLog(AbstractNecrifyPlugin plugin, UUID uuid) {
        this.plugin = plugin;
        this.userManager = plugin.getUserManager();
        this.logger = plugin.getLogger();
        this.uuid = uuid;
        Objects.requireNonNull(uuid, "uuid must not be null.");
    }

    public NecrifyPunishmentLog(AbstractNecrifyPlugin plugin, Punishment punishment) {
        this.logger = plugin.getLogger();
        this.userManager = plugin.getUserManager();
        this.plugin = plugin;
        this.punishment = punishment;
        this.uuid = punishment.getUuid();
        Objects.requireNonNull(uuid, "uuid must not be null.");
    }

    /**
     * Loads the log from the database. This method should be called right after instantiation of this object, otherwise
     * there will be no data contained in this object.
     * <p>This method is ran synchronously and should not be called on the main thread. Use an asynchronous context yourself.
     *
     * @param loadIfAlreadyLoaded whether to load the log if it is already loaded
     * @return true if there was log data found and loaded, otherwise false
     */
    public synchronized boolean load(boolean loadIfAlreadyLoaded) {
        if (!loadIfAlreadyLoaded && !entries.isEmpty()) {
            return false;
        }
        if (punishment == null) {
            punishment = new HistoricalPunishment(uuid, null, null, null, this);
        }
        //Prevents deadlock. See method documentation for more information.
        //This procedure should be save since the values inside the historical punishment are updated afterwards, so there
        //should be no missing data.
        plugin.getHistoricalPunishmentCache().put(uuid, punishment);
        AtomicInteger index = new AtomicInteger();
        var entries = Query.query("SELECT id, actor_id, message, expiration, reason, predecessor, successor, action, " +
                        "begins_at, created_at FROM punishment_log WHERE punishment_id = ? ORDER BY id ASC")
                .single(Call.of().bind(uuid, Adapters.UUID_ADAPTER))
                .map(row -> fromRow(row, plugin, this, punishment, index.getAndIncrement())).all();
        if (entries.isEmpty()) {
            //If there are no entries, the punishment is invalid and should be removed from the cache for historical punishments
            //it got cached into above.
            plugin.getHistoricalPunishmentCache().invalidate(uuid);
            return false;
        }
        Collections.sort(entries);
        this.entries.clear();
        this.entries.addAll(entries);
        if (punishment instanceof HistoricalPunishment historicalPunishment) {
            historicalPunishment.setCreationTime(getLatestEntry().beginsAt())
                    .setExpirationTime(getLatestEntry().duration().expiration())
                    .setUser(getEntry(PunishmentLogAction.INFORMATION).actor());
        }
        return true;
    }

    public static PunishmentLogEntry fromRow(Row row, AbstractNecrifyPlugin plugin, PunishmentLog log, Punishment punishment, int index) throws SQLException {
        var id = row.getInt(1);
        var actorUuid = Util.getUuid(row, 2);
        NecrifyUser actor;
        if (actorUuid == null) {
            actor = null;
        } else {
            actor = plugin.getUserManager().loadUser(actorUuid).join().orElseThrow(() -> new IllegalStateException("Actor not found " + actorUuid));
        }
        var message = row.getString(3);
        var duration = PunishmentDuration.fromTimestamp(row.getTimestamp(4));
        var reason = MiniMessage.miniMessage().deserialize(row.getString(5));
        var predecessor = getPunishment(Util.getUuid(row, 6), plugin);
        var successor = getPunishment(Util.getUuid(row, 7), plugin);
        var action = PunishmentLogActionRegistry.getAction(row.getString(8)).orElse(PunishmentLogAction.UNKNOWN);
        var beginsAt = row.getTimestamp(9).toLocalDateTime();
        var instant = row.getTimestamp(10).toLocalDateTime();
        return new PunishmentLogEntry(actor, message, duration, reason, predecessor, punishment, successor,
                beginsAt, action, log, instant, index);
    }

    @Nullable
    private static Punishment getPunishment(@Nullable UUID uuid, AbstractNecrifyPlugin plugin) {
        return uuid == null ? null : plugin.getPunishment(uuid).join().orElse(null);
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
        if (punishment == null || punishment instanceof HistoricalPunishment) {
            throw new IllegalStateException("Punishment no longer exists. This method is only applicable to still-existing punishments.");
        }
        if (action.onlyOnce() && getEntry(action) != null) {
            throw new IllegalArgumentException("This action can only be logged once.");
        }
        var entry = new PunishmentLogEntry(actor, message, PunishmentDuration.ofPunishment(punishment), punishment.getReason(),
                punishment.getPredecessor(), punishment, punishment.getSuccessor(),
                punishment.getCreationTime(), action, this, LocalDateTime.now(),
                entries.size());
        entries.add(entry);
        Util.executeAsync(() -> Query.query("INSERT INTO necrify_schema.punishment_log (punishment_id, actor_id, message, expiration, reason, predecessor, successor, action, begins_at created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")
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

    public void addEntry(PunishmentLogEntry entry) {
        entries.add(entry);
    }
}