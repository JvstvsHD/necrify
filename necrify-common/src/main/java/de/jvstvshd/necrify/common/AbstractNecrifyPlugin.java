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

package de.jvstvshd.necrify.common;

import de.jvstvshd.necrify.api.Necrify;
import de.jvstvshd.necrify.api.duration.PunishmentDuration;
import de.jvstvshd.necrify.api.punishment.Punishment;
import de.jvstvshd.necrify.api.punishment.PunishmentType;
import de.jvstvshd.necrify.api.punishment.PunishmentTypeRegistry;
import de.jvstvshd.necrify.api.punishment.StandardPunishmentType;
import de.jvstvshd.necrify.api.user.NecrifyUser;
import de.jvstvshd.necrify.common.commands.*;
import de.jvstvshd.necrify.common.punishment.NecrifyBan;
import de.jvstvshd.necrify.common.punishment.NecrifyKick;
import de.jvstvshd.necrify.common.punishment.NecrifyPunishmentFactory;
import de.jvstvshd.necrify.common.punishment.PunishmentBuilder;
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

import java.util.*;
import java.util.concurrent.ExecutorService;

public abstract class AbstractNecrifyPlugin implements Necrify {

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

    public abstract NecrifyKick createKick(Component reason, NecrifyUser user, UUID punishmentUuid);

    public abstract Logger getLogger();

    public abstract Set<Pair<String, UUID>> getOnlinePlayers();
}
