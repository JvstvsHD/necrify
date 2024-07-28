/*
 * This file is part of Necrify (formerly Velocity Punishment), which is licensed under the MIT license.
 *
 * Copyright (c) 2022-2024 JvstvsHD
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package de.jvstvshd.necrify.common.commands;

import com.mojang.brigadier.LiteralMessage;
import de.jvstvshd.necrify.api.duration.PunishmentDuration;
import de.jvstvshd.necrify.api.user.NecrifyUser;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.incendo.cloud.brigadier.suggestion.TooltipSuggestion;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.context.CommandInput;
import org.incendo.cloud.minecraft.extras.suggestion.ComponentTooltipSuggestion;
import org.incendo.cloud.parser.ArgumentParseResult;
import org.incendo.cloud.parser.ArgumentParser;
import org.incendo.cloud.suggestion.SuggestionProvider;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class PunishmentDurationParser implements ArgumentParser<NecrifyUser, PunishmentDuration> {

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
        return (context, input) -> {
            var string = input.peekString();
            ComponentTooltipSuggestion suggestion;
            try {
                var duration = PunishmentDuration.parse(string);
                var expiration = duration.expirationAsString();
                suggestion = ComponentTooltipSuggestion.suggestion(string + " | correct (hover over me)", MiniMessage.miniMessage().deserialize("<green>until " + expiration));
            } catch (PunishmentDuration.Parser.ParseException e) {
                suggestion = ComponentTooltipSuggestion.suggestion(string + " | incorrect (hover over me)", MiniMessage.miniMessage().deserialize("<red>Invalid duration format: " + e.getMessage()));
            }
            return CompletableFuture.completedFuture(List.of(suggestion));
        };
    }
}
