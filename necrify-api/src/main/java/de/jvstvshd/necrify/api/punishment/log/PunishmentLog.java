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

package de.jvstvshd.necrify.api.punishment.log;

import de.jvstvshd.necrify.api.punishment.Punishment;
import de.jvstvshd.necrify.api.user.NecrifyUser;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Represents a log of a punishment, containing all actions that have been performed on the punishment.
 * @since 1.2.2
 */
public interface PunishmentLog {

    /**
     * Returns the punishment this log belongs to.
     * @return the punishment this log belongs to.
     */
    @NotNull
    Punishment getPunishment();

    /**
     * Returns a list containing all entries of this log.
     * @return a list containing all entries of this log.
     */
    @NotNull
    List<PunishmentLogEntry> getEntries();

    /**
     * Returns a list containing all entries of this log with the given action. If no entries with the given action are found, an empty list is returned.
     * @param action the action to filter the entries by
     * @return a list containing all entries of this log with the given action.
     */
    @NotNull
    List<PunishmentLogEntry> getEntries(@NotNull PunishmentLogAction action);

    /**
     * Returns the first entry of this log with the given action. If no entry with the given action is found, null is returned.
     * @param action the action to filter the entries by
     * @return the first entry of this log with the given action.
     */
    @Nullable
    PunishmentLogEntry getEntry(@NotNull PunishmentLogAction action);

    /**
     * Returns the entry at the given index. If the index is out of bounds, an exception is thrown.
     * @param index the index of the entry
     * @return the entry at the given index.
     * @throws IndexOutOfBoundsException if the index is out of bounds
     */
    @NotNull
    PunishmentLogEntry getEntry(int index);

    /**
     * Returns the latest entry of this log. If the log is empty, an exception is thrown.
     * @return the latest entry of this log.
     * @throws IndexOutOfBoundsException if the log is empty
     */
    @NotNull
    default PunishmentLogEntry getLatestEntry() {
        return getEntry(getEntries().size() - 1);
    }

    /**
     * Logs a new action with the given message. The action is automatically associated with the punishment of this log.
     * @param action the action to log
     * @param message the message to log
     */
    void log(@NotNull PunishmentLogAction action, @NotNull String message, @NotNull NecrifyUser actor);
}
