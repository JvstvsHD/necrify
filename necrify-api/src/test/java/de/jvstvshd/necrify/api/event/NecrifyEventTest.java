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

import de.jvstvshd.necrify.api.event.state.NecrifyInitializedEvent;
import de.jvstvshd.necrify.api.event.state.NecrifyPreInitializationEvent;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;

public class NecrifyEventTest {

    private final static Logger logger = LoggerFactory.getLogger(NecrifyEventTest.class);
    private EventDispatcher dispatcher;

    @BeforeEach
    public void setup() {
        dispatcher = new EventDispatcher(Executors.newCachedThreadPool(), new Slf4jLogger(logger));
    }

    @Test
    public void testEventDispatch() {
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
