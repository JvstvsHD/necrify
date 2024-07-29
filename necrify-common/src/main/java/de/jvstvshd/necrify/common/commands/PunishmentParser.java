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

import de.jvstvshd.necrify.api.Necrify;
import de.jvstvshd.necrify.api.punishment.Punishment;
import de.jvstvshd.necrify.api.user.NecrifyUser;
import de.jvstvshd.necrify.common.util.Util;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.context.CommandInput;
import org.incendo.cloud.parser.ArgumentParseResult;
import org.incendo.cloud.parser.ArgumentParser;

import java.util.concurrent.CompletableFuture;

public class PunishmentParser implements ArgumentParser.FutureArgumentParser<NecrifyUser, Punishment> {

    private final Necrify necrify;

    public PunishmentParser(Necrify necrify) {
        this.necrify = necrify;
    }

    @Override
    public @NonNull CompletableFuture<@NonNull ArgumentParseResult<Punishment>> parseFuture(@NonNull CommandContext<NecrifyUser> commandContext, @NonNull CommandInput commandInput) {
        var uuidString = commandInput.peekString();
        var uuid = Util.fromString(uuidString);
        if (uuid.isEmpty()) {
            return CompletableFuture.completedFuture(ArgumentParseResult.failure(new PunishmentParseException("command.punishment.uuid-parse-error", uuidString)));
        }
        return necrify.getPunishment(uuid.get()).handle((punishment, throwable) -> {
            if (throwable != null) {
                return ArgumentParseResult.failure(new PunishmentParseException("error.internal"));
            }
            if (punishment.isPresent()) {
                commandInput.readString();
                return ArgumentParseResult.success(punishment.get());
            } else {
                return ArgumentParseResult.failure(new PunishmentParseException("command.punishment.unknown-punishment-id", uuidString));
            }
        });
    }

    public static class PunishmentParseException extends Exception {

        private final String[] replacements;

        public PunishmentParseException(String message, String...replacements) {
            super(message);
            this.replacements = replacements;
        }

        public String[] getReplacements() {
            return replacements;
        }
    }
}
