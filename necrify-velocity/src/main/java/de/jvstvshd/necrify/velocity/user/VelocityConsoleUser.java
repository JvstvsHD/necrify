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
package de.jvstvshd.necrify.velocity.user;

import com.velocitypowered.api.proxy.ConsoleCommandSource;
import de.jvstvshd.necrify.api.message.MessageProvider;
import de.jvstvshd.necrify.common.user.AbstractConsoleUser;
import net.kyori.adventure.text.Component;
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
    public void sendMessage(@NotNull String key, Component... args) {
        sendMessage(provider().provide(key, getLocale(), args));
    }

    @Override
    public void sendErrorMessage() {
        sendMessage(provider().internalError());
    }
}
