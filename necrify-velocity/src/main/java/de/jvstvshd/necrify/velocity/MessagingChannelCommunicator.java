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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.io.ByteStreams;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.player.ServerPostConnectEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import de.jvstvshd.necrify.api.event.punishment.PunishmentCancelledEvent;
import de.jvstvshd.necrify.api.event.punishment.PunishmentChangedEvent;
import de.jvstvshd.necrify.api.event.punishment.PunishmentPersecutedEvent;
import de.jvstvshd.necrify.api.punishment.Mute;
import de.jvstvshd.necrify.api.punishment.util.ReasonHolder;
import de.jvstvshd.necrify.common.plugin.MuteData;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

public class MessagingChannelCommunicator {

    private final Map<RegisteredServer, List<MuteData>> messageQueue = new ConcurrentHashMap<>();

    private final ProxyServer server;
    private final Logger logger;
    private final List<RegisteredServer> supportingServers;
    private boolean supportedEverywhere = false;

    public MessagingChannelCommunicator(ProxyServer server, Logger logger) {
        this.server = server;
        this.logger = logger;
        this.supportingServers = new ArrayList<>();
    }

    /**
     * Tries to send the specified {@code muteData } to all registered servers. As a plugin message can only be sent if a player is connected to the server,
     * the message will be queued if no player is connected to the server and will be sent as soon as a player connects to the server.
     *
     * @param mute the mute data to send
     * @param type the type of the mute ({@link MuteData#ADD}, {@link MuteData#REMOVE} or {@link MuteData#UPDATE})
     * @throws JsonProcessingException if the mute data could not be serialized
     */
    public void queueMute(Mute mute, int type) throws Exception {
        var muteData = from(mute, type, ReasonHolder::getReason);
        queueMute(muteData);
    }

    private void queueMute(MuteData muteData) throws JsonProcessingException {
        for (RegisteredServer allServer : server.getAllServers()) {
            var result = sendMessage(allServer, muteData);
            if (!result) {
                messageQueue.computeIfAbsent(allServer, server -> new ArrayList<>()).add(muteData);
            }
        }
    }

    private MuteData from(Mute mute, int type, Function<Mute, Component> reason) {
        return new MuteData(mute.getUser().getUuid(), MiniMessage.miniMessage().serialize(reason.apply(mute)), mute.getDuration().expiration(), type, mute.getPunishmentUuid());
    }

    @SuppressWarnings("UnstableApiUsage")
    @Subscribe
    public void onChooseInitialServer(ServerPostConnectEvent event) {
        var queue = messageQueue;
        server.getAllServers().stream().filter(queue::containsKey).toList().forEach(registeredServer -> registeredServer.ping().whenComplete((serverPing, throwable) -> {
                    if (throwable != null) return;
                    var messagesTemp = queue.get(registeredServer);
                    if (messagesTemp == null || messagesTemp.isEmpty()) return;
                    var messages = new ArrayList<>(messagesTemp);

                    for (MuteData message : messages) {
                        try {
                            boolean sent = sendMessage(registeredServer, message);
                            if (sent) {
                                messageQueue.get(registeredServer).remove(message);
                            }
                        } catch (JsonProcessingException e) {
                            logger.error("Could not send message to server {}", registeredServer.getServerInfo().getName(), e);
                        }
                    }
                })
        );
    }

    private boolean sendMessage(RegisteredServer server, MuteData muteData) throws JsonProcessingException {
        return server.sendPluginMessage(NecrifyVelocityPlugin.MUTE_DATA_CHANNEL_IDENTIFIER, serializeMuteData(muteData));
    }

    @SuppressWarnings("UnstableApiUsage")
    public byte[] serializeMuteData(MuteData muteData) throws JsonProcessingException {
        var dataOutput = ByteStreams.newDataOutput();
        var serialized = MuteData.OBJECT_MAPPER.writeValueAsString(muteData);
        dataOutput.writeUTF(serialized);
        return dataOutput.toByteArray();
    }

    public synchronized List<RegisteredServer> testRecipients() {
        byte[] message = new byte[]{0x00};
        supportingServers.clear();
        for (RegisteredServer allServer : server.getAllServers()) {
            if (allServer.sendPluginMessage(NecrifyVelocityPlugin.MUTE_DATA_CHANNEL_IDENTIFIER, message)) {
                supportingServers.add(allServer);
            } else {
                supportedEverywhere = false;
            }
        }
        var servers = new ArrayList<>(server.getAllServers());
        servers.removeIf(supportingServers::contains);
        return servers;
    }

    public boolean isSupportedEverywhere() {
        return supportedEverywhere;
    }

    public boolean isPlayerReachable(Player player) {
        return player.getCurrentServer().map(serverConnection -> supportingServers.contains(serverConnection.getServer())).orElse(false);
    }

    @Subscribe
    public void onPlayerDisconnect(DisconnectEvent event) {
        try {
            queueMute(new MuteData(event.getPlayer().getUniqueId(), null, null, MuteData.RESET, null));
        } catch (Exception e) {
            logger.error("Could not queue mute for player {}", event.getPlayer().getUniqueId(), e);
        }
    }

    @org.greenrobot.eventbus.Subscribe
    public void onPunishmentPersecution(PunishmentPersecutedEvent event) {
        if (event.getPunishment() instanceof Mute mute) {
            try {
                queueMute(mute, MuteData.ADD);
            } catch (Exception e) {
                logger.error("Could not queue mute for player {}", mute.getUser().getUuid(), e);
            }
        }
    }

    @org.greenrobot.eventbus.Subscribe
    public void onPunishmentChange(PunishmentChangedEvent event) {
        if (event.getPunishment() instanceof Mute mute) {
            try {
                queueMute(mute, MuteData.UPDATE);
            } catch (Exception e) {
                logger.error("Could not queue mute for player {}", mute.getUser().getUuid(), e);
            }
        }
    }

    @org.greenrobot.eventbus.Subscribe
    public void onPunishmentRemoved(PunishmentCancelledEvent event) {
        if (event.getPunishment() instanceof Mute mute) {
            try {
                queueMute(mute, MuteData.REMOVE);
            } catch (Exception e) {
                logger.error("Could not queue mute for player {}", mute.getUser().getUuid(), e);
            }
        }
    }
}
