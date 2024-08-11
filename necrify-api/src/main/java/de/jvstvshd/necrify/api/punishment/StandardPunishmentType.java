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

import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * A collection of standard punishment types which are also used by the default implementation of the punishment system.
 */
public enum StandardPunishmentType implements PunishmentType {

    TEMPORARY_BAN(false, "BAN", 1),
    PERMANENT_BAN(true, "PERMANENT_BAN", 2),
    TEMPORARY_MUTE(false, "MUTE", 3),
    PERMANENT_MUTE(true, "PERMANENT_MUTE", 4),
    KICK(false, "KICK", 5);

    private final boolean isPermanent;
    private final String typeString;
    private final int id;

    StandardPunishmentType(boolean isPermanent, String typeString, int id) {
        this.isPermanent = isPermanent;
        this.typeString = typeString;
        this.id = id;
    }

    /**
     * Checks if the punishment type is permanent.
     * @return {@code true} if the punishment type is permanent, {@code false} otherwise.
     */
    public boolean isPermanent() {
        return isPermanent;
    }

    @Override
    public String getName() {
        return typeString;
    }

    @Override
    public int getId() {
        return id;
    }

    @Override
    public @NotNull List<PunishmentType> getRelatedTypes() {
        return switch (this) {
            case TEMPORARY_BAN, PERMANENT_BAN -> List.of(TEMPORARY_BAN, PERMANENT_BAN);
            case TEMPORARY_MUTE, PERMANENT_MUTE -> List.of(TEMPORARY_MUTE, PERMANENT_MUTE);
            default -> List.of(this);
        };
    }
}
