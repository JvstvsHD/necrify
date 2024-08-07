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

public interface EventOrigin {

    static EventOrigin ofString(String name) {
        return new StringEventOrigin(name);
    }

    static EventOrigin ofClass(Class<?> clazz) {
        return new ClassEventOrigin(clazz);
    }

    boolean originatesFrom(Object object);

    static EventOrigin nullOrigin() {
        return new NullEventOrigin();
    }

    class NullEventOrigin implements EventOrigin {

        @Override
        public boolean originatesFrom(Object object) {
            return false;
        }
    }
}
