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

import de.chojo.sadu.queries.api.call.Call;
import de.chojo.sadu.queries.api.query.Query;
import de.jvstvshd.necrify.api.PunishmentException;
import de.jvstvshd.necrify.api.duration.PunishmentDuration;
import de.jvstvshd.necrify.api.event.punishment.PunishmentChangedEvent;
import de.jvstvshd.necrify.api.punishment.Punishment;
import de.jvstvshd.necrify.api.punishment.StandardPunishmentType;
import de.jvstvshd.necrify.api.punishment.TemporalPunishment;
import de.jvstvshd.necrify.api.user.NecrifyUser;
import de.jvstvshd.necrify.common.AbstractNecrifyPlugin;
import de.jvstvshd.necrify.common.io.Adapters;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public abstract class AbstractTemporalPunishment extends AbstractPunishment implements TemporalPunishment {

    private final PunishmentDuration duration;

    public AbstractTemporalPunishment(NecrifyUser user, Component reason, UUID punishmentUuid, PunishmentDuration duration, AbstractNecrifyPlugin plugin, Punishment successor) {
        super(user, reason, punishmentUuid, plugin, successor);
        this.duration = Objects.requireNonNull(duration, "temporal punishment must have a duration");
    }

    public PunishmentDuration getDuration() {
        return duration;
    }

    @Override
    public String toString() {
        return "AbstractTemporalPunishment{" +
                "duration=" + duration +
                "} " + super.toString();
    }

    @Override
    public boolean isValid() {
        return super.isValid() && isOngoing();
    }

    @Override
    protected void checkValidity() {
        if (!isValid()) {
            throw new IllegalStateException("punishment is invalid (probably isOngoing returned false)");
        }
    }

    @Override
    public final CompletableFuture<Punishment> change(@NotNull PunishmentDuration newDuration, Component newReason) throws PunishmentException {
        if (!getType().isBan() && !getType().isMute()) {
            throw new IllegalStateException("only bans and mutes can be changed");
        }
        return executeAsync(() -> {
            Query.query(APPLY_CHANGE)
                    .single(Call.of()
                            .bind(convertReason(newReason))
                            .bind(newDuration.expirationAsTimestamp())
                            .bind(newDuration.isPermanent())
                            .bind(getPunishmentUuid(), Adapters.UUID_ADAPTER))
                    .update();
            var builder = new PunishmentBuilder(getPlugin())
                    .withUser(getUser())
                    .withReason(newReason)
                    .withDuration(newDuration)
                    .withPunishmentUuid(getPunishmentUuid());
            Punishment punishment;
            if (getType().isBan()) {
                punishment = builder.buildBan();
            } else if (getType().isMute()) {
                punishment = builder.buildMute();
            } else {
                throw new IllegalStateException("punishment type is not a ban or mute");
            }
            getPlugin().getEventDispatcher().dispatch(new PunishmentChangedEvent(punishment, this));
            return punishment;
        }, getService());
    }

    @Override
    protected CompletableFuture<Punishment> applyCancellation() throws PunishmentException {
        return executeAsync(() -> {
            Query.query(APPLY_CANCELLATION)
                    .single(Call.of().bind(getPunishmentUuid(), Adapters.UUID_ADAPTER))
                    .delete();
            return this;
        }, getService());
    }

    @Override
    public abstract @NotNull StandardPunishmentType getType();

    @Override
    protected CompletableFuture<Punishment> applyPunishment() throws PunishmentException {
        checkValidity();
        var duration = getDuration().absolute();
        return executeAsync(() -> {
            Query.query(APPLY_PUNISHMENT)
                    .single(Call.of()
                            .bind(getUser().getUuid(), Adapters.UUID_ADAPTER)
                            .bind(getType().getId())
                            .bind(duration.expirationAsTimestamp())
                            .bind(convertReason(getReason()))
                            .bind(getPunishmentUuid(), Adapters.UUID_ADAPTER))
                    .insert();
            return this;
        }, getService());
    }
}
