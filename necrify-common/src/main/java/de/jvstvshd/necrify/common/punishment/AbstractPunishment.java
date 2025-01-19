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

package de.jvstvshd.necrify.common.punishment;


import de.jvstvshd.necrify.api.duration.PunishmentDuration;
import de.jvstvshd.necrify.api.event.EventDispatcher;
import de.jvstvshd.necrify.api.event.punishment.PunishmentCancelledEvent;
import de.jvstvshd.necrify.api.event.punishment.PunishmentPersecutedEvent;
import de.jvstvshd.necrify.api.message.MessageProvider;
import de.jvstvshd.necrify.api.punishment.Punishment;
import de.jvstvshd.necrify.api.punishment.log.PunishmentLog;
import de.jvstvshd.necrify.api.punishment.log.PunishmentLogAction;
import de.jvstvshd.necrify.api.punishment.log.PunishmentLogEntry;
import de.jvstvshd.necrify.api.user.NecrifyUser;
import de.jvstvshd.necrify.common.AbstractNecrifyPlugin;
import de.jvstvshd.necrify.common.punishment.log.NecrifyPunishmentLog;
import de.jvstvshd.necrify.common.util.Util;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.LocalDateTime;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

public abstract class AbstractPunishment implements Punishment {

    private final Component reason;
    private final ExecutorService executor;
    private final NecrifyUser user;
    private final UUID punishmentUuid;
    private final MessageProvider messageProvider;
    private final EventDispatcher eventDispatcher;
    private final AbstractNecrifyPlugin plugin;
    private LocalDateTime creationTime;
    private Punishment successor;
    private PunishmentLog cachedLog;

    @Language("sql")
    protected final static String APPLY_PUNISHMENT = "INSERT INTO necrify_punishment" +
            " (uuid, type, expiration, reason, punishment_id, issued_at) VALUES (?, ?, ?, ?, ?, ?)";
    @Language("sql")
    protected final static String APPLY_CANCELLATION
            = "DELETE FROM necrify_punishment WHERE punishment_id = ?";
    @Language("sql")
    protected final static String APPLY_CHANGE = "UPDATE necrify_punishment SET reason = ?, expiration = ?, issued_at = ? WHERE punishment_id = ?";
    @Language("sql")
    protected final static String APPLY_SUCCESSOR = "UPDATE necrify_punishment SET successor = ? WHERE punishment_id = ?";
    @Language("sql")
    protected final static String APPLY_TIMESTAMP_UPDATE = "UPDATE necrify_punishment SET expiration = ?, issued_at = ? WHERE punishment_id = ?";

    private final boolean validity;

    public AbstractPunishment(@NotNull NecrifyUser user,
                              @NotNull Component reason,
                              @NotNull UUID punishmentUuid,
                              @NotNull AbstractNecrifyPlugin plugin,
                              @Nullable Punishment successor,
                              @Nullable LocalDateTime issuedAt) {
        this.reason = Objects.requireNonNull(reason, "punishment must be reasoned");
        this.executor = plugin.getExecutor();
        this.user = Objects.requireNonNull(user, "punishment must be bound to a user");
        this.punishmentUuid = Objects.requireNonNull(punishmentUuid, "punishment must have a uuid");
        this.successor = successor;
        this.validity = true;
        this.messageProvider = plugin.getMessageProvider();
        this.eventDispatcher = plugin.getEventDispatcher();
        this.plugin = plugin;
        this.creationTime = issuedAt;
    }

    public @NotNull Component getReason() {
        return reason;
    }

    @Override
    public @NotNull Component getRawReason() {
        return reason;
    }

    public ExecutorService getExecutor() {
        return executor;
    }

    protected <T> CompletableFuture<T> executeAsync(Callable<T> task, ExecutorService executorService) {
        var future = new CompletableFuture<T>();
        executorService.execute(() -> {
            try {
                future.complete(task.call());
            } catch (Exception e) {
                future.completeExceptionally(e);
            }
        });
        return future;
    }

    public @NotNull NecrifyUser getUser() {
        return user;
    }

    public @NotNull UUID getPunishmentUuid() {
        return punishmentUuid;
    }

    protected String convertReason(Component component) {
        return MiniMessage.miniMessage().serialize(component);
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean isValid() {
        return validity;
    }

    @Override
    public final CompletableFuture<Punishment> punish() {
        creationTime = LocalDateTime.now();
        return applyPunishment().whenCompleteAsync((punishment, throwable) -> {
            if (punishment != null) {
                getEventDispatcher().dispatch(new PunishmentPersecutedEvent(punishment));
                log(PunishmentLogAction.CREATED, "Punishment has been created.");
            }
            disposeCachedLog();
        });
    }

    @Override
    public final CompletableFuture<Punishment> cancel() {
        return applyCancellation().whenCompleteAsync((punishment, throwable) -> {
            if (punishment != null) {
                getEventDispatcher().dispatch(new PunishmentCancelledEvent(punishment));
                log(PunishmentLogAction.REMOVED, null);
            }
        });
    }

    protected abstract CompletableFuture<Punishment> applyPunishment();

    protected CompletableFuture<Punishment> applyCancellation() {
        throw new UnsupportedOperationException("cancellation is not supported for this punishment");
    }

    @Override
    public String toString() {
        return "AbstractPunishment{" +
                "reason=" + reason +
                ", service=" + executor +
                ", userUuid=" + user.getUuid() +
                ", userName=" + user.getUsername() +
                ", punishmentUuid=" + punishmentUuid +
                ", messageProvider=" + messageProvider +
                ", validity=" + validity +
                "} " + super.toString();
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Punishment that)) return false;
        return punishmentUuid.equals(that.getPunishmentUuid());
    }

    @Override
    public int hashCode() {
        return punishmentUuid.hashCode();
    }

    protected void checkValidity() {
        if (!isValid()) {
            throw new IllegalStateException("punishment is invalid");
        }
    }

    @NotNull
    public MessageProvider getMessageProvider() {
        return messageProvider;
    }

    @Override
    public @NotNull UUID getUuid() {
        return punishmentUuid;
    }

    @NotNull
    public EventDispatcher getEventDispatcher() {
        return eventDispatcher;
    }

    @NotNull
    public AbstractNecrifyPlugin getPlugin() {
        return plugin;
    }

    @Override
    public boolean hasSuccessor() {
        return successor != null;
    }

    @Override
    public @NotNull Punishment getSuccessor() {
        if (!hasSuccessor()) {
            throw new NoSuchElementException("this punishment has no successor");
        }
        return successor;
    }

    @Override
    public @NotNull LocalDateTime getCreationTime() {
        if (creationTime == null) {
            throw new IllegalStateException("punishment has not been created yet");
        }
        return creationTime;
    }

    @Override
    public @Nullable Punishment getPredecessor() {
        return user.getPunishments().stream().filter(punishment -> punishment.hasSuccessor() && punishment.getSuccessor().equals(this)).findFirst().orElse(null);
    }

    @Override
    public boolean hasBeenCreated() {
        return creationTime != null;
    }

    void setSuccessor0(Punishment successor) {
        this.successor = successor;
    }

    @Override
    public @NotNull CompletableFuture<PunishmentLog> loadPunishmentLog() {
        if (cachedLog != null) {
            return CompletableFuture.completedFuture(cachedLog);
        }
        return Util.executeAsync(() -> {
            var log = new NecrifyPunishmentLog(plugin, this);
            log.load(false);
            //if (plugin.getConfig().getConfiguration().getDataBaseData().getSqlType().startsWith("postgres")) {
            cachedLog = log;
            //}
            return log;
        }, executor);
    }

    public void disposeCachedLog() {
        if (plugin.getConfig().getConfiguration().getDataBaseData().getSqlType().startsWith("postgres")) return;
        cachedLog = null;
    }

    @Override
    public @NotNull PunishmentLogEntry createCurrentLogEntry() {
        return new PunishmentLogEntry(plugin.getSystemUser(), "Current state", PunishmentDuration.ofPunishment(this),
                getReason(), getPredecessor(), this, getSuccessorOrNull(), getCreationTime(), PunishmentLogAction.CREATED,
                cachedLog, LocalDateTime.now(), -1);
    }

    public PunishmentLog getCachedLog() {
        return cachedLog;
    }

    public void log(PunishmentLogAction action, String message) {
        loadPunishmentLog().thenAccept(log -> log.log(action, message, plugin.getSystemUser()));
    }
}
