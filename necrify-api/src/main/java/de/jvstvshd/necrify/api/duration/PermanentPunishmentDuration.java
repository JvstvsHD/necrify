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
package de.jvstvshd.necrify.api.duration;

import org.jetbrains.annotations.NotNull;

import java.time.LocalDateTime;

/**
 * A permanent punishment duration (whose expiration date is {@link #MAX}.
 *
 * @since 1.0.1
 */
public class PermanentPunishmentDuration extends AbsolutePunishmentDuration implements PunishmentDuration {

    /**
     * A constant for a permanent punishment duration.
     */
    public static final PermanentPunishmentDuration PERMANENT = new PermanentPunishmentDuration();

    private PermanentPunishmentDuration() {
        super(MAX);
    }

    @Override
    public boolean isPermanent() {
        return true;
    }

    @Override
    public LocalDateTime expiration() {
        return MAX;
    }

    @Override
    public String expirationAsString() {
        return "Permanent";
    }

    @Override
    public String remainingDuration() {
        return "Permanent";
    }

    /**
     * @param other the object to be compared.
     * @return either 0 if the other object is also a permanent punishment duration or -1 if not.
     */
    @SuppressWarnings("ComparatorMethodParameterNotUsed")
    @Override
    public int compareTo(@NotNull PunishmentDuration other) {
        return other.isPermanent() ? 0 : -1;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof PunishmentDuration pd && pd.isPermanent();
    }

    @Override
    public int hashCode() {
        return MAX.hashCode();
    }

    @Override
    public String toString() {
        return "PermanentPunishmentDuration{} " + super.toString();
    }
}
