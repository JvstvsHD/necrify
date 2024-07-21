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

    @JsonProperty("whitelist-activated")
    @JsonAlias("whitelistActivated")
    private boolean whitelistActivated;

    @JsonProperty("allow-top-level-commands")
    @JsonAlias("allowTopLevelCommands")
    private boolean allowTopLevelCommands;

    public ConfigData(DataBaseData dataBaseData, Locale defaultLanguage, boolean whitelistActivated, boolean allowTopLevelCommands) {
        this.dataBaseData = dataBaseData;
        this.defaultLanguage = defaultLanguage;
        this.whitelistActivated = whitelistActivated;
        this.allowTopLevelCommands = allowTopLevelCommands;
    }

    public ConfigData() {
        this(new DataBaseData(), Locale.ENGLISH, false, true);
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
}
