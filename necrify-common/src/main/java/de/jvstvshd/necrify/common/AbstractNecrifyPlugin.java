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

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import de.jvstvshd.necrify.api.Necrify;
import de.jvstvshd.necrify.api.duration.PunishmentDuration;
import de.jvstvshd.necrify.api.punishment.Punishment;
import de.jvstvshd.necrify.api.punishment.PunishmentType;
import de.jvstvshd.necrify.api.punishment.PunishmentTypeRegistry;
import de.jvstvshd.necrify.api.punishment.StandardPunishmentType;
import de.jvstvshd.necrify.api.template.NecrifyTemplate;
import de.jvstvshd.necrify.api.template.TemplateManager;
import de.jvstvshd.necrify.api.user.NecrifyUser;
import de.jvstvshd.necrify.common.commands.*;
import de.jvstvshd.necrify.common.config.ConfigurationManager;
import de.jvstvshd.necrify.common.punishment.NecrifyKick;
import de.jvstvshd.necrify.common.punishment.NecrifyPunishmentFactory;
import de.jvstvshd.necrify.common.punishment.log.NecrifyPunishmentLog;
import de.jvstvshd.necrify.common.template.MinecraftTemplateManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.annotations.AnnotationParser;
import org.incendo.cloud.exception.ArgumentParseException;
import org.incendo.cloud.exception.CommandExecutionException;
import org.incendo.cloud.exception.handling.ExceptionHandler;
import org.incendo.cloud.minecraft.extras.parser.ComponentParser;
import org.incendo.cloud.parser.ParserDescriptor;
import org.incendo.cloud.parser.standard.StringParser;
import org.incendo.cloud.type.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.io.IOException;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ExecutorService;

public abstract class AbstractNecrifyPlugin implements Necrify {

    public static final String VERSION = BuildParameters.VERSION;
    public static final String GIT_COMMIT = BuildParameters.GIT_COMMIT;
    public static final String BUILD_NUMBER = BuildParameters.BUILD_NUMBER;

    protected final ExecutorService executorService;
    protected final ConfigurationManager configurationManager;
    private final Cache<UUID, Punishment> historicalPunishmentCache =
            Caffeine.newBuilder().maximumSize(100).expireAfterWrite(Duration.ofMinutes(10)).build();
    private final Logger logger;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    private TemplateManager templateManager = new MinecraftTemplateManager(this, miniMessage);

    public AbstractNecrifyPlugin(ExecutorService executorService, ConfigurationManager configurationManager, Logger logger) {
        this.executorService = executorService;
        this.configurationManager = configurationManager;
        this.logger = logger;
    }

    @Override
    public @NotNull ExecutorService getExecutor() {
        return executorService;
    }

    /**
     * Registers the punishment types of the plugin to the {@link PunishmentTypeRegistry}. This method should be called
     * before any user-input is processed, as the registry is used to determine the type of punishment that should be created.
     */
    public final void registerFactories() {
        var factory = new NecrifyPunishmentFactory(this);
        for (StandardPunishmentType type : StandardPunishmentType.values()) {
            PunishmentTypeRegistry.registerType(type, factory);
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
        manager.exceptionController()
                .registerHandler(ArgumentParseException.class, ExceptionHandler.unwrappingHandler(TemplateParser.ParseException.class))
                .registerHandler(TemplateParser.ParseException.class, context -> context.context().sender().sendMessage("command.template.not-found", NamedTextColor.RED, Component.text(context.exception().getTemplate(), NamedTextColor.YELLOW)));
        manager.exceptionController()
                .registerHandler(CommandExecutionException.class, ExceptionHandler.unwrappingHandler(Throwable.class))
                .registerHandler(Throwable.class, context -> {
                    logger.error("An internal error occurred while executing a command", context.exception());
                    var component = getMessageProvider().provide("error.internal");
                    context.context().sender().sendMessage(component);
                });

        manager.captionRegistry().registerProvider((caption, user) -> {
            var component = getMessageProvider().provide(caption.key(), user.getLocale());
            return PlainTextComponentSerializer.plainText().serialize(component);
        });
        var parserRegistry = manager.parserRegistry();
        parserRegistry.registerParser(ParserDescriptor.of(new NecrifyUserParser(this.getUserManager()), NecrifyUser.class));
        parserRegistry.registerParser(ComponentParser.componentParser(miniMessage, StringParser.StringMode.QUOTED));
        parserRegistry.registerParser(ParserDescriptor.of(new PunishmentDurationParser(getMessageProvider()), PunishmentDuration.class));
        parserRegistry.registerParser(ParserDescriptor.of(new PunishmentParser(this), Punishment.class));
        parserRegistry.registerParser(ParserDescriptor.of(new TemplateParser(getTemplateManager()), NecrifyTemplate.class));
        parserRegistry.registerParser(ParserDescriptor.of(new PunishmentTypeParser(), PunishmentType.class));
        var commands = new NecrifyCommand(this);
        parser.parse(commands);
    }

    //TODO: Move config to necrify-common
    public String getDefaultReason(PunishmentType type) {
        return configurationManager.getConfiguration().getPunishmentConfigData().getPunishmentMessages().get(type.getId());
    }

    public boolean loadConfig() {
        try {
            configurationManager.load();
            if (configurationManager.getConfiguration().isWhitelistActivated()) {
                logger.info("Whitelist is activated. This means that nobody can join this server beside players you have explicitly allowed to join this server via /necrify user <player> whitelist (toggles current state).");
            }
        } catch (IOException e) {
            logger.error("Could not load configuration", e);
            logger.error("Aborting start-up");
            return false;
        }
        return true;
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

    public ConfigurationManager getConfig() {
        return configurationManager;
    }

    public Logger getLogger() {
        return logger;
    }

    /**
     * Returns a historical punishment by its uuid. This method should be used to retrieve a punishment that is not active anymore
     * and only if there is no other way to retrieve the punishment if it is still active.<br>
     * This method executes synchronously and should not be called on the main thread.
     *
     * @param punishmentUuid the uuid of the punishment to retrieve
     * @param <T>            the type of the punishment
     * @return the punishment or null if it could not be found
     */
    @SuppressWarnings("unchecked")
    public <T extends Punishment> T getHistoricalPunishment(UUID punishmentUuid) {
        var cached = historicalPunishmentCache.getIfPresent(punishmentUuid);
        if (cached != null) {
            return (T) cached;
        }
        var log = new NecrifyPunishmentLog(this, punishmentUuid);
        if (log.load(false)) {
            var result = log.getPunishment();
            //noinspection ConstantValue in this case, #getPunishment may return null if there is no such punishment for this id
            if (result != null) {
                historicalPunishmentCache.put(punishmentUuid, result);
                return (T) result;
            }
            return (T) log.getPunishment();
        }
        return null;
    }

    /**
     * Returns a cached punishment by its uuid. This method should be used to retrieve a punishment before loading
     * it from the database. This method is useful if the punishment is already loaded and should be retrieved from the cache.
     * <p>
     * This method invokes the following steps to retrieve the punishment:
     * <ul>
     *     <li>Iterate over all loaded users and check if the punishment is present in the user's punishment list</li>
     *     <li>Check if the punishment is present in the historical punishment cache</li>
     * </ul>
     * If the punishment is not found in any of the above steps, an empty optional is returned.
     *
     * @param punishmentUuid the uuid of the punishment to retrieve
     * @param <T>            the type of the punishment to which the return value is casted
     * @return the punishment or an empty optional if it could not be found
     */
    @SuppressWarnings("unchecked")
    public <T extends Punishment> Optional<T> getCachedPunishment(UUID punishmentUuid) {
        for (NecrifyUser loadedUser : getUserManager().getLoadedUsers()) {
            var punishment = loadedUser.getPunishment(punishmentUuid);
            if (punishment.isPresent()) {
                return (Optional<T>) punishment;
            }
        }
        var cachedHPunishment = historicalPunishmentCache.getIfPresent(punishmentUuid);
        return Optional.ofNullable((T) cachedHPunishment);
    }

    /**
     * Returns the cache used for historical punishments. It should be used to cache punishments that are not active anymore only.<br>
     * <b>Important Note:</b>
     * If loading a historical punishment (punishment A) via {@link NecrifyPunishmentLog#load(boolean)}, it might encounter
     * another historical punishment (punishment B) when loading successors/predecessors. When it gets loaded likewise,
     * it will try to retrieve A from a cache or load it again if not cached. But since A still gets constructed, it is
     * not yet in the cache. So, A is loaded again, such as B and so on... Therefore, this process creates a deadlock that
     * consumes many resources, particularly database connections. This also results in a high load on the database and
     * timeout for further connections and other operations, which is nothing to be desired. To prevent this,
     * this method should be used to cache the punishment (A) before it loads any other punishments (B) that are related to it.
     */
    public Cache<UUID, Punishment> getHistoricalPunishmentCache() {
        return historicalPunishmentCache;
    }

    @Override
    public @NotNull TemplateManager getTemplateManager() {
        return templateManager;
    }

    @Override
    public void setTemplateManager(@NotNull TemplateManager templateManager) {
        this.templateManager = templateManager;
    }

    public abstract NecrifyKick createKick(Component reason, NecrifyUser user, UUID punishmentUuid);

    //TODO return just a set of objects. Create a new User object that does not get loaded from the database.
    public abstract Set<Pair<String, UUID>> getOnlinePlayers();

    @NotNull
    public abstract NecrifyUser getSystemUser();

    public abstract boolean isWhitelistActive();
}
