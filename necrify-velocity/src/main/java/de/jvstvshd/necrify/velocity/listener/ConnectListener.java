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

package de.jvstvshd.necrify.velocity.listener;

import com.velocitypowered.api.event.ResultedEvent;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.proxy.ProxyServer;
import de.jvstvshd.necrify.api.punishment.Ban;
import de.jvstvshd.necrify.api.punishment.Punishment;
import de.jvstvshd.necrify.api.user.NecrifyUser;
import de.jvstvshd.necrify.common.punishment.ChainedPunishment;
import de.jvstvshd.necrify.common.util.Util;
import de.jvstvshd.necrify.velocity.NecrifyVelocityPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

public final class ConnectListener {
    private final NecrifyVelocityPlugin plugin;
    private final ExecutorService service;
    private final ProxyServer proxyServer;

    public ConnectListener(NecrifyVelocityPlugin plugin,
                           ExecutorService service, ProxyServer proxyServer) {
        this.plugin = plugin;
        this.service = service;
        this.proxyServer = proxyServer;
    }

    @Subscribe
    public void onConnect(LoginEvent event) {
        NecrifyUser user;
        try {
            var userResult = plugin.getUserManager().loadOrCreateUser(event.getPlayer().getUniqueId()).get(10, TimeUnit.SECONDS);
            if (userResult.isEmpty()) {
                //there is no user instance for the joined player in the database, therefore he has never been punished, so he can join
                return;
            }
            user = userResult.get();
        } catch (Exception e) {
            plugin.getLogger().error("Cannot retrieve user instance for player {} ({})", event.getPlayer().getUsername(), event.getPlayer().getUniqueId(), e);
            event.setResult(ResultedEvent.ComponentResult.denied(plugin.getMessageProvider().internalError()));
            return;
        }
        List<Punishment> punishments = new ArrayList<>(user.getPunishments());
        if (plugin.isWhitelistActive()) {
            if (!user.isWhitelisted()) {
                event.setResult(ResultedEvent.ComponentResult.denied(Component.translatable("whitelist.blacklisted").color(NamedTextColor.RED)));
                return;
            }
        }
        punishments.stream().filter(punishment -> !punishment.isOngoing()).forEach(Punishment::cancel);
        punishments.removeIf(punishment -> !punishment.isOngoing());
        List<Ban> bans = new ArrayList<>();
        for (Punishment punishment : punishments) {
            if (punishment instanceof Ban velocityBan)
                bans.add(velocityBan);
        }
        try {
            plugin.communicator().recalculateMuteInformation(user);
        } catch (Exception e) {
            plugin.getLogger().error("Cannot send mute to bungee", e);
        }
        if (bans.isEmpty()) {
            return;
        }
        final Ban ban = Util.getLongestPunishment(bans);
        if (ban == null)
            return;
        Component deny = ChainedPunishment.of(ban, plugin).createFullReason(event.getPlayer().getEffectiveLocale());
        event.setResult(ResultedEvent.ComponentResult.denied(deny));
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (ConnectListener) obj;
        return Objects.equals(this.plugin, that.plugin) &&
                Objects.equals(this.service, that.service) &&
                Objects.equals(this.proxyServer, that.proxyServer);
    }

    @Override
    public int hashCode() {
        return Objects.hash(plugin, service, proxyServer);
    }

    @Override
    public String toString() {
        return "ConnectListener[" +
                "plugin=" + plugin + ", " +
                "service=" + service + ", " +
                "proxyServer=" + proxyServer + ']';
    }
}
