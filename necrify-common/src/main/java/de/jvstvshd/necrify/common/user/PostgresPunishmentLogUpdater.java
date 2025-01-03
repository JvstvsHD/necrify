/*
 * This file is part of Necrify (formerly Velocity Punishment), a plugin designed to manage player's punishments for the platforms Velocity and partly Paper.
 * Copyright (C) 2022-2025 JvstvsHD
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

package de.jvstvshd.necrify.common.user;

import de.chojo.sadu.queries.api.call.Call;
import de.chojo.sadu.queries.api.query.Query;
import de.jvstvshd.necrify.api.event.EventDispatcher;
import de.jvstvshd.necrify.api.punishment.Punishment;
import de.jvstvshd.necrify.api.user.UserManager;
import de.jvstvshd.necrify.common.AbstractNecrifyPlugin;
import de.jvstvshd.necrify.common.event.PostgresPunishmentLogUpdateEvent;
import de.jvstvshd.necrify.common.punishment.log.NecrifyPunishmentLog;
import de.jvstvshd.necrify.common.util.Util;
import org.postgresql.PGConnection;
import org.postgresql.PGNotification;
import org.slf4j.Logger;

import javax.sql.DataSource;
import java.util.UUID;
import java.util.concurrent.Callable;

/**
 * This class is responsible for updating punishment log instances of punishments when they are updated.
 * This is handled through Postgres' NOTIFY/LISTEN mechanism.<br>
 * Only loaded punishment logs get updated.
 */
public class PostgresPunishmentLogUpdater implements Callable<Void> {

    public static final String CHANNEL_NAME = "necrify_punishment_log_update";

    private final DataSource dataSource;
    private final UserManager userManager;
    private final EventDispatcher dispatcher;
    private final Logger logger;
    private final AbstractNecrifyPlugin plugin;

    public PostgresPunishmentLogUpdater(UserManager userManager, DataSource dataSource, AbstractNecrifyPlugin plugin) {
        this.dataSource = dataSource;
        this.userManager = userManager;
        this.dispatcher = plugin.getEventDispatcher();
        this.logger = plugin.getLogger();
        this.plugin = plugin;
    }

    @Override
    public Void call() throws Exception {
        try (var connection = dataSource.getConnection()) {
            PGConnection pgConnection = connection.unwrap(PGConnection.class);
            try (var statement = connection.createStatement()) {
                statement.execute("LISTEN necrify_punishment_log_update");
                while (!Thread.currentThread().isInterrupted()) {
                    var notifications = pgConnection.getNotifications();
                    for (PGNotification notification : notifications) {
                        handleNotification(notification.getParameter());
                    }
                }
            }
        }
        return null;
    }

    private void handleNotification(String notification) {
        var split = notification.split(" ");
        if (split.length != 2) {
            return;
        }
        try {
            var uuid = UUID.fromString(split[0]);
            var id = Integer.parseInt(split[1]);
            var punishment = getPunishment(uuid);
            if (punishment == null) {
                return;
            }
            var log = Util.getCachedLog(punishment);
            if (log == null) {
                return;
            }
            var result = Query.query("SELECT id, actor_id, message, expiration, reason, predecessor, successor, action, begins_at, created_at FROM punishment_log WHERE id = ?;")
                    .single(Call.of().bind(id))
                    .map(row -> NecrifyPunishmentLog.fromRow(row, plugin, log, punishment, log.getEntries().size()))
                    .first();
            result.ifPresent(punishmentLogEntry ->
                    dispatcher.dispatch(new PostgresPunishmentLogUpdateEvent(punishment, punishmentLogEntry)));
        } catch (Exception e) {
            logger.error("An exception occurred while handling the notification: ", e);
        }
    }

    private Punishment getPunishment(UUID uuid) {
        return userManager.getLoadedUsers().stream()
                .flatMap(user -> user.getPunishments().stream())
                .filter(punishment -> punishment.getPunishmentUuid().equals(uuid)).findFirst().orElse(null);

    }
}
