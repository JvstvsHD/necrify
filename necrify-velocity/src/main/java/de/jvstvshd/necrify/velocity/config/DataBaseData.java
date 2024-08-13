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
import de.chojo.sadu.core.databases.Database;
import de.chojo.sadu.mariadb.databases.MariaDb;
import de.chojo.sadu.mysql.databases.MySql;
import de.chojo.sadu.postgresql.databases.PostgreSql;
import de.chojo.sadu.sqlite.databases.SqLite;

import java.util.Locale;

public class DataBaseData {
    private final String host;
    private final String password;
    private final String username;
    private final String database;
    private final String port;
    @JsonProperty("sql-type")
    @JsonAlias("sqlType")
    private final String sqlType;

    @JsonProperty("max-pool-size")
    @JsonAlias("maxPoolSize")
    private final int maxPoolSize;

    @JsonProperty("min-idle")
    @JsonAlias("minIdle")
    private final int minIdle;

    private final String postgresSchema;

    public DataBaseData(String host, String password, String username, String database, String port, String sqlType, int maxPoolSize, int minIdle, String postgresSchema) {
        this.host = host;
        this.password = password;
        this.username = username;
        this.database = database;
        this.port = port;
        this.sqlType = sqlType;
        this.maxPoolSize = maxPoolSize;
        this.minIdle = minIdle;
        this.postgresSchema = postgresSchema;
    }

    public DataBaseData() {
        this("localhost", "password", "username", "database", "5432", "sqlite", 10, 5, "punishment");
    }

    public String getHost() {
        return host;
    }

    public String getPassword() {
        return password;
    }

    public String getUsername() {
        return username;
    }

    public String getDatabase() {
        return database;
    }

    public String getPort() {
        return port;
    }

    public int getMaxPoolSize() {
        return maxPoolSize;
    }

    public int getMinIdle() {
        return minIdle;
    }

    public String getSqlType() {
        return sqlType;
    }

    public String getPostgresSchema() {
        return postgresSchema;
    }

    @SuppressWarnings("UnstableApiUsage")
    public Database<?, ?> sqlType() {
        return switch (sqlType.toLowerCase(Locale.ROOT)) {
            case "mariadb" -> MariaDb.get();
            case "mysql" -> MySql.get();
            case "postgres", "postgresql" -> PostgreSql.get();
            case "sqlite" -> SqLite.get();
            default -> throw new IllegalStateException("Unexpected value: " + sqlType.toLowerCase(Locale.ROOT));
        };
    }
}
