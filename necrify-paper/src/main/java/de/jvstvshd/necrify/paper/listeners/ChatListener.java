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

import de.jvstvshd.necrify.paper.NecrifyPaperPlugin;
import io.papermc.paper.event.player.AsyncChatEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.Comparator;
import java.util.stream.Collectors;

public class ChatListener implements Listener {

    private final NecrifyPaperPlugin plugin;

    public ChatListener(NecrifyPaperPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onChat(AsyncChatEvent event) {
        var mutes = plugin.cachedMutes().stream().filter(muteData -> event.getPlayer().getUniqueId().equals(muteData.getPlayer().getUniqueId())).collect(Collectors.toList());
        if (mutes.isEmpty()) return;
        mutes.sort(Comparator.comparing(MuteInformation::getDuration));
        var queue = new ArrayDeque<>(mutes);
        while (!queue.isEmpty()) {
            var mute = queue.poll();
            if (!mute.getDuration().isPermanent() && mute.getDuration().expiration().isBefore(LocalDateTime.now())) {
                plugin.cachedMutes().remove(mute);
                continue;
            }
            event.setCancelled(true);
            event.getPlayer().sendMessage(mute.getReason());
            break;
        }
    }
}
