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

import org.jetbrains.annotations.NotNull;

/**
 * Represents an action that has been performed on a punishment. This only contains what type of action has been performed,
 * not the actual action itself.
 *
 * @since 1.2.2
 */
public interface PunishmentLogAction {

    /**
     * Returns the name of this action.
     *
     * @return the name of this action.
     */
    @NotNull
    String name();

    /**
     * Returns whether this action can only be logged once. If this is true, the action can not be logged multiple times for the same punishment.
     * This also makes using {@link PunishmentLog#getEntry(PunishmentLogAction)} possible.
     *
     * @return whether this action can only be logged once.
     */
    boolean onlyOnce();

    /**
     * A punishment was created. This action can only be logged once.
     */
    PunishmentLogAction CREATED = new SimplePunishmentLogAction("created", true);

    /**
     * The reason of a punishment was changed. This action can be logged multiple times. The reason stored in the associated
     * {@link PunishmentLogEntry} is the new reason. The old reason can be retrieved from the previous entry.
     */
    PunishmentLogAction CHANGE_REASON = new SimplePunishmentLogAction("change_reason", false);

    /**
     * The duration of a punishment was changed. This action can be logged multiple times. The duration stored in the associated
     * {@link PunishmentLogEntry} is the new duration. The old duration can be retrieved from the previous entry.
     */
    PunishmentLogAction CHANGE_DURATION = new SimplePunishmentLogAction("change_duration", false);

    /**
     * The predecessor of a punishment was changed. This action can be logged multiple times. The predecessor stored in the associated
     * {@link PunishmentLogEntry} is the new predecessor. The old predecessor can be retrieved from the previous entry.
     */
    PunishmentLogAction CHANGE_PREDECESSOR = new SimplePunishmentLogAction("change_predecessor", false);

    /**
     * The successor of a punishment was changed. This action can be logged multiple times. The successor stored in the associated
     * {@link PunishmentLogEntry} is the new successor. The old successor can be retrieved from the previous entry.
     */
    PunishmentLogAction CHANGE_SUCCESSOR = new SimplePunishmentLogAction("change_successor", false);

    /**
     * A punishment was removed. This action can only be logged once.
     */
    PunishmentLogAction REMOVED = new SimplePunishmentLogAction("removed", true);

    /**
     * An unknown action was performed. This action is returned as default if the stored action type cannot be resolved to
     * a proper type. This action can be logged multiple times.
     */
    PunishmentLogAction UNKNOWN = new SimplePunishmentLogAction("unknown", false);

    /**
     * A simple implementation of {@link PunishmentLogAction}. This class only contains the name and whether the action can only be logged once or more.
     * @param name the name of the action
     * @param onlyOnce whether the action can only be logged once
     */
    record SimplePunishmentLogAction(String name, boolean onlyOnce) implements PunishmentLogAction {

        @Override
        public @NotNull String name() {
            return name;
        }
    }
}
