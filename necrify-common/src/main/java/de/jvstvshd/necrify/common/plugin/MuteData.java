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
package de.jvstvshd.necrify.common.plugin;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.time.LocalDateTime;
import java.util.UUID;

public class MuteData {

    public static final String MUTE_DATA_CHANNEL_IDENTIFIER = "necrify:mutedata";
    public static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().registerModule(new JavaTimeModule());
    public static final int ADD = 0;
    public static final int REMOVE = 1;
    public static final int UPDATE = 2;
    public static final int RESET = 3;
    public static final int UNKNOWN = -1;

    private final UUID uuid;
    private final String reason;
    private final LocalDateTime expiration;
    private final int type;
    private final UUID punishmentId;

    public MuteData(@JsonProperty("uuid") UUID uuid, @JsonProperty("reason") String reason,
                    @JsonProperty("expiration") LocalDateTime expiration, @JsonProperty("type") int type, @JsonProperty("punishment_id") UUID punishmentId) {
        this.uuid = uuid;
        this.reason = reason;
        this.expiration = expiration;
        this.type = type;
        this.punishmentId = punishmentId;
    }

    public UUID getUuid() {
        return uuid;
    }

    public String getReason() {
        return reason;
    }

    public LocalDateTime getExpiration() {
        return expiration;
    }

    public int getType() {
        return type;
    }

    @JsonIgnore
    public boolean isAdd() {
        return type == ADD;
    }

    @JsonIgnore
    public boolean isRemove() {
        return type == REMOVE;
    }

    @JsonIgnore
    public boolean isUpdate() {
        return type == UPDATE;
    }

    public UUID getPunishmentId() {
        return punishmentId;
    }
}
