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

package de.jvstvshd.necrify.api.user;

import de.jvstvshd.necrify.api.message.MessageProvider;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

/**
 * Represents an entity that is able to interact with the server via messages and may send commands to it. It is also
 * able to hold permissions.
 */
public interface CommandSender {

    /**
     * Sends a message to the command sender. The message may be given in form of a {@link net.kyori.adventure.text.TranslatableComponent},
     * which will be translated to the correct language when being displayed.
     * @param message a non-null component that represents the message to be sent.
     */
    void sendMessage(@NotNull Component message);

    /**
     * Sends a message to the command sender. The message must contain a valid translation key that is present in the
     * language files of the server. The message will be translated to the correct language when being displayed, if a
     * translation is available. If this key does not map to a translation, the key itself will be displayed.
     * <p>
     * Per default, this will use {@link de.jvstvshd.necrify.api.message.MessageProvider#provide(String, Component...)}
     * @param key a non-null string that represents the translation key of the message to be sent.
     */
    void sendMessage(@NotNull String key, Component... args);

    /**
     * Sends an error message to the command sender. This should be used to inform the command sender about an error
     * that happened within the command execution, e.g. while updating data in the database. Ideally, this yields the same
     * result as {@code sendMessage("error.internal")} content-wise, but may differ style-wise.
     * @see #sendMessage(String, Component...) 
     * @see MessageProvider#internalError() 
     * @see MessageProvider#internalError(Locale) 
     */
    void sendErrorMessage();

    /**
     * Checks whether the command sender has a certain permission. This requires a permission system to be set up
     * and the entity in question to exist.
     * @param permission a non-null string that represents the permission to be checked.
     * @return true if the command sender has the permission, false otherwise.
     */
    boolean hasPermission(@NotNull String permission);
}
