/*
 * This file is part of Necrify (formerly Velocity Punishment), a plugin designed to manage player's punishments for the platforms Velocity and partly Paper.
 * Copyright (C) 2022-2025 JvstvsHD
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

package de.jvstvshd.necrify.common.template;

import de.jvstvshd.necrify.api.duration.PunishmentDuration;
import de.jvstvshd.necrify.api.punishment.PunishmentType;
import de.jvstvshd.necrify.api.template.NecrifyTemplate;
import de.jvstvshd.necrify.api.template.NecrifyTemplateStage;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

public record MinecraftTemplateStage(NecrifyTemplate template, PunishmentType punishmentType,
                                     PunishmentDuration duration, Component reason) implements NecrifyTemplateStage {

    @Override
    public @NotNull CompletableFuture<Void> changeDuration(PunishmentDuration duration) {
        return null;
    }

    @Override
    public @NotNull CompletableFuture<Void> delete() {
        return null;
    }

    @Override
    public @NotNull PunishmentType punishmentType() {
        return null;
    }
}
