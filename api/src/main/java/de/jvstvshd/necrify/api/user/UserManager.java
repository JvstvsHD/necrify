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

package de.jvstvshd.necrify.api.user;

import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 *
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
     * Puts the user into the cache and the underlying storage.
     *
     * @param user the user to put
     */
    void putUser(@NotNull NecrifyUser user);
}
