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

import de.jvstvshd.necrify.api.user.NecrifyUser;
import de.jvstvshd.necrify.api.user.UserManager;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.context.CommandInput;
import org.incendo.cloud.parser.ArgumentParseResult;
import org.incendo.cloud.parser.ArgumentParser;

import java.util.concurrent.CompletableFuture;

public class NecrifyUserParser implements ArgumentParser.FutureArgumentParser<NecrifyUser, NecrifyUser> {

    private final UserManager userManager;

    public NecrifyUserParser(UserManager userManager) {
        this.userManager = userManager;
    }

    @Override
    public @NonNull CompletableFuture<@NonNull ArgumentParseResult<NecrifyUser>> parseFuture(@NonNull CommandContext<NecrifyUser> commandContext, @NonNull CommandInput commandInput) {
        var target = commandInput.peekString();
        return userManager.loadOrCreateUser(target).handle((necrifyUser, throwable) -> {
            if (throwable != null) {
                return ArgumentParseResult.failure(throwable);
            }
            if (necrifyUser.isPresent()) {
                commandInput.readString();
                return ArgumentParseResult.success(necrifyUser.get());
            }
            return ArgumentParseResult.failure(new UserNotFoundParseException(NecrifyUser.class, commandContext, target));
        });
    }
}
