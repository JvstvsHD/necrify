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

import com.github.benmanes.caffeine.cache.Cache;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.proxy.ProxyServer;
import de.chojo.sadu.queries.api.call.Call;
import de.chojo.sadu.queries.api.query.Query;
import de.jvstvshd.necrify.api.event.origin.EventOrigin;
import de.jvstvshd.necrify.api.event.punishment.PunishmentCancelledEvent;
import de.jvstvshd.necrify.api.event.punishment.PunishmentChangedEvent;
import de.jvstvshd.necrify.api.event.punishment.PunishmentPersecutedEvent;
import de.jvstvshd.necrify.api.event.user.UserLoadedEvent;
import de.jvstvshd.necrify.api.user.NecrifyUser;
import de.jvstvshd.necrify.api.user.UserManager;
import de.jvstvshd.necrify.common.io.Adapters;
import de.jvstvshd.necrify.common.user.MojangAPI;
import de.jvstvshd.necrify.velocity.NecrifyVelocityPlugin;
import de.jvstvshd.necrify.velocity.internal.Util;
import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

import static de.jvstvshd.necrify.velocity.internal.Util.executeAsync;
import static java.util.Optional.empty;

//20.06.2024: Only stub implementation to allow ConnectionListener to work.
public class VelocityUserManager implements UserManager {

    @Language("sql")
    private static final String SELECT_USER_QUERY = "SELECT name, whitelisted FROM punishment.necrify_user WHERE uuid = ?;";

    @Language("sql")
    private static final String SELECT_USER_BY_NAME_QUERY = "SELECT uuid, whitelisted FROM punishment.necrify_user WHERE name = ?";

    @Language("sql")
    private static final String SELECT_USER_PUNISHMENTS_QUERY =
            "SELECT type, expiration, reason, punishment_id FROM punishment.necrify_punishment WHERE uuid = ?;";

    @Language("sql")
    private static final String INSERT_NEW_USER = "INSERT INTO punishment.necrify_user (uuid, name, whitelisted) VALUES (?, ?, ?) ON CONFLICT (uuid) DO NOTHING;";

    private final ExecutorService executor;
    private final ProxyServer server;
    private final Cache<UUID, VelocityUser> userCache;
    private final Cache<String, UUID> nameCache;
    private final NecrifyVelocityPlugin plugin;

    public VelocityUserManager(ExecutorService executor, ProxyServer server, Cache<UUID, VelocityUser> userCache, Cache<String, UUID> nameCache, NecrifyVelocityPlugin plugin) {
        this.executor = executor;
        this.server = server;
        this.userCache = userCache;
        this.nameCache = nameCache;
        this.plugin = plugin;
    }

    @Override
    public @NotNull Optional<NecrifyUser> getUser(@NotNull UUID uuid) {
        return Optional.ofNullable(userCache.getIfPresent(uuid));
    }

    @Override
    public @NotNull Optional<NecrifyUser> getUser(@NotNull String player) {
        var uuid = tryAcquireUuid(player);
        if (uuid != null) {
            return getUser(uuid);
        }
        return empty();
    }

    @Override
    public @NotNull CompletableFuture<Optional<NecrifyUser>> loadUser(@NotNull UUID uuid) {
        var cached = getUser(uuid);
        if (cached.isPresent()) {
            return CompletableFuture.completedFuture(cached);
        }
        return executeAsync(() -> {
            var player = server.getPlayer(uuid);
            var user = Query.query(SELECT_USER_QUERY)
                    .single(Call.of().bind(uuid, Adapters.UUID_ADAPTER))
                    .map(row -> new VelocityUser(uuid, row.getString(1), row.getBoolean(2), player.orElse(null), plugin))
                    .first();
            user.ifPresent(velocityUser -> {
                Query.query(SELECT_USER_PUNISHMENTS_QUERY)
                        .single(Call.of().bind(uuid, Adapters.UUID_ADAPTER))
                        .map(velocityUser::addPunishment).all();
            });
            //will cause compilation error: return user.map(this::cache);
            return user.map(velocityUser -> {
                cache(velocityUser);
                plugin.getEventDispatcher().dispatch(new UserLoadedEvent(velocityUser).setOrigin(EventOrigin.ofClass(getClass())));
                return velocityUser;
            });
        }, executor);
    }


    @Override
    public @NotNull CompletableFuture<Optional<NecrifyUser>> loadUser(@NotNull String player) {
        var cached = getUser(player);
        if (cached.isPresent()) {
            return CompletableFuture.completedFuture(cached);
        }
        var parsedUuid = Util.parseUuid(player);
        if (parsedUuid != null) {
            return loadUser(parsedUuid);
        }
        return executeAsync(() -> {
            var user = Query.query(SELECT_USER_BY_NAME_QUERY)
                    .single(Call.of().bind(player))
                    .map(row -> new VelocityUser(row.getObject(1, UUID.class), player, row.getBoolean(2), plugin))
                    .first();
            user.ifPresent(velocityUser -> {
                Query.query(SELECT_USER_PUNISHMENTS_QUERY)
                        .single(Call.of().bind(velocityUser.getUuid(), Adapters.UUID_ADAPTER))
                        .map(velocityUser::addPunishment).all();
            });
            //will cause compilation error: return user.map(this::cache);
            return user.map(velocityUser -> {
                cache(velocityUser);
                plugin.getEventDispatcher().dispatch(new UserLoadedEvent(velocityUser).setOrigin(EventOrigin.ofClass(getClass())));
                return velocityUser;
            });
        }, executor);
    }

    @Override
    public @NotNull CompletableFuture<Optional<NecrifyUser>> createUser(@NotNull UUID uuid) {
        return executeAsync(() -> {
            var name = MojangAPI.getPlayerName(uuid);
            return name.map(s -> createUser(uuid, s));
        }, executor);
    }

    @Override
    public @NotNull CompletableFuture<Optional<NecrifyUser>> createUser(@NotNull String player) {
        var parsedUuid = Util.parseUuid(player);
        if (parsedUuid != null) {
            return loadUser(parsedUuid);
        }
        return executeAsync(() -> {
            var uuid = MojangAPI.getUuid(player);
            return uuid.map(id -> createUser(id, player));
        }, executor);
    }

    /**
     * Expects to be executed in an async context (otherwise blocks the current thread) and the user to exist.
     *
     * @param uuid non-null uuid of the user
     * @param name non-null name of the user
     * @return the created user
     * @throws IllegalStateException if the user already exists
     */
    private NecrifyUser createUser(UUID uuid, String name) {
        var result = Query.query(INSERT_NEW_USER)
                .single(Call.of().bind(uuid, Adapters.UUID_ADAPTER).bind(name).bind(false))
                .insert();
        if (!result.changed()) {
            throw new IllegalStateException("user already exists");
        }
        var user = new VelocityUser(uuid, name, false, plugin);
        plugin.getEventDispatcher().dispatch(new UserLoadedEvent(user));
        return cache(user);
    }

    @Override
    public @NotNull CompletableFuture<Optional<NecrifyUser>> loadOrCreateUser(@NotNull UUID uuid) {
        return loadUser(uuid).thenApplyAsync(optional -> optional.or(() -> createUser(uuid).join()), executor);
    }

    @Override
    public @NotNull CompletableFuture<Optional<NecrifyUser>> loadOrCreateUser(@NotNull String player) {
        return loadUser(player).thenApplyAsync(optional -> optional.or(() -> createUser(player).join()), executor);
    }

    @Override
    public void putUser(@NotNull NecrifyUser user) {
        cache(new VelocityUser(user.getUuid(), user.getUsername(), user.isWhitelisted(), plugin));
    }

    @Nullable
    private UUID tryAcquireUuid(String name) {
        var parsed = Util.fromString(name);
        if (parsed.isPresent()) {
            return parsed.get();
        }
        var onlinePlayer = server.getPlayer(name);
        if (onlinePlayer.isPresent()) {
            return onlinePlayer.get().getUniqueId();
        }
        return nameCache.getIfPresent(name);
    }

    private VelocityUser cache(@NotNull VelocityUser user) {
        userCache.put(user.getUuid(), user);
        if (user.getUsername() != null) {
            nameCache.put(user.getUsername(), user.getUuid());
        }
        return user;
    }

    @Subscribe
    public void onPlayerDisconnect(DisconnectEvent event) {
        var player = event.getPlayer();
        var uuid = player.getUniqueId();
        var user = userCache.getIfPresent(uuid);
        if (user == null) return;
        user.setPlayer(null);
    }

    @Subscribe
    public void onPlayerJoin(LoginEvent event) {
        var player = event.getPlayer();
        var uuid = player.getUniqueId();
        var user = userCache.getIfPresent(uuid);
        if (user == null) return;
        user.setPlayer(player);
    }

    @org.greenrobot.eventbus.Subscribe
    public void onUserLoaded(UserLoadedEvent event) {
        if (event.getOrigin().originatesFrom(getClass())) return;
        var user = event.getUser();
        if (user instanceof VelocityUser) {
            cache((VelocityUser) user);
        }
    }

    @org.greenrobot.eventbus.Subscribe
    public void onPunishmentEnforced(PunishmentPersecutedEvent event) {
        var punishment = event.getPunishment();
        if (punishment.getUser() instanceof VelocityUser user) {
            user.addPunishment(punishment);
        }
    }

    @org.greenrobot.eventbus.Subscribe
    public void onPunishmentCancelled(PunishmentCancelledEvent event) {
        var punishment = event.getPunishment();
        if (punishment.getUser() instanceof VelocityUser user) {
            user.removePunishment(punishment);
        }
    }

    @org.greenrobot.eventbus.Subscribe
    public void onPunishmentChanged(PunishmentChangedEvent event) {
        var punishment = event.getPunishment();
        if (punishment.getUser() instanceof VelocityUser user) {
            user.removePunishment(punishment);
            user.addPunishment(punishment);
        }
    }
}