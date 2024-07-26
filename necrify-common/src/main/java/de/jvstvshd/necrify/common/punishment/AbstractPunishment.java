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

package de.jvstvshd.necrify.common.punishment;


import de.jvstvshd.necrify.api.event.EventDispatcher;
import de.jvstvshd.necrify.api.event.punishment.PunishmentCancelledEvent;
import de.jvstvshd.necrify.api.event.punishment.PunishmentPersecutedEvent;
import de.jvstvshd.necrify.api.message.MessageProvider;
import de.jvstvshd.necrify.api.punishment.Punishment;
import de.jvstvshd.necrify.api.user.NecrifyUser;
import de.jvstvshd.necrify.common.AbstractNecrifyPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

public abstract class AbstractPunishment implements Punishment {

    private final Component reason;
    private final ExecutorService service;
    private final NecrifyUser user;
    private final UUID punishmentUuid;
    private final MessageProvider messageProvider;
    private final EventDispatcher eventDispatcher;
    private final AbstractNecrifyPlugin plugin;
    private Punishment successor;

    @Language("sql")
    protected final static String APPLY_PUNISHMENT = "INSERT INTO punishment.necrify_punishment" +
            " (uuid, type, expiration, reason, punishment_id) VALUES (?, ?, ?, ?, ?)";
    @Language("sql")
    protected final static String APPLY_CANCELLATION
            = "DELETE FROM punishment.necrify_punishment WHERE punishment_id = ?";
    @Language("sql")
    protected final static String APPLY_CHANGE = "UPDATE necrify_punishment SET reason = ?, expiration = ?, permanent = ? WHERE punishment_id = ?";
    private final boolean validity;


    public AbstractPunishment(@NotNull NecrifyUser user, @NotNull Component reason, @NotNull UUID punishmentUuid, @NotNull AbstractNecrifyPlugin plugin, @Nullable Punishment successor) {
        this.reason = Objects.requireNonNull(reason, "punishment must be reasoned");
        this.service = plugin.getService();
        this.user = Objects.requireNonNull(user, "punishment must be bound to a user");
        this.punishmentUuid = Objects.requireNonNull(punishmentUuid, "punishment must have a uuid");
        this.successor = successor;
        this.validity = true;
        this.messageProvider = plugin.getMessageProvider();
        this.eventDispatcher = plugin.getEventDispatcher();
        this.plugin = plugin;
    }

    public @NotNull Component getReason() {
        return reason;
    }

    public ExecutorService getService() {
        return service;
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
        return applyPunishment().whenCompleteAsync((punishment, throwable) -> {
            if (punishment != null) {
                plugin.getEventDispatcher().dispatch(new PunishmentPersecutedEvent(punishment));
            }
        });
    }

    @Override
    public final CompletableFuture<Punishment> cancel() {
        return applyCancellation().whenCompleteAsync((punishment, throwable) -> {
            if (punishment != null) {
                plugin.getEventDispatcher().dispatch(new PunishmentCancelledEvent(punishment));
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
                ", service=" + service +
                ", user=" + user +
                ", punishmentUuid=" + punishmentUuid +
                ", messageProvider=" + messageProvider +
                ", validity=" + validity +
                "} " + super.toString();
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AbstractPunishment that)) return false;

        return punishmentUuid.equals(that.punishmentUuid);
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
    public void setSuccessor(@NotNull Punishment successor) {
        this.successor = successor;
    }
}
