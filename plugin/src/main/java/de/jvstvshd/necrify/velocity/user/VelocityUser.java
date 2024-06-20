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

package de.jvstvshd.necrify.velocity.user;

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonParser;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import de.jvstvshd.necrify.api.duration.PunishmentDuration;
import de.jvstvshd.necrify.api.message.MessageProvider;
import de.jvstvshd.necrify.api.punishment.*;
import de.jvstvshd.necrify.api.user.NecrifyUser;
import de.jvstvshd.necrify.common.punishment.NecrifyBan;
import de.jvstvshd.necrify.common.punishment.NecrifyMute;
import de.jvstvshd.necrify.velocity.impl.VelocityKick;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.sql.DataSource;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

public class VelocityUser implements NecrifyUser {

    private final UUID uuid;
    private final List<Punishment> punishments;
    private final DataSource dataSource;
    private final ExecutorService service;
    private final MessageProvider messageProvider;
    private final ProxyServer server;
    private String name;
    private Player player;

    public VelocityUser(@NotNull UUID uuid, @Nullable String name, @Nullable Player player, List<Punishment> punishments, DataSource dataSource, ExecutorService service, MessageProvider messageProvider, ProxyServer server) {
        this.punishments = punishments;
        this.player = player;
        this.name = name;
        this.uuid = uuid;
        this.dataSource = dataSource;
        this.service = service;
        this.messageProvider = messageProvider;
        this.server = server;
    }

    @Override
    public @NotNull UUID getUuid() {
        return uuid;
    }

    @Override
    public @Nullable String getUsername() {
        return name;
    }

    @Override
    public @NotNull Ban ban(@Nullable Component reason, @NotNull PunishmentDuration duration) {
        return punish(new NecrifyBan(this, reason, dataSource, service, duration, messageProvider));
    }

    @Override
    public @NotNull Ban banPermanent(@Nullable Component reason) {
        return ban(reason, PunishmentDuration.permanent());
    }

    @Override
    public @NotNull Mute mute(@Nullable Component reason, @NotNull PunishmentDuration duration) {
        return punish(new NecrifyMute(this, reason, dataSource, service, duration, messageProvider));
    }

    @Override
    public @NotNull Mute mutePermanent(@Nullable Component reason) {
        return mute(reason, PunishmentDuration.permanent());
    }

    @Override
    public @NotNull Kick kick(@Nullable Component reason) {
        return new VelocityKick(this, reason, dataSource, service, UUID.randomUUID(), messageProvider);
    }

    private <T extends Punishment> T punish(T punishment) {
        punishments.add(punishment);
        punishment.punish();
        return punishment;
    }

    @SuppressWarnings("unchecked")
    @Override
    public @NotNull <T extends Punishment> List<T> getPunishments(PunishmentType... types) {
        if (types == null || types.length == 0)
            return (List<T>) ImmutableList.copyOf(punishments);
        return (List<T>) ImmutableList.copyOf(punishments.stream().filter(punishment -> {
            for (PunishmentType type : types) {
                if (punishment.getType().equals(type))
                    return true;
            }
            return false;
        }).toList());
    }

    @Override
    public @NotNull CompletableFuture<String> queryUsername(boolean update) {
        HttpClient httpClient = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder(URI.create("https://sessionserver.mojang.com/session/minecraft/profile/" + uuid)).GET().build();
        return httpClient
                .sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> JsonParser.parseString(response.body()).getAsJsonObject().get("name").getAsString())
                .thenApplyAsync(s -> {
                    if (update)
                        name = s;
                    return s;
                });
    }

    public Optional<Player> queryPlayer() {
        var opt = server.getPlayer(uuid);
        return opt.map(value -> player = value);
    }

    @Override
    public void sendMessage(@NotNull Component message) {
        queryPlayer().ifPresent(player -> player.sendMessage(message));
    }

    @Override
    public boolean hasPermission(@NotNull String permission) {
        return queryPlayer().map(player -> player.hasPermission(permission)).orElse(false);
    }

    public Player getPlayer() {
        return player;
    }
}
