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
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * A punishment duration that is relative to the current time meaning its expiration date is never fixed.
 * To retrieve the expiration date, use {@link #expiration()} or use {@link #absolute()} to convert this instance into a absolut punishment duration.
 *
 * @since 1.0.1
 */
public class RelativePunishmentDuration implements PunishmentDuration {

    private final Duration duration;

    RelativePunishmentDuration(Duration duration) {
        this.duration = duration;
    }

    /**
     * A {@link PunishmentDuration} whose duration is 0ms.
     *
     * @return a non-duration {@link PunishmentDuration}
     */
    public static PunishmentDuration zero() {
        return new RelativePunishmentDuration(Duration.ofMillis(0));
    }

    @Override
    public boolean isPermanent() {
        return !expiration().isBefore(LocalDateTime.MAX);
    }

    @Override
    public AbsolutePunishmentDuration absolute() {
        return new AbsolutePunishmentDuration(expiration());
    }

    @Override
    public RelativePunishmentDuration relative() {
        return this;
    }

    @Override
    public LocalDateTime expiration() {
        return LocalDateTime.now().plus(duration);
    }

    @Override
    public Timestamp expirationAsTimestamp() {
        return Timestamp.valueOf(expiration());
    }

    @Override
    public String expirationAsString() {
        return absolute().expirationAsString();
    }

    @Override
    public String remainingDuration(StringRepresentation mode) {
        return representDuration(Duration.between(LocalDateTime.now(), expiration()), mode);
    }

    @Override
    public String toString() {
        return "PunishmentDuration{" +
                "duration=" + duration +
                ", expiration=" + expiration() +
                '}';
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (RelativePunishmentDuration) obj;
        return Objects.equals(this.duration, that.duration);
    }

    @Override
    public int hashCode() {
        return duration.hashCode();
    }

    @Override
    public int compareTo(@NotNull PunishmentDuration other) {
        return duration.compareTo(other.relative().javaDuration());
    }

    private String representDuration(Duration duration, StringRepresentation mode) {
        List<String> values = Stream.concat(Stream.of(String.valueOf(duration.toDaysPart())),
                Stream.of(duration.toHoursPart(), duration.toMinutesPart(), duration.toSecondsPart()).map(this::normalizeTimeUnit)).toList();
        List<Character> units = Arrays.asList('d', 'h', 'm', 's');
        var l = IntStream.range(0, values.size())
                .boxed()
                .collect(Collectors.toMap(units::get, values::get, (s, s2) -> s + s2, LinkedHashMap::new)).entrySet();
        //using an unordered Map implementation like HashMap causes the entry set to be unordered which produces a random ordered output string
        var valuesMapped = l.stream();
        Stream<Map.Entry<Character, String>> stream = switch (mode) {
            case FULL -> valuesMapped;
            case SHORT -> valuesMapped.filter(entry -> !zeroDuration(entry.getValue()));
            case LONG -> {
                int skip = 0;
                for (int i = 0; i < values.size(); i++) {
                    if (skip != i) break; //only remove trailing 0-value time units
                    if (zeroDuration(values.get(i))) skip++;
                }
                yield valuesMapped.skip(skip);
            }
        };
        return stream.map(entry -> entry.getValue() + entry.getKey()).collect(Collectors.joining(""));
    }

    private boolean zeroDuration(String duration) {
        return duration.chars().allMatch(value -> value == '0');
    }

    private String normalizeTimeUnit(long value) {
        String s = String.valueOf(value);
        if (value == 0)
            return "00";
        if (s.length() < 2)
            return "0" + s;
        return s;
    }
}
