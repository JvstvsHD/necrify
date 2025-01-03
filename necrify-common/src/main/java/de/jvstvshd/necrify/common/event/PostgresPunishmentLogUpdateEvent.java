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

package de.jvstvshd.necrify.common.event;

import de.jvstvshd.necrify.api.event.punishment.PunishmentEvent;
import de.jvstvshd.necrify.api.punishment.Punishment;
import de.jvstvshd.necrify.api.punishment.log.PunishmentLogEntry;

/**
 * Event that is called when a punishment log notification from Postgres is received.
 * This event is only internal and gets called before the log entry is inserted into the log.
 */
public class PostgresPunishmentLogUpdateEvent extends PunishmentEvent {

    private final PunishmentLogEntry newEntry;

    public PostgresPunishmentLogUpdateEvent(Punishment punishment, PunishmentLogEntry newEntry) {
        super("postgres_punishment_log_update", punishment);
        this.newEntry = newEntry;
    }

    public PunishmentLogEntry getNewEntry() {
        return newEntry;
    }
}
