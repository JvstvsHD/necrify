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

package de.jvstvshd.necrify.api.template;

import de.jvstvshd.necrify.api.duration.PunishmentDuration;
import de.jvstvshd.necrify.api.punishment.PunishmentType;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

/**
 * A stage of a {@link NecrifyTemplate template} that can be used to punish a user gradually. One stage consists of
 * the punishment type, the punishment duration and the punishment reason.
 */
public interface NecrifyTemplateStage extends Comparable<NecrifyTemplateStage> {

    /**
     * Returns the punishment duration of this stage that would apply if this stage was used to punish someone.
     * @return the punishment duration of this stage
     */
    @NotNull
    PunishmentDuration duration();

    /**
     * Returns the reason of this stage.
     * @return the reason of this stage
     */
    @NotNull
    Component reason();

    /**
     * Returns the template this stage belongs to.
     * @return the associated template
     */
    @NotNull
    NecrifyTemplate template();

    /**
     * The index of this stage in its template. Starting from 0 with the first stage until the last index with the last stage to apply.
     * @return this stage's index in its template
     */
    int index();

    /**
     * Changes the punishment duration of this stage.
     * @param duration the new punishment duration
     * @return a {@link CompletableFuture} finishing either successfully or with an error but with no result value
     */
    @NotNull
    CompletableFuture<Void> changeDuration(PunishmentDuration duration);

    /**
     * Deletes this stage from its associated template
     * @return a {@link CompletableFuture} finishing either successfully or with an error but with no result value
     */
    @NotNull
    CompletableFuture<Void> delete();

    /**
     * Returns the punishment type of this stage.
     * @return the punishment type of this stage
     */
    @NotNull
    PunishmentType punishmentType();

    /**
     * If there is another stage with a higher index, returns it, otherwise throws {@link java.util.NoSuchElementException}
     * @return the stage with the next higher index
     * @throws java.util.NoSuchElementException if there is no next stage
     */
    @NotNull
    NecrifyTemplateStage next();

    /**
     * If there is another stage with a higher index, returns it, otherwise returns this stage.
     * @return the stage with the next higher index or this stage if there is no next stage
     */
    @NotNull
    NecrifyTemplateStage nextOrThis();
}
