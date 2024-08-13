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

package de.jvstvshd.necrify.velocity.impl;

import com.velocitypowered.api.proxy.ProxyServer;
import com.zaxxer.hikari.HikariDataSource;
import de.jvstvshd.necrify.api.duration.PunishmentDuration;
import de.jvstvshd.necrify.api.punishment.*;
import de.jvstvshd.necrify.api.user.NecrifyUser;
import de.jvstvshd.necrify.common.punishment.PunishmentBuilder;
import de.jvstvshd.necrify.common.util.Util;
import de.jvstvshd.necrify.velocity.NecrifyVelocityPlugin;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.ApiStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Deprecated(since = "1.2.0", forRemoval = true)
@ApiStatus.ScheduledForRemoval(inVersion = "1.3.0")
public class DefaultPunishmentManager implements PunishmentManager {

    private final ProxyServer proxyServer;
    private final HikariDataSource dataSource;
    private final ExecutorService service = Executors.newCachedThreadPool();
    private final NecrifyVelocityPlugin plugin;

    public DefaultPunishmentManager(ProxyServer proxyServer, HikariDataSource dataSource, NecrifyVelocityPlugin plugin) {
        this.proxyServer = proxyServer;
        this.dataSource = dataSource;
        this.plugin = plugin;
    }

    @Override
    public Ban createBan(UUID player, Component reason, PunishmentDuration duration) {
        return PunishmentBuilder.newBuilder(plugin)
                .withUser(getUser(player))
                .withReason(reason)
                .withDuration(duration)
                .buildBan();
    }

    @Override
    public Mute createMute(UUID player, Component reason, PunishmentDuration duration) {
        return PunishmentBuilder.newBuilder(plugin)
                .withUser(getUser(player))
                .withReason(reason)
                .withDuration(duration)
                .buildMute();
    }

    //temporary workaround so that this class still functions
    private NecrifyUser getUser(UUID uuid) {
        return plugin.getUserManager().loadOrCreateUser(uuid).join().get();
    }

    private CompletableFuture<NecrifyUser> loadUser(UUID uuid) {
        return plugin.getUserManager().loadOrCreateUser(uuid).thenApply(Optional::get);
    }

    @SuppressWarnings("unchecked")
    @Override
    public CompletableFuture<List<Punishment>> getPunishments(UUID player, Executor service, PunishmentType... types) {
        return loadUser(player).thenApplyAsync(necrifyUser -> necrifyUser.getPunishments(types));
    }

    @SuppressWarnings("unchecked")
    @Override
    public CompletableFuture<Optional<Punishment>> getPunishment(UUID punishmentId, Executor service) {
        return plugin.getPunishment(punishmentId);
    }


    public ProxyServer getServer() {
        return proxyServer;
    }

    public NecrifyVelocityPlugin plugin() {
        return plugin;
    }

    @Override
    public CompletableFuture<Boolean> isBanned(UUID playerUuid, Executor executor) {
        return Util.executeAsync(() -> !getPunishments(playerUuid, executor, StandardPunishmentType.TEMPORARY_BAN, StandardPunishmentType.PERMANENT_BAN).get().isEmpty(), executor);
    }
}
