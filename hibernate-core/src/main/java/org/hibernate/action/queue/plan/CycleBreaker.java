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
import org.hibernate.metamodel.mapping.SelectableMapping;
import org.hibernate.metamodel.mapping.SelectableMappings;

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
				// No edge found by standard criteria
				// Use fallback strategies (these work even when chooseEdgeToBreak can't decide)

				// Strategy 1: Check if this is a DELETE-only cycle
				if (isDeleteOnlyCycle(cycle)) {
					// DELETE cycles cannot be broken with NULL strategy
					// Break an arbitrary edge to allow topological sort to proceed
					// Rely on deferrable constraints or batch execution
					// NOTE: This works even if all edges are non-breakable (DELETE edges always are)
					breakArbitraryDeleteEdge(cycle);
					continue;
				}

				// For non-DELETE cycles, check if ANY edges are breakable
				if (!hasAnyBreakableEdge(cycle)) {
					// All edges are marked as unbreakable - cannot proceed
					throw new IllegalStateException("Unbreakable cycle detected for SCC: " + describeScc(scc));
				}

				// Strategy 2: Try to break an UPDATE edge (UPDATEs can be deferred)
				final GraphEdge updateEdge = findUpdateEdge(cycle);
				if (updateEdge != null) {
					updateEdge.setBroken(true);
					// UPDATE operations don't need patches - they reference existing rows
					continue;
				}

				// Strategy 3: Try to break a deferrable edge
				final GraphEdge deferrableEdge = findDeferrableEdge(cycle);
				if (deferrableEdge != null) {
					deferrableEdge.setBroken(true);
					// Deferrable constraints are checked at transaction commit
					continue;
				}

				// Strategy 4: Break a non-DELETE edge as last resort
				// Prefer INSERT over UPDATE, as INSERTs can potentially use NULL strategy
				final GraphEdge nonDeleteEdge = findNonDeleteEdge(cycle);
				if (nonDeleteEdge != null) {
					nonDeleteEdge.setBroken(true);
					// Try to install patch even for non-nullable FK
					// This may fail at runtime but allows planning to proceed
					if (nonDeleteEdge.getKeyNode() != null &&
						nonDeleteEdge.getKeyNode().group().kind() == MutationKind.INSERT) {
						installPatchForEdge(nonDeleteEdge);
					}
					continue;
				}

				// If we get here, cycle contains only DELETE operations with no deferrable edges
				// This should not happen as isDeleteOnlyCycle() would have caught it
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

	private boolean hasAnyBreakableEdge(List<GraphEdge> cycle) {
		for (GraphEdge e : cycle) {
			if (!e.isBroken() && e.isBreakable()) {
				return true;
			}
		}
		return false;
	}

	private void installPatchForEdge(GraphEdge chosen) {
		// NOTE: this is always an INSERT

		// we need to patch the key side of the foreign key
		final PlannedOperationGroup keyGroup = chosen.getKeyNode().group();

		// This planner only applies NULL-in-INSERT strategy
		if ( keyGroup.kind() != MutationKind.INSERT)  {
			return;
		}

		final String table = keyGroup.tableExpression();

		for ( PlannedOperation op : keyGroup.operations()) {
			if (op.getBindingPatch() == null) {
				op.setBindingPatch( new BindingPatch(table, toSet(chosen.getChildColumnsToNull())) );
			}
			else {
				final Set<SelectableMapping> merged = new LinkedHashSet<>(op.getBindingPatch().fkColumnsToNull());
				apply( chosen.getChildColumnsToNull(), merged );
				op.setBindingPatch( new BindingPatch(op.getBindingPatch().tableName(), merged) );
			}
		}
	}

	private static Set<SelectableMapping> toSet(SelectableMappings mappings) {
		final var set = new LinkedHashSet<SelectableMapping>();
		apply( mappings, set );
		return set;
	}

	private static void apply(SelectableMappings selectableMappings, Set<SelectableMapping> set) {
		selectableMappings.forEachSelectable( (selectionIndex, selectableMapping) -> set.add(selectableMapping) );
	}

	private boolean isDeleteOnlyCycle(List<GraphEdge> cycle) {
		// Check if all nodes in the cycle are DELETE operations
		for (GraphEdge e : cycle) {
			final PlannedOperationGroup fromGroup = e.getFrom().group();
			final PlannedOperationGroup toGroup = e.getTo().group();

			if (fromGroup.kind() != MutationKind.DELETE) {
				return false;
			}
			if (toGroup.kind() != MutationKind.DELETE) {
				return false;
			}
		}
		return true;
	}

	private void breakArbitraryDeleteEdge(List<GraphEdge> cycle) {
		// For DELETE cycles, we can't use NULL strategy
		// Break an arbitrary edge to allow topological sort to proceed
		// The database must handle the cycle via deferrable constraints or cascade
		//
		// NOTE: DELETE edges are always marked as non-breakable (can't use NULL-INSERT)
		// But for DELETE-only cycles, we MUST break an edge to allow any progress.
		// This is especially true for self-referencing FKs where you need to delete rows.

		GraphEdge toBreak = null;

		// Prefer deferrable edges if available (database can defer FK checks)
		for (GraphEdge e : cycle) {
			if (e.isBroken()) continue;
			if (e.isDeferrable()) {
				toBreak = e;
				break;
			}
		}

		// Otherwise just pick the first non-broken edge (ignore isBreakable for DELETE cycles)
		if (toBreak == null) {
			for (GraphEdge e : cycle) {
				if (!e.isBroken()) {
					toBreak = e;
					break;
				}
			}
		}

		if (toBreak != null) {
			toBreak.setBroken(true);
		}
	}

	/**
	 * Find an UPDATE edge in the cycle. UPDATE operations can be safely broken
	 * because they reference existing rows and don't create new dependencies.
	 */
	private GraphEdge findUpdateEdge(List<GraphEdge> cycle) {
		for (GraphEdge e : cycle) {
			if (e.isBroken()) {
				continue;
			}
			// Check if either end of the edge is an UPDATE operation
			if (e.getFrom().group().kind() == MutationKind.UPDATE ||
				e.getTo().group().kind() == MutationKind.UPDATE) {
				return e;
			}
		}
		return null;
	}

	/**
	 * Find a deferrable edge in the cycle. Deferrable constraints are checked
	 * at transaction commit, so breaking these edges is safe.
	 */
	private GraphEdge findDeferrableEdge(List<GraphEdge> cycle) {
		for (GraphEdge e : cycle) {
			if (e.isBroken()) {
				continue;
			}
			if (e.isDeferrable()) {
				return e;
			}
		}
		return null;
	}

	/**
	 * Find a non-DELETE edge in the cycle. As a last resort, we can break
	 * INSERT edges even with non-nullable FKs, though this may cause runtime errors.
	 */
	private GraphEdge findNonDeleteEdge(List<GraphEdge> cycle) {
		// First try to find INSERT edges (more likely to work with patches)
		for (GraphEdge e : cycle) {
			if (e.isBroken()) {
				continue;
			}
			if (e.getFrom().group().kind() == MutationKind.INSERT &&
				e.getTo().group().kind() == MutationKind.INSERT) {
				return e;
			}
		}

		// Then try any non-DELETE edge
		for (GraphEdge e : cycle) {
			if (e.isBroken()) {
				continue;
			}
			if (e.getFrom().group().kind() != MutationKind.DELETE ||
				e.getTo().group().kind() != MutationKind.DELETE) {
				return e;
			}
		}

		return null;
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
