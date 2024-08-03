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
