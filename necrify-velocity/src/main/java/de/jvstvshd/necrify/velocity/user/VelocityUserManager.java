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

import com.github.benmanes.caffeine.cache.Cache;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import de.jvstvshd.necrify.api.event.punishment.PunishmentCancelledEvent;
import de.jvstvshd.necrify.api.event.punishment.PunishmentChangedEvent;
import de.jvstvshd.necrify.api.event.punishment.PunishmentPersecutedEvent;
import de.jvstvshd.necrify.api.event.user.UserLoadedEvent;
import de.jvstvshd.necrify.common.AbstractNecrifyPlugin;
import de.jvstvshd.necrify.common.event.PostgresPunishmentLogUpdateEvent;
import de.jvstvshd.necrify.common.user.AbstractUserManager;
import de.jvstvshd.necrify.common.util.Util;
import de.jvstvshd.necrify.velocity.NecrifyVelocityPlugin;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutorService;

public class VelocityUserManager extends AbstractUserManager<VelocityUser> {
    
    private final ProxyServer server;

    public VelocityUserManager(ExecutorService executor, ProxyServer server, Cache<UUID, VelocityUser> userCache, Cache<String, UUID> nameCache, NecrifyVelocityPlugin plugin) {
        super(executor, userCache, nameCache, plugin);
        this.server = server;
    }

    @Override
    public VelocityUser constructUser(UUID uuid, String playerName, boolean whitelisted, AbstractNecrifyPlugin plugin) {
        //TODO remove cast with introduction of proper storage abstraction
        return new VelocityUser(uuid, playerName, whitelisted, server.getPlayer(uuid).orElse(null), (NecrifyVelocityPlugin) plugin);
    }

    @Override
    public Optional<UUID> getUuidIfOnline(String name) {
        return server.getPlayer(name).map(Player::getUniqueId);
    }

    @Subscribe
    public void onPlayerDisconnect(DisconnectEvent event) {
        var player = event.getPlayer();
        var uuid = player.getUniqueId();
        var user = getUserCache().getIfPresent(uuid);
        if (user == null) return;
        user.setPlayer(null);
    }

    @Subscribe
    public void onPlayerJoin(LoginEvent event) {
        var player = event.getPlayer();
        var uuid = player.getUniqueId();
        var user = getUserCache().getIfPresent(uuid);
        if (user == null) return;
        user.setPlayer(player);
    }

    @org.greenrobot.eventbus.Subscribe(priority = Integer.MAX_VALUE)
    public void onUserLoaded(UserLoadedEvent event) {
        if (event.getOrigin().originatesFrom(getClass())) return;
        var user = event.getUser();
        if (user instanceof VelocityUser velocityUser) {
            cache(velocityUser);
        }
    }

    @org.greenrobot.eventbus.Subscribe(priority = Integer.MAX_VALUE)
    public void onPunishmentEnforced(PunishmentPersecutedEvent event) {
        var punishment = event.getPunishment();
        if (punishment.getUser() instanceof VelocityUser user) {
            user.addPunishment(punishment);
        }
    }

    @org.greenrobot.eventbus.Subscribe(priority = Integer.MAX_VALUE)
    public void onPunishmentCancelled(PunishmentCancelledEvent event) {
        var punishment = event.getPunishment();
        if (punishment.getUser() instanceof VelocityUser user) {
            user.removePunishment(punishment);
        }
    }

    @org.greenrobot.eventbus.Subscribe(priority = Integer.MAX_VALUE)
    public void onPunishmentChanged(PunishmentChangedEvent event) {
        var punishment = event.getPunishment();
        if (punishment.getUser() instanceof VelocityUser user) {
            user.removePunishment(punishment);
            user.addPunishment(punishment);
        }
    }

    @org.greenrobot.eventbus.Subscribe
    public void onPostgresPunishmentLogUpdate(PostgresPunishmentLogUpdateEvent event) {
        var log = Util.getCachedLog(event.getPunishment());
        if (log == null) return;
        log.addEntry(event.getNewEntry());
    }
}