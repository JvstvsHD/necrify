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

package de.jvstvshd.necrify;

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
import de.chojo.sadu.databases.Database;
import de.chojo.sadu.databases.PostgreSql;
import de.chojo.sadu.databases.SqLite;
import de.chojo.sadu.datasource.DataSourceCreator;
import de.chojo.sadu.datasource.stage.ConfigurationStage;
import de.chojo.sadu.jdbc.RemoteJdbcConfig;
import de.chojo.sadu.updater.SqlUpdater;
import de.chojo.sadu.wrapper.QueryBuilderConfig;
import de.jvstvshd.necrify.api.Necrify;
import de.jvstvshd.necrify.api.message.MessageProvider;
import de.jvstvshd.necrify.api.punishment.Punishment;
import de.jvstvshd.necrify.api.punishment.PunishmentManager;
import de.jvstvshd.necrify.api.punishment.util.PlayerResolver;
import de.jvstvshd.necrify.api.user.UserManager;
import de.jvstvshd.necrify.commands.*;
import de.jvstvshd.necrify.common.plugin.MuteData;
import de.jvstvshd.necrify.config.ConfigurationManager;
import de.jvstvshd.necrify.impl.DefaultPlayerResolver;
import de.jvstvshd.necrify.impl.DefaultPunishmentManager;
import de.jvstvshd.necrify.listener.ConnectListener;
import de.jvstvshd.necrify.message.ResourceBundleMessageProvider;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Plugin(id = "necrify", name = "Necrify", version = "1.0.0-SNAPSHOT", description = "A simple punishment plugin for Velocity", authors = {"JvstvsHD"})
public class NecrifyPlugin implements Necrify {

    private final ProxyServer server;
    private final Logger logger;
    private final ConfigurationManager configurationManager;
    private final ExecutorService service = Executors.newCachedThreadPool();
    private final Path dataDirectory;
    public static final ChannelIdentifier MUTE_DATA_CHANNEL_IDENTIFIER = MinecraftChannelIdentifier.from(MuteData.MUTE_DATA_CHANNEL_IDENTIFIER);
    private PunishmentManager punishmentManager;
    private HikariDataSource dataSource;
    private PlayerResolver playerResolver;
    private MessageProvider messageProvider;
    private UserManager userManager;


    private static final String MUTES_DISABLED_STRING = """
            Since 1.19.1, cancelling chat messages on proxy is not possible anymore. Therefore, we have to listen to the chat event on the actual game server. This means
            that there has to be a spigot/paper extension to this plugin which is not yet available unless there's a possibility. Therefore all mute related features won't work at the moment.
            If you use 1.19 or lower you will not be affected by this.The progress of the extension can be found here: https://github.com/JvstvsHD/necrify/issues/6""".replace("\n", " ");
    private final MessagingChannelCommunicator communicator;

    /**
     * Since 1.19.1, cancelling chat messages on proxy is not possible anymore. Therefore, we have to listen to the chat event on the actual game server. This means
     * that there has to be a spigot/paper extension to this plugin which is not yet available unless there's a possibility. Therefore all mute related features are disabled for now.
     * If you use 1.19 or lower you will not be affected by this.The progress of the extension can be found <a href=https://github.com/JvstvsHD/necrify/issues/6>here</a>.
     * For this reason, every mute related feature is deprecated and marked as for removal until this extension is available.
     */
    public static final Component MUTES_DISABLED = Component.text(MUTES_DISABLED_STRING);

    @Inject
    public NecrifyPlugin(ProxyServer server, Logger logger, @DataDirectory Path dataDirectory) {
        this.server = server;
        this.logger = logger;
        this.configurationManager = new ConfigurationManager(dataDirectory.resolve("config.yml"));
        this.playerResolver = new DefaultPlayerResolver(server);
        this.communicator = new MessagingChannelCommunicator(server, logger);
        this.dataDirectory = dataDirectory;
    }

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
        }
        QueryBuilderConfig.setDefault(QueryBuilderConfig.builder().withExceptionHandler(e -> logger.error("An error occurred during a database request", e)).build());
        dataSource = createDataSource();
        punishmentManager = new DefaultPunishmentManager(server, dataSource, this);
        try {
            updateDatabase();
            initDataSource();
        } catch (SQLException | IOException e) {
            logger.error("Could not create table necrify_punishment in database {}", dataSource.getDataSourceProperties().get("dataSource.databaseName"), e);
        }
        setup(server.getCommandManager(), server.getEventManager());
        logger.info("Velocity Punishment Plugin v1.2.0-SNAPSHOT has been loaded. This is only a dev build and thus may be unstable.");
    }

    private void setup(CommandManager commandManager, EventManager eventManager) {
        eventManager.register(this, communicator);
        eventManager.register(this, new ConnectListener(this, Executors.newCachedThreadPool(), server));
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
            case "sqlite" -> DataSourceCreator.create(SqLite.get())
                    .configure(sqLiteJdbc -> sqLiteJdbc.path(dataDirectory.resolve("punishment.db")))
                    .create();
            case "postgresql" ->
                    DataSourceCreator.create(PostgreSql.get()).configure(jdbcConfig -> jdbcConfig.host(dbData.getHost())

                                    .port(dbData.getPort())
                                    .database(dbData.getDatabase())
                                    .user(dbData.getUsername())
                                    .password(dbData.getPassword())
                            )
                            .create();

            default ->
                    DataSourceCreator.create((Database<RemoteJdbcConfig<?>, ?>) dbData.sqlType()).configure(jdbcConfig -> jdbcConfig.host(dbData.getHost())
                                    .port(dbData.getPort())
                                    .database(dbData.getDatabase())
                                    .user(dbData.getUsername())
                                    .password(dbData.getPassword()))
                            .create();
        };
        return stage.withMaximumPoolSize(dbData.getMaxPoolSize())
                .withMinimumIdle(dbData.getMinIdle())
                .withPoolName("necrify-hikari")
                .forSchema(dbData.getPostgresSchema())
                .build();
    }

    @SuppressWarnings("UnstableApiUsage")
    private void updateDatabase() throws IOException, SQLException {
        if (configurationManager.getConfiguration().getDataBaseData().sqlType().name().equalsIgnoreCase("postgresql")) {
            SqlUpdater.builder(dataSource, PostgreSql.get())
                    .setSchemas(configurationManager.getConfiguration().getDataBaseData().getPostgresSchema())
                    .execute();
        } else {
            logger.warn("Database type is not (yet) supported for automatic updates. Please update the database manually.");
        }
    }

    private void initDataSource() throws SQLException {
        try (Connection connection = dataSource.getConnection(); PreparedStatement statement =
                connection.prepareStatement("CREATE TABLE IF NOT EXISTS necrify_punishment (uuid  VARCHAR (36), name VARCHAR (16), type VARCHAR (1000), expiration DATETIME (6), " +
                        "reason VARCHAR (1000), punishment_id VARCHAR (36))")) {
            statement.execute();
        }
        try (Connection connection = dataSource.getConnection(); PreparedStatement statement =
                connection.prepareStatement("CREATE TABLE IF NOT EXISTS necrify_whitelist (uuid VARCHAR (36))")) {
            statement.execute();
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

    @SuppressWarnings("removal")
    @Override
    public ProxyServer getServer() {
        return server;
    }

    @Override
    public @NotNull ExecutorService getService() {
        return service;
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

    @Override
    public <T extends Punishment> CompletableFuture<Optional<T>> getPunishment(@NotNull UUID punishmentId) {
        return Necrify.super.getPunishment(punishmentId);
    }
}
