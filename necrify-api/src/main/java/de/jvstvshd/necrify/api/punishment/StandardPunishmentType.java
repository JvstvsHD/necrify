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

package de.jvstvshd.necrify.api.punishment;

import de.jvstvshd.necrify.api.Necrify;
import de.jvstvshd.necrify.api.duration.PunishmentDuration;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

import java.sql.Timestamp;
import java.util.Map;
import java.util.UUID;

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
}