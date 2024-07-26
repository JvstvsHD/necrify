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
