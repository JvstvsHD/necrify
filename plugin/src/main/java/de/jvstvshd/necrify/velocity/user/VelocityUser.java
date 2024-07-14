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
import de.chojo.sadu.mapper.wrapper.Row;
import de.chojo.sadu.queries.api.call.Call;
import de.chojo.sadu.queries.api.query.Query;
import de.jvstvshd.necrify.api.duration.PunishmentDuration;
import de.jvstvshd.necrify.api.event.user.UserDeletedEvent;
import de.jvstvshd.necrify.api.message.MessageProvider;
import de.jvstvshd.necrify.api.punishment.*;
import de.jvstvshd.necrify.api.user.NecrifyUser;
import de.jvstvshd.necrify.api.user.UserDeletionReason;
import de.jvstvshd.necrify.common.io.Adapters;
import de.jvstvshd.necrify.common.punishment.PunishmentBuilder;
import de.jvstvshd.necrify.velocity.NecrifyVelocityPlugin;
import de.jvstvshd.necrify.velocity.internal.Util;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.sql.DataSource;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
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
    private final NecrifyVelocityPlugin plugin;
    private boolean whitelisted;
    private String name;
    private Player player;

    public VelocityUser(@NotNull UUID uuid, @Nullable String name, boolean whitelisted, @Nullable Player player, NecrifyVelocityPlugin plugin) {
        this.whitelisted = whitelisted;
        this.plugin = plugin;
        this.punishments = new ArrayList<>();
        this.player = player;
        this.name = name;
        this.uuid = uuid;
        this.dataSource = plugin.getDataSource();
        this.service = plugin.getService();
        this.messageProvider = plugin.getMessageProvider();
        this.server = plugin.getServer();
    }

    public VelocityUser(@NotNull UUID uuid, @Nullable String name, boolean whitelisted, NecrifyVelocityPlugin plugin) {
        this.whitelisted = whitelisted;
        this.plugin = plugin;
        this.punishments = new ArrayList<>();
        this.server = plugin.getServer();
        this.player = server.getPlayer(uuid).orElse(null);
        this.name = name;
        this.uuid = uuid;
        this.dataSource = plugin.getDataSource();
        this.service = plugin.getService();
        this.messageProvider = plugin.getMessageProvider();

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
        return punish(PunishmentBuilder.newBuilder(plugin)
                .withDuration(duration)
                .withReason(reason)
                .withUser(this)
                .buildBan());
    }

    @Override
    public @NotNull Ban banPermanent(@Nullable Component reason) {
        return ban(reason, PunishmentDuration.permanent());
    }

    @Override
    public @NotNull Mute mute(@Nullable Component reason, @NotNull PunishmentDuration duration) {
        return punish(PunishmentBuilder.newBuilder(plugin)
                .withDuration(duration)
                .withReason(reason)
                .withUser(this)
                .buildMute());
    }

    @Override
    public @NotNull Mute mutePermanent(@Nullable Component reason) {
        return mute(reason, PunishmentDuration.permanent());
    }

    @Override
    public @NotNull Kick kick(@Nullable Component reason) {
        var kick = PunishmentBuilder.newBuilder(plugin)
                .withReason(reason)
                .withUser(this)
                .buildKick();
        kick.punish();
        return kick;
    }

    private <T extends Punishment> T punish(T punishment) {
        punishments.add(punishment);
        punishment.punish();
        return punishment;
    }

    @SuppressWarnings("unchecked")
    @Override
    public @NotNull <T extends Punishment> List<T> getPunishments(PunishmentType... types) {
        validatePunishments();
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

    private synchronized void validatePunishments() {
        punishments.removeIf(punishment -> !punishment.isOngoing());
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

    public Punishment addPunishment(Row row) throws SQLException {
        final StandardPunishmentType type = StandardPunishmentType.getById(row.getInt(1));
        final Timestamp timestamp = row.getTimestamp(2);
        final PunishmentDuration duration = PunishmentDuration.fromTimestamp(timestamp);
        final Component reason = MiniMessage.miniMessage().deserialize(row.getString(3));
        final UUID punishmentUuid = row.getObject(4, UUID.class);
        var builder = PunishmentBuilder.newBuilder(plugin)
                .withDuration(duration)
                .withReason(reason)
                .withUser(this)
                .withPunishmentUuid(punishmentUuid);
        Punishment punishment;
        switch (type) {
            case BAN, PERMANENT_BAN -> punishment = builder.buildBan();
            case MUTE, PERMANENT_MUTE -> punishment = builder.buildMute();
            case KICK -> punishment = builder.buildKick();
            default -> throw new UnsupportedOperationException("unhandled punishment type: " + type.getName());
        }
        punishments.add(punishment);
        return punishment;
    }

    @Override
    public boolean isWhitelisted() {
        return whitelisted;
    }

    @Override
    public void setWhitelisted(boolean whitelisted) {
        if (whitelisted == this.whitelisted)
            return;
        Util.executeAsync(() -> {
            Query.query("UPDATE punishment.necrify_user SET whitelisted = ? WHERE uuid = ?;")
                    .single(Call.of().bind(whitelisted).bind(uuid, Adapters.UUID_ADAPTER))
                    .update();
            this.whitelisted = whitelisted;
            if (!whitelisted && plugin.whitelistActive()) {
                kick(messageProvider.provide("whitelist.removed"));
            }
            return null;
        }, service);
    }

    public void setPlayer(Player player) {
        this.player = player;
    }

    public void addPunishment(Punishment punishment) {
        punishments.add(punishment);
    }

    public void removePunishment(Punishment punishment) {
        punishments.remove(punishment);
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof VelocityUser that)) return false;

        return uuid.equals(that.uuid);
    }

    @Override
    public int hashCode() {
        return uuid.hashCode();
    }

    @Override
    public String toString() {
        return "VelocityUser{" +
                "uuid=" + uuid +
                ", punishments=" + punishments +
                ", dataSource=" + dataSource +
                ", service=" + service +
                ", messageProvider=" + messageProvider +
                ", server=" + server +
                ", plugin=" + plugin +
                ", whitelisted=" + whitelisted +
                ", name='" + name + '\'' +
                ", player=" + player +
                '}';
    }

    @Override
    public void delete(@NotNull UserDeletionReason reason) {
        plugin.getEventDispatcher().dispatch(new UserDeletedEvent(this, reason));
        throw new UnsupportedOperationException("not implemented yet");
    }
}
