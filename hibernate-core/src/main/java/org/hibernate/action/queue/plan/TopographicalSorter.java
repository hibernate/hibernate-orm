/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.plan;

import org.hibernate.action.queue.graph.Graph;
import org.hibernate.action.queue.graph.GraphEdge;
import org.hibernate.action.queue.graph.GroupNode;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

/// Performs topological sorting on a dependency graph to determine the
/// correct execution order for mutation operations using Kahn's Algorithm
/// for breadth-first searching.
///
/// @author Steve Ebersole
public class TopographicalSorter {
	public List<GroupNode> sort(Graph graph) {
		// Initialize all nodes with in-degree 0
		final Map<GroupNode, Integer> indegree = new HashMap<>();
		for (GroupNode n : graph.nodes()) {
			indegree.put(n, 0);
		}

		// Count incoming edges for each node
		for (GroupNode u : graph.nodes()) {
			for ( GraphEdge e : graph.outgoing().getOrDefault(u, List.of())) {
				if (e.isBroken()) {
					// Skip broken edges!
					// See CycleBreaker
					continue;
				}
				indegree.put(e.getTo(), indegree.get(e.getTo()) + 1);
			}
		}

		final PriorityQueue<GroupNode> q = new PriorityQueue<>( Comparator.comparingLong( GroupNode::stableId ));
		for (var en : indegree.entrySet()) {
			if (en.getValue() == 0) {
				q.add(en.getKey());
			}
		}

		final ArrayList<GroupNode> order = new ArrayList<>(graph.nodes().size());
		while (!q.isEmpty()) {
			GroupNode u = q.remove();
			order.add(u);

			for (GraphEdge e : graph.outgoing().getOrDefault(u, List.of())) {
				if (e.isBroken()) {
					continue;
				}
				int d = indegree.merge(e.getTo(), -1, Integer::sum);
				if (d == 0) {
					q.add(e.getTo());
				}
			}
		}

		if (order.size() != graph.nodes().size()) {
			throw new IllegalStateException("Graph still cyclic after breaks (bug).");
		}

		return order;
	}
}
