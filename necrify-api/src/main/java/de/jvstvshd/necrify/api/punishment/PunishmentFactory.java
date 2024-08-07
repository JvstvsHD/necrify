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

import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * A factory to create punishments of a specific type.
 * @since 1.2.0
 */
@FunctionalInterface
public interface PunishmentFactory {

    /**
     * Creates a new punishment of the given type with the given data.
     * @param type the type of the punishment
     * @param data the data to create the punishment with
     * @return the created punishment
     */
    @NotNull
    Punishment createPunishment(@NotNull PunishmentType type, @NotNull Map<String, Object> data);
}
