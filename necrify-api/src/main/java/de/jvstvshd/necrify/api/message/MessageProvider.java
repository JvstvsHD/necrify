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
import org.jetbrains.annotations.ApiStatus;
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
    default Component provide(@NotNull String key, @Nullable Locale locale, Component... args) {
        return provide(key, locale, autoPrefixed(), args);
    }

    /**
     * Provides a {@link net.kyori.adventure.text.TranslatableComponent} containing the message specified by the given key.
     * This does not need a locale as it is determined by the server when sending the [{@link net.kyori.adventure.text.TranslatableComponent}] to the client.
     *
     * @param key  an unique key identifying the message to provide.
     * @param args additional components that fill in placeholders in the message.
     * @return the formatted message ready to be translated to the user.
     */
    @NotNull
    default Component provide(@NotNull String key, Component... args) {
        return provide(key, autoPrefixed(), args);
    }

    /**
     * Provides a message to the user with the given key in the given locale. If the locale is null, the system's {@link #defaultLocale() default Locale}
     * is used.
     *
     * @param key    an unique key identifying the message to provide.
     * @param locale the locale in whose language the message should be provided.
     * @param prefixed whether the prefix should be prefixed or not - overwriting the auto-prefixing behaviour.
     * @param args   additional components that fill in placeholders in the message.
     * @return the translated and formatted message.
     * @since 1.2.2
     */
    @NotNull
    Component provide(@NotNull String key, @Nullable Locale locale, boolean prefixed, Component... args);

    /**
     * Provides a {@link net.kyori.adventure.text.TranslatableComponent} containing the message specified by the given key.
     * This does not need a locale as it is determined by the server when sending the [{@link net.kyori.adventure.text.TranslatableComponent}] to the client.
     *
     * @param key  an unique key identifying the message to provide.
     * @param args additional components that fill in placeholders in the message.
     * @param prefixed whether the prefix should be prefixed or not - overwriting the auto-prefixing behaviour.
     * @return the formatted message ready to be translated to the user.
     * @since 1.2.2
     */
    @NotNull
    Component provide(@NotNull String key, boolean prefixed, Component... args);

    /**
     * Provides a message to the user describing that an internal error occurred while executing a command.
     * The message is prefixed depending on the value of {@link #autoPrefixed()}.
     *
     * @param locale the locale in whose language the message should be provided. If locale is null, the system's {@link #defaultLocale() default Locale}
     *               is used.
     * @return the translated message stating the occurrence of an internal error.
     */
    @NotNull
    default Component internalError(@Nullable Locale locale) {
        return internalError(locale, autoPrefixed());
    }

    /**
     * Provides a message to the user describing that an internal error occurred while executing a command.
     * The message is prefixed depending on the value of {@link #autoPrefixed()}. It is translated when sent to the user
     * as a {@link net.kyori.adventure.text.TranslatableComponent}.
     *
     * @return an translatable component stating the occurrence of an internal error.
     */
    @NotNull
    default Component internalError() {
        return internalError(autoPrefixed());
    }

    /**
     * Provides this system's prefix to display in front of messages.
     *
     * @return the prefix component
     */
    @NotNull
    Component prefix();

    /**
     * Provides a message to the user describing that an internal error occurred while executing a command.
     * The message is prefixed depending on the value of {@code prefixed}.
     *
     * @param locale   the locale in whose language the message should be provided. If locale is null, the system's {@link #defaultLocale() default Locale}
     *                 is used.
     * @param prefixed whether the prefix should be prefixed or not - overwriting the auto-prefixing behaviour.
     * @return the translated message stating the occurrence of an internal error.
     * @since 1.2.2
     */
    @NotNull
    Component internalError(@Nullable Locale locale, boolean prefixed);

    /**
     * Provides a message to the user describing that an internal error occurred while executing a command.
     * The message is prefixed depending on the value of {@code prefixed}. It is translated when sent to the user
     * as a {@link net.kyori.adventure.text.TranslatableComponent}.
     *
     * @param prefixed whether the prefix should be prefixed or not - overwriting the auto-prefixing behaviour.
     * @return an translatable component stating the occurrence of an internal error.
     * @since 1.2.2
     */
    @NotNull
    Component internalError(boolean prefixed);

    /**
     * Provides a message to the user with the given key in the default locale. This yields the same result content-wise
     * as calling {@link #provide(String, Locale, Component...)}, but as String instead of a Component.
     *
     * @param key    an unique key identifying the message to provide.
     * @param locale the locale in whose language the message should be provided. If locale is null, the system's {@link #defaultLocale() default Locale}
     * @param args   additional components that fill in placeholders in the message.
     * @param prefixed whether the prefix should be prefixed or not - overwriting the auto-prefixing behaviour.
     * @return the translated and formatted message as a string.
     */
    @NotNull
    default String provideString(@NotNull String key, @Nullable Locale locale, boolean prefixed, Component... args) {
        return PlainTextComponentSerializer.plainText().serialize(provide(key, locale, prefixed, args));
    }

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
        return provideString(key, locale, autoPrefixed(), args);
    }

    /**
     * Prefixes the provided components with this system's prefix into one component containing firstly the prefix and then
     * all provided components in order of the array.
     *
     * @param args the components to prefix.
     * @return one prefixed component.
     */
    @NotNull
    default Component prefixed(Component... args) {
        Component comp = prefix();
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
     * Returns whether this message provider automatically prefixes messages. If this is true, the message provider will
     * automatically prefix messages with the result of {@link #prefix()}. This behaviour can be bypassed by using an
     * {@link #unprefixedProvider()}, though this will only be supported on autoPrefixed variants.
     *
     * @return whether this message provider automatically prefixes messages.
     */
    boolean autoPrefixed();

    /**
     * Sets whether this message provider should automatically prefix messages. A new instance of the message provider is returned
     * that may be the same as the original one, but with a different or same auto-prefixing behavior.
     *
     * @param autoPrefixed true if messages should be automatically prefixed, false otherwise.
     * @return a new message provider with the changed auto-prefixing behavior.
     * @since 1.2.0/1.2.2
     */
    MessageProvider autoPrefixed(boolean autoPrefixed);

    /**
     * Returns a new message provider that does not prefix messages.
     *
     * @return a new message provider that does not prefix messages.
     * @throws UnsupportedOperationException if this message provider does not support changing the auto-prefixing
     *                                       behavior (default behaviour) or does not have a fixed auto-prefix setting
     * @deprecated since 1.2.2, MessageProvider offers possibilities to change the auto-prefixing behavior by calling
     * {@link #autoPrefixed(boolean)}. This method is deprecated and will be removed in 1.3.0.
     */
    @Deprecated(forRemoval = true)
    @ApiStatus.ScheduledForRemoval(inVersion = "1.3.0")
    default MessageProvider unprefixedProvider() {
        throw new UnsupportedOperationException("This message provider does not support changing the auto-prefixing behavior.");
    }
}