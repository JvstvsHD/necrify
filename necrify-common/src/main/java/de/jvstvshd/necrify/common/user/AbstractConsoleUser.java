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

package de.jvstvshd.necrify.common.user;

import de.jvstvshd.necrify.api.duration.PunishmentDuration;
import de.jvstvshd.necrify.api.message.MessageProvider;
import de.jvstvshd.necrify.api.punishment.*;
import de.jvstvshd.necrify.api.user.NecrifyUser;
import de.jvstvshd.necrify.api.user.UserDeletionReason;
import net.kyori.adventure.text.Component;
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

    private void throwUnsupported() {
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
        throwUnsupported();
        return null;
    }

    @Override
    public @NotNull CompletableFuture<Ban> banPermanent(@Nullable Component reason) {
        throwUnsupported();
        return null;
    }

    @Override
    public @NotNull CompletableFuture<Mute> mute(@Nullable Component reason, @NotNull PunishmentDuration duration) {
        throwUnsupported();
        return null;
    }

    @Override
    public @NotNull CompletableFuture<Mute> mutePermanent(@Nullable Component reason) {
        throwUnsupported();
        return null;
    }

    @Override
    public @NotNull CompletableFuture<Kick> kick(@Nullable Component reason) {
        throwUnsupported();
        return null;
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
    public void setWhitelisted(boolean whitelisted) {
        throwUnsupported();
    }

    @Override
    public void delete(@NotNull UserDeletionReason reason) {
        throwUnsupported();
    }

    @Override
    public boolean hasPermission(@NotNull String permission) {
        return true;
    }

    @Override
    public Locale getLocale() {
        return locale;
    }

    public MessageProvider provider() {
        return provider;
    }
}
