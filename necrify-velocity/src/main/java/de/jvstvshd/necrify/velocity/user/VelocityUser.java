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

package de.jvstvshd.necrify.velocity.user;

import com.google.common.collect.ImmutableList;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import de.chojo.sadu.queries.api.call.Call;
import de.chojo.sadu.queries.api.query.Query;
import de.jvstvshd.necrify.api.duration.PunishmentDuration;
import de.jvstvshd.necrify.api.event.user.UserDeletedEvent;
import de.jvstvshd.necrify.api.message.MessageProvider;
import de.jvstvshd.necrify.api.punishment.*;
import de.jvstvshd.necrify.api.punishment.log.PunishmentLog;
import de.jvstvshd.necrify.api.user.NecrifyUser;
import de.jvstvshd.necrify.api.user.UserDeletionReason;
import de.jvstvshd.necrify.common.io.Adapters;
import de.jvstvshd.necrify.common.punishment.PunishmentBuilder;
import de.jvstvshd.necrify.common.punishment.log.NecrifyPunishmentLog;
import de.jvstvshd.necrify.common.user.MojangAPI;
import de.jvstvshd.necrify.common.util.Util;
import de.jvstvshd.necrify.velocity.NecrifyVelocityPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.sql.DataSource;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

public class VelocityUser implements NecrifyUser {

    private final UUID uuid;
    private final List<Punishment> punishments;
    private final DataSource dataSource;
    private final ExecutorService executor;
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
        this.executor = plugin.getExecutor();
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
        this.executor = plugin.getExecutor();
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
    public @NotNull CompletableFuture<Ban> ban(@Nullable Component reason, @NotNull PunishmentDuration duration) {
        return punish(PunishmentBuilder.newBuilder(plugin)
                .withDuration(duration)
                .withReason(reason)
                .withUser(this)
                .buildBan());
    }

    @Override
    public @NotNull CompletableFuture<Ban> banPermanent(@Nullable Component reason) {
        return ban(reason, PunishmentDuration.permanent());
    }

    @Override
    public @NotNull CompletableFuture<Mute> mute(@Nullable Component reason, @NotNull PunishmentDuration duration) {
        return punish(PunishmentBuilder.newBuilder(plugin)
                .withDuration(duration)
                .withReason(reason)
                .withUser(this)
                .buildMute());
    }

    @Override
    public @NotNull CompletableFuture<Mute> mutePermanent(@Nullable Component reason) {
        return mute(reason, PunishmentDuration.permanent());
    }

    @Override
    public @NotNull CompletableFuture<Kick> kick(@Nullable Component reason) {
        var kick = PunishmentBuilder.newBuilder(plugin)
                .withReason(reason)
                .withUser(this)
                .buildKick();
        kick.punish();
        return CompletableFuture.completedFuture(kick);
    }

    //We're just returning the same instance that was passed in via 'punishment', so we can safely cast it to T.
    @SuppressWarnings("unchecked")
    private <T extends Punishment> CompletableFuture<T> punish(T punishment) {
        punishments.add(punishment);
        return (CompletableFuture<T>) punishment.punish().whenComplete((ignored, throwable) -> {
            if (throwable != null) {
                plugin.getLogger().error("An error occurred while punishing user {}", punishment.getUser().getUuid(), throwable);
                punishment.getUser().sendErrorMessage();
            }
        });
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
        return MojangAPI.getPlayerNameAsync(uuid, executor).thenApplyAsync(s -> {
            if (update)
                name = s.orElse(null);
            return s.orElse(null);
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
    public void sendMessage(@NotNull String key, TextColor color, Component... args) {
        sendMessage(messageProvider.provide(key, args).color(color));
    }

    @Override
    public void sendErrorMessage() {
        sendMessage(messageProvider.internalError());
    }

    @Override
    public boolean hasPermission(@NotNull String permission) {
        return queryPlayer().map(player -> player.hasPermission(permission)).orElse(false);
    }

    public Player getPlayer() {
        return player;
    }

    @Override
    public boolean isWhitelisted() {
        return whitelisted;
    }

    @Override
    public CompletableFuture<Boolean> setWhitelisted(boolean whitelisted) {
        if (whitelisted == this.whitelisted)
            CompletableFuture.completedFuture(false);
        return de.jvstvshd.necrify.common.util.Util.executeAsync(() -> {
            Query.query("UPDATE necrify_user SET whitelisted = ? WHERE uuid = ?;")
                    .single(Call.of().bind(whitelisted).bind(uuid, Adapters.UUID_ADAPTER))
                    .update();
            this.whitelisted = whitelisted;
            if (!whitelisted && plugin.isWhitelistActive()) {
                kick(messageProvider.provide("whitelist.removed").color(NamedTextColor.RED));
            }
            return true;
        }, executor);
    }

    public void setPlayer(Player player) {
        this.player = player;
    }

    public void addPunishment(Punishment punishment) {
        if (punishments.contains(punishment))
            return;
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
                ", service=" + executor +
                ", messageProvider=" + messageProvider +
                ", server=" + server +
                ", plugin=" + plugin +
                ", whitelisted=" + whitelisted +
                ", name='" + name + '\'' +
                ", player=" + player +
                '}';
    }

    @Override
    public CompletableFuture<Integer> delete(@NotNull UserDeletionReason reason) {
        plugin.getEventDispatcher().dispatch(new UserDeletedEvent(this, reason));
        return Util.executeAsync(() -> Query
                .query("DELETE FROM necrify_user WHERE uuid = ?;")
                .single(Call.of().bind(uuid, Adapters.UUID_ADAPTER))
                .delete().rows() + punishments.size(), executor);
    }

    @Override
    public @NotNull Locale getLocale() {
        var defaultLocale = plugin.getConfig().getConfiguration().getDefaultLanguage();
        if (player != null) {
            return Objects.requireNonNullElse(player.getEffectiveLocale(), defaultLocale);
        }
        return defaultLocale;
    }
}
