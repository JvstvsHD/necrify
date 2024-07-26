package de.jvstvshd.necrify.common.commands;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.incendo.cloud.caption.Caption;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.exception.parsing.ParserException;

public class UserNotFoundParseException extends ParserException {

    private final String playerName;

    public UserNotFoundParseException(@NonNull Class<?> argumentParser, @NonNull CommandContext<?> context, String playerName) {
        super(argumentParser, context, Caption.of(""));
        this.playerName = playerName;
    }

    public String playerName() {
        return playerName;
    }
}
