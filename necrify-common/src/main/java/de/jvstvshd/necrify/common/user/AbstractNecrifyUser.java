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

package de.jvstvshd.necrify.common.user;

import com.google.common.collect.ImmutableList;
import de.chojo.sadu.queries.api.call.Call;
import de.chojo.sadu.queries.api.query.Query;
import de.jvstvshd.necrify.api.duration.PunishmentDuration;
import de.jvstvshd.necrify.api.event.user.UserDeletedEvent;
import de.jvstvshd.necrify.api.message.MessageProvider;
import de.jvstvshd.necrify.api.punishment.*;
import de.jvstvshd.necrify.api.template.NecrifyTemplate;
import de.jvstvshd.necrify.api.template.NecrifyTemplateStage;
import de.jvstvshd.necrify.api.user.NecrifyUser;
import de.jvstvshd.necrify.api.user.UserDeletionReason;
import de.jvstvshd.necrify.common.AbstractNecrifyPlugin;
import de.jvstvshd.necrify.common.io.Adapters;
import de.jvstvshd.necrify.common.punishment.PunishmentBuilder;
import de.jvstvshd.necrify.common.util.Util;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.audience.MessageType;
import net.kyori.adventure.identity.Identified;
import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;

public abstract class AbstractNecrifyUser implements NecrifyUser {

    private final MessageProvider messageProvider;
    private final AbstractNecrifyPlugin plugin;
    private final List<Punishment> punishments;
    private String username;
    private final UUID uuid;
    private final ExecutorService executor;
    private boolean whitelisted;

    protected AbstractNecrifyUser(UUID uuid, String username, AbstractNecrifyPlugin plugin, boolean whitelisted) {
        this.messageProvider = plugin.getMessageProvider();
        this.plugin = plugin;
        this.username = username;
        this.uuid = uuid;
        this.executor = plugin.getExecutor();
        this.whitelisted = whitelisted;
        this.punishments = Collections.synchronizedList(new ArrayList<>());
    }

    @Override
    public @NotNull UUID getUuid() {
        return uuid;
    }

    @Override
    public @Nullable String getUsername() {
        return username;
    }

    @Override
    public @NotNull CompletableFuture<Ban> ban(@Nullable Component reason, @NotNull PunishmentDuration duration) {
        return punishCasted(PunishmentBuilder.newBuilder(plugin)
                .withDuration(duration)
                .withReason(reason)
                .withUser(this)
                .buildBan());
    }

    @Override
    public @NotNull CompletableFuture<Ban> banPermanent(@Nullable Component reason) {
        return ban(reason, PunishmentDuration.PERMANENT);
    }

    @Override
    public @NotNull CompletableFuture<Mute> mute(@Nullable Component reason, @NotNull PunishmentDuration duration) {
        return punishCasted(PunishmentBuilder.newBuilder(plugin)
                .withDuration(duration)
                .withReason(reason)
                .withUser(this)
                .buildMute());
    }

    @Override
    public @NotNull CompletableFuture<Mute> mutePermanent(@Nullable Component reason) {
        return mute(reason, PunishmentDuration.PERMANENT);
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
    public <T extends Punishment> @NotNull CompletableFuture<T> punishCasted(Punishment punishment) {
        return (CompletableFuture<T>) punish(punishment);
    }

    @Override
    public @NotNull CompletableFuture<Punishment> punish(@NotNull Punishment punishment) {
        punishments.add(punishment);
        return punishment.punish();
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

    protected synchronized void validatePunishments() {
        punishments.removeIf(punishment -> !punishment.isOngoing());
    }

    @Override
    public @NotNull Optional<NecrifyTemplateStage> getCurrentTemplateStage(@NotNull NecrifyTemplate template) {
        return Optional.empty();
    }

    @Override
    public @NotNull NecrifyTemplateStage getNextTemplateStage(@NotNull NecrifyTemplate template) {
        return null;
    }

    @Override
    public @NotNull CompletableFuture<Punishment> punishModelled(@NotNull NecrifyTemplate template) {
        return null;
    }

    @Override
    public @NotNull CompletableFuture<String> queryUsername(boolean update) {
        return MojangAPI.getPlayerNameAsync(uuid, executor).thenApplyAsync(s -> {
            if (update)
                username = s.orElse(null);
            return s.orElse(null);
        });
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

    public void addPunishment(Punishment punishment) {
        if (punishments.contains(punishment))
            return;
        punishments.add(punishment);
    }

    public void removePunishment(Punishment punishment) {
        punishments.remove(punishment);
    }

    @Override
    public CompletableFuture<Integer> delete(@NotNull UserDeletionReason reason) {
        plugin.getEventDispatcher().dispatch(new UserDeletedEvent(this, reason));
        return Util.executeAsync(() -> Query
                .query("DELETE FROM necrify_user WHERE uuid = ?;")
                .single(Call.of().bind(uuid, Adapters.UUID_ADAPTER))
                .delete().rows() + punishments.size(), executor);
    }

    public abstract void executeOnAudience(@NotNull Consumer<Audience> consumer);

    public MessageProvider getProvider() {
        return messageProvider;
    }

    //--- implementations of Audience

    @Override
    public void sendMessage(@NotNull String key, TextColor color, Component... args) {
        sendMessage(messageProvider.provide(key, args).color(color));
    }

    @Override
    public void sendErrorMessage() {
        sendMessage(messageProvider.internalError());
    }

    @Override
    public void sendMessage(@NotNull Component message) {
        executeOnAudience(audience -> audience.sendMessage(message));
    }

    @Override
    public void sendMessage(@NotNull Identified source, @NotNull Component message) {
        executeOnAudience(audience -> audience.sendMessage(source, message));
    }

    @Override
    public void sendMessage(@NotNull Identity source, @NotNull ComponentLike message, @NotNull MessageType type) {
        executeOnAudience(audience -> audience.sendMessage(source, message, type));
    }
}