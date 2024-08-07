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

import de.jvstvshd.necrify.api.event.state.NecrifyInitializedEvent;
import de.jvstvshd.necrify.api.event.state.NecrifyPreInitializationEvent;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;

public class NecrifyEventTest {

    private final static Logger logger = LoggerFactory.getLogger(NecrifyEventTest.class);
    private EventDispatcher dispatcher;

    @BeforeEach
    public void setup() {
        dispatcher = new EventDispatcher(Executors.newCachedThreadPool(), new Slf4jLogger(logger));
    }

    @Test
    public void testEventDispatch() {CompletableFuture.supplyAsync(() -> {throw new RuntimeException("so");}).thenApply((object) -> object).whenComplete((object, throwable) -> System.out.println(throwable));
        dispatcher.register(this);
        dispatcher.dispatch(new NecrifyPreInitializationEvent());
        dispatcher.dispatch(new NecrifyInitializedEvent());
    }

    @Subscribe(threadMode = ThreadMode.ASYNC)
    public void onEvent(NecrifyInitializedEvent event) {
        logger.info(Thread.currentThread().getName());
        logger.info("Necrify initialized.");
    }

    @Subscribe(threadMode = ThreadMode.POSTING, priority = 3)
    public void onEventHigherPriority(NecrifyPreInitializationEvent event) {
        logger.info(Thread.currentThread().getName());
        logger.info("Necrify initialized. Higher priority.");
    }

    @Subscribe(threadMode = ThreadMode.POSTING, priority = 2)
    public void onEventLowerPriority(NecrifyPreInitializationEvent event) {
        logger.info(Thread.currentThread().getName());
        logger.info("Necrify initialized. Lower priority.");
        event.cancel();
        logger.info("Event cancelled.");
    }

    @Subscribe(threadMode = ThreadMode.POSTING, priority = 1)
    public void onEventLowestPriority(NecrifyPreInitializationEvent event) {
        throw new IllegalStateException("This event should not be called.");
    }
}
