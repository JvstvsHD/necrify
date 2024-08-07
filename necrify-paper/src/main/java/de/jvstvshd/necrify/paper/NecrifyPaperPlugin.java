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
package de.jvstvshd.necrify.paper;

import de.jvstvshd.necrify.common.plugin.MuteData;
import de.jvstvshd.necrify.paper.listeners.ChatListener;
import de.jvstvshd.necrify.paper.listeners.MessagingChannelListener;
import de.jvstvshd.necrify.paper.listeners.MuteInformation;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

public class NecrifyPaperPlugin extends JavaPlugin {

    private final List<MuteInformation> cachedMutes = new ArrayList<>();

    @Override
    public void onEnable() {
        getLogger().info("NecrifyPaperPlugin has been enabled!");
        getServer().getMessenger().registerIncomingPluginChannel(this, MuteData.MUTE_DATA_CHANNEL_IDENTIFIER, new MessagingChannelListener(this));
        getServer().getPluginManager().registerEvents(new ChatListener(this), this);
    }

    @Override
    public void onDisable() {
        getLogger().info("NecrifyPaperPlugin has been disabled!");
    }

    public List<MuteInformation> cachedMutes() {
        return cachedMutes;
    }
}
