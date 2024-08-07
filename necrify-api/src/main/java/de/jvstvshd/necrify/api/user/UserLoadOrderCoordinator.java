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
package de.jvstvshd.necrify.api.user;

import de.jvstvshd.necrify.api.punishment.Punishment;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * A utility class that is responsible for ordering a list of punishments to be loaded in the correct order. This reflects
 * the {@link Punishment#getSuccessor() succession order of punishments} so that every punishment can be instantiated with
 * the instance of its successor and is not referencing a null value.
 * <p>This method only accepts the generic {@code Map<String, Object>} also taken by
 * {@link de.jvstvshd.necrify.api.punishment.PunishmentTypeRegistry#createPunishment(int, Map)} since there are no punishment instances at this point.</p>
 * <p>This class uses the Kahn's algorithm to perform a topological sort on the list of punishments. The algorithm is
 * guaranteed to work if the graph is a directed acyclic graph (DAG). If the graph contains a cycle, the algorithm will
 * throw an {@link IllegalStateException}.</p>
 * <p><a href="https://en.wikipedia.org/wiki/Topological_sorting#Kahn's_algorithm">See here fore more information</a></p>
 * @since 1.2.0
 */
public final class UserLoadOrderCoordinator {

    private UserLoadOrderCoordinator() {
        throw new UnsupportedOperationException("This class cannot be instantiated");
    }

    /**
     * Sorts the list of punishments topologically. This method utilizes the Kahn's algorithm to sort the list of
     * punishments in the correct order. The list of nodes must contain the following key-value-pairs:
     * <ul>
     *     <li>{@code punishmentUuid} - the {@link UUID} of the punishment</li>
     *     <li>{@code successorId} - the {@link UUID} of the successor punishment</li>
     *     <li>Any other key-value-pairs that are required for the instantiation of the punishment may remain in this map</li>
     * </ul>
     * If the upper conditions are not met, the method will yield unexpected results.
     * @param nodes the list of nodes to be sorted
     * @return the sorted list of nodes that respects the succession order of the punishments
     * @throws IllegalStateException if the graph contains a cycle
     */
    @NotNull
    public static List<Map<String, Object>> topologicalSort(@NotNull List<Map<String, Object>> nodes) {
        Map<UUID, Integer> inDegree = new HashMap<>();
        Map<UUID, List<UUID>> graph = new HashMap<>();
        List<Map<String, Object>> sortedList = new ArrayList<>();

        for (Map<String, Object> node : nodes) {
            UUID nodeId = (UUID) node.get("punishmentUuid");
            UUID successorId = (UUID) node.get("successorId");
            inDegree.putIfAbsent(nodeId, 0);
            graph.putIfAbsent(nodeId, new ArrayList<>());

            if (successorId != null) {
                graph.get(nodeId).add(successorId);
                inDegree.put(successorId, inDegree.getOrDefault(successorId, 0) + 1);
            }
        }

        Queue<UUID> queue = new LinkedList<>();
        for (UUID nodeId : inDegree.keySet()) {
            if (inDegree.get(nodeId) == 0) {
                queue.add(nodeId);
            }
        }

        while (!queue.isEmpty()) {
            UUID nodeId = queue.poll();
            sortedList.add(findNodeById(nodes, nodeId));

            for (UUID neighbor : graph.get(nodeId)) {
                inDegree.put(neighbor, inDegree.get(neighbor) - 1);
                if (inDegree.get(neighbor) == 0) {
                    queue.add(neighbor);
                }
            }
        }

        if (sortedList.size() != nodes.size()) {
            throw new IllegalStateException("Graph has at least one cycle");
        }
        Collections.reverse(sortedList);
        return sortedList;
    }

    /**
     * Finds a node by its {@link UUID punishmentUuid} in the list of nodes.
     * @param nodes the list of nodes
     * @param nodeId the {@link UUID punishmentUuid} of the node to be found
     * @return the node with the given {@link UUID punishmentUuid} or {@code null} if no such node exists
     */
    @Nullable
    private static Map<String, Object> findNodeById(List<Map<String, Object>> nodes, UUID nodeId) {
        for (Map<String, Object> node : nodes) {
            if (nodeId.equals(node.get("punishmentUuid"))) {
                return node;
            }
        }
        return null;
    }
}
