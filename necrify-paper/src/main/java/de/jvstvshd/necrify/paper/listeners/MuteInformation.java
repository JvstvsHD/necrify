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

package de.jvstvshd.necrify.paper.listeners;

import de.jvstvshd.necrify.api.duration.PunishmentDuration;
import de.jvstvshd.necrify.common.plugin.MuteData;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;

public class MuteInformation {

    private final Player player;
    private final UUID punishmentUUID;
    private Component reason;
    private PunishmentDuration duration;

    public MuteInformation(Component reason, PunishmentDuration duration, Player player, UUID punishmentUUID) {
        this.reason = reason;
        this.duration = duration;
        this.player = player;
        this.punishmentUUID = punishmentUUID;
    }

    public static MuteInformation from(MuteData muteData) {
        return new MuteInformation(MiniMessage.miniMessage().deserialize(muteData.getReason()),
                PunishmentDuration.from(muteData.getExpiration()), Bukkit.getPlayer(muteData.getUuid()), muteData.getPunishmentId());
    }

    public void updateTo(MuteInformation other) {
        synchronized (this) {
            this.reason = other.reason;
            this.duration = other.duration;
        }
    }

    public Component getReason() {
        return reason;
    }

    public MuteInformation setReason(Component reason) {
        this.reason = reason;
        return this;
    }

    public PunishmentDuration getDuration() {
        return duration;
    }

    public MuteInformation setDuration(PunishmentDuration duration) {
        this.duration = duration;
        return this;
    }

    public Player getPlayer() {
        return player;
    }

    public UUID getPunishmentUUID() {
        return punishmentUUID;
    }
}
