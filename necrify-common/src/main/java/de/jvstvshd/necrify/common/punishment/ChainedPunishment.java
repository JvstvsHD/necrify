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

package de.jvstvshd.necrify.common.punishment;

import de.jvstvshd.necrify.api.punishment.StandardPunishmentType;
import de.jvstvshd.necrify.api.punishment.TemporalPunishment;
import de.jvstvshd.necrify.common.AbstractNecrifyPlugin;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ChainedPunishment extends AbstractTemporalPunishment {

    private final List<TemporalPunishment> bans;

    private ChainedPunishment(List<TemporalPunishment> bans, AbstractNecrifyPlugin plugin) {
        this(bans.getFirst(), bans, plugin);
    }

    private ChainedPunishment(TemporalPunishment tp, List<TemporalPunishment> bans, AbstractNecrifyPlugin plugin) {
        super(tp.getUser(), tp.getReason(), tp.getPunishmentUuid(), tp.getDuration(), plugin, tp.getSuccessorOrNull(), tp.getCreationTime());
        this.bans = bans;
    }

    @Override
    public @NotNull Component getReason() {
        return bans.stream().map(TemporalPunishment::getReason).reduce(Component.empty(), (a, b) -> a.append(Component.newline()).append(b));
    }

    public static ChainedPunishment of(TemporalPunishment last, AbstractNecrifyPlugin plugin) {
        List<TemporalPunishment> bans = new ArrayList<>();
        TemporalPunishment current = last;
        while (current.getPredecessor() != null) {
            bans.add(current);
            current = (TemporalPunishment) current.getPredecessor();
        }
        bans.add(current);
        return new ChainedPunishment(bans, plugin);
    }

    @Override
    public @NotNull StandardPunishmentType getType() {
        return bans.getFirst().getType().standard();
    }

    @Override
    public boolean isPermanent() {
        return bans.stream().anyMatch(TemporalPunishment::isPermanent);
    }

    @SuppressWarnings("OptionalGetWithoutIsPresent")
    @Override
    public @NotNull Component createFullReason(@Nullable Locale locale) {
        return bans.stream().max((a, b) -> b.getDuration().expiration().compareTo(a.getDuration().expiration()))
                .get() //list will never be empty
                .createFullReason(locale);
    }
}