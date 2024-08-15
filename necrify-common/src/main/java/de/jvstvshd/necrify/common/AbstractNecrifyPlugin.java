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

package de.jvstvshd.necrify.common;

import de.jvstvshd.necrify.api.Necrify;
import de.jvstvshd.necrify.api.duration.PunishmentDuration;
import de.jvstvshd.necrify.api.punishment.Punishment;
import de.jvstvshd.necrify.api.punishment.PunishmentTypeRegistry;
import de.jvstvshd.necrify.api.punishment.StandardPunishmentType;
import de.jvstvshd.necrify.api.user.NecrifyUser;
import de.jvstvshd.necrify.common.commands.*;
import de.jvstvshd.necrify.common.punishment.NecrifyKick;
import de.jvstvshd.necrify.common.punishment.NecrifyPunishmentFactory;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.annotations.AnnotationParser;
import org.incendo.cloud.exception.ArgumentParseException;
import org.incendo.cloud.exception.handling.ExceptionHandler;
import org.incendo.cloud.minecraft.extras.parser.ComponentParser;
import org.incendo.cloud.parser.ParserDescriptor;
import org.incendo.cloud.parser.standard.StringParser;
import org.incendo.cloud.type.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutorService;

public abstract class AbstractNecrifyPlugin implements Necrify {

    public static final String VERSION = BuildParameters.VERSION;
    public static final String GIT_COMMIT = BuildParameters.GIT_COMMIT;
    public static final String BUILD_NUMBER = BuildParameters.BUILD_NUMBER;

    protected ExecutorService executorService;

    public AbstractNecrifyPlugin(ExecutorService executorService) {
        this.executorService = executorService;
    }

    @Override
    public @NotNull ExecutorService getExecutor() {
        return executorService;
    }

    @Override
    public @NotNull ExecutorService getService() {
        return getExecutor();
    }

    /**
     * Registers the punishment types of the plugin to the {@link PunishmentTypeRegistry}. This method should be called
     * before any user-input is processed, as the registry is used to determine the type of punishment that should be created.
     */
    public final void registerRegistries() {
        var registry = new NecrifyPunishmentFactory(this);
        for (StandardPunishmentType type : StandardPunishmentType.values()) {
            PunishmentTypeRegistry.registerType(type, registry);
        }
    }

    /**
     * Registers commands for the plugin via the {@link AnnotationParser} from the cloud framework. It is possible to only
     * register the commands of the /necrify root, but also the top-level ones (e.g. /ban, /kick, etc.).
     *
     * @param manager          the command manager to register the commands to.
     * @param topLevelCommands whether to register top-level commands (/ban, /kick, etc.) or not (i.e. only /necrify commands).
     */
    public final void registerCommands(CommandManager<NecrifyUser> manager, boolean topLevelCommands) {
        AnnotationParser<NecrifyUser> parser = new AnnotationParser<>(manager, NecrifyUser.class);
        final var oldExtractor = parser.commandExtractor();
        if (!topLevelCommands) {
            parser.commandExtractor(instance -> {
                var commands = new ArrayList<>(oldExtractor.extractCommands(instance));
                return commands.stream().filter(commandDescriptor -> commandDescriptor.commandToken().startsWith("necrify")).toList();
            });
        }

        manager.exceptionController()
                .registerHandler(ArgumentParseException.class, context -> {
                    var component = getMessageProvider().prefixed(Component.text(context.exception().getCause().getMessage()).color(NamedTextColor.DARK_RED));
                    context.context().sender().sendMessage(component);
                });
        manager.exceptionController()
                .registerHandler(ArgumentParseException.class, ExceptionHandler.unwrappingHandler(UserNotFoundParseException.class))
                .registerHandler(UserNotFoundParseException.class, context -> {
                    var component = getMessageProvider()
                            .provide("commands.general.not-found", Component.text(context.exception().playerName(), NamedTextColor.YELLOW))
                            .color(NamedTextColor.RED);
                    context.context().sender().sendMessage(component);
                });
        manager.exceptionController()
                .registerHandler(ArgumentParseException.class, ExceptionHandler.unwrappingHandler(PunishmentParser.PunishmentParseException.class))
                .registerHandler(PunishmentParser.PunishmentParseException.class, context -> {
                    var replacements = Arrays.stream(context.exception().getReplacements()).map(s -> Component.text(s, NamedTextColor.YELLOW)).toArray(Component[]::new);
                    var component = getMessageProvider().provide(context.exception().getMessage(), replacements).color(NamedTextColor.RED);
                    context.context().sender().sendMessage(component);
                });
        manager.exceptionController()
                .registerHandler(ArgumentParseException.class, ExceptionHandler.unwrappingHandler(PunishmentDuration.Parser.ParseException.class))
                .registerHandler(PunishmentDuration.Parser.ParseException.class, context -> {
                    var component = getMessageProvider().provide("command.punishment.duration.invalid", Component.text(context.exception().getMessage(), NamedTextColor.YELLOW)).color(NamedTextColor.RED);
                    context.context().sender().sendMessage(component);
                });
        manager.captionRegistry().registerProvider((caption, user) -> {
            var component = getMessageProvider().provide(caption.key(), user.getLocale());
            return PlainTextComponentSerializer.plainText().serialize(component);
        });
        var parserRegistry = manager.parserRegistry();
        parserRegistry.registerParser(ParserDescriptor.of(new NecrifyUserParser(this.getUserManager()), NecrifyUser.class));
        parserRegistry.registerParser(ComponentParser.componentParser(MiniMessage.miniMessage(), StringParser.StringMode.GREEDY));
        parserRegistry.registerParser(ParserDescriptor.of(new PunishmentDurationParser(getMessageProvider()), PunishmentDuration.class));
        parserRegistry.registerParser(ParserDescriptor.of(new PunishmentParser(this), Punishment.class));
        var commands = new NecrifyCommand(this);
        parser.parse(commands);
    }

    //TODO: Move config to necrify-common
    public String getDefaultReason(StandardPunishmentType type) {
        return "<red>You were " + switch (type) {
            case KICK -> "kicked from the server.";
            case TEMPORARY_BAN, PERMANENT_BAN -> "banned from the server.";
            case TEMPORARY_MUTE, PERMANENT_MUTE -> "muted.";
        } + "</red>";
    }

    @SuppressWarnings("ConstantValue")
    public static String buildInfo() {
        var buildInfo = "v" + VERSION + " (running on commit " + GIT_COMMIT;
        //build number is only available when built by the CI (GitHub Actions or Jenkins)
        if (!BUILD_NUMBER.equalsIgnoreCase("-1")) {
            buildInfo += " build " + BUILD_NUMBER;
        }
        return buildInfo + ")";
    }

    public abstract NecrifyKick createKick(Component reason, NecrifyUser user, UUID punishmentUuid);

    public abstract Logger getLogger();

    public abstract Set<Pair<String, UUID>> getOnlinePlayers();

    public abstract boolean isWhitelistActive();

    //TODO kick all non-whitelisted players
    public abstract void setWhitelistActive(boolean active) throws IOException;
}
