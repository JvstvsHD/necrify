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

import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;

/**
 * A template is a set of {@link NecrifyTemplateStage stages} that are used to unify punishments so that for every same
 * reason of punishing the same punishment is issued with the option to stage this. For example, if some player is cheating,
 * they can be banned at the first occasion for 30 days and after that permanently if they chose to cheat yet again.
 * <p>It is also possible to set up different types of punishments for the same template, so as for example one could be muted
 * for using inappropriate language and then banned for the same reason in the next stage.</p>
 * <p>Stages are uniquely identified by their {@link #name() name}.</p>
 *
 * @see TemplateManager
 * @see NecrifyTemplateStage
 * @since 1.2.3
 */
public interface NecrifyTemplate {

    /**
     * The unique name of this template used to identify it.
     *
     * @return the name of this template
     */
    @NotNull
    String name();

    /**
     * @return a list of all stages compromised within this template
     */
    @NotNull
    Collection<NecrifyTemplateStage> stages();

    /**
     * Attempts to return the stage corresponding with the given index. If the index is out of bounds or there exist
     * no stages, an exception is thrown.
     *
     * @param index the index of the stage
     * @return the stage at the given index
     * @throws java.util.NoSuchElementException if there are no stages
     * @throws IndexOutOfBoundsException        if the index is out of bounds
     */
    @NotNull
    NecrifyTemplateStage getStage(int index);

    /**
     * Adds a new stage to this template.
     *
     * @param stage the stage to add
     * @return a {@link CompletableFuture} finishing either successfully or with an error but with no result value
     */
    @NotNull
    CompletableFuture<Void> addStage(NecrifyTemplateStage stage);

    /**
     * Deletes this template. This will remove all stages added to this template.
     *
     * @return a {@link CompletableFuture} finishing either successfully or with an error but with no result value
     */
    CompletableFuture<Integer> delete();

    /**
     * Removes the stage at the specified index from this template.
     *
     * @param index the index of the stage to be removed
     * @return the removed stage that was previously located at the specified index
     * @throws IndexOutOfBoundsException if the specified index is invalid or out of bounds
     */
    NecrifyTemplateStage removeStage(int index);
}