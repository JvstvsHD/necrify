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

import de.jvstvshd.necrify.api.PunishmentException;
import de.jvstvshd.necrify.api.punishment.util.ReasonHolder;
import de.jvstvshd.necrify.api.user.NecrifyUser;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Super interface for all sort of punishments..<br>
 * To punish a player, obtain a {@link NecrifyUser} instance from {@link de.jvstvshd.necrify.api.user.UserManager} and
 * select an adequate method (e.g. {@link NecrifyUser#banPermanent(Component)}) for the punishment you wish.<br>
 */
public interface Punishment extends ReasonHolder {

    /**
     * Determines whether the expiration of this punishment is after {@link LocalDateTime#now()} or not.
     *
     * @return true, if the expiration date is after now, otherwise false
     */
    boolean isOngoing();

    /**
     * Punishes the player finally.
     *
     * @return a {@link CompletableFuture} containing the exerted punishment
     * @throws PunishmentException if the punishment could not be exerted (inside the future)
     */
    CompletableFuture<Punishment> punish();

    /**
     * Cancels this punishment thus allowing the player e.g. to join the server
     *
     * @return a {@link CompletableFuture} containing the cancelled punishment
     * @throws PunishmentException if the punishment could not be cancelled (inside the future)
     */
    CompletableFuture<Punishment> cancel();

    /**
     * Changes the duration and reason of this punishment. This method can be used if a player created an appeal an it was accepted.
     *
     * @param newReason   the new reason which should be displayed to the player or null if the reason should not be changed
     * @return a {@link CompletableFuture} containing the new punishment
     * @throws PunishmentException if the punishment could not be changed
     * @see #cancel()
     */
    CompletableFuture<Punishment> change(@Nullable Component newReason);

    /**
     * Returns the type of this punishment. By default, this is a field from {@link StandardPunishmentType}.
     *
     * @return the type of this punishment
     * @see StandardPunishmentType
     */
    @NotNull
    PunishmentType getType();

    /**
     * @return the id of this punishment.
     */
    @NotNull
    UUID getPunishmentUuid();

    /**
     * This will return the value of {@link NecrifyUser#getUuid()} from {@link #getUser()}.
     *
     * @return the uuid of the punished player.
     * @since 1.0.1
     */
    @NotNull
    UUID getUuid();

    /**
     * @return the user this punishment is affecting
     * @since 1.2.0
     */
    @NotNull
    NecrifyUser getUser();
}
