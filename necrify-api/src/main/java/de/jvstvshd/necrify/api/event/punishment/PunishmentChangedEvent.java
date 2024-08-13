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

package de.jvstvshd.necrify.api.event.punishment;

import de.jvstvshd.necrify.api.punishment.Punishment;
import org.jetbrains.annotations.NotNull;

/**
 * Represents an event that is related to a punishment that has been changed.
 *
 * @since 1.2.0
 */
public class PunishmentChangedEvent extends PunishmentEvent {

    private final Punishment oldPunishment;

    public PunishmentChangedEvent(Punishment punishment, Punishment oldPunishment) {
        super("punishment_changed", punishment);
        this.oldPunishment = oldPunishment;
    }

    /**
     * Gets the old punishment that has been changed. This is the punishment that has been changed before the new punishment was set.
     * Do not use this instance for any further actions that change any state bound to this instance.
     * @return the old punishment that has been changed.
     */
    @NotNull
    public Punishment getOldPunishment() {
        return oldPunishment;
    }
}
