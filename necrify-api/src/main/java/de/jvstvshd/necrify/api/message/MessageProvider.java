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
package de.jvstvshd.necrify.api.message;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

/**
 * An interface that is responsible for providing messages to the user. The messages are displayed internationalized to the
 * end user, so the implementation should be able to provide messages in different languages or a default language.
 */
public interface MessageProvider {

    /**
     * Provides a message to the user with the given key in the given locale. If the locale is null, the system's {@link #defaultLocale() default Locale}
     * is used.
     *
     * @param key    an unique key identifying the message to provide.
     * @param locale the locale in whose language the message should be provided.
     * @param args   additional components that fill in placeholders in the message.
     * @return the translated and formatted message.
     */
    @NotNull
    Component provide(@NotNull String key, @Nullable Locale locale, Component... args);

    /**
     * Provides a {@link net.kyori.adventure.text.TranslatableComponent} containing the message specified by the given key.
     * This does not need a locale as it is determined by the server when sending the [{@link net.kyori.adventure.text.TranslatableComponent}] to the client.
     *
     * @param key  an unique key identifying the message to provide.
     * @param args additional components that fill in placeholders in the message.
     * @return the formatted message ready to be translated to the user.
     */
    @NotNull
    Component provide(@NotNull String key, Component... args);

    /**
     * Provides a message to the user describing that an internal error occurred while executing a command.
     * The message is prefixed depending on the value of {@link #autoPrefixed()}.
     *
     * @param locale the locale in whose language the message should be provided. If locale is null, the system's {@link #defaultLocale() default Locale}
     *               is used.
     * @return the translated message stating the occurrence of an internal error.
     */
    @NotNull
    Component internalError(@Nullable Locale locale);

    /**
     * Provides a message to the user describing that an internal error occurred while executing a command.
     * The message is prefixed depending on the value of {@link #autoPrefixed()}. It is translated when sent to the user
     * as a {@link net.kyori.adventure.text.TranslatableComponent}.
     *
     * @return an translatable component stating the occurrence of an internal error.
     */
    @NotNull
    Component internalError();

    /**
     * Provides this system's prefix to display in front of messages.
     *
     * @return the prefix component
     */
    @NotNull
    Component prefix();

    /**
     * Provides a message to the user with the given key in the default locale. This yields the same result content-wise
     * as calling {@link #provide(String, Locale, Component...)}, but as String instead of a Component.
     *
     * @param key    an unique key identifying the message to provide.
     * @param locale the locale in whose language the message should be provided. If locale is null, the system's {@link #defaultLocale() default Locale}
     * @param args   additional components that fill in placeholders in the message.
     * @return the translated and formatted message as a string.
     */
    @NotNull
    default String provideString(@NotNull String key, @Nullable Locale locale, Component... args) {
        return PlainTextComponentSerializer.plainText().serialize(provide(key, locale, args));
    }

    /**
     * Prefixes the provided components into one component.
     *
     * @param args the components to prefix.
     * @return one prefixed component.
     */
    @NotNull
    default Component prefixed(Component... args) {
        Component comp;
        if (autoPrefixed()) {
            comp = prefix();
        } else {
            comp = Component.empty();
        }
        for (Component arg : args) {
            comp = comp.append(arg);
        }
        return comp;
    }

    /**
     * Provides the default locale of the system. This is used when no locale is provided in the methods or the translation
     * does not exist in the provided locale. Per default, this is {@link Locale#ENGLISH}.
     *
     * @return the default locale of the system.
     */
    @NotNull
    default Locale defaultLocale() {
        return Locale.ENGLISH;
    }

    /**
     * @return whether this message provider automatically prefixes messages.
     */
    boolean autoPrefixed();

    /**
     * Sets whether this message provider should automatically prefix messages.
     *
     * @param autoPrefixed true if messages should be automatically prefixed, false otherwise.
     * @throws UnsupportedOperationException if this message provider does not support changing the auto-prefixing behavior (default behaviour)
     */
    default void autoPrefixed(boolean autoPrefixed) {
        throw new UnsupportedOperationException("This message provider does not support changing the auto-prefixing behavior.");
    }

    /**
     * Returns a new message provider that does not prefix messages.
     *
     * @return a new message provider that does not prefix messages.
     */
    default MessageProvider unprefixedProvider() {
        throw new UnsupportedOperationException("This message provider does not support changing the auto-prefixing behavior.");
    }
}
