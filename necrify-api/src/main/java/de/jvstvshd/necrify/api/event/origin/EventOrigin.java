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
 * A mean of origin for an event. This can be used to check if an event originates from this source.
 */
public interface EventOrigin {

    /**
     * Creates a new {@link EventOrigin} instance from a string. This can be used to determine the source of an event.
     * @param name the name of the origin.
     * @return a new {@link EventOrigin} instance.
     */
    static EventOrigin ofString(String name) {
        return new StringEventOrigin(name);
    }

    /**
     * Creates a new {@link EventOrigin} instance from a class. This can be used to determine the source of an event.
     * @param clazz the class that is the origin of the event.
     * @return a new {@link EventOrigin} instance.
     */
    static EventOrigin ofClass(Class<?> clazz) {
        return new ClassEventOrigin(clazz);
    }

    /**
     * Checks if the event originates from the given object. The given object can be any object, but should be adapted
     * to the specific situation. If the origin checks only for classes, object should be an instance of {@link Class}.
     * @param object the object to check.
     * @return {@code true} if the event originates from the object, {@code false} otherwise.
     */
    boolean originatesFrom(@NotNull Object object);

    /**
     * Creates a new {@link EventOrigin} instance that does not specifies an origin. This can be uses as a default value.
     * @return an unspecified {@link EventOrigin} instance.
     */
    static EventOrigin nullOrigin() {
        return new NullEventOrigin();
    }

    /**
     * A mean of origin for an event that is based on a class. This special instance is used when the origin is not specified
     * or not known. {@link #originatesFrom(Object)} returns therefore always {@code false}.
     */
    class NullEventOrigin implements EventOrigin {

        @Override
        public boolean originatesFrom(@NotNull Object object) {
            return false;
        }
    }
}
