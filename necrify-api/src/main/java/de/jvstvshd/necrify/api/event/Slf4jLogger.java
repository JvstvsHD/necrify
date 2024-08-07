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
