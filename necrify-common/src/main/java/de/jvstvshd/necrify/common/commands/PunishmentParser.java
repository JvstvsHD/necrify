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
