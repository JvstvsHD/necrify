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

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * A central registry for {@link PunishmentType}s. This class is used to register and retrieve punishment types by their integer IDs.
 * @since 1.2.0
 */
public final class PunishmentTypeRegistry {

    private static final Map<Integer, PunishmentType> types = new HashMap<>();
    private static final Map<PunishmentType, PunishmentFactory> punishmentFactories = new HashMap<>();

    /**
     * Registers a new {@link PunishmentType} with the given ID and factory to create punishments of this type.
     * @param type the type to register
     * @param factory the factory to create punishments of this type
     */
    public static void registerType(PunishmentType type, PunishmentFactory factory) {
        types.put(type.getId(), type);
        punishmentFactories.put(type, factory);
    }

    /**
     * Retrieves a {@link PunishmentType} by its ID.
     * @param id the ID of the type
     * @return the type or null if not found
     */
    public static PunishmentType getType(int id) {
        return types.get(id);
    }

    /**
     * Creates a new punishment of the given type with the given data.
     * @param type the type of the punishment
     * @param data the data to create the punishment with
     * @return the created punishment
     * @throws IllegalStateException if no factory is registered for the given type
     * @throws IllegalArgumentException if the data is invalid or incomplete
     */
    @NotNull
    public static Punishment createPunishment(@NotNull PunishmentType type, @NotNull Map<String, Object> data) {
        PunishmentFactory factory = punishmentFactories.get(type);
        if (factory == null) {
            throw new IllegalStateException("No factory registered for punishment type: " + type.getName());
        }
        return factory.createPunishment(type, data);
    }

    /**
     * Creates a new punishment of the given type with the given data.
     * @param id the ID of the type
     * @param data the data to create the punishment with
     * @return the created punishment
     * @throws IllegalArgumentException if no punishment type is found for the given ID
     * @throws IllegalStateException if no factory is registered for the found type
     */
    @NotNull
    public static Punishment createPunishment(int id, @NotNull Map<String, Object> data) {
        PunishmentType type = getType(id);
        if (type == null) {
            throw new IllegalArgumentException("No punishment type found for ID: " + id);
        }
        return createPunishment(type, data);
    }

    private PunishmentTypeRegistry() {
        throw new UnsupportedOperationException("This class cannot be instantiated.");
    }
}
