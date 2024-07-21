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

package de.jvstvshd.necrify.api.message;

import net.kyori.adventure.text.Component;
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
     * Prefixes the provided components into one component.
     * @param args the components to prefix.
     * @return one prefixed component.
     */
    @NotNull
    default Component prefixed(Component... args) {
        var comp = prefix();
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
}
