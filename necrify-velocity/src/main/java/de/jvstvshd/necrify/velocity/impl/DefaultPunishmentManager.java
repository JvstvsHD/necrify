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

package de.jvstvshd.necrify.velocity.impl;

import com.velocitypowered.api.proxy.ProxyServer;
import com.zaxxer.hikari.HikariDataSource;
import de.jvstvshd.necrify.api.duration.PunishmentDuration;
import de.jvstvshd.necrify.api.punishment.*;
import de.jvstvshd.necrify.api.user.NecrifyUser;
import de.jvstvshd.necrify.common.punishment.PunishmentBuilder;
import de.jvstvshd.necrify.velocity.NecrifyVelocityPlugin;
import de.jvstvshd.necrify.velocity.internal.Util;
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