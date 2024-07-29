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

package de.jvstvshd.necrify.common.punishment;

import de.jvstvshd.necrify.api.duration.PunishmentDuration;
import de.jvstvshd.necrify.api.punishment.Punishment;
import de.jvstvshd.necrify.api.punishment.PunishmentFactory;
import de.jvstvshd.necrify.api.punishment.PunishmentType;
import de.jvstvshd.necrify.api.user.NecrifyUser;
import de.jvstvshd.necrify.common.AbstractNecrifyPlugin;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.UUID;

public class NecrifyPunishmentFactory implements PunishmentFactory {

    private final AbstractNecrifyPlugin plugin;

    public NecrifyPunishmentFactory(AbstractNecrifyPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull Punishment createPunishment(@NotNull PunishmentType type, @NotNull Map<String, Object> data) {
        final PunishmentDuration duration = (PunishmentDuration) data.get("duration");
        final Component reason = (Component) data.get("reason");
        final UUID punishmentUuid = (UUID) data.get("punishmentUuid");
        final NecrifyUser user = (NecrifyUser) data.get("user");
        var builder = PunishmentBuilder.newBuilder(plugin)
                .withDuration(duration)
                .withReason(reason)
                .withUser(user)
                .withPunishmentUuid(punishmentUuid);
        Punishment punishment;
        switch (type.standard()) {
            case TEMPORARY_BAN, PERMANENT_BAN -> punishment = builder.buildBan();
            case TEMPORARY_MUTE, PERMANENT_MUTE -> punishment = builder.buildMute();
            case KICK -> punishment = builder.buildKick();
            default -> throw new UnsupportedOperationException("unhandled punishment type: " + type.getName());
        }
        return punishment;
    }
}
