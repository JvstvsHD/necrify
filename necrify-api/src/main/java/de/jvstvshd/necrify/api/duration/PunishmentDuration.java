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

import org.jetbrains.annotations.ApiStatus;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.Temporal;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Class holding the duration and expiration date of a punishment.
 *
 * @see #parse(String)
 */
public interface PunishmentDuration extends Comparable<PunishmentDuration> {

    /**
     * Parses a string to a {@link PunishmentDuration} using {@link Parser}. More information about how a duration will be parsed can be found <a href="https://github.com/JvstvsHD/Necrify#duration">here</a>.
     *
     * @param source the source string.
     * @return the parsed duration
     * @throws Parser.ParseException if... <ul>
     *                                          <li>the source string is empty</li>
     *                                          <li>the source string does not contain parsable tokens</li>
     *                                          <li>the source string does not contain a unit character after a number</li>
     *                                          <li>the source string contains an unknown unit character</li>
     *                                          <li>the numeric value is negative</li>
     *                                      </ul>
     * @see Parser#parse()
     */
    static PunishmentDuration parse(String source) {
        return new Parser(source).parse();
    }

    /**
     * A constant for a permanent punishment duration. The expiration date is 31.12.9999, 23:59:59.
     */
    PunishmentDuration PERMANENT = PermanentPunishmentDuration.PERMANENT;

    /**
     * Creates a new permanent (expiration date: 31.12.9999, 23:59:59) absolute punishment duration.
     *
     * @return a permanent duration
     * @deprecated since 1.2.2, use {@link #PERMANENT} instead
     */
    @ApiStatus.ScheduledForRemoval(inVersion = "1.3.0")
    @Deprecated(forRemoval = true, since = "1.2.2")
    static PunishmentDuration permanent() {
        return PERMANENT;
    }

    /**
     * Creates a new absolute punishment duration with the given expiration date.
     *
     * @param ldt the expiration date
     * @return a new absolute punishment duration
     * @throws IllegalArgumentException if the expiration date is in the past
     * @since 1.0.1
     */
    static PunishmentDuration from(LocalDateTime ldt) {
        return AbsolutePunishmentDuration.from(ldt);
    }

    /**
     * Converts the given {@link Timestamp} into a {@link PunishmentDuration}. The duration is absolute because it already was before.
     *
     * @param timestamp the timestamp which should be converted
     * @return the converted duration
     */
    static PunishmentDuration fromTimestamp(Timestamp timestamp) {
        return from(timestamp.toLocalDateTime());
    }

    /**
     * Converts the given milliseconds into a {@link PunishmentDuration}. The duration is relative the given amount into
     * the future from a given point in time. It is absolute as soon as a punishment is enforced.
     *
     * @param millis how long (in milliseconds) the punishment should last
     * @return the converted duration
     */
    static PunishmentDuration fromMillis(long millis) {
        return fromDuration(Duration.ofMillis(millis));
    }

    /**
     * Converts the given {@link Duration} into a {@link PunishmentDuration}. The duration is relative the given length
     * into the future from a given point in time. It is absolute as soon as a punishment is enforced.
     *
     * @param duration how long the punishment should last
     * @return the converted duration
     */
    static PunishmentDuration fromDuration(Duration duration) {
        var rpd = new RelativePunishmentDuration(duration);
        if (rpd.isPermanent())
            return PermanentPunishmentDuration.PERMANENT;
        return rpd;
    }

    /**
     * Whether this duration is permanent meaning the expiration should be 31.12.9999, 23:59:59.
     *
     * @return whether this duration is permanent
     * @see PermanentPunishmentDuration#PERMANENT
     * @see AbsolutePunishmentDuration#MAX
     * @see AbsolutePunishmentDuration#MAX_TIMESTAMP
     */
    boolean isPermanent();

    /**
     * Ensures this {@link PunishmentDuration} is absolute. If it is already absolute, this instance will be returned.
     * If it is relative, it will be converted to an absolute duration.
     *
     * @return an absolute duration
     * @see AbsolutePunishmentDuration
     */
    AbsolutePunishmentDuration absolute();

    /**
     * Ensures this {@link PunishmentDuration} is relative. If it is already relative, this instance will be returned.
     * If it is absolute, it will be converted to a relative duration.
     *
     * @return a relative duration
     * @see RelativePunishmentDuration
     */
    RelativePunishmentDuration relative();

    /**
     * The expiration date of this duration.
     *
     * @return the expiration date
     */
    LocalDateTime expiration();

    /**
     * The expiration date of this duration as {@link Timestamp}.
     *
     * @return the duration
     * @since 1.0.1
     */
    Timestamp expirationAsTimestamp();

    /**
     * Formats the expiration date of this duration in the following format as {@link String}, if not otherwise specified: {@code dd/MM/yyyy HH:mm:ss}.
     *
     * @return the end of this punishment as string
     * @since 1.0.1
     */
    String expirationAsString();

    /**
     * Formats the remaining duration in the same format as parsed by {@link Parser}, unless specified otherwise.
     *
     * @return the remaining duration
     */
    String remainingDuration();

    /**
     * The initial duration (before any changes to the punishment this duration was created for).
     *
     * @return the initial duration
     * @throws UnsupportedOperationException default; if this method is not implemented by its underlying implementation
     * @since 1.0.1
     */
    //TODO support this since there is Punishment#creationTime and #totalDuration
    default PunishmentDuration initialDuration() {
        throw new UnsupportedOperationException("Initial durations are not stored.");
    }

    /**
     * The remaining duration as a {@link Duration} object.
     * @return the remaining duration
     * @since 1.2.0
     * @see Duration#between(Temporal, Temporal)
     */
    default Duration javaDuration() {
        return Duration.between(LocalDateTime.now(), expiration());
    }

    /**
     * A class for parsing player inputs into a valid {@link PunishmentDuration}.
     * Allowed format: <b>{@code \d+[smhdSMHD]}</b> (e.g. 1m (one minute), 2d (two days), 1d6h (one day and six hours) or 1h30m (one hour and thirty minutes))
     */
    class Parser {
        private final static Map<Character, TimeUnit> characterMapping = new HashMap<>() {
            {
                put('s', TimeUnit.SECONDS);
                put('m', TimeUnit.MINUTES);
                put('h', TimeUnit.HOURS);
                put('d', TimeUnit.DAYS);
            }
        };
        private final String source;
        private final Map<TimeUnit, Long> rawDuration;

        public Parser(String source) {
            this.source = source;
            this.rawDuration = new HashMap<>();
        }

        /**
         * Converts a string into multiple {@link TimeUnit}s and their corresponding values. For retrieving the values, use {@link #parse()}.
         *
         * @see #parse()
         */
        public void convert() {
            String[] numbers = source.split("[sSmMhHdD]");
            Map<TimeUnit, Long> converted = new HashMap<>();
            int index = 0;
            for (String number : numbers) {
                index += number.length();
                final long numericValue;
                try {
                    numericValue = Long.parseLong(number);
                } catch (NumberFormatException e) {
                    throw new ParseException("Not a number: " + e.getMessage());
                }
                if (numericValue < 0)
                    throw new ParseException("Illegal numeric value: " + numericValue);
                final char unit;
                try {
                    unit = Character.toLowerCase(source.charAt(index));
                } catch (IndexOutOfBoundsException e) {
                    throw new ParseException("Number is not followed by unit marking character.");
                }
                TimeUnit timeUnit = characterMapping.get(unit);
                if (timeUnit == null)
                    throw new ParseException("Unknown time unit for character '" + unit + "'");
                converted.put(timeUnit, numericValue);
                index++;
            }
            rawDuration.clear();
            rawDuration.putAll(converted);
        }

        /**
         * Parses the source string into a {@link PunishmentDuration}. This usually is a {@link RelativePunishmentDuration}
         * since the player input contains only relative information and no absolute expiration date.
         *
         * @return the parsed duration
         */
        public PunishmentDuration parse() {
            if (source.isEmpty())
                throw new ParseException("Source string is empty.");
            convert();
            if (rawDuration.isEmpty()) {
                throw new ParseException("Converted map is empty.");
            }
            return fromMillis(durationToMillis());
        }

        private long convertToMillis(TimeUnit unit, long value) {
            return unit.toMillis(value);
        }

        private long durationToMillis() {
            long total = 0;
            for (Map.Entry<TimeUnit, Long> entry : rawDuration.entrySet()) {
                total += convertToMillis(entry.getKey(), entry.getValue());
            }
            return total;
        }

        /**
         * An exception that is thrown when a player input could not be parsed into a valid {@link PunishmentDuration}.
         */
        public static class ParseException extends RuntimeException {
            public ParseException(String message) {
                super(message);
            }
        }
    }
}
