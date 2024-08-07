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
