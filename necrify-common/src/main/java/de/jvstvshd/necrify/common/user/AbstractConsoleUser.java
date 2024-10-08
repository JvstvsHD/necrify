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

import de.jvstvshd.necrify.api.duration.PunishmentDuration;
import de.jvstvshd.necrify.api.message.MessageProvider;
import de.jvstvshd.necrify.api.punishment.*;
import de.jvstvshd.necrify.api.punishment.log.PunishmentLog;
import de.jvstvshd.necrify.api.user.NecrifyUser;
import de.jvstvshd.necrify.api.user.UserDeletionReason;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public abstract class AbstractConsoleUser implements NecrifyUser {

    private final Locale locale;
    private final MessageProvider provider;

    public AbstractConsoleUser(Locale locale, MessageProvider provider) {
        this.locale = locale;
        this.provider = provider;
    }

    public AbstractConsoleUser(MessageProvider provider) {
        this.provider = provider;
        this.locale = Locale.getDefault();
    }

    private <T> T throwUnsupported() {
        throw new UnsupportedOperationException("This method is not supported for console users.");
    }

    @Override
    public @NotNull UUID getUuid() {
        return new UUID(0, 0);
    }

    @Override
    public @Nullable String getUsername() {
        return "CONSOLE";
    }

    @Override
    public @NotNull CompletableFuture<Ban> ban(@Nullable Component reason, @NotNull PunishmentDuration duration) {
        return throwUnsupported();
    }

    @Override
    public @NotNull CompletableFuture<Ban> banPermanent(@Nullable Component reason) {
        return throwUnsupported();
    }

    @Override
    public @NotNull CompletableFuture<Mute> mute(@Nullable Component reason, @NotNull PunishmentDuration duration) {
        return throwUnsupported();
    }

    @Override
    public @NotNull CompletableFuture<Mute> mutePermanent(@Nullable Component reason) {
        return throwUnsupported();
    }

    @Override
    public @NotNull CompletableFuture<Kick> kick(@Nullable Component reason) {
        return throwUnsupported();
    }

    @Override
    public @NotNull <T extends Punishment> List<T> getPunishments(PunishmentType... types) {
        return List.of();
    }

    @Override
    public @NotNull CompletableFuture<String> queryUsername(boolean update) {
        return CompletableFuture.completedFuture("CONSOLE");
    }

    @Override
    public boolean isWhitelisted() {
        return false;
    }

    @Override
    public CompletableFuture<Boolean> setWhitelisted(boolean whitelisted) {
        return throwUnsupported();
    }

    @Override
    public CompletableFuture<Integer> delete(@NotNull UserDeletionReason reason) {
        return throwUnsupported();
    }

    @Override
    public boolean hasPermission(@NotNull String permission) {
        return true;
    }

    @Override
    public @NotNull Locale getLocale() {
        return locale;
    }

    @Override
    public void sendMessage(@NotNull String key, TextColor color, Component... args) {
        sendMessage(provider().provide(key, getLocale(), args).color(color));
    }

    public MessageProvider provider() {
        return provider;
    }

    @Override
    public @NotNull CompletableFuture<PunishmentLog> loadPunishmentLog() {
        return throwUnsupported();
    }
}
