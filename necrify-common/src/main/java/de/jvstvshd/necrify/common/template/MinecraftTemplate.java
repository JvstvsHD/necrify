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

import de.jvstvshd.necrify.api.template.NecrifyTemplate;
import de.jvstvshd.necrify.api.template.NecrifyTemplateStage;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;

public record MinecraftTemplate(String name, Collection<NecrifyTemplateStage> stages) implements NecrifyTemplate {

    @Override
    public @NotNull String name() {
        return name;
    }

    @Override
    public @NotNull Collection<NecrifyTemplateStage> stages() {
        return stages;
    }

    @Override
    public @NotNull NecrifyTemplateStage getStage(int index) {
        return stages().stream().skip(index).findFirst().orElseThrow();
    }

    @Override
    public @NotNull CompletableFuture<Void> addStage(NecrifyTemplateStage stage) {
        return null;
    }

    @Override
    public CompletableFuture<Integer> delete() {
        return null;
    }
}
