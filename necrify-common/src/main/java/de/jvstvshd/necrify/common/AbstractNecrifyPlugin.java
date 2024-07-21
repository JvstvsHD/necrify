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

package de.jvstvshd.necrify.common;

import de.jvstvshd.necrify.api.Necrify;
import de.jvstvshd.necrify.api.user.NecrifyUser;
import de.jvstvshd.necrify.common.commands.NecrifyCommand;
import de.jvstvshd.necrify.common.punishment.NecrifyKick;
import net.kyori.adventure.text.Component;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.annotations.AnnotationParser;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.ExecutorService;

public abstract class AbstractNecrifyPlugin implements Necrify {

    protected ExecutorService executorService;

    public AbstractNecrifyPlugin(ExecutorService executorService) {
        this.executorService = executorService;
    }

    @Override
    public @NotNull ExecutorService getExecutor() {
        return executorService;
    }

    @Override
    public @NotNull ExecutorService getService() {
        return getExecutor();
    }

    public abstract NecrifyKick createKick(Component reason, NecrifyUser user, UUID punishmentUuid);

    /**
     * Registers commands for the plugin via the {@link AnnotationParser} from the cloud framework. It is possible to only
     * register the commands of the /necrify root, but also the top-level ones (e.g. /ban, /kick, etc.).
     *
     * @param manager          the command manager to register the commands to.
     * @param topLevelCommands whether to register top-level commands (/ban, /kick, etc.) or not (i.e. only /necrify commands).
     */
    public final void registerCommands(CommandManager<NecrifyUser> manager, boolean topLevelCommands) {
        AnnotationParser<NecrifyUser> parser = new AnnotationParser<>(manager, NecrifyUser.class);
        final var oldExtractor = parser.commandExtractor();
        if (!topLevelCommands) {
            parser.commandExtractor(instance -> {
                var commands = new ArrayList<>(oldExtractor.extractCommands(instance));
                return commands.stream().filter(commandDescriptor -> commandDescriptor.commandToken().startsWith("necrify")).toList();
            });
        }
        parser.parse(new NecrifyCommand(this));
    }
}
