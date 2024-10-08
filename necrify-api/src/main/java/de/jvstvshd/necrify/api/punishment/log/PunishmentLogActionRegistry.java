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

package de.jvstvshd.necrify.api.punishment.log;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * A central registry for {@link PunishmentLogAction}s. This class is used to register and retrieve punishment log actions by their names.
 * @since 1.2.2
 */
public class PunishmentLogActionRegistry {

    static {
        registerAction(PunishmentLogAction.CREATED);
        registerAction(PunishmentLogAction.CHANGE_REASON);
        registerAction(PunishmentLogAction.CHANGE_DURATION);
        registerAction(PunishmentLogAction.CHANGE_PREDECESSOR);
        registerAction(PunishmentLogAction.CHANGE_SUCCESSOR);
        registerAction(PunishmentLogAction.REMOVED);
    }

    private final static Map<String, PunishmentLogAction> actions = new HashMap<>();

    /**
     * Registers a new {@link PunishmentLogAction}. If an action with the same name is already registered, it will be replaced.
     * @param action the action to register
     */
    public static void registerAction(@NotNull PunishmentLogAction action) {
        actions.put(action.name(), action);
    }

    /**
     * Retrieves a {@link PunishmentLogAction} by its name. If no action with the given name is registered, an empty optional is returned.
     * @param name the name of the action
     * @return the action or an empty optional if not found
     */
    public static Optional<PunishmentLogAction> getAction(@NotNull String name) {
        return Optional.ofNullable(actions.get(name));
    }

    /**
     * Unregisters a {@link PunishmentLogAction} by its name. If no action with the given name is registered, null is returned.
     * @param name the name of the action
     * @return the unregistered action or null if not found
     */
    @Nullable
    public static PunishmentLogAction unregisterAction(String name) {
        return actions.remove(name);
    }

    /**
     * Returns a copy of all registered {@link PunishmentLogAction}s.
     * @return a copy of all registered actions
     */
    public static Map<String, PunishmentLogAction> getActions() {
        return new HashMap<>(actions);
    }
}
