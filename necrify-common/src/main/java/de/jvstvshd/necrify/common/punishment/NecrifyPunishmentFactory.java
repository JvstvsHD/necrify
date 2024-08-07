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
import de.jvstvshd.necrify.api.punishment.PunishmentFactory;
import de.jvstvshd.necrify.api.punishment.PunishmentType;
import de.jvstvshd.necrify.api.user.NecrifyUser;
import de.jvstvshd.necrify.common.AbstractNecrifyPlugin;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

public class NecrifyPunishmentFactory implements PunishmentFactory {

    private final AbstractNecrifyPlugin plugin;

    public NecrifyPunishmentFactory(AbstractNecrifyPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull Punishment createPunishment(@NotNull PunishmentType type, @NotNull Map<String, Object> data) {
        final PunishmentDuration duration = (PunishmentDuration) data.get("duration");
        final Component reason = (Component) data.get("reason");
        final UUID punishmentUuid = (UUID) data.get("punishmentUuid");
        final NecrifyUser user = (NecrifyUser) data.get("user");
        final Punishment successor = (Punishment) data.get("successor");
        final LocalDateTime creationTime = (LocalDateTime) data.get("issued_at");
        var builder = PunishmentBuilder.newBuilder(plugin)
                .withDuration(duration)
                .withReason(reason)
                .withUser(user)
                .withPunishmentUuid(punishmentUuid)
                .withSuccessor(successor)
                .withCreationTime(creationTime);
        Punishment punishment;
        switch (type.standard()) {
            case TEMPORARY_BAN, PERMANENT_BAN -> punishment = builder.buildBan();
            case TEMPORARY_MUTE, PERMANENT_MUTE -> punishment = builder.buildMute();
            case KICK -> punishment = builder.buildKick();
            default -> throw new UnsupportedOperationException("unhandled punishment type: " + type.getName());
        }
        return punishment;
    }
}
