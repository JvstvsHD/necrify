/*
 * This file is part of Necrify (formerly Velocity Punishment), a plugin designed to manage player's punishments for the platforms Velocity and partly Paper.
 * Copyright (C) 2022-2025 JvstvsHD
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

package de.jvstvshd.necrify.api.template;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * This interface is responsible for loading and creating {@link NecrifyTemplate}s.
 *
 * @since 1.2.3
 */
public interface TemplateManager {

    /**
     * Loads all templates from the storage. This action is not meant to get called often and should only be used
     * on start-up.
     *
     * @return a {@link CompletableFuture} containing all loaded templates or an error
     */
    @NotNull
    CompletableFuture<Collection<NecrifyTemplate>> loadTemplates();

    /**
     * Tries to retrieve a template with the given name.
     *
     * @param name the name of the template
     * @return an {@link Optional} containing the template if it exists
     */
    @NotNull
    Optional<NecrifyTemplate> getTemplate(String name);

    /**
     * Creates a new template with the given name. This operation is executed asynchronously and might throw an
     * {@link IllegalArgumentException} if the name is invalid or already taken.
     *
     * @param name the name of the template
     * @return a {@link CompletableFuture} containing the created template
     * @throws IllegalArgumentException if the name is invalid or already taken
     */
    @NotNull
    CompletableFuture<NecrifyTemplate> createTemplate(String name);
}
