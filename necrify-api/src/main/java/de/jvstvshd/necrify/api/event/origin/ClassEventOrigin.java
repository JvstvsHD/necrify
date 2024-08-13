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

package de.jvstvshd.necrify.api.event.origin;

import org.jetbrains.annotations.NotNull;

/**
 * A mean of origin for an event that is based on a class. This can be used to check if an event originates from a specific
 * class. This is not bound to a specific instance of this class.
 * @param origin the class that is the origin of the event.
 */
public record ClassEventOrigin(Class<?> origin) implements EventOrigin {

    @Override
    public boolean originatesFrom(@NotNull Object object) {
        return origin.isInstance(object);
    }
}
