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
package de.jvstvshd.necrify.velocity.impl;

import de.jvstvshd.necrify.api.punishment.Punishment;
import de.jvstvshd.necrify.api.user.NecrifyUser;
import de.jvstvshd.necrify.common.AbstractNecrifyPlugin;
import de.jvstvshd.necrify.common.punishment.NecrifyKick;
import de.jvstvshd.necrify.velocity.user.VelocityUser;
import net.kyori.adventure.text.Component;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class VelocityKick extends NecrifyKick {

    public VelocityKick(NecrifyUser user, Component reason, UUID punishmentUuid, AbstractNecrifyPlugin plugin, LocalDateTime issuedAt) {
        super(user, reason, punishmentUuid, plugin, issuedAt);
    }

    @Override
    protected CompletableFuture<Punishment> applyPunishment() {
        if (!(getUser() instanceof VelocityUser))
            throw new IllegalStateException("Cannot kick user: User is not a VelocityUser.");
        var player = ((VelocityUser) getUser()).getPlayer();
        if (player != null)
            player.disconnect(createFullReason(null));
        return CompletableFuture.completedFuture(this);
    }
}
