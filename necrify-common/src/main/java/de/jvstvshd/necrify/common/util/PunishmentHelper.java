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

package de.jvstvshd.necrify.common.util;

import de.jvstvshd.necrify.api.message.MessageProvider;
import de.jvstvshd.necrify.api.punishment.Punishment;
import de.jvstvshd.necrify.api.punishment.TemporalPunishment;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public class PunishmentHelper {

    private PunishmentHelper() {
        throw new IllegalStateException("Utility class");
    }

    public static Component buildPunishmentData(Punishment punishment, MessageProvider provider) {
        return Component.text()
                .append(provider.provide("helper.type").color(NamedTextColor.AQUA),
                        Component.text(punishment.getType().getName()).color(NamedTextColor.YELLOW),
                        Component.newline(),
                        provider.provide("helper.reason").color(NamedTextColor.AQUA),
                        punishment.getReason(),
                        Component.newline(),
                        punishment instanceof TemporalPunishment temporalPunishment ?
                                buildPunishmentDataTemporal(temporalPunishment, provider) : Component.text(""),
                        Component.newline()
                )
                .build();
    }

    public static Component buildPunishmentDataTemporal(TemporalPunishment punishment, MessageProvider provider) {
        return punishment.isPermanent() ? Component.text("permanent").color(NamedTextColor.RED) : Component.text()
                .append(provider.provide("helper.temporal.duration").color(NamedTextColor.AQUA),
                        Component.text(punishment.getDuration().remainingDuration()).color(NamedTextColor.YELLOW),
                        Component.newline(),
                        provider.provide("helper.temporal.end").color(NamedTextColor.AQUA),
                        Component.text(punishment.getDuration().expirationAsString()).color(NamedTextColor.YELLOW))
                .build();
    }
}
