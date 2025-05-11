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
import org.incendo.cloud.type.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;

public abstract class AbstractNecrifyUser implements NecrifyUser {

    private final MessageProvider messageProvider;
    private final AbstractNecrifyPlugin plugin;
    private final List<Punishment> punishments;
    private final Map<NecrifyTemplate, NecrifyTemplateStage> templateStages = new ConcurrentHashMap<>();
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
        return Optional.ofNullable(templateStages.get(template));
    }

    @Override
    public @NotNull NecrifyTemplateStage getNextTemplateStage(@NotNull NecrifyTemplate template) {
        try {
            if (templateStages.containsKey(template)) {
                return templateStages.get(template).nextOrThis();
            } else {
                return template.getStage(0);
            }
        } catch (NoSuchElementException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public @NotNull CompletableFuture<Punishment> punishModelled(@NotNull NecrifyTemplate template) {
        var nextStage = getNextTemplateStage(template);
        Map<String, Object> data = Map.of("user", this, "duration", nextStage.duration(), "punishmentUuid", UUID.randomUUID(),
                "reason", nextStage.reason());
        var punishment = PunishmentTypeRegistry.createPunishment(nextStage.punishmentType(), data);
        return punishment.punish().whenComplete((punishment1, throwable) -> {
            var secondNextStage = nextStage.nextOrThis();
            if (secondNextStage.equals(nextStage)) return;
            updateUserStage(nextStage);
        });
    }

    @Override
    public CompletableFuture<Void> amnesty(@NotNull NecrifyTemplate template, int stageIndex) {
        if (stageIndex < 0) {
            throw new IllegalArgumentException("stageIndex must be >= 0");
        }
        if (stageIndex > template.stages().size() - 1) {
            throw new IllegalArgumentException("stageIndex must be < " + template.stages().size());
        }
        if (templateStages.containsKey(template)) {
            if (templateStages.get(template).index() == stageIndex - 1) {
                return CompletableFuture.completedFuture(null);
            }
            if (stageIndex == 0) {
                return Util.executeAsync(() -> {
                    Query.query("DELETE FROM necrify_punishment_template_user_stage WHERE user_id = ? AND template_id = (SELECT id FROM necrify_punishment_template WHERE name = ?);")
                            .single(Call.of().bind(uuid, Adapters.UUID_ADAPTER).bind(template.name()))
                            .update();
                    templateStages.remove(template);
                    return null;
                }, executor);
            } else {
                var newStage = template.getStage(stageIndex - 1);
                return updateUserStage(newStage).thenAccept( unused -> templateStages.put(template, newStage));
            }
        }
        return CompletableFuture.completedFuture(null);
    }

    public CompletableFuture<Void> updateUserStage(NecrifyTemplateStage stage) {
        return Util.executeAsync(() -> {
            Query.query("WITH template_data AS (SELECT id FROM necrify_punishment_template t WHERE t.name = ?), stage_data AS (SELECT id FROM necrify_punishment_template_stage s WHERE s.template_id = (SELECT id FROM template_data) AND s.index = ?) " +
                            "INSERT INTO necrify_punishment_template_user_stage (user_id, template_id, stage_id) " +
                            "VALUES (?, (SELECT id FROM template_data), (SELECT id FROM stage_data))" +
                            "ON CONFLICT (user_id, template_id) DO UPDATE SET stage_id = (SELECT id FROM stage_data);")
                    .single(Call.of().bind(stage.template().name()).bind(stage.index()).bind(uuid, Adapters.UUID_ADAPTER))
                    .update();
            templateStages.put(stage.template(), stage);
            return null;
        }, executor);
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

    /**
     * Loads all template stages of this user from the storage. This method will be executed asynchronously and will not
     * block the calling thread unless using {@link CompletableFuture#join()} or similar.
     */
    public CompletableFuture<Void> loadTemplateStages() {
        return Util.executeAsync(() -> {
            Query.query("SELECT template.name, stage.index FROM necrify_punishment_template_stage stage, " +
                            "necrify_punishment_template template, necrify_punishment_template_user_stage users WHERE " +
                            "users.user_id = ? AND template.id = users.template_id AND stage.template_id = template.id")
                    .single(Call.of().bind(uuid, Adapters.UUID_ADAPTER))
                    .map(row -> {
                        var template = plugin.getTemplateManager().getTemplate(row.getString(1));
                        if (template.isEmpty()) {
                            plugin.getLogger().warn("Template {} not found for user {}", row.getString(1), uuid);
                            return null;
                        }
                        try {
                            var stage = template.get().getStage(row.getInt(2));
                            return Pair.of(template.get(), stage);
                        } catch (NoSuchElementException | IndexOutOfBoundsException e) {
                            plugin.getLogger().warn("Template stage {} not found in template {} for user {}", row.getInt(2), template.get().name(), uuid);
                            return null;
                        }
                    }).all().forEach(pair -> {
                        if (pair == null) return;
                        templateStages.put(pair.first(), pair.second());
                    });
            return null;
        }, executor);
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