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

import de.jvstvshd.necrify.api.event.NecrifyEvent;
import org.jetbrains.annotations.NotNull;

/**
 * When this event is dispatched, the state of the Necrify API has changed. It may is now initialized, disabled, etc.
 *
 * @since 1.2.0
 */
public abstract class NecrifyStateEvent extends NecrifyEvent {

    private final State state;

    /**
     * Creates a new {@link NecrifyStateEvent} with the given state. There should only be one class of this event for each state.
     * @param state the state of the Necrify API.
     */
    public NecrifyStateEvent(State state) {
        super("necrify_state_" + state.name());
        this.state = state;
    }

    /**
     * Gets the state of the Necrify API. This can be used to determine the current state of the API.
     * @return the state of the Necrify API.
     */
    @NotNull
    public State getState() {
        return state;
    }

    /**
     * Represents the state of the Necrify API.
     *
     * @param name an unique name for the state.
     * @since 1.2.0
     */
    public record State(String name) {

        /**
         * The state before the initialization of the Necrify API, when the API is not yet ready to be used.
         */
        public static final State PRE_INITIALIZATION = new State("pre_initialization");

        /**
         * The Necrify API is now fully initialized and ready to be used.
         */
        public static final State INITIALIZED = new State("initialized");

        /**
         * The Necrify API is now being disabled. The API can still be used in event handlers, but will be disabled after all event handlers have been executed.
         */
        public static final State DISABLING = new State("disabling");

        /**
         * The Necrify API is now disabled and can no longer be used. No services, commands, etc. are available anymore.
         * Moreover, the underlying platform's API may also be shut down.
         */
        public static final State DISABLED = new State("disabled");
    }
}
