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

import de.chojo.sadu.queries.api.call.Call;
import de.chojo.sadu.queries.api.query.Query;
import de.jvstvshd.necrify.api.duration.PunishmentDuration;
import de.jvstvshd.necrify.api.punishment.PunishmentType;
import de.jvstvshd.necrify.api.template.NecrifyTemplate;
import de.jvstvshd.necrify.api.template.NecrifyTemplateStage;
import de.jvstvshd.necrify.common.AbstractNecrifyPlugin;
import de.jvstvshd.necrify.common.util.Util;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

public record MinecraftTemplateStage(NecrifyTemplate template, PunishmentType punishmentType,
                                     PunishmentDuration duration, Component reason,
                                     int index, AbstractNecrifyPlugin plugin) implements NecrifyTemplateStage {

    @Override
    public @NotNull CompletableFuture<Void> changeDuration(PunishmentDuration duration) {
        return Util.executeAsync(() -> {
            Query.query("UPDATE necrify_punishment_template_stage SET duration = ? WHERE template_id = (SELECT id FROM necrify_punishment_template WHERE name = ?) AND index = ?")
                    .single(Call.of().bind(duration.javaDuration().toMillis()).bind(template.name()).bind(index))
                    .update();
            return null;
        }, plugin.getExecutor());
    }

    @Override
    public @NotNull CompletableFuture<Void> delete() {
        return Util.executeAsync(() -> {
            Query.query("DELETE FROM necrify_punishment_template_stage WHERE template_id = (SELECT id FROM necrify_punishment_template WHERE name = ?) AND index = ?")
                    .single(Call.of().bind(template.name()).bind(index))
                    .delete();
            template.removeStage(index);
            return null;
        }, plugin.getExecutor());
    }

    @Override
    public @NotNull PunishmentType punishmentType() {
        return punishmentType;
    }

    @Override
    public int compareTo(@NotNull NecrifyTemplateStage o) {
        return Integer.compare(index, o.index());
    }

    @Override
    public @NotNull NecrifyTemplateStage next() {
        return template.getStage(index + 1);
    }

    @Override
    public @NotNull NecrifyTemplateStage nextOrThis() {
        return index + 1 < template.stages().size() ? template.getStage(index + 1) : this;
    }
}
