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

package de.jvstvshd.necrify.api.user;

/**
 * Represents a reasoning why a user instance got deleted. This can have lots of causes, but may be done due to data protection
 * regulations.
 */
public interface UserDeletionReason {

    /**
     * The user requested the deletion of their account, possibly due to privacy concerns and/or data protection regulations.
     */
    UserDeletionReason USER_REQUESTED = new UserDeletionReasonImpl("User requested deletion.");

    /**
     * The user was deleted by the system or a team member.
     */
    UserDeletionReason USER_DELETED = new UserDeletionReasonImpl("User was deleted by the system or a team member.");

    /**
     * The reasoning why this user was deleted.
     * @return the reason why this user was deleted.
     */
    String reason();

    record UserDeletionReasonImpl(String reason) implements UserDeletionReason {
    }
}
