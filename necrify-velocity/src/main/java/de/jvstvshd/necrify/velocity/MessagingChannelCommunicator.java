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

    public MessagingChannelCommunicator(ProxyServer server, Logger logger) {
        this.server = server;
        this.logger = logger;
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