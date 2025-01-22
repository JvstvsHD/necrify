/*
 * This file is part of Necrify (formerly Velocity Punishment), a plugin designed to manage player's punishments for the platforms Velocity and partly Paper.
 * Copyright (C) 2022-2024 JvstvsHD
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

import de.jvstvshd.necrify.api.duration.PunishmentDuration;
import de.jvstvshd.necrify.api.message.MessageProvider;
import de.jvstvshd.necrify.api.user.NecrifyUser;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.context.CommandInput;
import org.incendo.cloud.minecraft.extras.suggestion.ComponentTooltipSuggestion;
import org.incendo.cloud.parser.ArgumentParseResult;
import org.incendo.cloud.parser.ArgumentParser;
import org.incendo.cloud.suggestion.SuggestionProvider;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class PunishmentDurationParser implements ArgumentParser<NecrifyUser, PunishmentDuration> {

    private final MessageProvider provider;

    public PunishmentDurationParser(MessageProvider provider) {
        this.provider = provider;
    }

    @Override
    public @NonNull ArgumentParseResult<@NonNull PunishmentDuration> parse(@NonNull CommandContext<@NonNull NecrifyUser> commandContext, @NonNull CommandInput commandInput) {
        var input = commandInput.peekString();
        if (input.endsWith("(hover over me)"))
            input = input.substring(0, input.length() - " (hover over me)".length());
        try {
            var duration = PunishmentDuration.parse(input);
            commandInput.readString();
            return ArgumentParseResult.success(duration);
        } catch (PunishmentDuration.Parser.ParseException e) {
            return ArgumentParseResult.failure(e);
        }
    }
    @Override
    public @NonNull SuggestionProvider<NecrifyUser> suggestionProvider() {
        var unprefixedProvider = this.provider.autoPrefixed(false);
        return (context, input) -> {
            var string = input.peekString();
            ComponentTooltipSuggestion suggestion;
            var hoverOverMe = " (" + unprefixedProvider.provideString("suggestion.hover-over-me", context.sender().getLocale()).trim() + ")";// (hover over me)
            try {
                var duration = PunishmentDuration.parse(string);
                var expiration = duration.expirationAsString();
                var correct = unprefixedProvider.provideString("suggestion.correct", context.sender().getLocale());
                var until = unprefixedProvider.provideString("suggestion.until", context.sender().getLocale()) + " ";
                suggestion = ComponentTooltipSuggestion.suggestion(string + " | " + correct + hoverOverMe, MiniMessage.miniMessage().deserialize("<green>" + until + expiration));
            } catch (PunishmentDuration.Parser.ParseException e) {
                var incorrect = unprefixedProvider.provideString("suggestion.incorrect", context.sender().getLocale());
                var invalidDuration = unprefixedProvider.provideString("suggestion.invalid-duration", context.sender().getLocale()) + " ";
                suggestion = ComponentTooltipSuggestion.suggestion(string + " | " + incorrect + hoverOverMe, MiniMessage.miniMessage().deserialize("<red>" + invalidDuration + e.getMessage()));
            }
            return CompletableFuture.completedFuture(List.of(suggestion));
        };
    }
}
