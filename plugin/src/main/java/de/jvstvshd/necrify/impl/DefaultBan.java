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

package de.jvstvshd.necrify.impl;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import de.jvstvshd.necrify.api.PunishmentException;
import de.jvstvshd.necrify.api.duration.PunishmentDuration;
import de.jvstvshd.necrify.api.message.MessageProvider;
import de.jvstvshd.necrify.api.punishment.Ban;
import de.jvstvshd.necrify.api.punishment.Punishment;
import de.jvstvshd.necrify.api.punishment.StandardPunishmentType;
import de.jvstvshd.necrify.api.punishment.util.PlayerResolver;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

import javax.sql.DataSource;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

public class DefaultBan extends AbstractTemporalPunishment implements Ban {

    public DefaultBan(UUID playerUuid, Component reason, DataSource dataSource, PlayerResolver playerResolver, DefaultPunishmentManager punishmentManager, ExecutorService service, PunishmentDuration duration, MessageProvider messageProvider) {
        super(playerUuid, reason, dataSource, playerResolver, punishmentManager, service, duration, messageProvider);
    }

    public DefaultBan(UUID playerUuid, Component reason, DataSource dataSource, ExecutorService service, DefaultPunishmentManager punishmentManager, UUID punishmentUuid, PlayerResolver playerResolver, PunishmentDuration duration, MessageProvider messageProvider) {
        super(playerUuid, reason, dataSource, service, punishmentManager, punishmentUuid, playerResolver, duration, messageProvider);
    }

    @Override
    public boolean isOngoing() {
        return getDuration().expiration().isAfter(LocalDateTime.now());
    }

    @Override
    public CompletableFuture<Punishment> punish() throws PunishmentException {
        var punishment = super.punish();
        tryKick();
        return punishment;
    }

    private void tryKick() {
        Optional<Player> optionalPlayer = getPunishmentManager().getServer().getPlayer(getPlayerUuid());
        if (optionalPlayer.isEmpty())
            return;
        var reason = createFullReason(optionalPlayer.get());
        optionalPlayer.get().disconnect(reason);
    }

    @Override
    public Component createFullReason(CommandSource source) {
        if (!isValid()) {
            return Component.text("INVALID").decorate(TextDecoration.BOLD).color(NamedTextColor.DARK_RED);
        }
        if (isPermanent()) {
            return getMessageProvider().provide("punishment.ban.permanent.full-reason", getReason());
        } else {
            var until = Component.text(getDuration().expiration().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
                    .color(NamedTextColor.YELLOW);
            return getMessageProvider().provide("punishment.ban.temp.full-reason",
                    Component.text(getDuration().remainingDuration()).color(NamedTextColor.YELLOW), getReason(), until);

        }
    }

    @Override
    public boolean isPermanent() {
        return getDuration().isPermanent();
    }

    @Override
    public StandardPunishmentType getType() {
        return getDuration().isPermanent() ? StandardPunishmentType.PERMANENT_BAN : StandardPunishmentType.BAN;
    }
}
