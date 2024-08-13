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

package de.jvstvshd.necrify.common.user;

import de.chojo.sadu.mapper.wrapper.Row;
import de.jvstvshd.necrify.api.duration.PunishmentDuration;
import de.jvstvshd.necrify.api.punishment.Punishment;
import de.jvstvshd.necrify.api.punishment.PunishmentTypeRegistry;
import de.jvstvshd.necrify.api.punishment.StandardPunishmentType;
import de.jvstvshd.necrify.api.user.NecrifyUser;
import de.jvstvshd.necrify.api.user.UserLoadOrderCoordinator;
import de.jvstvshd.necrify.common.util.Util;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.*;

public final class UserLoader {

    private final List<Map<String, Object>> data = new ArrayList<>();
    private final NecrifyUser user;

    public UserLoader(NecrifyUser user) {
        this.user = user;
    }

    public void addData(Map<String, Object> data) {
        this.data.add(data);
    }

    public Void addDataFromRow(Row row) {
        try {
            final StandardPunishmentType type = PunishmentTypeRegistry.getType(row.getInt(1)).standard();
            final Timestamp timestamp = row.getTimestamp(2);
            final PunishmentDuration duration = PunishmentDuration.fromTimestamp(timestamp);
            final Component reason = MiniMessage.miniMessage().deserialize(row.getString(3));
            final UUID punishmentUuid = Util.getUuid(row, 4);
            final UUID successorId = Util.getUuid(row, 5);
            final LocalDateTime issuedAt = row.getTimestamp(6).toLocalDateTime();
            var data = new HashMap<String, Object>() {
                {
                    put("type", type);
                    put("duration", duration);
                    put("reason", reason);
                    put("punishmentUuid", punishmentUuid);
                    put("user", user);
                    put("successorId", successorId);
                    put("issued_at", issuedAt);
                }
            };
            addData(data);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return null;
    }

    public List<Punishment> loadPunishments() {
        var ordered = UserLoadOrderCoordinator.topologicalSort(data);
        Map<UUID, Punishment> loaded = new HashMap<>();
        for (Map<String, Object> dataMap : ordered) {
            var type = (StandardPunishmentType) dataMap.get("type");
            if (dataMap.get("successorId") != null) {
                var successor = loaded.get((UUID) dataMap.get("successorId"));
                dataMap.put("successor", successor);
            }
            var punishment = PunishmentTypeRegistry.createPunishment(type, dataMap);
            loaded.put(punishment.getPunishmentUuid(), punishment);
        }
        return new ArrayList<>(loaded.values());

    }

    private String c(Punishment p) {
        if (p == null) {
            return "null";
        }
        return p.getPunishmentUuid().toString();
    }

    public NecrifyUser getUser() {
        return user;
    }

    public List<Map<String, Object>> getData() {
        return data;
    }
}
