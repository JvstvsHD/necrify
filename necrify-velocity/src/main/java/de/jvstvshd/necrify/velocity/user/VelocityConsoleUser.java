package de.jvstvshd.necrify.velocity.user;

import com.velocitypowered.api.proxy.ConsoleCommandSource;
import de.jvstvshd.necrify.api.message.MessageProvider;
import de.jvstvshd.necrify.common.user.AbstractConsoleUser;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

public class VelocityConsoleUser extends AbstractConsoleUser {

    private final ConsoleCommandSource console;

    public VelocityConsoleUser(Locale locale, MessageProvider provider, ConsoleCommandSource console) {
        super(locale, provider);
        this.console = console;
    }

    public VelocityConsoleUser(MessageProvider provider, ConsoleCommandSource console) {
        super(provider);
        this.console = console;
    }

    @Override
    public void sendMessage(@NotNull Component message) {
        console.sendMessage(message);
    }

    @Override
    public void sendMessage(@NotNull String key, Component... args) {
        sendMessage(provider().provide(key, getLocale(), args));
    }

    @Override
    public void sendErrorMessage() {
        sendMessage(provider().internalError());
    }
}
