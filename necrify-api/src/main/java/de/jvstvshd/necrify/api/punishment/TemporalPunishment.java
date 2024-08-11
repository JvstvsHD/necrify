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

import de.jvstvshd.necrify.api.PunishmentException;
import de.jvstvshd.necrify.api.duration.PunishmentDuration;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;

/**
 * An interface containing some methods to only punish a player for a defined duration.
 * @implSpec Instances of this interface must expire sometime in the future. This means that you may use non-permanent
 * {@link PunishmentDuration punishment durations} for this type of punishment; but you may also use
 * {@link de.jvstvshd.necrify.api.duration.PermanentPunishmentDuration permanent ones} as long as they expire sometime,
 * which may be the year 10,000 or so. If your punishment does not expire or has no infinite duration, use {@link Punishment} instead.
 *
 * @see Ban
 * @see Mute
 */
public interface TemporalPunishment extends Punishment {

    /**
     * Returns the duration of this punishment. This only provides information about when the punishment will end, not its
     * creation time.
     * @return the duration of the underlying punishment
     */
    @NotNull
    PunishmentDuration getDuration();

    /**
     * Whether or not this punishment is permanent. This is equivalent to {@code getDuration().isPermanent()}.
     * @return true if the punishment is permanent, otherwise false
     * @see PunishmentDuration#isPermanent()
     */
    boolean isPermanent();

    /**
     * Changes the duration and reason of this punishment. This method can be used if a player created an appeal an it was accepted.
     *
     * @param newDuration  the new duration of the punishment (relative to the point of punishment issuance)
     * @param creationTime the new creation time of the punishment, or null if it should remain the same
     * @param newReason    the new reason which should be displayed to the player, or null if it should remain the same
     * @return a {@link CompletableFuture} containing the new punishment
     * @see #cancel()
     * @see #change(Component)
     */
    @NotNull
    CompletableFuture<Punishment> change(@NotNull PunishmentDuration newDuration, @Nullable LocalDateTime creationTime, @Nullable Component newReason) throws PunishmentException;

    @Override
    default CompletableFuture<Punishment> change(@Nullable Component newReason) {
        return null;
    }

    /**
     * Returns the total duration of this punishment from {@link #getCreationTime()} until the expiration date.
     *
     * @return the total duration of this punishment
     * @since 1.2.0
     */
    @NotNull
    PunishmentDuration totalDuration();
}
