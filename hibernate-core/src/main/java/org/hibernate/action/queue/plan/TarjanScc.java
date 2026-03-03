/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.plan;

import org.hibernate.action.queue.graph.Graph;
import org.hibernate.action.queue.graph.GraphEdge;
import org.hibernate.action.queue.graph.GroupNode;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * @author Steve Ebersole
 */
public class TarjanScc {
	static List<List<GroupNode>> compute(Graph graph) {
		final Map<GroupNode, Integer> index = new HashMap<>();
		final Map<GroupNode, Integer> lowlink = new HashMap<>();
		final Deque<GroupNode> stack = new ArrayDeque<>();
		final Set<GroupNode> onStack = new HashSet<>();
		final List<List<GroupNode>> out = new ArrayList<>();
		final int[] idx = {0};

		for (GroupNode v : graph.nodes()) {
			if (!index.containsKey(v)) {
				strongConnect(graph, v, idx, index, lowlink, stack, onStack, out);
			}
		}
		return out;
	}

	private static void strongConnect(
			Graph graph,
			GroupNode v,
			int[] idx,
			Map<GroupNode, Integer> index,
			Map<GroupNode, Integer> lowlink,
			Deque<GroupNode> stack,
			Set<GroupNode> onStack,
			List<List<GroupNode>> out
	) {
		index.put(v, idx[0]);
		lowlink.put(v, idx[0]);
		idx[0]++;

		stack.push(v);
		onStack.add(v);

		for ( GraphEdge e : graph.outgoing().getOrDefault(v, List.of())) {
			if (e.isBroken()) {
				continue;
			}

			GroupNode w = e.getTo();
			if (!index.containsKey(w)) {
				strongConnect(graph, w, idx, index, lowlink, stack, onStack, out);
				lowlink.put(v, Math.min(lowlink.get(v), lowlink.get(w)));
			}
			else if (onStack.contains(w)) {
				lowlink.put(v, Math.min(lowlink.get(v), index.get(w)));
			}
		}

		if ( Objects.equals(lowlink.get(v), index.get(v))) {
			final ArrayList<GroupNode> scc = new ArrayList<>();
			GroupNode w;
			do {
				w = stack.pop();
				onStack.remove(w);
				scc.add(w);
			} while (w != v);

			scc.sort( Comparator.comparingLong( GroupNode::stableId ));
			out.add(scc);
		}
	}
}
