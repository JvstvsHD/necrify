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
import de.chojo.sadu.queries.api.call.adapter.Adapter;
import de.chojo.sadu.queries.api.call.adapter.AdapterMapping;
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
import de.jvstvshd.necrify.common.util.Util;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.Timestamp;
import java.sql.Types;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public abstract class AbstractTemporalPunishment extends AbstractPunishment implements TemporalPunishment {

    private final PunishmentDuration duration;

    public AbstractTemporalPunishment(NecrifyUser user, Component reason, UUID punishmentUuid, PunishmentDuration duration, AbstractNecrifyPlugin plugin, Punishment successor, LocalDateTime issuedAt) {
        super(user, reason, punishmentUuid, plugin, successor, issuedAt);
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
    public final CompletableFuture<Punishment> change(@NotNull PunishmentDuration newDuration, @Nullable LocalDateTime creationTime, Component newReason) throws PunishmentException {
        if (!getType().isBan() && !getType().isMute()) {
            throw new IllegalStateException("only bans and mutes can be changed");
        }
        return executeAsync(() -> {
            var newCreatedAt = creationTime == null ? getCreationTime() : creationTime;
            var newRsn = newReason == null ? getReason() : newReason;
            Query.query(APPLY_CHANGE)
                    .single(Call.of()
                            .bind(convertReason(newRsn))
                            .bind(newDuration.expirationAsTimestamp())
                            .bind(Timestamp.valueOf(newCreatedAt))
                            .bind(getPunishmentUuid(), Adapters.UUID_ADAPTER))
                    .update();
            if (hasSuccessor()) { //we have to update the successor's time of issuance and expiration accordingly
                updateSuccessor().join();
            }
            var builder = new PunishmentBuilder(getPlugin())
                    .withUser(getUser())
                    .withReason(newReason)
                    .withDuration(newDuration)
                    .withPunishmentUuid(getPunishmentUuid())
                    .withSuccessor(getSuccessorOrNull())
                    .withCreationTime(getCreationTime());
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
        }, getExecutor());
    }

    @Override
    protected CompletableFuture<Punishment> applyCancellation() throws PunishmentException {
        return executeAsync(() -> {
            if (hasSuccessor()) {
                updateSuccessor().join();
            }
            var predecessor = getUser().getPunishments().stream()
                    .filter(punishment -> punishment.getSuccessorOrNull() != null && punishment.getSuccessorOrNull().equals(this)).findFirst().orElse(null);
            if (predecessor != null) {
                Query.query(APPLY_SUCCESSOR)
                        .single(Call.of().bind(null, new Adapter<UUID>() {
                            @Override
                            public AdapterMapping<UUID> mapping() {
                                return null;
                            }

                            @Override
                            public int type() {
                                return Types.NULL;
                            }
                        }).bind(predecessor.getPunishmentUuid(), Adapters.UUID_ADAPTER))
                        .update();
                if (hasSuccessor()) {
                    predecessor.setSuccessor(getSuccessor()).join();
                }
            }
            Query.query(APPLY_CANCELLATION)
                    .single(Call.of().bind(getPunishmentUuid(), Adapters.UUID_ADAPTER))
                    .delete();
            return this;
        }, getExecutor());
    }

    private CompletableFuture<Punishment> updateSuccessor() {
        var successor = getSuccessor();
        if (successor instanceof TemporalPunishment temporalSuccessor) {
            var total = temporalSuccessor.totalDuration();
            LocalDateTime newExpiration = LocalDateTime.now().plus(total.javaDuration());
            temporalSuccessor.change(PunishmentDuration.from(newExpiration), LocalDateTime.now(), successor.getReason()).join();
        } //we just assume that everything except a TemporalPunishment expires immediately or not in an infinite time
        return CompletableFuture.completedFuture(successor);
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
                            .bind(getPunishmentUuid(), Adapters.UUID_ADAPTER)
                            .bind(Timestamp.valueOf(getCreationTime())))
                    .insert();
            return this;
        }, getExecutor());
    }

    @Override
    public @NotNull CompletableFuture<Punishment> setSuccessor(@NotNull Punishment successor) {
        if (!getType().getRelatedTypes().contains(successor.getType())) {
            throw new IllegalArgumentException("successor punishment is not related to this punishment");
        }
        if (!getUser().equals(successor.getUser())) {
            throw new IllegalArgumentException("successor punishment is not for the same user");
        }
        if (Util.circularSuccessionChain(this, successor)) {
            throw new IllegalStateException("circular successor chain detected");
        }
        return Util.executeAsync(() -> {
            Query.query(APPLY_SUCCESSOR)
                    .single(Call.of().bind(successor.getUuid(), Adapters.UUID_ADAPTER).bind(getPunishmentUuid(), Adapters.UUID_ADAPTER))
                    .update();
            setSuccessor0(successor);
            LocalDateTime successorNewExpiration;
            if (successor instanceof TemporalPunishment temporalSuccessor) {
                var total = temporalSuccessor.totalDuration();
                successorNewExpiration = duration.expiration().plus(total.javaDuration());
            } else {
                successorNewExpiration = PunishmentDuration.permanent().expiration();
            }
            var issuanceSuccessor = duration.expiration();
            Query.query(APPLY_TIMESTAMP_UPDATE)
                    .single(Call.of()
                            .bind(Timestamp.valueOf(successorNewExpiration))
                            .bind(issuanceSuccessor)
                            .bind(successor.getPunishmentUuid(), Adapters.UUID_ADAPTER))
                    .update();
            if (successor instanceof TemporalPunishment temporalSuccessor) {
                temporalSuccessor.change(PunishmentDuration.from(successorNewExpiration), issuanceSuccessor, successor.getReason()).join();
            }
            return this;
        }, getExecutor());
    }

    @Override
    public @NotNull PunishmentDuration totalDuration() {
        return PunishmentDuration.fromDuration(Duration.between(getCreationTime(), getDuration().expiration()));
    }
}
