package de.jvstvshd.necrify.velocity.user;

import com.velocitypowered.api.proxy.ProxyServer;
import de.jvstvshd.necrify.api.message.MessageProvider;
import de.jvstvshd.necrify.api.punishment.PunishmentManager;
import de.jvstvshd.necrify.api.punishment.util.PlayerResolver;
import de.jvstvshd.necrify.api.user.NecrifyUser;
import de.jvstvshd.necrify.api.user.UserManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.sql.DataSource;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

//20.06.2024: Only stub implementation to allow ConnectionListener to work.
public class VelocityUserManager implements UserManager {

    private final PlayerResolver resolver;
    private final DataSource dataSource;
    private final ExecutorService service;
    private final MessageProvider messageProvider;
    private final ProxyServer server;
    private final PunishmentManager punishmentManager;


    public VelocityUserManager(PlayerResolver resolver, DataSource dataSource, ExecutorService service, MessageProvider messageProvider, ProxyServer server, PunishmentManager punishmentManager) {
        this.resolver = resolver;
        this.dataSource = dataSource;
        this.service = service;
        this.messageProvider = messageProvider;
        this.server = server;
        this.punishmentManager = punishmentManager;
    }

    @Override
    public @Nullable NecrifyUser getUser(@NotNull UUID uuid) {
        notImplemented();
        return null;
    }

    @Override
    public @Nullable NecrifyUser getUser(@NotNull String name) {
        notImplemented();
        return null;
    }

    @Override
    public @NotNull CompletableFuture<@Nullable NecrifyUser> loadUser(@NotNull UUID uuid) {

        return CompletableFuture.supplyAsync(() -> new VelocityUser(uuid,
                resolver.getOrQueryPlayerName(uuid, service).join(),
                null,
                punishmentManager.getPunishments(uuid, service).join(),
                dataSource,
                service,
                messageProvider,
                server));
    }

    @Override
    public @NotNull CompletableFuture<@Nullable NecrifyUser> loadUser(@NotNull String name) {
        notImplemented();
        return null;
    }

    private void notImplemented() {
        throw new UnsupportedOperationException("Not implemented yet.");
    }
}
