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
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.NoSuchElementException;
import java.util.concurrent.CompletableFuture;

/**
 * Super interface for all kick implementation.<br>
 * Unsupported operations:
 * <ul>
 *     <li>{@link Punishment#cancel()}</li>
 *     <li>{@link Punishment#change(Component)}</li>
 * </ul>
 */
public interface Kick extends Punishment {

    @Override
    default boolean isOngoing() {
        return false;
    }

    @Override
    default CompletableFuture<Punishment> cancel() throws PunishmentException {
        throw new UnsupportedOperationException("kick lasts only one moment");
    }

    @Override
    default CompletableFuture<Punishment> change(Component newReason) throws PunishmentException {
        throw new UnsupportedOperationException("kick lasts only one moment");
    }

    @Override
    default boolean hasSuccessor() {
        return false;
    }

    @Override
    @NotNull
    default Punishment getSuccessor() {
        throw new NoSuchElementException("kicks do not have a successor");
    }

    @NotNull
    @Override
    default CompletableFuture<Punishment> setSuccessor(@NotNull Punishment successor) {
        throw new UnsupportedOperationException("kick lasts only one moment");
    }

    @Override
    @Nullable
    default Punishment getPredecessor() {
        return null;
    }
}
