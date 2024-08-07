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
package de.jvstvshd.necrify.velocity.listener;

import com.velocitypowered.api.event.ResultedEvent;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.proxy.ProxyServer;
import de.jvstvshd.necrify.api.punishment.Ban;
import de.jvstvshd.necrify.api.punishment.Mute;
import de.jvstvshd.necrify.api.punishment.Punishment;
import de.jvstvshd.necrify.api.user.NecrifyUser;
import de.jvstvshd.necrify.common.plugin.MuteData;
import de.jvstvshd.necrify.velocity.NecrifyVelocityPlugin;
import de.jvstvshd.necrify.velocity.internal.Util;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
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
        List<Mute> mutes = new ArrayList<>();
        for (Punishment punishment : punishments) {
            if (punishment instanceof Ban velocityBan)
                bans.add(velocityBan);
            if (punishment instanceof Mute mute)
                mutes.add(mute);
        }
        for (Mute mute : mutes) {
            try {
                plugin.communicator().queueMute(mute, MuteData.ADD);
            } catch (Exception e) {
                plugin.getLogger().error("Cannot send mute to bungee", e);
            }
        }
        if (bans.isEmpty()) {
            return;
        }
        final Ban ban = Util.getLongestPunishment(bans);
        if (ban == null)
            return;
        Component deny = ban.createFullReason(event.getPlayer().getEffectiveLocale());
        event.setResult(ResultedEvent.ComponentResult.denied(deny));
    }

    public NecrifyVelocityPlugin plugin() {
        return plugin;
    }

    public ExecutorService service() {
        return service;
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
