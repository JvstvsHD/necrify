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

package de.jvstvshd.necrify.api.event;

import de.jvstvshd.necrify.api.event.origin.EventOrigin;

/**
 * Represents an event that can be dispatched by the {@link EventDispatcher}.
 * All events must extend this class.
 *
 * @since 1.2.0
 */
public abstract class NecrifyEvent {

    private final String name;
    private EventOrigin origin = null;
    private EventDispatcher executingDispatcher = null;

    public NecrifyEvent(String name) {
        this.name = name;
    }

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