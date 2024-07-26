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
import de.jvstvshd.necrify.api.user.NecrifyUser;
import de.jvstvshd.necrify.common.AbstractNecrifyPlugin;
import net.kyori.adventure.text.Component;

import java.util.UUID;

public class PunishmentBuilder {

    private final AbstractNecrifyPlugin plugin;
    private NecrifyUser user;
    private Component reason;
    private PunishmentDuration duration;
    private UUID punishmentUuid;
    private Punishment successor;

    public PunishmentBuilder(AbstractNecrifyPlugin plugin) {
        this.plugin = plugin;
    }

    public static PunishmentBuilder newBuilder(AbstractNecrifyPlugin plugin) {
        return new PunishmentBuilder(plugin);
    }

    public NecrifyUser user() {
        return user;
    }

    public PunishmentBuilder withUser(NecrifyUser user) {
        this.user = user;
        return this;
    }

    public Component reason() {
        return reason;
    }

    public PunishmentBuilder withReason(Component reason) {
        this.reason = reason;
        return this;
    }

    public PunishmentDuration duration() {
        return duration;
    }

    public PunishmentBuilder withDuration(PunishmentDuration duration) {
        this.duration = duration;
        return this;
    }

    public UUID punishmentUuid() {
        return punishmentUuid;
    }

    public PunishmentBuilder withPunishmentUuid(UUID punishmentUuid) {
        this.punishmentUuid = punishmentUuid;
        return this;
    }

    public Punishment successor() {
        return successor;
    }

    public PunishmentBuilder withSuccessor(Punishment successor) {
        this.successor = successor;
        return this;
    }

    public NecrifyBan buildBan() {
        validateValues();
        return new NecrifyBan(user, reason, punishmentUuid, duration, plugin, successor);
    }

    /**
     * Builds a kick punishment. This method ignores the value of {@link #duration()}, {@link #successor()} and {@link #plugin}.
     *
     * @return The kick punishment.
     */
    public NecrifyKick buildKick() {
        validateValues();
        return plugin.createKick(reason, user, punishmentUuid);
    }

    public NecrifyMute buildMute() {
        validateValues();
        return new NecrifyMute(user, reason, punishmentUuid, duration, plugin, successor);
    }

    private void validateValues() {
        if (punishmentUuid == null)
            punishmentUuid = UUID.randomUUID();
        if (user == null)
            throw new NullPointerException("user is null");
        if (reason == null)
            throw new NullPointerException("reason is null");
        if (duration == null)
            throw new NullPointerException("duration is null");
    }
}
