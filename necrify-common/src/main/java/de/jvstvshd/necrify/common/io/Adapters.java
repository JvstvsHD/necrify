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

package de.jvstvshd.necrify.common.io;

import de.chojo.sadu.core.conversion.UUIDConverter;
import de.chojo.sadu.queries.api.call.adapter.Adapter;
import de.chojo.sadu.queries.api.call.adapter.AdapterMapping;
import de.chojo.sadu.queries.call.adapter.UUIDAdapter;

import java.sql.JDBCType;
import java.sql.PreparedStatement;
import java.sql.Types;
import java.util.Objects;
import java.util.UUID;

public class Adapters {

    public static final Adapter<UUID> UUID_ADAPTER =
            Adapter.create(UUID.class, (preparedStatement, parameterIndex, x) -> {
                switch (NecrifyDatabase.SQL_TYPE) {
                    case "postgres", "postgresql" -> preparedStatement.setObject(parameterIndex, x);
                    //For unknown reasons, MariaDB does not like setObject for UUIDs.
                    default -> preparedStatement.setBytes(parameterIndex, UUIDConverter.convert(x));
                }
            }, Types.BINARY);

    public static final Adapter<UUID> UUID_NULL_ADAPTER = new Adapter<UUID>() {
        @Override
        public AdapterMapping<UUID> mapping() {
            return null;
        }

        @Override
        public int type() {
            return Types.NULL;
        }
    };
}
