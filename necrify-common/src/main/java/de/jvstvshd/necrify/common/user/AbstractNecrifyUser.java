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

import de.jvstvshd.necrify.api.message.MessageProvider;
import de.jvstvshd.necrify.api.user.NecrifyUser;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.audience.MessageType;
import net.kyori.adventure.identity.Identified;
import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.text.format.TextColor;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

public abstract class AbstractNecrifyUser implements NecrifyUser {

    private final MessageProvider provider;

    protected AbstractNecrifyUser(MessageProvider provider) {
        this.provider = provider;
    }

    @Override
    public void sendMessage(@NotNull String key, TextColor color, Component... args) {
        sendMessage(provider.provide(key, args).color(color));
    }

    @Override
    public void sendErrorMessage() {
        sendMessage(provider.internalError());
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

    public abstract void executeOnAudience(@NotNull Consumer<Audience> consumer);

    public MessageProvider getProvider() {
        return provider;
    }
}
