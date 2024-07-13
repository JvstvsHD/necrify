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

package de.jvstvshd.necrify.velocity;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.base.Joiner;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Inject;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.event.EventManager;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.messages.ChannelIdentifier;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import com.zaxxer.hikari.HikariDataSource;
import de.chojo.sadu.core.databases.Database;
import de.chojo.sadu.core.jdbc.RemoteJdbcConfig;
import de.chojo.sadu.datasource.DataSourceCreator;
import de.chojo.sadu.datasource.stage.ConfigurationStage;
import de.chojo.sadu.postgresql.databases.PostgreSql;
import de.chojo.sadu.queries.api.call.Call;
import de.chojo.sadu.queries.api.query.Query;
import de.chojo.sadu.queries.configuration.QueryConfiguration;
import de.chojo.sadu.sqlite.databases.SqLite;
import de.chojo.sadu.updater.SqlUpdater;
import de.jvstvshd.necrify.api.event.EventDispatcher;
import de.jvstvshd.necrify.api.event.Slf4jLogger;
import de.jvstvshd.necrify.api.event.origin.EventOrigin;
import de.jvstvshd.necrify.api.event.user.UserLoadedEvent;
import de.jvstvshd.necrify.api.message.MessageProvider;
import de.jvstvshd.necrify.api.punishment.Punishment;
import de.jvstvshd.necrify.api.punishment.PunishmentManager;
import de.jvstvshd.necrify.api.punishment.util.PlayerResolver;
import de.jvstvshd.necrify.api.user.UserManager;
import de.jvstvshd.necrify.common.AbstractNecrifyPlugin;
import de.jvstvshd.necrify.common.io.Adapters;
import de.jvstvshd.necrify.common.plugin.MuteData;
import de.jvstvshd.necrify.velocity.commands.*;
import de.jvstvshd.necrify.velocity.config.ConfigurationManager;
import de.jvstvshd.necrify.velocity.impl.DefaultPlayerResolver;
import de.jvstvshd.necrify.velocity.impl.DefaultPunishmentManager;
import de.jvstvshd.necrify.velocity.internal.Util;
import de.jvstvshd.necrify.velocity.listener.ConnectListener;
import de.jvstvshd.necrify.velocity.message.ResourceBundleMessageProvider;
import de.jvstvshd.necrify.velocity.user.VelocityUser;
import de.jvstvshd.necrify.velocity.user.VelocityUserManager;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Path;
import java.sql.SQLException;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;

@Plugin(id = "necrify", name = "Necrify", version = "1.2.0-SNAPSHOT", description = "A simple punishment plugin for Velocity", authors = {"JvstvsHD"})
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
        try {
            configurationManager.load();
            if (configurationManager.getConfiguration().isWhitelistActivated()) {
                logger.info("Whitelist is activated. This means that nobody can join this server beside players you have explicitly allowed to join this server via /whitelist <player> add");
            }
            this.messageProvider = new ResourceBundleMessageProvider(configurationManager.getConfiguration());
        } catch (IOException e) {
            logger.error("Could not load configuration", e);
            logger.error("Aborting start-up");
            return;
        }
        dataSource = createDataSource();
        QueryConfiguration.setDefault(QueryConfiguration.builder(dataSource).setExceptionHandler(e -> logger.error("An error occurred during a database request", e)).build());
        punishmentManager = new DefaultPunishmentManager(server, dataSource, this);
        this.userManager = new VelocityUserManager(getExecutor(), server, Caffeine.newBuilder().maximumSize(100).expireAfterWrite(Duration.ofMinutes(10)).build(), Caffeine.newBuilder().maximumSize(100).expireAfterWrite(Duration.ofMinutes(10)).build(), this);
        try {
            updateDatabase();
        } catch (SQLException | IOException e) {
            logger.error("Could not create table necrify_punishment in database {}", dataSource.getDataSourceProperties().get("dataSource.databaseName"), e);
        }
        setup(server.getCommandManager(), server.getEventManager());
        var notSupportedServers = communicator.testRecipients();
        if (!communicator.isSupportedEverywhere()) {
            logger.warn("Persecution of mutes cannot be granted on the following servers as the required paper plugin is not installed: {}",
                    Joiner.on(", ").join(notSupportedServers));
        }
        eventDispatcher.register(communicator);
        eventDispatcher.register(userManager);
        logger.info("Velocity Punishment Plugin v1.2.0-SNAPSHOT has been loaded. This is only a dev build and thus may be unstable.");
    }

    private void setup(CommandManager commandManager, EventManager eventManager) {
        eventManager.register(this, communicator);
        eventManager.register(this, new ConnectListener(this, Executors.newCachedThreadPool(), server));
        eventManager.register(this, userManager);
        logger.info(MUTES_DISABLED_STRING);

        commandManager.register(BanCommand.banCommand(this));

        commandManager.register(TempbanCommand.tempbanCommand(this));
        commandManager.register(PunishmentRemovalCommand.unbanCommand(this));
        commandManager.register(PunishmentRemovalCommand.unmuteCommand(this));
        commandManager.register(PunishmentCommand.punishmentCommand(this));
        commandManager.register(MuteCommand.muteCommand(this));
        commandManager.register(TempmuteCommand.tempmuteCommand(this));
        commandManager.register(KickCommand.kickCommand(this));
        commandManager.register(WhitelistCommand.whitelistCommand(this));
    }

    @SuppressWarnings({"unchecked", "UnstableApiUsage"})
    private HikariDataSource createDataSource() {
        var dbData = configurationManager.getConfiguration().getDataBaseData();
        ConfigurationStage stage = switch (dbData.sqlType().name().toLowerCase()) {
            case "sqlite" ->
                    DataSourceCreator.create(SqLite.get()).configure(sqLiteJdbc -> sqLiteJdbc.path(dataDirectory.resolve("punishment.db"))).create();
            case "postgresql" ->
                    DataSourceCreator.create(PostgreSql.get()).configure(jdbcConfig -> jdbcConfig.host(dbData.getHost())

                            .port(dbData.getPort()).database(dbData.getDatabase()).user(dbData.getUsername()).password(dbData.getPassword())).create();

            default ->
                    DataSourceCreator.create((Database<RemoteJdbcConfig<?>, ?>) dbData.sqlType()).configure(jdbcConfig -> jdbcConfig.host(dbData.getHost()).port(dbData.getPort()).database(dbData.getDatabase()).user(dbData.getUsername()).password(dbData.getPassword())).create();
        };
        return stage.withMaximumPoolSize(dbData.getMaxPoolSize()).withMinimumIdle(dbData.getMinIdle()).withPoolName("necrify-hikari").forSchema(dbData.getPostgresSchema()).build();
    }

    @SuppressWarnings("UnstableApiUsage")
    private void updateDatabase() throws IOException, SQLException {
        if (configurationManager.getConfiguration().getDataBaseData().sqlType().name().equalsIgnoreCase("postgresql")) {
            SqlUpdater.builder(dataSource, PostgreSql.get()).setSchemas(configurationManager.getConfiguration().getDataBaseData().getPostgresSchema())
                    //.preUpdateHook(new SqlVersion(1, 1), connection -> )
                    .execute();
        } else {
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

    public Logger getLogger() {
        return logger;
    }

    public boolean whitelistActive() {
        return configurationManager.getConfiguration().isWhitelistActivated();
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
        return Util.executeAsync(() -> (Optional<T>) Query.query("SELECT u.* FROM punishment.necrify_user u " + "INNER JOIN punishment.necrify_punishment p ON u.uuid = p.uuid " + "WHERE p.punishment_id = ?;").single(Call.of().bind(punishmentId, Adapters.UUID_ADAPTER)).map(row -> {
            var userId = row.getObject(1, UUID.class);
            var cachedUser = getUserManager().getUser(userId);
            if (cachedUser.isPresent()) {
                return cachedUser.get().getPunishment(punishmentId).orElse(null);
            }
            var user = new VelocityUser(row.getObject(1, UUID.class), row.getString(2), row.getBoolean(3), this);
            getExecutor().execute(() -> {
                Query.query("SELECT type, expiration, reason, punishment_id FROM punishment.necrify_punishment WHERE uuid = ?;").single(Call.of().bind(userId, Adapters.UUID_ADAPTER)).map(user::addPunishment).all();
                getEventDispatcher().dispatch(new UserLoadedEvent(user).setOrigin(EventOrigin.ofClass(getClass())));
            });
            return user.getPunishment(punishmentId).orElse(null);
        }).first(), getExecutor());
    }

    @Override
    public @NotNull EventDispatcher getEventDispatcher() {
        return eventDispatcher;
    }

    @Override
    public void setEventDispatcher(@NotNull EventDispatcher eventDispatcher) {
        this.eventDispatcher = eventDispatcher;
    }
}
