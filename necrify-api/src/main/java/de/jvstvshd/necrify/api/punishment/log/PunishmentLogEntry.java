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

import de.jvstvshd.necrify.api.duration.PunishmentDuration;
import de.jvstvshd.necrify.api.punishment.Punishment;
import de.jvstvshd.necrify.api.user.NecrifyUser;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.Objects;

/**
 * Represents an entry in a {@link PunishmentLog}. This contains all information about a punishment log entry.
 * There is no information about old values if they have been changed, this has to be done by using {@link #previous() the previous entry}.
 *
 * @param actor       the actor who performed the action or null if the user does not exist anymore
 * @param message     the message of the action
 * @param duration    the duration of the punishment
 * @param reason      the reason of the punishment
 * @param predecessor the predecessor of the punishment or null if there is none
 * @param punishment  the punishment this entry belongs to
 * @param successor   the successor of the punishment or null if there is none
 * @param action      the action that was performed
 * @param log         the log this entry belongs to
 * @param instant     the instant the action was performed
 * @param index       the index of this entry in the log (0-based)
 * @since 1.2.2
 */
public record PunishmentLogEntry(@Nullable NecrifyUser actor, @Nullable String message,
                                 @NotNull PunishmentDuration duration, @NotNull Component reason,
                                 @Nullable Punishment predecessor, @NotNull Punishment punishment,
                                 @Nullable Punishment successor,
                                 @NotNull PunishmentLogAction action, @NotNull PunishmentLog log,
                                 @NotNull Instant instant, int index) implements Comparable<PunishmentLogEntry> {

    /**
     * Returns the previous entry in the log. If this is the first entry, this entry is returned.
     *
     * @return the previous entry in the log
     */
    @NotNull
    public PunishmentLogEntry previous() {
        return Objects.requireNonNullElse(log.getEntries().get(index - 1), this);
    }

    /**
     * Returns the next entry in the log. If this is the last entry, this entry is returned.
     *
     * @return the next entry in the log
     */
    @NotNull
    public PunishmentLogEntry next() {
        return Objects.requireNonNullElse(log.getEntries().get(index + 1), this);
    }

    /**
     * Returns the affected user of the punishment. This is the user who is affected by the punishment and equivalent
     * to {@link #punishment()}.{@link Punishment#getUser() getUser()}.
     *
     * @return the affected user of the punishment
     */
    @NotNull
    public NecrifyUser getAffectedUser() {
        return punishment.getUser();
    }

    @Override
    public int compareTo(@NotNull PunishmentLogEntry o) {
        return Integer.compare(index, o.index);
    }
}