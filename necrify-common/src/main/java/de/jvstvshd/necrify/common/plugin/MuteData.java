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
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.jetbrains.annotations.ApiStatus;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Represents a data object that is used to transfer mute data between sub-servers. This is used to synchronize mute data between sub-servers
 * and this proxy. Since Minecraft 1.19.1, chat messages cannot be cancelled on the proxy, so this must be done on the sub-servers.
 * <p>Mute information status updates are sent to the sub-servers using their plugin messaging channels.</p>
 *
 * <b>Deprecation notice:</b> The fields {@link MuteData#ADD}, {@link MuteData#REMOVE} and {@link MuteData#UPDATE} are deprecated in favor
 * of usage of {@link MuteData#RECALCULATION} and {@link MuteData#RESET}. The former ones are only used for compatibility reasons.
 */
public class MuteData {

    public static final String MUTE_DATA_CHANNEL_IDENTIFIER = "necrify:mutedata";
    public static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    @Deprecated(forRemoval = true)
    @ApiStatus.ScheduledForRemoval(inVersion = "1.3.0")
    public static final int ADD = 0;
    @Deprecated(forRemoval = true)
    @ApiStatus.ScheduledForRemoval(inVersion = "1.3.0")
    public static final int REMOVE = 1;
    @Deprecated(forRemoval = true)
    @ApiStatus.ScheduledForRemoval(inVersion = "1.3.0")
    public static final int UPDATE = 2;

    /**
     * The protocol version of the mute data. This is used to ensure that the data is correctly interpreted by the sub-servers.
     * Unexpected results may occur if the version of incoming data does not match.
     */
    public static final int PROTOCOL_VERSION = 1;

    /**
     * Starting from 1.2.0-rc.1, this and {@link #RECALCULATION} are the only type that is actively used. All other ones exist merely for compatibility reasons.
     * <p>Used to indicate that all mute data for a user should be reset. This requires only the field {@link MuteData#getUuid()} to be set.</p>
     */
    public static final int RESET = 3;
    /**
     * Starting from 1.2.0-rc.1, this and {@link #RESET} are the only type that is actively used. All other ones exist merely for compatibility reasons.
     * <p>Used to let sub-servers know that the mute status of a user has changed. This requires all fields of
     * {@link MuteData#MuteData(int, UUID, String, LocalDateTime, UUID, int)} to be set, though {@link MuteData#getPunishmentId()}
     * may be omitted.</p>
     */
    public static final int RECALCULATION = 4;
    public static final int UNKNOWN = -1;

    private final UUID uuid;
    private final String reason;
    private final LocalDateTime expiration;
    private final int type;
    private final UUID punishmentId;
    private final int version;

    public MuteData(@JsonProperty("type") int type,
                    @JsonProperty("uuid") UUID uuid,
                    @JsonProperty("reason") String reason,
                    @JsonProperty("expiration") LocalDateTime expiration,
                    @JsonProperty("punishment_id") UUID punishmentId,
                    @JsonProperty("version") int version) {
        this.uuid = uuid;
        this.reason = reason;
        this.expiration = expiration;
        this.type = type;
        this.punishmentId = punishmentId;
        this.version = version;
    }

    public MuteData(UUID uuid, String reason, LocalDateTime expiration, int type, UUID punishmentId) {
        this.uuid = uuid;
        this.reason = reason;
        this.expiration = expiration;
        this.type = type;
        this.punishmentId = punishmentId;
        this.version = PROTOCOL_VERSION;
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

    public int getVersion() {
        return version;
    }

    @Deprecated
    @JsonIgnore
    public boolean isAdd() {
        return type == ADD;
    }

    @Deprecated
    @JsonIgnore
    public boolean isRemove() {
        return type == REMOVE;
    }

    @Deprecated
    @JsonIgnore
    public boolean isUpdate() {
        return type == UPDATE;
    }

    public UUID getPunishmentId() {
        return punishmentId;
    }
}
