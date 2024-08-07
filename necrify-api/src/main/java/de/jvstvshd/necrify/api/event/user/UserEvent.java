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
package de.jvstvshd.necrify.api.event.user;

import de.jvstvshd.necrify.api.event.NecrifyEvent;
import de.jvstvshd.necrify.api.user.NecrifyUser;
import org.jetbrains.annotations.NotNull;

/**
 * Represents an event that is related to a user or his actions.
 *
 * @since 1.2.0
 */
public abstract class UserEvent extends NecrifyEvent {

    private final NecrifyUser user;

    public UserEvent(String name, @NotNull NecrifyUser user) {
        super(name);
        this.user = user;
    }

    @NotNull
    public NecrifyUser getUser() {
        return user;
    }
}
