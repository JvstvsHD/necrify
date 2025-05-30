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

import de.jvstvshd.necrify.api.duration.PunishmentDuration;
import de.jvstvshd.necrify.api.punishment.*;
import de.jvstvshd.necrify.api.template.NecrifyTemplate;
import de.jvstvshd.necrify.api.template.NecrifyTemplateStage;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Represents a user of the Necrify API. An user is always associated with an existing Minecraft account. Per MC account
 * and system, there can only exist one user instance. Instances are uniquely identified by their UUID, the same as the
 * one used by Mojang.
 * <p>
 * User objects are used to enforce punishments and to retrieve a user's current and past punishments, including changes
 * to them (not yet implemented).
 *
 * @since 1.2.0
 */
public interface NecrifyUser extends CommandSender {

    /**
     * The UUID of the user. This UUID is the same as the one used by Mojang and is unique to each user.
     *
     * @return the UUID of the user.
     */
    @NotNull
    UUID getUuid();

    /**
     * The username of the user. This is the username of the user's Minecraft account. This method may return null if the
     * name is unknown to the system since only a {@link #getUuid() UUID} is required to identify a user. If a non-null
     * value is returned, it is only the last-known username. The current username associated with the UUID may differ.
     *
     * @return the last-known username of the user
     */
    @Nullable
    String getUsername();

    /**
     * Bans the user with the given reason and duration. A banned user is not able to join the server this system belongs to
     * while the ban is active. The duration is the time the ban is active. For permanent bans you may also refer to
     * {@link #banPermanent(Component)}. If this user is still online, he will be removed from the server immediately after
     * the ban was enforced. The {@link Punishment#punish()} method is called before the object instance is returned. An
     * additional, manual call to this method is not required and may cause data inconsistency.
     *
     * @param reason   The reason why the user is banned. This reason is displayed to the user when they try to join the server.
     *                 If null, a configurable default reason is used.
     * @param duration How long the ban is active.
     * @return the ban object representing the ban. The ban may not be active yet, as the execution takes some time to complete.
     */
    @NotNull
    CompletableFuture<Ban> ban(@Nullable Component reason, @NotNull PunishmentDuration duration);

    /**
     * Bans the user permanently with the given reason. A permanently banned user is not able to join the server this system
     * belongs to ever again. If this user is still online, he will be removed from the server immediately after the ban was
     * enforced. The {@link Punishment#punish()} method is called before the object instance is returned. An additional, manual
     * call to this method is not required and may cause data inconsistency.
     *
     * @param reason The reason why the user is banned. This reason is displayed to the user when they try to join the server.
     *               If null, a configurable default reason is used.
     * @return the ban object representing the ban. The ban may not be active yet, as the execution takes some time to complete.
     */
    @NotNull
    CompletableFuture<Ban> banPermanent(@Nullable Component reason);

    /**
     * Mutes the user with the given reason and duration. A muted user is not able to send messages in the chat of the server
     * this system belongs to while the mute is active. The duration is the time the mute is active. For permanent mutes,
     * you may also refer to {@link #mutePermanent(Component)}. The {@link Punishment#punish()} method is called before the
     * object instance is returned. An additional, manual call to this method is not required and may cause data inconsistency.
     *
     * @param reason   The reason why the user is muted. This reason is displayed to the user when they try to send a message.
     *                 If null, a configurable default reason is used.
     * @param duration How long the mute is active.
     * @return the mute object representing the mute. The mute may not be active yet, as the execution takes some time to complete.
     */
    @NotNull
    CompletableFuture<Mute> mute(@Nullable Component reason, @NotNull PunishmentDuration duration);

    /**
     * Mutes the user permanently with the given reason. A permanently muted user is not able to send messages in the chat of
     * the server this system belongs to ever again. The {@link Punishment#punish()} method is called before the object instance
     * is returned. An additional, manual call to this method is not required and may cause data inconsistency.
     *
     * @param reason The reason why the user is muted. This reason is displayed to the user when they try to send a message.
     *               If null, a configurable default reason is used.
     * @return the mute object representing the mute. The mute may not be active yet, as the execution takes some time to complete.
     */
    @NotNull
    CompletableFuture<Mute> mutePermanent(@Nullable Component reason);

    /**
     * Kicks the user with the given reason. A kicked user is removed from the server this system belongs to. They are able to
     * join again immediately after being kicked if not banned. The reason is displayed to the user when they are kicked.
     *
     * @param reason The reason why the user is kicked. This reason is displayed to the user when they are kicked.
     *               If null, a configurable default reason is used.
     * @return the kick object representing the kick. The kick may not be active yet, as the execution takes some time to complete.
     */
    @NotNull
    CompletableFuture<Kick> kick(@Nullable Component reason);

    /**
     * Punishes the user with the given punishment. This method will automatically determine the type of punishment
     * and use it to punish the user.
     *
     * @param punishment the punishment to apply
     * @return a {@link CompletableFuture} containing the punishment if the operation completed successfully
     * @since 1.2.3
     */
    @NotNull
    CompletableFuture<Punishment> punish(@NotNull Punishment punishment);

    /**
     * This method queries all punishments with the given {@link UUID} of a player and returns them in a list.
     * All punishments that are returned are still running, i.e. they are not expired or revoked.
     *
     * @param <T>   the type of punishment(s), matching them in <code>type</code>
     * @param types the types of punishments to query. If no type is given, all punishments that are stored as running
     *              punishments are queried (not including one-time punishments like kicks).
     * @return the list of punishments which are stored at the moment. This list may contains punishments which are over.
     */
    @NotNull
    <T extends Punishment> List<T> getPunishments(PunishmentType... types);

    /**
     * Queries the account username through the Mojang API. This method may take some time to complete. You may also update
     * the user name by setting the update parameter to true. If the username is updated, it is also returned. If not, only
     *
     * @param update Whether the username should be updated. If true, the username is updated and returned. If false, it will only be returned.
     * @return a {@link CompletableFuture} containing the username of the user. This future may be completed exceptionally if the username could not be queried.
     */
    @NotNull
    CompletableFuture<String> queryUsername(boolean update);

    /**
     * Checks whether the user is whitelisted. If the user is whitelisted and the server whitelist is active,
     * they are able to join the server this system belongs to.
     *
     * @return true if the user is whitelisted, false otherwise.
     */
    boolean isWhitelisted();

    /**
     * Sets the user to be whitelisted. If the user is whitelisted and the server whitelist is active,
     * they are able to join the server this system belongs to.
     * <p>
     * The user also gets kicked if they are online and get blacklisted if the whitelist is active.
     *
     * @param whitelisted true if the user is whitelisted, false otherwise.
     * @return a {@link CompletableFuture} containing the result of the operation. This future completes with true if anything
     * has changed and false if the whitelist status is already set to the given value.
     */
    CompletableFuture<Boolean> setWhitelisted(boolean whitelisted);

    /**
     * Deletes this user from the system. This will also delete all punishments associated with this user.
     * This action is irreversible and should be used with caution.
     * <p>
     * The reason for the deletion is required and is used to log the deletion reason and inform listeners about the deletion
     * and its reasoning.
     *
     * @param reason the reason for the deletion.
     * @return a {@link CompletableFuture} containing the amount of affected rows and punishments.
     */
    CompletableFuture<Integer> delete(@NotNull UserDeletionReason reason);

    /**
     * Gets a punishment by its UUID. This punishment has to be valid, i.e. it is still running. A punishment that
     * was revoked or expired is not returned by this method.
     *
     * @param punishmentUuid the UUID of the punishment.
     * @return the punishment with the given UUID, if it exists.
     */
    default Optional<Punishment> getPunishment(@NotNull UUID punishmentUuid) {
        return getPunishments().stream().filter(punishment -> punishment.getPunishmentUuid().equals(punishmentUuid)).findFirst();
    }

    /**
     * Gets this player's current locale. This locale is used to provide messages in the user's language. If the locale
     * is not set, the default locale is used. Note: The locale may be provided from the language the user has set in
     * their Minecraft client. This information is only sent after the client connected to the server, so calls to this
     * during the connection process usually return the default locale.
     *
     * @return the locale of the user.
     */
    @NotNull
    Locale getLocale();

    /**
     * Gets the current template stage of the given template. If this user has no punishments applied through this template,
     * this method will return an empty optional.
     *
     * @param template the template to get the current stage for
     * @return the current stage of the template for this user or an empty optional
     * @since 1.2.3
     */
    @NotNull Optional<NecrifyTemplateStage> getCurrentTemplateStage(@NotNull NecrifyTemplate template);

    /**
     * Gets the next template stage of the given template. If this user has no punishments applied through this template,
     * this method will return the first stage of the template.
     * <p>If the user already maxed out this template, this method may throw an error.</p>
     *
     * @param template the template to get the next stage for
     * @return the next stage of the template for this user
     * @throws IllegalStateException if the user already maxed out this template
     * @since 1.2.3
     */
    @NotNull
    NecrifyTemplateStage getNextTemplateStage(@NotNull NecrifyTemplate template);

    /**
     * Punishes the user with the given template. This method will automatically determine the next stage of the template
     * and use it to punish the user. If the user already maxed out this template, this method may throw an error.
     *
     * @param template the template to use for the punishment
     * @return the punishment object representing the punishment contained in a {@link CompletableFuture} if the operation completed successfully
     * @throws IllegalStateException if the user already maxed out this template
     * @see #getNextTemplateStage(NecrifyTemplate)
     * @since 1.2.3
     */
    @NotNull
    CompletableFuture<Punishment> punishModelled(@NotNull NecrifyTemplate template);

    /**
     * Amnesties this user regarding the given template meaning a reset of the user-bound stage index of this template.
     * If {@code stageIndex == 0}, this user will receive full amnesty regarding the template. {@code stageIndex} always
     * marks the index of a stage that this user would receive as soon as punished again through the template.
     * @param template the template to use for the amnesty
     * @param stageIndex the stage index that would be received by this user after amnesty
     * @return a {@link CompletableFuture}
     * @since 1.2.3
     * @throws IllegalArgumentException if {@code stageIndex} is negative or greater than the maximum stage index of the given template
     */
    CompletableFuture<Void> amnesty(@NotNull NecrifyTemplate template, int stageIndex);
}