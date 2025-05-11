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

import de.jvstvshd.necrify.api.punishment.PunishmentType;
import de.jvstvshd.necrify.api.punishment.PunishmentTypeRegistry;
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

public class PunishmentTypeParser implements ArgumentParser<NecrifyUser, PunishmentType> {

    @Override
    public @NonNull ArgumentParseResult<@NonNull PunishmentType> parse(@NonNull CommandContext<@NonNull NecrifyUser> commandContext, @NonNull CommandInput commandInput) {
        var input = commandInput.peekString();
        ArgumentParseResult<PunishmentType> success;
        try {
            success = ArgumentParseResult.success(PunishmentTypeRegistry.getType(Integer.parseInt(input)));
        } catch (NumberFormatException e) {
            var optional = PunishmentTypeRegistry.getPunishmentTypes().stream()
                    .filter(punishmentType -> punishmentType.getName().equalsIgnoreCase(input)).findFirst()
                    .map(ArgumentParseResult::success);
            if (optional.isPresent()) {
                success = optional.get();
            } else {
                return ArgumentParseResult.failure(new PunishmentTypeNotFoundException(input));
            }
        }
        commandInput.readString();
        return success;
    }

    @Override
    public @NonNull SuggestionProvider<NecrifyUser> suggestionProvider() {
        return (context, input) -> CompletableFuture.completedFuture(PunishmentTypeRegistry.getPunishmentTypes()
                .stream()
                .filter(punishmentType -> punishmentType.getName().toLowerCase().startsWith(input.peekString().toLowerCase()))
                .map(punishmentType -> ComponentTooltipSuggestion.suggestion(punishmentType.getName(),
                        MiniMessage.miniMessage().deserialize(punishmentType.getName())))
                .toList());
    }

    public static class PunishmentTypeNotFoundException extends Exception {
        private final String input;

        public PunishmentTypeNotFoundException(String input) {
            this.input = input;
        }
    }
}
