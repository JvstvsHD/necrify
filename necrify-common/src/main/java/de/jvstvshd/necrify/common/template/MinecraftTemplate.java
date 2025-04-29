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
import de.jvstvshd.necrify.api.template.NecrifyTemplate;
import de.jvstvshd.necrify.api.template.NecrifyTemplateStage;
import de.jvstvshd.necrify.common.AbstractNecrifyPlugin;
import de.jvstvshd.necrify.common.util.Util;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.CompletableFuture;

public class MinecraftTemplate implements NecrifyTemplate {

    private final Set<NecrifyTemplateStage> stages = Collections.synchronizedSet(new TreeSet<>());
    private final String name;
    private final AbstractNecrifyPlugin plugin;
    private final MiniMessage miniMessage;

    public MinecraftTemplate(String name, AbstractNecrifyPlugin plugin, MiniMessage miniMessage) {
        this.name = name;
        this.plugin = plugin;
        this.miniMessage = miniMessage;
    }

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
        return Util.executeAsync(() -> {
            Query.query("INSERT INTO necrify_punishment_template_stage (template_id, index, duration, type, reason) " +
                            "VALUES ((SELECT id FROM necrify_punishment_template WHERE name = ?), ?, ?, ?, ?)")
                    .single(Call.of().bind(name).bind(stage.index()).bind(stage.duration().javaDuration().toMillis())
                            .bind(stage.punishmentType().getId()).bind(miniMessage.serialize(stage.reason())))
                    .insert().rows();
            addStage0(stage);
            return null;
        }, plugin.getExecutor());
    }

    public void addStage0(NecrifyTemplateStage stage) {
        stages.add(stage);
    }

    //TODO fix behaviour for stages in the middle
    @Override
    public NecrifyTemplateStage removeStage(int index) {
        var stage = getStage(index);
        stages.remove(stage);
        return stage;
    }

    @Override
    public CompletableFuture<Integer> delete() {
        return Util.executeAsync(() -> {
            var rows = Query.query("DELETE FROM necrify_punishment_template WHERE name = ?")
                    .single(Call.of().bind(name))
                    .delete().rows();
            ((MinecraftTemplateManager) plugin.getTemplateManager()).removeTemplate(name);
            return rows;
        }, plugin.getExecutor());
    }
}
