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
