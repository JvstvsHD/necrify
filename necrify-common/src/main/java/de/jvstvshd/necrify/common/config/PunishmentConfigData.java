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

package de.jvstvshd.necrify.common.config;

import java.util.Map;

public class PunishmentConfigData {

    private final Map<Integer, String> punishmentMessages;

    public PunishmentConfigData(Map<Integer, String> punishmentMessages) {
        this.punishmentMessages = punishmentMessages;
    }

    public PunishmentConfigData() {
        this.punishmentMessages = Map.of(
                1, "<red>You are banned.",
                2, "<red>You were permanently banned.",
                3, "<red>You were muted.",
                4, "<red>You were permanently muted.",
                5, "<red>You were kicked.");
    }

    public Map<Integer, String> getPunishmentMessages() {
        return punishmentMessages;
    }
}
