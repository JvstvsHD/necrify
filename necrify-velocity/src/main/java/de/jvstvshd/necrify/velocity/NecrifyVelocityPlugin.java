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

package de.jvstvshd.necrify.velocity;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.base.Joiner;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.VelocityBrigadierMessage;
import com.velocitypowered.api.event.EventManager;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ConsoleCommandSource;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.messages.ChannelIdentifier;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import com.zaxxer.hikari.HikariDataSource;
import de.chojo.sadu.core.databases.Database;
import de.chojo.sadu.core.jdbc.RemoteJdbcConfig;
import de.chojo.sadu.core.updater.SqlVersion;
import de.chojo.sadu.datasource.DataSourceCreator;
import de.chojo.sadu.datasource.stage.ConfigurationStage;
import de.chojo.sadu.mariadb.databases.MariaDb;
import de.chojo.sadu.mysql.databases.MySql;
import de.chojo.sadu.postgresql.databases.PostgreSql;
import de.chojo.sadu.queries.api.call.Call;
import de.chojo.sadu.queries.api.query.Query;
import de.chojo.sadu.queries.configuration.QueryConfiguration;
import de.chojo.sadu.updater.SqlUpdater;
import de.jvstvshd.necrify.api.event.EventDispatcher;
import de.jvstvshd.necrify.api.event.Slf4jLogger;
import de.jvstvshd.necrify.api.event.origin.EventOrigin;
import de.jvstvshd.necrify.api.event.user.UserLoadedEvent;
import de.jvstvshd.necrify.api.message.MessageProvider;
import de.jvstvshd.necrify.api.punishment.Punishment;
import de.jvstvshd.necrify.api.punishment.PunishmentManager;
import de.jvstvshd.necrify.api.punishment.util.PlayerResolver;
import de.jvstvshd.necrify.api.user.NecrifyUser;
import de.jvstvshd.necrify.api.user.UserManager;
import de.jvstvshd.necrify.common.AbstractNecrifyPlugin;
import de.jvstvshd.necrify.common.io.Adapters;
import de.jvstvshd.necrify.common.io.NecrifyDatabase;
import de.jvstvshd.necrify.common.plugin.MuteData;
import de.jvstvshd.necrify.common.punishment.NecrifyKick;
import de.jvstvshd.necrify.common.user.UserLoader;
import de.jvstvshd.necrify.common.util.Util;
import de.jvstvshd.necrify.velocity.config.ConfigurationManager;
import de.jvstvshd.necrify.velocity.impl.DefaultPlayerResolver;
import de.jvstvshd.necrify.velocity.impl.DefaultPunishmentManager;
import de.jvstvshd.necrify.velocity.impl.VelocityKick;
import de.jvstvshd.necrify.velocity.listener.ConnectListener;
import de.jvstvshd.necrify.velocity.message.ResourceBundleMessageProvider;
import de.jvstvshd.necrify.velocity.user.VelocityConsoleUser;
import de.jvstvshd.necrify.velocity.user.VelocityUser;
import de.jvstvshd.necrify.velocity.user.VelocityUserManager;
import dev.vankka.dependencydownload.DependencyManager;
import dev.vankka.dependencydownload.repository.StandardRepository;
import dev.vankka.mcdependencydownload.velocity.classpath.VelocityClasspathAppender;
import io.leangen.geantyref.TypeToken;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.incendo.cloud.SenderMapper;
import org.incendo.cloud.brigadier.suggestion.TooltipSuggestion;
import org.incendo.cloud.execution.ExecutionCoordinator;
import org.incendo.cloud.minecraft.extras.parser.ComponentParser;
import org.incendo.cloud.minecraft.extras.suggestion.ComponentTooltipSuggestion;
import org.incendo.cloud.type.tuple.Pair;
import org.incendo.cloud.velocity.CloudInjectionModule;
import org.incendo.cloud.velocity.VelocityCommandManager;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Plugin(id = "necrify", name = "Necrify", version = AbstractNecrifyPlugin.VERSION, description = "A simple punishment plugin for Velocity", authors = {"JvstvsHD"})
public class NecrifyVelocityPlugin extends AbstractNecrifyPlugin {

    private final ProxyServer server;
    private final Logger logger;
    private final ConfigurationManager configurationManager;
    private static final String MUTES_DISABLED_STRING = """
            Since 1.19.1, cancelling chat messages on proxy is not possible anymore. Therefore, we have to listen to the chat event on the actual game server. This means
            that there has to be a spigot/paper extension to this plugin which is not yet available unless there's a possibility. Therefore all mute related features won't work at the moment.
            If you use 1.19 or lower you will not be affected by this.""".replace("\n", " ");
    private final Path dataDirectory;
    public static final ChannelIdentifier MUTE_DATA_CHANNEL_IDENTIFIER = MinecraftChannelIdentifier.from(MuteData.MUTE_DATA_CHANNEL_IDENTIFIER);
    private PunishmentManager punishmentManager;
    private HikariDataSource dataSource;
    private PlayerResolver playerResolver;
    private MessageProvider messageProvider;
    private UserManager userManager;
    private final MessagingChannelCommunicator communicator;
    private EventDispatcher eventDispatcher;

    @Inject
    private Injector injector;

    /**
     * Since 1.19.1, cancelling chat messages on proxy is not possible anymore. Therefore, we have to listen to the chat event on the actual game server. This means
     * that there has to be a spigot/paper extension to this plugin which is not yet available unless there's a possibility. Therefore all mute related features are disabled for now.
     * If you use 1.19 or lower you will not be affected by this.The progress of the extension can be found <a href=https://github.com/JvstvsHD/necrify/issues/6>here</a>.
     * For this reason, every mute related feature is deprecated and marked as for removal until this extension is available.
     */
    public static final Component MUTES_DISABLED = Component.text(MUTES_DISABLED_STRING);

    @Inject
    public NecrifyVelocityPlugin(ProxyServer server, Logger logger, @DataDirectory Path dataDirectory) {
        super(Executors.newCachedThreadPool(new ThreadFactoryBuilder()
                .setUncaughtExceptionHandler((t, e) -> logger.error("An error occurred in thread {}", t.getName(), e))
                .build()));
        this.server = server;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
        this.configurationManager = new ConfigurationManager(dataDirectory.resolve("config.yml"));
        this.communicator = new MessagingChannelCommunicator(server, logger);
        this.playerResolver = new DefaultPlayerResolver(server);
        this.eventDispatcher = new EventDispatcher(getExecutor(), new Slf4jLogger(logger));
    }

    //TODO keep changed implementations and do not override them.
    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        Thread.setDefaultUncaughtExceptionHandler((t, e) -> logger.error("An error occurred in thread {}", t.getName(), e));
        long start = System.currentTimeMillis();
        try {
            DependencyManager manager = new DependencyManager(dataDirectory.resolve("cache"));
            manager.loadFromResource(getClass().getClassLoader().getResource("runtimeDownload.txt"));
            Executor executor = Executors.newCachedThreadPool();
            if (!manager.getAllPaths(true).stream().allMatch(Files::exists)) {
                manager.downloadAll(executor, Collections.singletonList(new StandardRepository("https://repo1.maven.org/maven2"))).join();
                logger.info("Relocating all dependencies...");
                long relocateStart = System.currentTimeMillis();
                manager.relocateAll(executor).join();
                logger.info("Successfully relocated all dependencies in {}ms", System.currentTimeMillis() - relocateStart);
            } else {
                var depField = manager.getClass().getDeclaredField("step");
                depField.setAccessible(true);
                AtomicInteger step = (AtomicInteger) depField.get(manager);
                step.set(2);
            }
            manager.loadAll(executor, new VelocityClasspathAppender(this, server)).join();
        } catch (Exception e) {
            logger.error("Could not load required dependencies. Aborting start-up", e);
            return;
        }
        logger.info("Successfully loaded all dependencies in {}ms", System.currentTimeMillis() - start);
        try {
            configurationManager.load();
            if (configurationManager.getConfiguration().isWhitelistActivated()) {
                logger.info("Whitelist is activated. This means that nobody can join this server beside players you have explicitly allowed to join this server via /necrify user <player> whitelist (toggles current state).");
            }
            this.messageProvider = new ResourceBundleMessageProvider(configurationManager.getConfiguration());
        } catch (IOException e) {
            logger.error("Could not load configuration", e);
            logger.error("Aborting start-up");
            return;
        }
        dataSource = createDataSource();
        QueryConfiguration.setDefault(QueryConfiguration.builder(dataSource).setThrowExceptions(true).build());
        punishmentManager = new DefaultPunishmentManager(server, dataSource, this);
        registerRegistries();
        this.userManager = new VelocityUserManager(getExecutor(), server, Caffeine.newBuilder().maximumSize(100).expireAfterWrite(Duration.ofMinutes(10)).build(), Caffeine.newBuilder().maximumSize(100).expireAfterWrite(Duration.ofMinutes(10)).build(), this);
        try {
            updateDatabase();
        } catch (SQLException | IOException e) {
            logger.error("Could not create table necrify_punishment in database {}", dataSource.getDataSourceProperties().get("dataSource.databaseName"), e);
        }
        setup(server.getEventManager());
        logger.warn("Persecution of mutes cannot be granted on all servers unless the required paper plugin is installed.");
        eventDispatcher.register(communicator);
        eventDispatcher.register(userManager);
        var buildInfo = "v" + VERSION + " (running on commit " + GIT_COMMIT;
        if (!BUILD_NUMBER.equalsIgnoreCase("-1")) {
            buildInfo += " build " + BUILD_NUMBER;
        }
        buildInfo += ")";
        logger.info("Velocity Punishment Plugin {} has been loaded. This is only a dev build and thus may be unstable.", buildInfo);
    }

    private void setup(EventManager eventManager) {
        eventManager.register(this, communicator);
        eventManager.register(this, new ConnectListener(this, Executors.newCachedThreadPool(), server));
        eventManager.register(this, userManager);
        logger.info(MUTES_DISABLED_STRING);

        final Injector childInjector = injector.createChildInjector(
                new CloudInjectionModule<>(
                        NecrifyUser.class,
                        ExecutionCoordinator.coordinatorFor(ExecutionCoordinator.nonSchedulingExecutor()),
                        SenderMapper.create(this::createUser, this::getCommandSource)));
        var cManager = childInjector.getInstance(Key.get(new TypeLiteral<VelocityCommandManager<NecrifyUser>>() {
        }));
        cManager.appendSuggestionMapper(suggestion -> {
            if (!(suggestion instanceof ComponentTooltipSuggestion componentTooltipSuggestion))
                return suggestion;

            return TooltipSuggestion.suggestion(suggestion.suggestion(), VelocityBrigadierMessage.tooltip(componentTooltipSuggestion.tooltip()));
        });
        var brigadierManager = cManager.brigadierManager();
        brigadierManager.setNativeNumberSuggestions(true);
        registerCommands(cManager, getConfig().getConfiguration().isAllowTopLevelCommands());
        brigadierManager.registerMapping(new TypeToken<ComponentParser<NecrifyUser>>() {
        }, builder -> builder.to(necrifyUserParser -> StringArgumentType.greedyString()).nativeSuggestions());
    }

    @SuppressWarnings({"unchecked", "UnstableApiUsage"})
    private HikariDataSource createDataSource() {
        var dbData = configurationManager.getConfiguration().getDataBaseData();
        NecrifyDatabase.SQL_TYPE = dbData.sqlType().name().toLowerCase();
        var driverClass = getDriverClass(dbData.sqlType().name().toLowerCase());
        ConfigurationStage stage = switch (dbData.sqlType().name().toLowerCase()) {
            case "postgresql" -> DataSourceCreator
                    .create(PostgreSql.get())
                    .configure(jdbcConfig -> jdbcConfig
                            .driverClass(driverClass)
                            .host(dbData.getHost())
                            .port(dbData.getPort())
                            .database(dbData.getDatabase())
                            .user(dbData.getUsername())
                            .password(dbData.getPassword()))
                    .create();

            default -> DataSourceCreator
                    .create((Database<RemoteJdbcConfig<?>, ?>) dbData.sqlType())
                    .configure(jdbcConfig -> jdbcConfig
                            .driverClass(driverClass)
                            .host(dbData.getHost())
                            .port(dbData.getPort())
                            .database(dbData.getDatabase())
                            .user(dbData.getUsername())
                            .password(dbData.getPassword()))
                    .create();
        };
        return stage
                .withMaximumPoolSize(dbData.getMaxPoolSize())
                .withMinimumIdle(dbData.getMinIdle())
                .withPoolName("necrify-hikari")
                .forSchema(dbData.getPostgresSchema())
                .build();
    }

    private Class<? extends java.sql.Driver> getDriverClass(String type) {
        return switch (type) {
            case "postgresql", "postgres" -> org.postgresql.Driver.class;
            case "mariadb" -> org.mariadb.jdbc.Driver.class;
            case "mysql" -> com.mysql.cj.jdbc.Driver.class;
            default -> throw new IllegalArgumentException("Unknown database type: " + type);
        };
    }

    @SuppressWarnings("UnstableApiUsage")
    private void updateDatabase() throws IOException, SQLException {
        Consumer<Connection> preUpdateHook = connection -> {
            var updatedReasons = Query.query("SELECT reason, punishment_id FROM necrify_punishment WHERE reason LIKE '%§%';")
                    .single()
                    .map(row -> {
                        var reason = LegacyComponentSerializer.legacySection().deserialize(row.getString(1));
                        var punishmentId = Util.parseUuid(row.getString(2));
                        return Query.query("UPDATE necrify_punishment SET reason = ? WHERE punishment_id = ?;")
                                .single(Call.of().bind(MiniMessage.miniMessage().serialize(reason)).bind(punishmentId.toString()))
                                .update();
                    }).all();
            logger.info("Updated {} reasons to minimessage format.", updatedReasons.size());
        };
        switch (configurationManager.getConfiguration().getDataBaseData().sqlType().name().toLowerCase(Locale.ROOT)) {
            case "postgresql", "postgres" ->
                    SqlUpdater.builder(dataSource, PostgreSql.get()).setSchemas(configurationManager.getConfiguration().getDataBaseData().getPostgresSchema())
                            .preUpdateHook(new SqlVersion(1, 1), preUpdateHook)
                            .execute();
            case "mariadb" -> SqlUpdater.builder(dataSource, MariaDb.get())
                    .preUpdateHook(new SqlVersion(1, 1), preUpdateHook)
                    .execute();
            case "mysql" -> SqlUpdater.builder(dataSource, MySql.get())
                    .preUpdateHook(new SqlVersion(1, 1), preUpdateHook)
                    .execute();
            default ->
                    logger.warn("Database type is not (yet) supported for automatic updates. Please update the database manually.");
        }
    }

    @Override
    public PunishmentManager getPunishmentManager() {
        return punishmentManager;
    }

    @Override
    public void setPunishmentManager(PunishmentManager punishmentManager) {
        this.punishmentManager = punishmentManager;
    }

    @Override
    public PlayerResolver getPlayerResolver() {
        return playerResolver;
    }

    @Override
    public void setPlayerResolver(PlayerResolver playerResolver) {
        this.playerResolver = playerResolver;
    }

    public ProxyServer getServer() {
        return server;
    }

    public HikariDataSource getDataSource() {
        return dataSource;
    }

    @Override
    public @NotNull MessageProvider getMessageProvider() {
        return messageProvider;
    }

    @NotNull
    public MessagingChannelCommunicator communicator() {
        return communicator;
    }

    @Override
    public void setMessageProvider(@NotNull MessageProvider messageProvider) {
        this.messageProvider = messageProvider;
    }

    @Override
    public Logger getLogger() {
        return logger;
    }

    @Override
    public boolean isWhitelistActive() {
        return configurationManager.getConfiguration().isWhitelistActivated();
    }

    @Override
    public void setWhitelistActive(boolean active) throws IOException {
        configurationManager.getConfiguration().setWhitelistActivated(active);
        configurationManager.save();
    }

    public ConfigurationManager getConfig() {
        return configurationManager;
    }

    @Override
    public @NotNull UserManager getUserManager() {
        return userManager;
    }

    @Override
    public void setUserManager(@NotNull UserManager userManager) {
        this.userManager = userManager;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends Punishment> CompletableFuture<Optional<T>> getPunishment(@NotNull UUID punishmentId) {
        return Util.executeAsync(() -> (Optional<T>) Query
                .query("SELECT u.* FROM necrify_user u INNER JOIN necrify_punishment p ON u.uuid = p.uuid WHERE p.punishment_id = ?;")
                .single(Call.of().bind(punishmentId, Adapters.UUID_ADAPTER))
                .map(row -> {
                    var userId = Util.getUuid(row, 1);
                    var user = createUser(userId, true);
                    return user.getPunishment(punishmentId).orElse(null);
                }).first(), getExecutor());
    }

    public NecrifyUser createUser(CommandSource source) {
        if (source instanceof Player) {
            return createUser(((Player) source).getUniqueId(), false);
        } else if (source instanceof ConsoleCommandSource) {
            return new VelocityConsoleUser(messageProvider, server.getConsoleCommandSource());
        } else {
            return new VelocityUser(UUID.randomUUID(), "unknown_source", false, this);
        }
    }

    /**
     * Creates a user with the given UUID. If the user is already cached, the cached user is returned.
     * <p>Note: this user does not hold any valid data besides his uuid and maybe player instance (if online). After returning
     * the value, the missing user data will be loaded, whereafter the {@link UserLoadedEvent} will be fired.</p>
     *
     * @param userId                  the UUID of the user to create.
     * @param loadPunishmentsDirectly whether to load the punishments directly or not. This influences if punishments are
     *                                loaded asynchronously or not. If set to true, punishments will be loaded blocking.
     * @return the created user.
     */
    //TODO allow for other implementations of UserManager to work with this -- extract this into its own class (with
    //TODO maybe some other internal stuff) and make it a component like user manager etc.
    //TODO 06.08.2024
    public NecrifyUser createUser(UUID userId, boolean loadPunishmentsDirectly) {
        var cachedUser = getUserManager().getUser(userId);
        if (cachedUser.isPresent()) {
            return cachedUser.get();
        }
        var user = new VelocityUser(userId, "unknown", false, this);
        Runnable loadPunishments = () -> {
            var loader = new UserLoader(user);
            Query.query("SELECT type, expiration, reason, punishment_id, successor, issued_at FROM necrify_punishment WHERE uuid = ?;")
                    .single(Call.of().bind(userId, Adapters.UUID_ADAPTER))
                    .map(loader::addDataFromRow)
                    .all();
            ((VelocityUserManager) userManager).loadPunishmentsToUser(loader);
            getEventDispatcher().dispatch(new UserLoadedEvent(user).setOrigin(EventOrigin.ofClass(getClass())));
        };
        if (loadPunishmentsDirectly) {
            loadPunishments.run();
        } else {
            getExecutor().execute(loadPunishments);
        }
        return user;
    }

    public CommandSource getCommandSource(NecrifyUser user) {
        if (user instanceof VelocityUser velocityUser) {
            var player = velocityUser.getPlayer();
            if (player != null) {
                return player;
            }
        }
        if ("console".equalsIgnoreCase(user.getUsername()) && user.getUuid().equals(new UUID(0, 0))) {
            return server.getConsoleCommandSource();
        }
        return server.getPlayer(user.getUuid()).orElse(null);
    }

    @Override
    public @NotNull EventDispatcher getEventDispatcher() {
        return eventDispatcher;
    }

    @Override
    public void setEventDispatcher(@NotNull EventDispatcher eventDispatcher) {
        this.eventDispatcher = eventDispatcher;
    }

    @Override
    public NecrifyKick createKick(Component reason, NecrifyUser user, UUID punishmentUuid) {
        return new VelocityKick(user, reason, punishmentUuid, this, LocalDateTime.now());
    }

    @Override
    public Set<Pair<String, UUID>> getOnlinePlayers() {
        return server.getAllPlayers().stream().map(player -> Pair.of(player.getUsername(), player.getUniqueId())).collect(Collectors.toSet());
    }
}
