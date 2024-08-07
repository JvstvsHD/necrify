/*
 * This file is part of Necrify (formerly Velocity Punishment), which is licensed under the MIT license.
 *
 * Copyright (c) 2022-2024 JvstvsHD
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
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

    public NecrifyStateEvent(State state) {
        super("necrify_state_" + state.name());
        this.state = state;
    }

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
