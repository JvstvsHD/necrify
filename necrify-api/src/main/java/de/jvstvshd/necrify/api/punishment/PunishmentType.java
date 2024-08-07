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
package de.jvstvshd.necrify.api.punishment;

import de.jvstvshd.necrify.api.Necrify;
import org.jetbrains.annotations.NotNull;

import java.util.List;
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

    /**
     * Creates a list that contains all punishment types that are related to this one in the sense that they are
     * similar in nature. This is useful for e.g. a permanent ban being related to a temporary ban when having to check
     * something for both types.
     * @return a list of related punishment types
     * @since 1.2.0
     */
    @NotNull
    List<PunishmentType> getRelatedTypes();
}
