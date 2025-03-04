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

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import de.jvstvshd.necrify.common.user.AbstractNecrifyUser;
import de.jvstvshd.necrify.velocity.NecrifyVelocityPlugin;
import net.kyori.adventure.audience.Audience;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

public class VelocityUser extends AbstractNecrifyUser {

    private final UUID uuid;
    private final ProxyServer server;
    private final NecrifyVelocityPlugin plugin;
    private Player player;

    public VelocityUser(@NotNull UUID uuid, @Nullable String name, boolean whitelisted, @Nullable Player player, NecrifyVelocityPlugin plugin) {
        super(uuid, name, plugin, whitelisted);
        this.plugin = plugin;
        this.player = player;
        this.uuid = uuid;
        this.server = plugin.getServer();
    }

    public VelocityUser(@NotNull UUID uuid, @Nullable String name, boolean whitelisted, NecrifyVelocityPlugin plugin) {
        super(uuid, name, plugin, whitelisted);
        this.plugin = plugin;
        this.server = plugin.getServer();
        this.player = server.getPlayer(uuid).orElse(null);
        this.uuid = uuid;

    }

    public Optional<Player> queryPlayer() {
        var opt = server.getPlayer(uuid);
        return opt.map(value -> player = value);
    }

    @Override
    public boolean hasPermission(@NotNull String permission) {
        return queryPlayer().map(player -> player.hasPermission(permission)).orElse(false);
    }

    public Player getPlayer() {
        return player;
    }

    public void setPlayer(Player player) {
        this.player = player;
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
    public @NotNull Locale getLocale() {
        var defaultLocale = plugin.getConfig().getConfiguration().getDefaultLanguage();
        if (player != null) {
            return Objects.requireNonNullElse(player.getEffectiveLocale(), defaultLocale);
        }
        return defaultLocale;
    }

    @Override
    public void executeOnAudience(@NotNull Consumer<Audience> consumer) {
        queryPlayer().ifPresent(consumer);
    }
}