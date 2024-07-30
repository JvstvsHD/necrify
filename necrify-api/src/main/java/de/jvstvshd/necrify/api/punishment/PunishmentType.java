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

package de.jvstvshd.necrify.api.punishment;

import de.jvstvshd.necrify.api.Necrify;

import java.util.Map;

/**
 * Represents a type of punishment.
 * @since 1.0.0
 */
public interface PunishmentType {

    /**
     * Gets the name of the punishment type.
     * @return the name of the punishment type.
     */
    String getName();

    /**
     * Gets the ID of the punishment type. This ID is unique for each punishment type.
     * @since 1.2.0
     * @return the ID of the punishment type.
     */
    int getId();

    /**
     * Determines whether the punishment is a mute or not.
     *
     * @return true if the punishment is a mute, false otherwise.
     * @since 1.0.1
     */
    default boolean isMute() {
        return this == StandardPunishmentType.TEMPORARY_MUTE || this == StandardPunishmentType.PERMANENT_MUTE;
    }

    /**
     * Determines whether the punishment is a ban or not.
     *
     * @return true if the punishment is a ban, false otherwise.
     * @since 1.0.1
     */
    default boolean isBan() {
        return this == StandardPunishmentType.TEMPORARY_BAN || this == StandardPunishmentType.PERMANENT_BAN;
    }

    /**
     * Returns this value as an instance of {@link StandardPunishmentType} if it is a standard punishment type or throws otherwise.
     * @return the standard punishment type.
     * @throws IllegalStateException if the punishment type is not a standard punishment type.
     * @since 1.2.0
     */
    default StandardPunishmentType standard() {
        if (this instanceof StandardPunishmentType) {
            return (StandardPunishmentType) this;
        } else {
            throw new IllegalStateException("Punishment type is not a standard punishment type.");
        }
    }
}