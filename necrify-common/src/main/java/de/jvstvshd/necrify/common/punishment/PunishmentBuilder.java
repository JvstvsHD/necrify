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
package de.jvstvshd.necrify.common.punishment;

import de.jvstvshd.necrify.api.duration.PunishmentDuration;
import de.jvstvshd.necrify.api.punishment.Punishment;
import de.jvstvshd.necrify.api.user.NecrifyUser;
import de.jvstvshd.necrify.common.AbstractNecrifyPlugin;
import net.kyori.adventure.text.Component;

import java.time.LocalDateTime;
import java.util.UUID;

public class PunishmentBuilder {

    private final AbstractNecrifyPlugin plugin;
    private NecrifyUser user;
    private Component reason;
    private PunishmentDuration duration;
    private UUID punishmentUuid;
    private Punishment successor;
    private LocalDateTime creationTime;

    public PunishmentBuilder(AbstractNecrifyPlugin plugin) {
        this.plugin = plugin;
    }

    public static PunishmentBuilder newBuilder(AbstractNecrifyPlugin plugin) {
        return new PunishmentBuilder(plugin);
    }

    public NecrifyUser user() {
        return user;
    }

    public PunishmentBuilder withUser(NecrifyUser user) {
        this.user = user;
        return this;
    }

    public Component reason() {
        return reason;
    }

    public PunishmentBuilder withReason(Component reason) {
        this.reason = reason;
        return this;
    }

    public PunishmentDuration duration() {
        return duration;
    }

    public PunishmentBuilder withDuration(PunishmentDuration duration) {
        this.duration = duration;
        return this;
    }

    public UUID punishmentUuid() {
        return punishmentUuid;
    }

    public PunishmentBuilder withPunishmentUuid(UUID punishmentUuid) {
        this.punishmentUuid = punishmentUuid;
        return this;
    }

    public Punishment successor() {
        return successor;
    }

    public PunishmentBuilder withSuccessor(Punishment successor) {
        this.successor = successor;
        return this;
    }

    public LocalDateTime creationTime() {
        return creationTime;
    }

    public PunishmentBuilder withCreationTime(LocalDateTime creationTime) {
        this.creationTime = creationTime;
        return this;
    }

    public NecrifyBan buildBan() {
        validateValues();
        return new NecrifyBan(user, reason, punishmentUuid, duration.absolute(), plugin, successor, creationTime);
    }

    /**
     * Builds a kick punishment. This method ignores the value of {@link #duration()}, {@link #successor()} and {@link #plugin}.
     *
     * @return The kick punishment.
     */
    public NecrifyKick buildKick() {
        validateValues();
        return plugin.createKick(reason, user, punishmentUuid);
    }

    public NecrifyMute buildMute() {
        validateValues();
        return new NecrifyMute(user, reason, punishmentUuid, duration.absolute(), plugin, successor, creationTime);
    }

    private void validateValues() {
        if (punishmentUuid == null)
            punishmentUuid = UUID.randomUUID();
        if (user == null)
            throw new NullPointerException("user is null");
        if (reason == null)
            throw new NullPointerException("reason is null");
    }
}
