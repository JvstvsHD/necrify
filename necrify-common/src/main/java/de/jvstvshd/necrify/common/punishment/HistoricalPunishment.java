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

import de.jvstvshd.necrify.api.PunishmentException;
import de.jvstvshd.necrify.api.duration.PunishmentDuration;
import de.jvstvshd.necrify.api.punishment.Punishment;
import de.jvstvshd.necrify.api.punishment.PunishmentType;
import de.jvstvshd.necrify.api.punishment.TemporalPunishment;
import de.jvstvshd.necrify.api.punishment.log.PunishmentLog;
import de.jvstvshd.necrify.api.punishment.log.PunishmentLogEntry;
import de.jvstvshd.necrify.api.template.NecrifyTemplateStage;
import de.jvstvshd.necrify.api.user.NecrifyUser;
import de.jvstvshd.necrify.common.util.EmptyPunishmentType;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Represents a punishment that already expired or was cancelled. This object is only a reference to the punishment that was deleted
 * for historical purposes and being able to see this punishment's log data. This type will never be returned by {@link NecrifyUser#getPunishments(PunishmentType...)}
 * or similar methods though it may be returned by {@link de.jvstvshd.necrify.api.Necrify#getPunishment(UUID)}. This object is immutable and does not support any
 * manipulation methods of the punishment.
 */
public class HistoricalPunishment implements TemporalPunishment {

    private final UUID punishmentUuid;
    private NecrifyUser user;
    private LocalDateTime creationTime;
    private final PunishmentLog log;
    private LocalDateTime expirationTime;

    public HistoricalPunishment(UUID punishmentUuid, @Nullable NecrifyUser user, LocalDateTime creationTime, LocalDateTime expirationTime, PunishmentLog log) {
        this.punishmentUuid = punishmentUuid;
        this.user = user;
        this.creationTime = creationTime;
        this.log = log;
        this.expirationTime = expirationTime;
    }

    @Override
    public boolean isOngoing() {
        return false;
    }

    @Override
    public CompletableFuture<Punishment> punish() {
        return throwException();
    }

    @Override
    public CompletableFuture<Punishment> cancel() {
        return throwException();
    }

    @Override
    public CompletableFuture<Punishment> change(@Nullable Component newReason) {
        return throwException();
    }

    @Override
    public @NotNull PunishmentType getType() {
        return EmptyPunishmentType.INSTANCE;
    }

    @Override
    public @NotNull UUID getPunishmentUuid() {
        return punishmentUuid;
    }

    @Override
    public @NotNull UUID getUuid() {
        return getPunishmentUuid();
    }

    @Override
    public @NotNull NecrifyUser getUser() {
        return user;
    }

    @Override
    public boolean hasSuccessor() {
        return false;
    }

    @Override
    public @NotNull Punishment getSuccessor() {
        return throwException();
    }

    @Override
    public @NotNull CompletableFuture<Punishment> setSuccessor(Punishment successor) {
        return throwException();
    }

    @Override
    public @NotNull LocalDateTime getCreationTime() {
        return creationTime;
    }

    @Override
    public boolean hasBeenCreated() {
        return true;
    }

    @Override
    public @Nullable Punishment getPredecessor() {
        return null;
    }

    @Override
    public @NotNull CompletableFuture<PunishmentLog> loadPunishmentLog() {
        return CompletableFuture.completedFuture(log);
    }

    @Override
    public @NotNull PunishmentLogEntry createCurrentLogEntry() {
        return throwException();
    }

    @Override
    public @NotNull Component getReason() {
        return log.getLatestEntry().reason();
    }

    @Override
    public @NotNull Component getRawReason() {
        return getReason();
    }

    @Override
    public @NotNull Component createFullReason(@Nullable Locale locale) {
        return getRawReason();
    }

    @Override
    public @NotNull PunishmentDuration getDuration() {
        return PunishmentDuration.fromDuration(Duration.between(creationTime, expirationTime));
    }

    @Override
    public boolean isPermanent() {
        return getDuration().isPermanent();
    }

    @Override
    public @NotNull CompletableFuture<Punishment> change(@NotNull PunishmentDuration newDuration, @Nullable LocalDateTime creationTime, @Nullable Component newReason) throws PunishmentException {
        return throwException();
    }

    @Override
    public @NotNull PunishmentDuration totalDuration() {
        return getDuration();
    }

    @Override
    public @NotNull Optional<NecrifyTemplateStage> getTemplateStage() {
        return Optional.empty();
    }

    private <T> T throwException() {
        throw new UnsupportedOperationException("This method is not supported on this object. This punishment was deleted and this" +
                " instance only serves as a reference to the punishment that was deleted.");
    }

    public HistoricalPunishment setUser(NecrifyUser user) {
        this.user = user;
        return this;
    }

    public HistoricalPunishment setCreationTime(LocalDateTime creationTime) {
        this.creationTime = creationTime;
        return this;
    }

    public HistoricalPunishment setExpirationTime(LocalDateTime expirationTime) {
        this.expirationTime = expirationTime;
        return this;
    }
}
