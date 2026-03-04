/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.plan;

import org.hibernate.action.queue.MutationKind;
import org.hibernate.action.queue.cyclebreak.BindingPatch;
import org.hibernate.action.queue.graph.Graph;
import org.hibernate.action.queue.graph.GraphEdge;
import org.hibernate.action.queue.graph.GroupNode;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/// Uses Tarjan’s Algorithm for finding strongly connected components (SCCs)
/// in a directed graph and using that information to break cycles.
///
/// @see GraphEdge#setBroken(boolean)
/// @see BindingPatch
/// @see org.hibernate.action.queue.cyclebreak.CycleBreakPatcher
///
/// @author Steve Ebersole
public class CycleBreaker {
	public CycleBreaker() {
	}

	public void applyCycleBreaks(Graph graph) {
		final List<List<GroupNode>> sccs = TarjanScc.compute(graph);

		for (List<GroupNode> scc : sccs) {
			if (scc.size() == 1 && !hasSelfLoop(graph, scc.get(0))) {
				continue;
			}
			breakSccCycles(graph, scc);
		}
	}

	private boolean hasSelfLoop(Graph graph, GroupNode n) {
		for ( GraphEdge e : graph.outgoing().getOrDefault(n, List.of())) {
			if (!e.isBroken() && e.getTo() == n) {
				return true;
			}
		}
		return false;
	}

	private void breakSccCycles(Graph graph, List<GroupNode> scc) {
		final Set<GroupNode> inScc = new HashSet<>(scc);

		while (true) {
			final List<GraphEdge> cycle = findAnyCycleInScc(graph, inScc);
			if (cycle.isEmpty()) {
				// SCC is now acyclic
				return;
			}

			final GraphEdge chosen = chooseEdgeToBreak(cycle);
			if (chosen == null) {
				throw new IllegalStateException("Unbreakable cycle detected for SCC: " + describeScc(scc));
			}

			// Mark edge as broken for topo sort
			chosen.setBroken( true );

			// Install BindingPatch on CHILD insert group ops (chosen.to == child)
			installPatchForEdge(chosen);
		}
	}

	private enum VisitState { UNVISITED, VISITING, VISITED }

	private List<GraphEdge> findAnyCycleInScc(Graph graph, Set<GroupNode> inScc) {
		final Map<GroupNode, VisitState> state = new HashMap<>();
		final Deque<GraphEdge> stack = new ArrayDeque<>();

		for (GroupNode start : inScc) {
			if (state.getOrDefault(start, VisitState.UNVISITED) != VisitState.UNVISITED) {
				continue;
			}
			final List<GraphEdge> cycle = depthFirstSearchForCycle(graph, start, inScc, state, stack);
			if (!cycle.isEmpty()) {
				return cycle;
			}
		}
		return List.of();
	}

	private List<GraphEdge> depthFirstSearchForCycle(
			Graph graph,
			GroupNode u,
			Set<GroupNode> inScc,
			Map<GroupNode, VisitState> state,
			Deque<GraphEdge> stack) {
		state.put(u, VisitState.VISITING);

		for (GraphEdge e : graph.outgoing().getOrDefault(u, List.of())) {
			if (e.isBroken()) {
				continue;
			}
			if (!inScc.contains(e.getTo())) {
				continue;
			}

			stack.addLast(e);

			final VisitState vs = state.getOrDefault(e.getTo(), VisitState.UNVISITED);
			if (vs == VisitState.UNVISITED) {
				final List<GraphEdge> found = depthFirstSearchForCycle(graph, e.getTo(), inScc, state, stack);
				if (!found.isEmpty()) {
					return found;
				}
			}
			else if (vs == VisitState.VISITING) {
				return extractCycle(stack, e.getTo());
			}

			stack.removeLast();
		}

		state.put(u, VisitState.VISITED);
		return List.of();
	}

	private List<GraphEdge> extractCycle(Deque<GraphEdge> stack, GroupNode cycleStartNode) {
		final ArrayList<GraphEdge> path = new ArrayList<>(stack);
		int start = 0;
		for (int i = 0; i < path.size(); i++) {
			if (path.get(i).getFrom() == cycleStartNode) { start = i; break; }
		}
		return path.subList(start, path.size());
	}

	private GraphEdge chooseEdgeToBreak(List<GraphEdge> cycle) {
		GraphEdge best = null;
		for (GraphEdge e : cycle) {
			if (e.isBroken()) {
				continue;
			}
			if (!e.isBreakable()) {
				continue;
			}

			if (best == null) {
				best = e;
			}
			else if (e.getBreakCost() < best.getBreakCost()) {
				best = e;
			}
			else if (e.getBreakCost() == best.getBreakCost() && e.getStableId() < best.getStableId()) {
				best = e;
			}
		}
		return best;
	}

	private void installPatchForEdge(GraphEdge chosen) {
		final PlannedOperationGroup childGroup = chosen.getTo().group();

		// This planner only applies NULL-in-INSERT strategy
		if ( childGroup.kind() != MutationKind.INSERT)  {
			return;
		}

		final String table = childGroup.tableExpression();
		final Set<String> cols = chosen.getChildColumnsToNull();

		for ( PlannedOperation op : childGroup.operations()) {
			if (op.getBindingPatch() == null) {
				op.setBindingPatch( new BindingPatch(table, cols) );
			}
			else {
				final Set<String> merged = new LinkedHashSet<>(op.getBindingPatch().fkColumnsToNull());
				merged.addAll(cols);
				op.setBindingPatch( new BindingPatch(op.getBindingPatch().tableName(), merged) );
			}
		}
	}


	private static String describeScc(List<GroupNode> scc) {
		StringBuilder sb = new StringBuilder("{");
		boolean first = true;
		for (GroupNode n : scc) {
			if (!first) sb.append(", ");
			first = false;
			sb.append(n.group().tableExpression()).append("#").append(n.group().kind());
		}
		return sb.append("}").toString();
	}
}
