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

import de.jvstvshd.necrify.api.message.MessageProvider;
import de.jvstvshd.necrify.api.punishment.Punishment;
import de.jvstvshd.necrify.api.user.NecrifyUser;
import de.jvstvshd.necrify.common.punishment.NecrifyKick;
import net.kyori.adventure.text.Component;

import javax.sql.DataSource;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

public class VelocityKick extends NecrifyKick {

    public VelocityKick(NecrifyUser user, Component reason, DataSource dataSource, ExecutorService service, UUID punishmentUuid, MessageProvider messageProvider) {
        super(user, reason, dataSource, service, punishmentUuid, messageProvider);
    }

    @Override
    public CompletableFuture<Punishment> punish() {
        if (!(getUser() instanceof VelocityUser))
            throw new IllegalStateException("Cannot kick user: User is not a VelocityUser.");
        var player = ((VelocityUser) getUser()).getPlayer();
        if (player != null)
            player.disconnect(createFullReason(null));
        return CompletableFuture.completedFuture(this);
    }
}
