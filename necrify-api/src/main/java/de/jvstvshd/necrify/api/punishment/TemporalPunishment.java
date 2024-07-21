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
import de.jvstvshd.necrify.api.duration.PunishmentDuration;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

/**
 * An interface containing some methods to only punish a player for a defined duration.
 *
 * @see Ban
 * @see Mute
 */
public interface TemporalPunishment extends Punishment {

    /**
     * @return the duration of the underlying punishment
     */
    PunishmentDuration getDuration();

    /**
     * @return true if the punishment is permanent, otherwise false
     * @see PunishmentDuration#isPermanent()
     */
    boolean isPermanent();

    /**
     * Changes the duration and reason of this punishment. This method can be used if a player created an appeal an it was accepted.
     *
     * @param newDuration the new duration of this punishment
     * @param newReason   the new reason which should be displayed to the player, or null if it should remain the same
     * @return a {@link CompletableFuture} containing the new punishment
     * @see #cancel()
     * @see #change(Component)
     */
    CompletableFuture<Punishment> change(@NotNull PunishmentDuration newDuration, @Nullable Component newReason) throws PunishmentException;

    @Override
    default CompletableFuture<Punishment> change(Component newReason) throws PunishmentException {
        return change(getDuration(), newReason);
    }
}
