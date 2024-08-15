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

package de.jvstvshd.necrify.api.punishment.util;

import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

/**
 * Represents a data object that is capable of holding a reason. This can be used to store the reason of a punishment and to create
 * a full reason with all information, including the expiration date of the punishment.
 */
public interface ReasonHolder {

    /**
     * Gets the reason of this punishment. All placeholders will be replaced with their actual values.
     * @return the reason of this punishment as as component
     *
     */
    @NotNull
    Component getReason();

    /**
     * Gets the reason of this punishment. All placeholders will remain as they are.
     * @return the reason of this punishment as as component
     */
    @NotNull
    Component getRawReason();
    /**
     * Creates the full reason inclusive when the ban ends (or that the ban is permanent).
     *
     * @param locale the locale to use for the reason (or null for the default locale); current behavior is not using this.
     * @return the full reason with all information.
     */
    @NotNull
    Component createFullReason(@Nullable Locale locale);
}
