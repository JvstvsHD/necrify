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
package de.jvstvshd.necrify.api;

import de.jvstvshd.necrify.api.event.EventDispatcher;
import de.jvstvshd.necrify.api.message.MessageProvider;
import de.jvstvshd.necrify.api.punishment.Punishment;
import de.jvstvshd.necrify.api.punishment.PunishmentManager;
import de.jvstvshd.necrify.api.punishment.PunishmentType;
import de.jvstvshd.necrify.api.punishment.util.PlayerResolver;
import de.jvstvshd.necrify.api.user.UserManager;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

/**
 * Core part of the velocity punishment api. Used to get and/or set components belonging to this api.
 * Example:<br>
 * <pre>{@code
 *     //Obtain an instance of Necrify's UserManager
 *     UserManager userManager = necrify.getUserManager();
 *     //Necrify User, obtained via player name
 *     NecrifyUser user = userManager.loadUser("username");
 *     //Alternatively, obtain the user via the player's uuid
 *     NecrifyUser user = userManager.loadUser("uuid");
 *     //You can also use the #get methods to obtain the user instance of either an online player or a cached player.
 *     //parse the duration of the punishment from a string in the format [number, ranging from 0 to Long.MAX_VALUE] and one char for s [second], m[minute], h[our], d[ay].
 *     PunishmentDuration duration = PunishmentDuration.parse("1d");
 *     //Create a reason as an adventure component.
 *     Component reason = Component.text("You are banned from this server!").color(NamedTextColor.RED);
 *     //Create the ban (or another punishment) with the corresponding method(s).
 *     Ban ban = user.ban(reason, duration);
 *     //You can now use this punishment instance for further operations:
 *     //Change the punishment
 *
 *
 * }</pre>
 * @author JvstvsHD
 * @version 1.0.0
 */
public interface Necrify {

    /**
     * @deprecated Will be removed with {@link PunishmentManager}.
     */
    @Deprecated(since = "1.2.0", forRemoval = true)
    @ApiStatus.ScheduledForRemoval(inVersion = "1.3.0")
    PunishmentManager getPunishmentManager();

    /**
     * @deprecated Will be removed with {@link PunishmentManager}.
     */
    @Deprecated(since = "1.2.0", forRemoval = true)
    @ApiStatus.ScheduledForRemoval(inVersion = "1.3.0")
    void setPunishmentManager(PunishmentManager punishmentManager);

    /**
     * @deprecated Will be removed with {@link PunishmentManager}.
     */
    @Deprecated(since = "1.2.0", forRemoval = true)
    @ApiStatus.ScheduledForRemoval(inVersion = "1.3.0")
    PlayerResolver getPlayerResolver();

    /**
     * @deprecated Will be removed with {@link PunishmentManager}.
     */
    @Deprecated(since = "1.2.0", forRemoval = true)
    @ApiStatus.ScheduledForRemoval(inVersion = "1.3.0")
    void setPlayerResolver(PlayerResolver playerResolver);

    /**
     * Returns the executor service used by the plugin.
     *
     * @return the executor service.
     * @deprecated Rename in favor of {@link #getExecutor()}.
     */
    @ApiStatus.ScheduledForRemoval(inVersion = "1.3.0")
    @Deprecated(since = "1.2.0", forRemoval = true)
    @NotNull
    ExecutorService getService();

    /**
     * Returns the executor service used by the plugin. This {@link ExecutorService} is used for asynchronous operations,
     * such as database queries, event dispatching or time-consuming network/IO operations.
     *
     * @return the executor service.
     */
    @NotNull
    ExecutorService getExecutor();

    @NotNull
    MessageProvider getMessageProvider();

    void setMessageProvider(@NotNull MessageProvider messageProvider);

    @NotNull
    UserManager getUserManager();

    void setUserManager(@NotNull UserManager userManager);

    /**
     * Retrieves a punishment by its id.
     *
     * @param punishmentId the id of the punishment.
     * @param <T>          the type of the punishment.
     * @return a future containing the punishment or {@link Optional#empty()} if not found.
     * @since 1.2.0
     */
    default <T extends Punishment> CompletableFuture<Optional<T>> getPunishment(@NotNull UUID punishmentId) {
        return getPunishmentManager().getPunishment(punishmentId, getService());
    }

    /**
     * Returns the system's event dispatcher.
     *
     * @return the event dispatcher.
     */
    @NotNull
    EventDispatcher getEventDispatcher();

    /**
     * Sets the event dispatcher.
     *
     * @param eventDispatcher the event dispatcher to set.
     */
    void setEventDispatcher(@NotNull EventDispatcher eventDispatcher);
}
