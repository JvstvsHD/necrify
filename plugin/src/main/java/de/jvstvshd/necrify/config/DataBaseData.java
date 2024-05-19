/*
 * This file is part of Velocity Punishment, which is licensed under the MIT license.
 *
 * Copyright (c) 2022 JvstvsHD
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

package de.jvstvshd.necrify.config;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import de.chojo.sadu.databases.*;

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
        this("localhost", "password", "username", "database", "5432", "postgres", 10, 5, "punishment");
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
