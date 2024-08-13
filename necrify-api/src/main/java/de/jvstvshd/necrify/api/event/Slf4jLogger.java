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

import org.greenrobot.eventbus.Logger;

import java.util.logging.Level;

/**
 * An adapter for the SLF4J logger to the GreenRobot EventBus logger.
 * This logger will delegate all messages to the SLF4J logger.
 * The level conversion is as follows:
 * <table border="1">
 *     <caption>Level conversion</caption>
 *     <tr>
 *         <th>Java Util Logging Level</th>
 *         <th>SLF4J Level</th>
 *     </tr>
 *     <tr>
 *         <td>SEVERE</td>
 *         <td>ERROR</td>
 *     </tr>
 *     <tr>
 *         <td>WARNING</td>
 *         <td>WARN</td>
 *     </tr>
 *     <tr>
 *         <td>INFO, CONFIG</td>
 *         <td>INFO</td>
 *     </tr>
 *     <tr>
 *         <td>FINER, FINEST, ALL</td>
 *         <td>TRACE</td>
 *     </tr>
 *     <tr>
 *         <td>Other</td>
 *         <td>DEBUG</td>
 *     </tr>
 *     <tr>
 *         <td>OFF</td>
 *         <td>No logging</td>
 *     </tr>
 * </table>
 *
 * @see Logger
 * @see org.slf4j.Logger
 * @since 1.2.0
 */
public class Slf4jLogger implements Logger {

    private final org.slf4j.Logger logger;

    public Slf4jLogger(org.slf4j.Logger logger) {
        this.logger = logger;
    }

    @Override
    public void log(Level level, String msg) {
        if (level == Level.OFF)
            return;
        logger.atLevel(convertLevel(level)).log(msg);
    }

    @Override
    public void log(Level level, String msg, Throwable th) {
        if (level == Level.OFF)
            return;
        logger.atLevel(convertLevel(level)).setCause(th).log(msg);
    }

    private org.slf4j.event.Level convertLevel(Level level) {
        return switch (level.getName()) {
            case "SEVERE" -> org.slf4j.event.Level.ERROR;
            case "WARNING" -> org.slf4j.event.Level.WARN;
            case "INFO", "CONFIG" -> org.slf4j.event.Level.INFO;
            case "FINER", "FINEST", "ALL" -> org.slf4j.event.Level.TRACE;
            default -> org.slf4j.event.Level.DEBUG;
        };
    }
}
