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

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

public class MojangAPI {

    @NotNull
    public static Optional<String> getPlayerName(@NotNull UUID uuid) throws IOException, InterruptedException {
        try (HttpClient httpClient = HttpClient.newHttpClient()) {
            HttpRequest request = HttpRequest.newBuilder(URI.create("https://sessionserver.mojang.com/session/minecraft/profile/" + uuid)).GET().build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return getPlayerName(response);
        }
    }

    @NotNull
    public static CompletableFuture<Optional<String>> getPlayerNameAsync(@NotNull UUID uuid, ExecutorService executor) {
        try (HttpClient httpClient = HttpClient.newBuilder().executor(executor).build()) {
            HttpRequest request = HttpRequest.newBuilder(URI.create("https://sessionserver.mojang.com/session/minecraft/profile/" + uuid)).GET().build();
            return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString()).thenApply(MojangAPI::getPlayerName);
        }
    }

    @NotNull
    private static Optional<String> getPlayerName(HttpResponse<String> response) {
        if (response.statusCode() != 200) {
            return Optional.empty();
        }
        JsonElement jsonElement = JsonParser.parseString(response.body());
        var nameElement = jsonElement.getAsJsonObject().get("name");
        if (nameElement == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(nameElement.getAsString());
    }


    @NotNull
    public static Optional<UUID> getUuid(@NotNull String name) throws IOException, InterruptedException {
        try (HttpClient httpClient = HttpClient.newHttpClient()) {
            HttpRequest request = HttpRequest.newBuilder(URI.create("https://api.mojang.com/users/profiles/minecraft/" + name)).GET().build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                return Optional.empty();
            }
            JsonElement jsonElement = JsonParser.parseString(response.body());
            if (jsonElement == null || jsonElement.isJsonNull()) {
                return Optional.empty();
            }
            var idElement = jsonElement.getAsJsonObject().get("id");
            if (idElement == null) {
                return Optional.empty();
            }
            var result = idElement.getAsString();
            UUID uuid;
            try {
                uuid = UUID.fromString(result);
            } catch (IllegalArgumentException e) {
                uuid = UUID.fromString(result.replaceAll(
                        "(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})",
                        "$1-$2-$3-$4-$5"));
            }
            return Optional.of(uuid);
        }
    }
}
