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


import de.chojo.sadu.base.QueryFactory;
import de.jvstvshd.necrify.api.message.MessageProvider;
import de.jvstvshd.necrify.api.punishment.Punishment;
import de.jvstvshd.necrify.api.user.NecrifyUser;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.jetbrains.annotations.NotNull;

import javax.sql.DataSource;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

public abstract class AbstractPunishment extends QueryFactory implements Punishment {

    private final Component reason;
    private final DataSource dataSource;
    private final ExecutorService service;
    private final NecrifyUser user;
    private final UUID punishmentUuid;
    private final MessageProvider messageProvider;

    protected final static String APPLY_PUNISHMENT = "INSERT INTO necrify_punishment" +
            " (uuid, name, type, expiration, reason, punishment_id) VALUES (?, ?, ?, ?, ?, ?)";
    protected final static String APPLY_CANCELLATION
            = "DELETE FROM necrify_punishment WHERE punishment_id = ?";
    protected final static String APPLY_CHANGE = "UPDATE necrify_punishment SET reason = ?, expiration = ?, permanent = ? WHERE punishment_id = ?";
    private final boolean validity;

    public AbstractPunishment(NecrifyUser user, Component reason, DataSource dataSource, ExecutorService service, MessageProvider messageProvider) {
        this(user, reason, dataSource, service, UUID.randomUUID(), messageProvider);
    }

    public AbstractPunishment(NecrifyUser user, Component reason, DataSource dataSource, ExecutorService service, UUID punishmentUuid, MessageProvider messageProvider) {
        super(dataSource);
        this.reason = reason;
        this.dataSource = dataSource;
        this.service = service;
        this.user = user;
        this.punishmentUuid = punishmentUuid;
        this.validity = true;
        this.messageProvider = messageProvider;
    }

    public DataSource getDataSource() {
        return dataSource;
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

    public NecrifyUser getUser() {
        return user;
    }

    public UUID getPunishmentUuid() {
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
    public String toString() {
        return "AbstractPunishment{" +
                "reason=" + reason +
                ", dataSource=" + dataSource +
                ", service=" + service +
                ", user=" + user +
                ", punishmentUuid=" + punishmentUuid +
                ", messageProvider=" + messageProvider +
                ", validity=" + validity +
                "} " + super.toString();
    }

    protected void checkValidity() {
        if (!isValid()) {
            throw new IllegalStateException("punishment is invalid");
        }
    }

    public MessageProvider getMessageProvider() {
        return messageProvider;
    }

    @Override
    public UUID getUuid() {
        return punishmentUuid;
    }
}
