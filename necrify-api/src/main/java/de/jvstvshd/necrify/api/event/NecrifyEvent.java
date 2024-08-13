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

package de.jvstvshd.necrify.api.event;

import de.jvstvshd.necrify.api.event.origin.EventOrigin;
import org.jetbrains.annotations.NotNull;

/**
 * Represents an event that can be dispatched by the {@link EventDispatcher}.
 * All events must extend this class.
 *
 * @since 1.2.0
 */
public abstract class NecrifyEvent {

    private final String name;
    private EventOrigin origin = EventOrigin.nullOrigin();
    private EventDispatcher executingDispatcher = null;

    /**
     * Creates a new event with the given name. The name should be unique and describe the event.
     * @param name the name of the event.
     */
    public NecrifyEvent(String name) {
        this.name = name;
    }

    /**
     * Gets the name of this event. The name should be unique and describe the event.
     * @return the name of this event.
     */
    @NotNull
    public String getName() {
        return name;
    }

    /**
     * Cancels the event. This will prevent the event from being executed further.
     */
    public final void cancel() {
        if (executingDispatcher != null) {
            executingDispatcher.cancelEvent(this);
        }
    }

    /**
     * Sets the dispatcher that is currently executing this event.
     *
     * @param executingDispatcher the dispatcher that is currently executing this event.
     * @return this event.
     */
    NecrifyEvent setExecutingDispatcher(EventDispatcher executingDispatcher) {
        this.executingDispatcher = executingDispatcher;
        return this;
    }

    /**
     * Gets the origin of this event.
     *
     * @return the origin of this event.
     */
    public EventOrigin getOrigin() {
        return origin;
    }

    /**
     * Sets the origin of this event.
     *
     * @param origin the origin of this event.
     * @return this event.
     */
    public NecrifyEvent setOrigin(EventOrigin origin) {
        this.origin = origin;
        return this;
    }
}
