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

package de.jvstvshd.necrify.common.util;

import org.slf4j.Logger;

public class Updater {

    public static void updateInformation(Logger logger) {
        logger.info("Check here for updates:");
        logger.info("https://hangar.papermc.io/JvstvsHD/Necrify/");
        logger.info("https://github.com/JvstvsHD/necrify/releases");
        logger.info("Dev builds: https://ci.jvstvshd.de/job/Necrify/ (Proceed with caution, these builds may be unstable! Do not use these in production!)");
    }
}
