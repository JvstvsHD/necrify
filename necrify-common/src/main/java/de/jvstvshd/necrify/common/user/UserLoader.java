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
            final UUID punishmentUuid = row.getObject(4, UUID.class);
            final UUID successorId = row.getObject(5, UUID.class);
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
