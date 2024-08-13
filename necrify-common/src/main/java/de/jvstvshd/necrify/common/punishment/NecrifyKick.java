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

import de.jvstvshd.necrify.api.punishment.Kick;
import de.jvstvshd.necrify.api.punishment.PunishmentType;
import de.jvstvshd.necrify.api.punishment.StandardPunishmentType;
import de.jvstvshd.necrify.api.user.NecrifyUser;
import de.jvstvshd.necrify.common.AbstractNecrifyPlugin;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.UUID;

public abstract class NecrifyKick extends AbstractPunishment implements Kick {

    public NecrifyKick(NecrifyUser user, Component reason, UUID punishmentUuid, AbstractNecrifyPlugin plugin, LocalDateTime issuedAt) {
        super(user, reason, punishmentUuid, plugin, null, issuedAt);
    }

    @Override
    public @NotNull PunishmentType getType() {
        return StandardPunishmentType.KICK;
    }

    @Override
    public @NotNull Component createFullReason(Locale locale) {
        return getReason();
    }
}
