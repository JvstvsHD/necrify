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

package de.jvstvshd.necrify.api.event.state;

/**
 * This event signals that the necrify api on the underlying platform is now being disabled. The API can still be used
 * normally in event handlers, but will be disabled after all event handlers have been executed.
 *
 * @since 1.2.0
 */
public class NecrifyDisablingEvent extends NecrifyStateEvent {

    public NecrifyDisablingEvent() {
        super(State.DISABLING);
    }
}
