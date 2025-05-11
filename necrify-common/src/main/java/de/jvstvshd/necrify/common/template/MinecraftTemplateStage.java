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

import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public final class MinecraftTemplateStage implements NecrifyTemplateStage {
    private final NecrifyTemplate template;
    private final PunishmentType punishmentType;
    private final PunishmentDuration duration;
    private final Component reason;
    private int index;
    private final AbstractNecrifyPlugin plugin;

    public MinecraftTemplateStage(NecrifyTemplate template, PunishmentType punishmentType,
                                  PunishmentDuration duration, Component reason,
                                  int index, AbstractNecrifyPlugin plugin) {
        this.template = template;
        this.punishmentType = punishmentType;
        this.duration = duration;
        this.reason = reason;
        this.index = index;
        this.plugin = plugin;
    }

    @Override
    public @NotNull CompletableFuture<Void> changeDuration(PunishmentDuration duration) {
        return Util.executeAsync(() -> {
            Query.query("UPDATE necrify_template_stage SET duration = ? WHERE template_id = (SELECT id FROM necrify_template WHERE name = ?) AND index = ?")
                    .single(Call.of().bind(duration.javaDuration().toMillis()).bind(template.name()).bind(index))
                    .update();
            return null;
        }, plugin.getExecutor());
    }

    @Override
    public @NotNull CompletableFuture<Void> delete() {
        return Util.executeAsync(() -> {
            Query.query("DELETE FROM necrify_template_stage WHERE template_id = (SELECT id FROM necrify_template WHERE name = ?) AND index = ?")
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

    @Override
    public void changeIndex(int index) {
        this.index = index;
    }

    @Override
    public @NotNull NecrifyTemplate template() {
        return template;
    }

    @Override
    public @NotNull PunishmentDuration duration() {
        return duration;
    }

    @Override
    public @NotNull Component reason() {
        return reason;
    }

    @Override
    public int index() {
        return index;
    }

    public AbstractNecrifyPlugin plugin() {
        return plugin;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (MinecraftTemplateStage) obj;
        return Objects.equals(this.template, that.template) &&
                Objects.equals(this.punishmentType, that.punishmentType) &&
                Objects.equals(this.duration, that.duration) &&
                Objects.equals(this.reason, that.reason) &&
                this.index == that.index &&
                Objects.equals(this.plugin, that.plugin);
    }

    @Override
    public int hashCode() {
        return Objects.hash(template, punishmentType, duration, reason, index, plugin);
    }

    @Override
    public String toString() {
        return "MinecraftTemplateStage[" +
                "template=" + template + ", " +
                "punishmentType=" + punishmentType + ", " +
                "duration=" + duration + ", " +
                "reason=" + reason + ", " +
                "index=" + index + ", " +
                "plugin=" + plugin + ']';
    }

}
