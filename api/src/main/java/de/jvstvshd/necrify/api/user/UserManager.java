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
import org.jetbrains.annotations.Nullable;

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
     * @return the user or null if not found
     */
    @Nullable
    NecrifyUser getUser(@NotNull UUID uuid);

    /**
     * Loads the user when online (platform player is wrapped into an user instance, only when online) or returns the
     * user if already loaded or cached.
     *
     * @param name the name of the user
     * @param name
     * @return the user or null if not found
     * @return
     */
    @Nullable
    NecrifyUser getUser(@NotNull String name);

    /**
     * Loads the user from the underlying storage asynchronously. If the user is online or cached, this method will return
     * the result of {@link #getUser(UUID)} as a {@link CompletableFuture#completedFuture(Object) completed future}.
     *
     * @param uuid the uuid of the user
     * @return a future containing the user or null if not found
     */
    @NotNull
    CompletableFuture<@Nullable NecrifyUser> loadUser(@NotNull UUID uuid);

    /**
     * Loads the user from the underlying storage asynchronously. If the user is online or cached, this method will return
     * the result of {@link #getUser(String)} as a {@link CompletableFuture#completedFuture(Object) completed future}.
     *
     * @param name the name of the user
     * @return a future containing the user or null if not found
     */
    @NotNull
    CompletableFuture<@Nullable NecrifyUser> loadUser(@NotNull String name);
}
