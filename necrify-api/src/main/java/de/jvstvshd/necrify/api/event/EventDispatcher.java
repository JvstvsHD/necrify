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

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Logger;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

/**
 * The event dispatcher is used to dispatch events to the event bus. Events may be cancelled or dispatched synchronously or asynchronously.
 * Moreover, listeners can be registered and unregistered; or you may directly listen to events.
 *
 * @see NecrifyEvent
 * @see EventBus
 * @since 1.2.0
 */
public class EventDispatcher {

    private final EventBus eventbus;
    private final ExecutorService executorService;

    /**
     * Creates a new event dispatcher that can be customized completely.
     *
     * @param eventbus        The event bus to use. See the <a href="https://greenrobot.org/eventbus/documentation/configuration/">
     *                        GreenRobot EventBus documentation</a> for more information.
     * @param executorService The executor service to use for asynchronous event dispatching. Note: this service must not
     *                        correspond with the {@link org.greenrobot.eventbus.EventBusBuilder#executorService(ExecutorService) event bus' executor service}.
     */
    public EventDispatcher(@NotNull EventBus eventbus, ExecutorService executorService) {
        this.eventbus = eventbus;
        this.executorService = executorService;
    }

    /**
     * Creates a new event dispatcher with the given executor service and logger. This event bus does not log messages
     * if an dispatched event has no subscriber and does not send an event if no subscriber is registered.
     *
     * @param executorService The executor service to use for asynchronous event dispatching and for asynchronous event
     *                        handling execution (only for {@link org.greenrobot.eventbus.ThreadMode thread modes}
     *                        {@link org.greenrobot.eventbus.ThreadMode#ASYNC} and {@link org.greenrobot.eventbus.ThreadMode#BACKGROUND}).
     * @param logger          The logger to use for logging messages. If null, sysout will be used for logging. It may be better
     *                        to delegate messages to the platform's logger, for example with the {@link Slf4jLogger}.
     */
    public EventDispatcher(ExecutorService executorService, Logger logger) {
        this.executorService = executorService;
        this.eventbus = EventBus
                .builder()
                .logNoSubscriberMessages(false)
                .sendNoSubscriberEvent(false)
                .logger(logger)
                .executorService(executorService)
                .build();
    }

    /**
     * Dispatches the given event synchronously. This method will block until all listeners have been executed. Listener
     * methods registered with {@link org.greenrobot.eventbus.ThreadMode#ASYNC} or {@link ThreadMode#BACKGROUND} will be executed
     * asynchronously through the given {@link org.greenrobot.eventbus.EventBusBuilder#executorService(ExecutorService) eventbus executor}; thus the method will not block until these listeners have been executed.
     *
     * @param event The event to dispatch.
     */
    public void dispatch(NecrifyEvent event) {
        eventbus.post(event.setExecutingDispatcher(this));
    }

    /**
     * Dispatches the given event asynchronously. This method will not block until all listeners have been executed.
     * Listener methods registered with {@link org.greenrobot.eventbus.ThreadMode#ASYNC} or {@link ThreadMode#BACKGROUND} will be executed
     * asynchronously; thus the method will not block until these listeners have been executed.
     *
     * @param event The event to dispatch.
     * @return A future that will be completed when the event has been dispatched.
     */
    public CompletableFuture<Void> dispatchAsync(NecrifyEvent event) {
        var future = new CompletableFuture<Void>();
        executorService.execute(() -> {
            dispatch(event);
            future.complete(null);
        });
        return future;
    }

    /**
     * Registers the given listener to the event bus. The listener will be able to listen to events via subscribed methods.
     * These methods must be annotated with {@link Subscribe} and must have a single parameter of the event type.
     *
     * @param listener the listener instance to register.
     */
    public void register(@NotNull Object listener) {
        eventbus.register(Objects.requireNonNull(listener, "listener must not be null in order to get registered"));
    }

    /**
     * Unregisters the given listener from the event bus. The listener will no longer be able to listen to events.
     *
     * @param listener the listener instance to unregister.
     */
    public void unregister(@NotNull Object listener) {
        eventbus.unregister(Objects.requireNonNull(listener, "listener must not be null in order to get unregistered"));
    }

    /**
     * Cancels the given event. This method will prevent the event from being delivered to any new listeners, though it
     * may already have been delivered to some listeners.
     * <p>This method is implicitly called though {@link NecrifyEvent#cancel()}</p>
     *
     * @param event the event to cancel.
     */
    public void cancelEvent(@NotNull NecrifyEvent event) {
        eventbus.cancelEventDelivery(Objects.requireNonNull(event, "event must not be null in order to get cancelled"));
    }
}
