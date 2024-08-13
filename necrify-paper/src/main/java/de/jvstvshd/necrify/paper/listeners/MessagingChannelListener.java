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

package de.jvstvshd.necrify.paper.listeners;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.io.ByteStreams;
import de.jvstvshd.necrify.common.plugin.MuteData;
import de.jvstvshd.necrify.paper.NecrifyPaperPlugin;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.jetbrains.annotations.NotNull;

public class MessagingChannelListener implements PluginMessageListener {

    private final NecrifyPaperPlugin plugin;

    public MessagingChannelListener(NecrifyPaperPlugin plugin) {
        this.plugin = plugin;
    }

    @SuppressWarnings("UnstableApiUsage")
    @Override
    public void onPluginMessageReceived(@NotNull String channel, @NotNull Player player, byte @NotNull [] message) {
        if (message.length == 0) return;
        var input = ByteStreams.newDataInput(message);
        var content = input.readUTF();
        MuteData data;
        try {
            data = MuteData.OBJECT_MAPPER.readValue(content, MuteData.class);
        } catch (JsonProcessingException e) {
            plugin.getSLF4JLogger().error("Could not parse MuteData", e);
            return;
        }
        if (data.getType() == MuteData.RESET) {
            plugin.cachedMutes().stream().filter(muteInformation -> muteInformation.getPlayer().getUniqueId().equals(data.getUuid())).forEach(plugin.cachedMutes()::remove);
            return;
        }
        var mute = MuteInformation.from(data);
        switch (data.getType()) {
            case MuteData.ADD -> plugin.cachedMutes().add(mute);
            case MuteData.REMOVE ->
                    plugin.cachedMutes().removeIf(muteInformation -> muteInformation.getPunishmentUUID().equals(mute.getPunishmentUUID()));
            case MuteData.UPDATE ->
                    plugin.cachedMutes().stream().filter(muteInformation -> muteInformation.getPunishmentUUID().equals(mute.getPunishmentUUID())).findFirst().ifPresent(muteInformation -> muteInformation.updateTo(mute));
        }
    }
}
