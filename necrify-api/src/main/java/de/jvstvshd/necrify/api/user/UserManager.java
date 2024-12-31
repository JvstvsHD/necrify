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

package de.jvstvshd.necrify.api.user;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Manages the users of the plugin. This includes loading, creating and caching users. The user manager is responsible for
 * the user's lifecycle and thus also for the accuracy and actuality of the user's data, including their punishments.
 * All user instances created by this interface should be cached.
 */
public interface UserManager {

    /**
     * Loads the user when online (platform player is wrapped into an user instance, only when online) or returns the
     * user if already loaded or cached.
     *
     * @param uuid the uuid of the user
     * @return the user or an empty Optional if not found
     */
    @NotNull
    Optional<NecrifyUser> getUser(@NotNull UUID uuid);

    /**
     * Loads the user when online (platform player is wrapped into an user instance, only when online) or returns the
     * user if already loaded or cached.
     *
     * @param player the user's name or uuid
     * @return the user or an empty Optional if not found
     */
    @NotNull
    Optional<NecrifyUser> getUser(@NotNull String player);

    /**
     * Loads the user from the underlying storage asynchronously. If the user is online or cached, this method will return
     * the result of {@link #getUser(UUID)} as a {@link CompletableFuture#completedFuture(Object) completed future}.
     *
     * @param uuid the uuid of the user
     * @return a future containing the user or an empty Optional if not found
     */
    @NotNull
    CompletableFuture<Optional<NecrifyUser>> loadUser(@NotNull UUID uuid);

    /**
     * Loads the user from the underlying storage asynchronously. If the user is online or cached, this method will return
     * the result of {@link #getUser(String)} as a {@link CompletableFuture#completedFuture(Object) completed future}.
     *
     * @param player the user's name or uuid
     * @return a future containing the user or an empty Optional if not found
     */
    @NotNull
    CompletableFuture<Optional<NecrifyUser>> loadUser(@NotNull String player);

    /**
     * Creates a new user with the given uuid. If the user already exists, this method will fail.
     *
     * @param uuid the uuid of the user
     * @return a future containing the user or an empty Optional if there is no Minecraft account associated with the uuid
     * @throws IllegalStateException if the user already exists
     */
    @NotNull
    CompletableFuture<Optional<NecrifyUser>> createUser(@NotNull UUID uuid);

    /**
     * Creates a new user with the given name. If the user already exists, this method will fail.
     *
     * @param player the user's name or uuid
     * @return a future containing the user or an empty Optional if there is no Minecraft account associated with the name
     * @throws IllegalStateException if the user already exists
     */
    @NotNull
    CompletableFuture<Optional<NecrifyUser>> createUser(@NotNull String player);

    /**
     * Loads the user with the given uuid or creates a new user if not found. At first, the cache is checked for the user.
     * If there is no result, the user is loaded from the underlying storage. If the user is not found, a new user is created.
     *
     * @param uuid the uuid of the user
     * @return a future containing the user or an empty Optional if there is no Minecraft account associated with the uuid
     */
    @NotNull
    CompletableFuture<Optional<NecrifyUser>> loadOrCreateUser(@NotNull UUID uuid);

    /**
     * Loads the user with the given name or creates a new user if not found. At first, the cache is checked for the user.
     * If there is no result, the user is loaded from the underlying storage. If the user is not found, a new user is created.
     *
     * @param player the user's name or uuid
     * @return a future containing the user or an empty Optional if there is no Minecraft account associated with the name
     */
    @NotNull
    CompletableFuture<Optional<NecrifyUser>> loadOrCreateUser(@NotNull String player);

    /**
     * Returns all loaded users.
     *
     * @return a collection of all loaded users
     */
    @NotNull Collection<? extends NecrifyUser> getLoadedUsers();
}