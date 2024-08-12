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
package de.jvstvshd.necrify.velocity.user;

import com.velocitypowered.api.proxy.ConsoleCommandSource;
import de.jvstvshd.necrify.api.message.MessageProvider;
import de.jvstvshd.necrify.common.user.AbstractConsoleUser;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

public class VelocityConsoleUser extends AbstractConsoleUser {

    private final ConsoleCommandSource console;

    public VelocityConsoleUser(Locale locale, MessageProvider provider, ConsoleCommandSource console) {
        super(locale, provider);
        this.console = console;
    }

    public VelocityConsoleUser(MessageProvider provider, ConsoleCommandSource console) {
        super(provider);
        this.console = console;
    }

    @Override
    public void sendMessage(@NotNull Component message) {
        console.sendMessage(message);
    }

    @Override
    public void sendErrorMessage() {
        sendMessage(provider().internalError());
    }
}
