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
package de.jvstvshd.necrify.api.punishment.util;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * An interface used to retrieve player names und uuids used for storing them in a database.
 * To retrieve a player's uuid, for example, there are three different ways:<br>
 * 1.: {@link #getPlayerUuid(String)}: returns the uuid of the player via {@link Optional} or {@link Optional#empty()} if this player is not online/cached/etc.<br>
 * 2.: {@link #queryPlayerUuid(String, Executor)} queries the player uuid via the mojang api or some internal methods.<br>
 * 3.: {@link #getOrQueryPlayerName(UUID, Executor)}: a combination of the first two possibilities. If the Optional returned by {@link #getPlayerUuid(String)} was empty,
 * {@link #getOrQueryPlayerUuid(String, Executor)} is called. The result of this operation will be returned.
 * @deprecated This interface's functionality will be superseded by the new
 * <a href="https://github.com/users/JvstvsHD/projects/5/views/1?pane=issue&itemId=65367115">NecrifyUser</a>.
 */
@Deprecated(since = "1.2.0", forRemoval = true)
@ApiStatus.ScheduledForRemoval(inVersion = "1.3.0")
public interface PlayerResolver {

    /**
     * Retrieves the player's name via  com.velocitypowered.api.proxy.ProxyServer#getPlayer(UUID) (on Velocity), a caching mechanism or related things.
     *
     * @param uuid the uuid of the player
     * @return the name, or {@link Optional#empty()}
     */
    Optional<String> getPlayerName(@NotNull UUID uuid);

    /**
     * Queries the player's name, for example through the Mojang API
     *
     * @param uuid     the uuid of the player
     * @param executor an executor to compute async operations
     * @return a {@link CompletableFuture} being completed with the player's name or null, if not found.
     */
    CompletableFuture<String> queryPlayerName(@NotNull UUID uuid, @NotNull Executor executor);

    /**
     * At first, {@link #getPlayerName(UUID)} is invoked. If the result is empty, the result of {@link #queryPlayerName(UUID, Executor)} will be returned.
     *
     * @param uuid     the uuid of the player
     * @param executor an executor to compute async operations
     * @return a {@link CompletableFuture} being completed with the player's name or null, if not found.
     */
    CompletableFuture<String> getOrQueryPlayerName(@NotNull UUID uuid, @NotNull Executor executor);

    /**
     * Retrieves the player's uuid via com.velocitypowered.api.proxy.ProxyServer#getPlayer(String) (on velocity), a caching mechanism or related things.
     *
     * @param name the name of the player
     * @return the name, or {@link Optional#empty()}
     */
    Optional<UUID> getPlayerUuid(@NotNull String name);

    /**
     * Queries the player's uuid, for example through the Mojang API
     *
     * @param name     the name of the player
     * @param executor an executor to compute async operations
     * @return a {@link CompletableFuture} being completed with the player's uuid or null, if not found.
     */
    CompletableFuture<UUID> queryPlayerUuid(@NotNull String name, @NotNull Executor executor);

    /**
     * At first, {@link #getPlayerUuid(String)} is invoked. If the result is empty, the result of {@link #queryPlayerUuid(String, Executor)} will be returned.
     *
     * @param name     the uuid of the player
     * @param executor an executor to compute async operations
     * @return a {@link CompletableFuture} being completed with the player's name or null, if not found.
     */
    CompletableFuture<UUID> getOrQueryPlayerUuid(@NotNull String name, @NotNull Executor executor);
}
