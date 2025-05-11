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

import java.sql.Timestamp;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * A punishment duration that has an absolute expiration date.
 *
 * @since 1.0.1
 */
public class AbsolutePunishmentDuration implements PunishmentDuration {

    /**
     * The maximum supported {@code LocalDateTime} value (9999-12-31T23:59:59).
     */
    public static final LocalDateTime MAX = LocalDateTime.of(9999, 12, 31, 23, 59, 59);

    /**
     * The maximum supported {@code Timestamp} value (9999-12-31T23:59:59).
     */
    public static final Timestamp MAX_TIMESTAMP = Timestamp.valueOf(MAX);
    private final LocalDateTime expiration;

    AbsolutePunishmentDuration(LocalDateTime expiration) {
        this.expiration = expiration;
    }

    /**
     * Creates a new {@code AbsolutePunishmentDuration} instance from the given {@code LocalDateTime}. The returned
     * duration expires at the given date and time.
     * @param ldt the expiration date and time
     * @return a new {@code AbsolutePunishmentDuration} instance
     */
    public static PunishmentDuration from(LocalDateTime ldt) {
        if (!ldt.isBefore(MAX))
            return PermanentPunishmentDuration.PERMANENT;
        return new AbsolutePunishmentDuration(ldt);
    }

    @Override
    public boolean isPermanent() {
        return !expiration.isBefore(MAX);
    }

    @Override
    public AbsolutePunishmentDuration absolute() {
        return this;
    }

    @Override
    public RelativePunishmentDuration relative() {
        return new RelativePunishmentDuration(Duration.between(LocalDateTime.now(), expiration));
    }

    @Override
    public LocalDateTime expiration() {
        return expiration;
    }

    @Override
    public Timestamp expirationAsTimestamp() {
        return Timestamp.valueOf(expiration);
    }

    @Override
    public String expirationAsString() {
        return expiration().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));
    }

    @Override
    public String remainingDuration(StringRepresentation mode) {
        return relative().remainingDuration(mode);
    }

    @Override
    public int compareTo(@NotNull PunishmentDuration other) {
        return expiration.compareTo(other.expiration());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        AbsolutePunishmentDuration that = (AbsolutePunishmentDuration) o;

        return expiration.equals(that.expiration);
    }

    @Override
    public int hashCode() {
        return expiration.hashCode();
    }

    @Override
    public String toString() {
        return "AbsolutePunishmentDuration{" +
                "expiration=" + expiration +
                '}';
    }
}
