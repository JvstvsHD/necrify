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

package de.jvstvshd.necrify.common.commands;

import de.jvstvshd.necrify.api.template.NecrifyTemplate;
import de.jvstvshd.necrify.api.template.TemplateManager;
import de.jvstvshd.necrify.api.user.NecrifyUser;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.context.CommandInput;
import org.incendo.cloud.minecraft.extras.suggestion.ComponentTooltipSuggestion;
import org.incendo.cloud.parser.ArgumentParseResult;
import org.incendo.cloud.parser.ArgumentParser;
import org.incendo.cloud.suggestion.SuggestionProvider;

import java.util.concurrent.CompletableFuture;

public class TemplateParser implements ArgumentParser<NecrifyUser, NecrifyTemplate> {

    private final TemplateManager templateManager;

    public TemplateParser(TemplateManager templateManager) {
        this.templateManager = templateManager;
    }

    @Override
    public @NonNull ArgumentParseResult<@NonNull NecrifyTemplate> parse(@NonNull CommandContext<@NonNull NecrifyUser> commandContext, @NonNull CommandInput commandInput) {
        var template = templateManager.getTemplate(commandInput.peekString());
        return template.map(value -> {
            commandInput.readString();
            return ArgumentParseResult.success(value);
        }).orElse(ArgumentParseResult.failure(new ParseException(commandInput.peekString())));
    }

    @Override
    public @NonNull SuggestionProvider<NecrifyUser> suggestionProvider() {
        return SuggestionProvider.blocking((context, input) -> templateManager.getTemplates()
                .stream()
                .filter(template -> template.name().toLowerCase().startsWith(input.peekString().toLowerCase()))
                .map(template -> ComponentTooltipSuggestion.suggestion(template.name(), MiniMessage.miniMessage().deserialize(template.name())))
                .toList());
    }

    public static class ParseException extends RuntimeException {
        private final String template;

        public ParseException(String template) {
            this.template = template;
        }

        public String getTemplate() {
            return template;
        }
    }
}
