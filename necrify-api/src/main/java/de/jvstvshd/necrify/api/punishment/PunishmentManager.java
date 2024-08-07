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

import de.jvstvshd.necrify.api.duration.PunishmentDuration;
import de.jvstvshd.necrify.api.punishment.util.PlayerResolver;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.ApiStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * An interface for managing punishments.
 *
 * @see Punishment
 * @deprecated This interface's functionality will be superseded by the new
 * <a href="https://github.com/users/JvstvsHD/projects/5/views/1?pane=issue&itemId=65367115">NecrifyUser</a>.
 */
@Deprecated(since = "1.2.0", forRemoval = true)
@ApiStatus.ScheduledForRemoval(inVersion = "1.3.0")
public interface PunishmentManager {

    /**
     * Prepares a ban for a player with custom reason and duration.
     *
     * @param player   the uuid of the player which should be banned (by either {link Player#getUniqueId() or {@link PlayerResolver#getPlayerUuid(String)}.
     * @param reason   the reason given as a {@link Component}.
     * @param duration the duration, which can be created via {@link PunishmentDuration#parse(String)} when it's source is from minecraft commands.
     * @return the prepared ban with the given reason and duration. This duration remains the same at any duration since it is only added when the player is banned.
     * Only {@link Ban#punish()} is needed to execute the punishment.
     */
    Ban createBan(UUID player, Component reason, PunishmentDuration duration);

    /**
     * Prepares a ban for a player with custom reason and duration. Its only difference to {@link #createBan(UUID, Component, PunishmentDuration)} is, that the {@link PunishmentDuration}
     * is {@link PunishmentDuration#permanent()}.
     *
     * @param player the uuid of the player which should be banned (by either  Player#getUniqueId() ()} or {@link PlayerResolver#getPlayerUuid(String)}.
     * @param reason the reason given as a {@link Component}.
     * @return the prepared ban with the given reason and duration. The duration is permanent, equals {@link java.time.LocalDateTime#MAX}
     * Only {@link Ban#punish()} is needed to execute the punishment.
     */
    default Ban createPermanentBan(UUID player, Component reason) {
        return createBan(player, reason, PunishmentDuration.permanent());
    }

    /**
     * Prepares a mute for a player with custom reason and duration.
     *
     * @param player   the uuid of the player which should be banned (by either  Player#getUniqueId() or {@link PlayerResolver#getPlayerUuid(String)}.
     * @param reason   the reason given as a {@link Component}.
     * @param duration the duration, which can be created via {@link PunishmentDuration#parse(String)} when it's source is from minecraft commands.
     * @return the prepared ban with the given reason and duration. This duration remains the same at any duration since it is only added when the player is banned.
     * Only {@link Mute#punish()} is needed to execute the punishment.
     */
    Mute createMute(UUID player, Component reason, PunishmentDuration duration);

    /**
     * Prepares a ban for a player with custom reason and duration. Its only difference to {@link #createMute(UUID, Component, PunishmentDuration)} is, that the {@link PunishmentDuration}
     * is {@link PunishmentDuration#permanent()}.
     *
     * @param player the uuid of the player which should be banned (by either Player#getUniqueId() or {@link PlayerResolver#getPlayerUuid(String)}
     * @param reason the reason given as a {@link Component}.
     * @return the prepared ban with the given reason and duration. The duration is permanent, equals {@link java.time.LocalDateTime#MAX}
     * Only {@link Mute#punish()} is needed to execute the punishment.
     */
    default Mute createPermanentMute(UUID player, Component reason) {
        return createMute(player, reason, PunishmentDuration.permanent());
    }

    /**
     * This method queries all punishments with the given {@link UUID} of a player and returns them in a list.
     *
     * @param player the player whose punishments should be queried
     * @param <T>    the type of punishment(s), matching them in <code>type</code>
     * @return the list of punishments which are stored at the moment. This list may contains punishments which are over.
     */
    <T extends Punishment> CompletableFuture<List<T>> getPunishments(UUID player, Executor service, PunishmentType... type);

    /**
     * Queries the punishment stored with the given {@code punishmentId}
     *
     * @param punishmentId the punishment id from the punishment that should be queried
     * @param service      an {@link Executor} which will be used to perform async operations
     * @param <T>          the type of punishment
     * @return an {@link Optional} containing the queried punishment or {@link Optional#empty()} if it was not found
     * @deprecated This method is deprecated and will be removed in 2.0.0. Use {@link de.jvstvshd.necrify.api.Necrify#getPunishment(UUID)} instead.
     */
    <T extends Punishment> CompletableFuture<Optional<T>> getPunishment(UUID punishmentId, Executor service);

    /**
     * Checks whether the player specified via <code>playerUuid</code>. By default this method must not be overwritten and throws an {@link UnsupportedOperationException}
     * @param playerUuid the uuid of the player
     * @param executor   an {@link Executor} used for async operations
     * @return a {@link CompletableFuture}, being completed with true if this player is banned and false if not
     * @throws UnsupportedOperationException if the method was not overwritten
     */
    default CompletableFuture<Boolean> isBanned(UUID playerUuid, Executor executor) {
        throw new UnsupportedOperationException("method was not implemented");
    }
}
