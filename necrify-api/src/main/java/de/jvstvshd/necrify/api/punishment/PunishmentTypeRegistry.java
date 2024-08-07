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
