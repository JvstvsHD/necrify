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

package de.jvstvshd.necrify.velocity.config;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Locale;

public class ConfigData {

    @JsonProperty("database")
    @JsonAlias("dataBaseData")
    private final DataBaseData dataBaseData;

    @JsonProperty("default-language")
    private final Locale defaultLanguage;

    @JsonProperty("punishment")
    private final PunishmentConfigData punishmentConfigData;

    @JsonProperty("whitelist-activated")
    @JsonAlias("whitelistActivated")
    private boolean whitelistActivated;

    @JsonProperty("allow-top-level-commands")
    @JsonAlias("allowTopLevelCommands")
    private boolean allowTopLevelCommands;

    public ConfigData(DataBaseData dataBaseData, Locale defaultLanguage, PunishmentConfigData punishmentConfigData, boolean whitelistActivated, boolean allowTopLevelCommands) {
        this.dataBaseData = dataBaseData;
        this.defaultLanguage = defaultLanguage;
        this.punishmentConfigData = punishmentConfigData;
        this.whitelistActivated = whitelistActivated;
        this.allowTopLevelCommands = allowTopLevelCommands;
    }

    public ConfigData() {
        this(new DataBaseData(), Locale.ENGLISH, new PunishmentConfigData(), false, true);
    }

    public final DataBaseData getDataBaseData() {
        return dataBaseData;
    }

    public Locale getDefaultLanguage() {
        return defaultLanguage;
    }

    public boolean isWhitelistActivated() {
        return whitelistActivated;
    }

    public void setWhitelistActivated(boolean whitelistActivated) {
        this.whitelistActivated = whitelistActivated;
    }

    public boolean isAllowTopLevelCommands() {
        return allowTopLevelCommands;
    }

    public void setAllowTopLevelCommands(boolean allowTopLevelCommands) {
        this.allowTopLevelCommands = allowTopLevelCommands;
    }

    public PunishmentConfigData getPunishmentConfigData() {
        return punishmentConfigData;
    }
}
