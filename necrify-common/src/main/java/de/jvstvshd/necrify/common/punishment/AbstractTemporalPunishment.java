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

import de.chojo.sadu.queries.api.call.Call;
import de.chojo.sadu.queries.api.query.Query;
import de.jvstvshd.necrify.api.PunishmentException;
import de.jvstvshd.necrify.api.duration.PunishmentDuration;
import de.jvstvshd.necrify.api.event.punishment.PunishmentChangedEvent;
import de.jvstvshd.necrify.api.punishment.Punishment;
import de.jvstvshd.necrify.api.punishment.StandardPunishmentType;
import de.jvstvshd.necrify.api.punishment.TemporalPunishment;
import de.jvstvshd.necrify.api.punishment.log.PunishmentLogAction;
import de.jvstvshd.necrify.api.user.NecrifyUser;
import de.jvstvshd.necrify.common.AbstractNecrifyPlugin;
import de.jvstvshd.necrify.common.io.Adapters;
import de.jvstvshd.necrify.common.util.Util;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextReplacementConfig;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.Timestamp;
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

    public @NotNull PunishmentDuration getDuration() {
        return duration;
    }

    @Override
    public boolean isOngoing() {
        return getDuration().expiration().isAfter(LocalDateTime.now());
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
    public @NotNull Component getReason() {
        return super.getReason().replaceText(TextReplacementConfig
                .builder()
                .matchLiteral("<UNTIL>")
                .replacement(Component.text(getDuration().remainingDuration(), NamedTextColor.YELLOW))
                .build());
    }

    @Override
    public final @NotNull CompletableFuture<Punishment> change(@NotNull PunishmentDuration newDuration, @Nullable LocalDateTime creationTime, Component newReason) throws PunishmentException {
        if (!getType().isBan() && !getType().isMute()) {
            throw new IllegalStateException("only bans and mutes can be changed");
        }
        var oldDuration = getDuration();
        var oldReason = getReason();
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
                    .withCreationTime(newCreatedAt)
                    .withPunishmentUuid(getPunishmentUuid())
                    .withSuccessor(getSuccessorOrNull());
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
            var predecessor = getPredecessor();
            if (predecessor != null) {
                if (hasSuccessor()) {
                    predecessor.setSuccessor(getSuccessor()).join();
                } else {
                    predecessor.setSuccessor(null);
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
                            .bind(convertReason(getRawReason()))
                            .bind(getPunishmentUuid(), Adapters.UUID_ADAPTER)
                            .bind(Timestamp.valueOf(getCreationTime())))
                    .insert();
            return this;
        }, getExecutor());
    }

    @Override
    public @NotNull CompletableFuture<Punishment> setSuccessor(Punishment successor) {
        if (successor == null) {
            return Util.executeAsync(() -> {
                Query.query(APPLY_SUCCESSOR)
                        .single(Call.of().bind(null, Adapters.UUID_NULL_ADAPTER).bind(getPunishmentUuid(), Adapters.UUID_ADAPTER))
                        .update();
                return this;
            }, getExecutor());
        }
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
                    .single(Call.of().bind(successor.getPunishmentUuid(), Adapters.UUID_ADAPTER).bind(getPunishmentUuid(), Adapters.UUID_ADAPTER))
                    .update();
            LocalDateTime successorNewExpiration;
            if (successor instanceof TemporalPunishment temporalSuccessor) {
                var total = temporalSuccessor.totalDuration();
                successorNewExpiration = duration.expiration().plus(total.javaDuration());
            } else {
                successorNewExpiration = PunishmentDuration.PERMANENT.expiration();
            }
            var issuanceSuccessor = duration.expiration();
            Query.query(APPLY_TIMESTAMP_UPDATE)
                    .single(Call.of()
                            .bind(Timestamp.valueOf(successorNewExpiration))
                            .bind(issuanceSuccessor)
                            .bind(successor.getPunishmentUuid(), Adapters.UUID_ADAPTER))
                    .update();
            if (successor instanceof TemporalPunishment temporalSuccessor) {
                var newSuccessor = temporalSuccessor.change(PunishmentDuration.from(successorNewExpiration), issuanceSuccessor, successor.getReason()).join();
                setSuccessor0(newSuccessor);
            } else {
                setSuccessor0(successor);
            }
            return this;
        }, getExecutor());
    }

    @Override
    public @NotNull PunishmentDuration totalDuration() {
        return PunishmentDuration.fromDuration(Duration.between(getCreationTime(), getDuration().expiration()));
    }
}
