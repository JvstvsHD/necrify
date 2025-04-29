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
import de.jvstvshd.necrify.api.punishment.PunishmentTypeRegistry;
import de.jvstvshd.necrify.api.template.NecrifyTemplate;
import de.jvstvshd.necrify.api.template.TemplateManager;
import de.jvstvshd.necrify.common.AbstractNecrifyPlugin;
import de.jvstvshd.necrify.common.util.Util;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.CompletableFuture;

public class MinecraftTemplateManager implements TemplateManager {

    private final AbstractNecrifyPlugin plugin;
    private final Set<NecrifyTemplate> templates = Collections.synchronizedSet(new HashSet<>());
    private final MiniMessage miniMessage;

    public MinecraftTemplateManager(AbstractNecrifyPlugin plugin, MiniMessage miniMessage) {
        this.plugin = plugin;
        this.miniMessage = miniMessage;
    }

    @Override
    public @NotNull CompletableFuture<Collection<? extends NecrifyTemplate>> loadTemplates() {
        return Util.executeAsync(() -> {
            Map<String, MinecraftTemplate> loadedTemplates = new HashMap<>();
            Query.query("SELECT name FROM necrify_punishment_template;")
                    .single(Call.of())
                    .map(row -> new MinecraftTemplate(row.getString(1), plugin, miniMessage))
                    .all().forEach(minecraftTemplate -> loadedTemplates.put(minecraftTemplate.name(), minecraftTemplate));
            Query.query("SELECT t.name, s.index, s.duration, s.type, s.reason FROM necrify_punishment_template t, necrify_punishment_template_stage s WHERE t.id = s.template_id")
                    .single(Call.of())
                    .map(row -> {
                        var templateName = row.getString(1);
                        var template = loadedTemplates.get(templateName);
                        var stage = new MinecraftTemplateStage(
                                template, PunishmentTypeRegistry.getType(row.getInt(4)),
                                PunishmentDuration.fromMillis(row.getLong(3)),
                                miniMessage.deserialize(row.getString(5)),
                                row.getInt(2), plugin);
                        template.addStage0(stage);
                        return null;
                    }).all();
            var values = loadedTemplates.values();
            templates.addAll(values);
            return values;
        }, plugin.getExecutor());
    }

    @Override
    public @NotNull Optional<NecrifyTemplate> getTemplate(String name) {
        return templates.stream().filter(t -> t.name().equals(name)).findFirst();
    }

    @Override
    public @NotNull CompletableFuture<NecrifyTemplate> createTemplate(String name) {
        if (templates.stream().anyMatch(t -> t.name().equals(name))) {
            throw new IllegalArgumentException("Template with name " + name + " already exists");
        }
        return Util.executeAsync(() -> {
            Query.query("INSERT INTO necrify_punishment_template (name) VALUES (?)")
                    .single(Call.call().bind(name))
                    .insert();
            var template = new MinecraftTemplate(name, plugin, miniMessage);
            templates.add(template);
            return template;
        }, plugin.getExecutor());
    }

    @Override
    public @NotNull Collection<? extends NecrifyTemplate> getTemplates() {
        return Collections.unmodifiableCollection(templates);
    }

    public void removeTemplate(String name) {
        templates.removeIf(t -> t.name().equals(name));
    }
}
