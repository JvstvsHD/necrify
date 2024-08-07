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
