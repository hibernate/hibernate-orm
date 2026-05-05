/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.internal.plan;

import org.hibernate.action.queue.spi.plan.FlushOperation;

import org.hibernate.action.queue.spi.MutationKind;
import org.hibernate.action.queue.spi.PlanningOptions;
import org.hibernate.action.queue.internal.constraint.DeferrableConstraintMode;
import org.hibernate.action.queue.internal.cyclebreak.BindingPatch;
import org.hibernate.action.queue.internal.graph.Graph;
import org.hibernate.action.queue.internal.graph.GraphEdge;
import org.hibernate.action.queue.internal.graph.GroupNode;
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

/// Makes a flush dependency graph schedulable by breaking selected cycle edges.
///
/// The graph expresses order dependencies between operation groups. If it is acyclic,
/// the planner can topologically sort it directly. However, flushes can naturally create
/// cycles, especially for bidirectional foreign keys, self-references, deletes, and
/// unique one-to-one swaps. This class finds those cycles and marks selected edges as
/// broken, which tells later planning to ignore those edges for ordering.
///
/// Edge-breaking policy:
///
/// 1. Break preferred-order edges first; they are ordering hints, not constraints.
/// 2. For unique-only update cycles, prefer an explicit unique patch candidate.
/// 3. Otherwise, prefer null-patchable FK/unique edges by break cost and stable tie-breaking.
/// 4. If no normal candidate exists, fall back through delete-only, update, deferrable,
/// See [  and finally non-delete edges.].
///
/// When a broken edge supports a temporary-null strategy, `CycleBreaker` installs a
/// `BindingPatch`: FK cycles patch INSERTs, while unique swaps patch UPDATEs. Each SCC
/// is processed until it is acyclic or no supported break remains.
///
/// See [GraphEdge#setBroken(boolean)].
/// See [BindingPatch].
/// See [org.hibernate.action.queue.internal.cyclebreak.CycleBreakPatcher].
///
/// @author Steve Ebersole
public class CycleBreaker {
	public CycleBreaker() {
	}

	public void applyCycleBreaks(
			Graph graph,
			PlanningOptions planningOptions,
			DeferrableConstraintMode deferrableConstraintMode) {
		final List<List<GroupNode>> sccs = TarjanScc.compute(graph);

		for (List<GroupNode> scc : sccs) {
			if (scc.size() == 1 && !hasSelfLoop(graph, scc.get(0))) {
				continue;
			}
			breakSccCycles(graph, scc, deferrableConstraintMode);
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

	private void breakSccCycles(
			Graph graph,
			List<GroupNode> scc,
			DeferrableConstraintMode deferrableConstraintMode) {
		final Set<GroupNode> inScc = new HashSet<>(scc);

		while (true) {
			final List<GraphEdge> cycle = findAnyCycleInScc(graph, scc, inScc);
			if (cycle.isEmpty()) {
				// SCC is now acyclic
				return;
			}

			final GraphEdge preferredOrderEdge = findPreferredOrderEdge(cycle);
			if (preferredOrderEdge != null) {
				preferredOrderEdge.setBroken(true);
				continue;
			}

			// Unique-only UPDATE cycles occur when swapping unique values, for example
			// one-to-one relationships. Required unique edges participate in this
			// classification, but only explicit NULL_PATCHABLE_UNIQUE edges can install
			// a NULL-then-patch update.
			if ( isUniqueUpdateOrderingCycle( cycle ) ) {
				final GraphEdge uniquePatchCandidate = findUniquePatchCandidate( cycle );
				if ( uniquePatchCandidate != null ) {
					uniquePatchCandidate.setBroken( true );
					installPatchForEdge( uniquePatchCandidate );
				}
				else {
					throw new UnbreakableUniqueCycleException(
							"Unbreakable unique update cycle detected for SCC: " + describeScc( scc )
					);
				}
				continue;
			}

			final GraphEdge chosen = chooseEdgeToBreak(cycle);
			if (chosen == null) {
				if (isDeleteOnlyCycle(cycle)) {
					breakArbitraryDeleteEdge(cycle, deferrableConstraintMode);
					continue;
				}

				final GraphEdge updateEdge = findUpdateEdge(cycle);
				if (updateEdge != null) {
					updateEdge.setBroken(true);
					continue;
				}

				if (!hasAnyBreakableEdge(cycle)) {
					throw new IllegalStateException("Unbreakable cycle detected for SCC: " + describeScc(scc));
				}

				final GraphEdge deferrableEdge = findEffectivelyDeferredEdge(cycle, deferrableConstraintMode);
				if (deferrableEdge != null) {
					deferrableEdge.setBroken(true);
					continue;
				}

				// Break a non-DELETE edge as last resort.
				final GraphEdge nonDeleteEdge = findNonDeleteEdge(cycle);
				if (nonDeleteEdge != null) {
					nonDeleteEdge.setBroken(true);
					// Only explicit patchable edges actually install a patch; required
					// edges return null from GraphEdge#getPatchCycleType().
					if (nonDeleteEdge.getPatchNode() != null &&
						nonDeleteEdge.getPatchNode().group().kind() == MutationKind.INSERT) {
						installPatchForEdge(nonDeleteEdge);
					}
					continue;
				}

				throw new IllegalStateException("Unbreakable cycle detected for SCC: " + describeScc(scc));
			}

			// Mark edge as broken for topo sort
			chosen.setBroken( true );

			// Install BindingPatch on CHILD insert group ops (chosen.to == child)
			installPatchForEdge(chosen);
		}
	}

	private enum VisitState { UNVISITED, VISITING, VISITED }

	private List<GraphEdge> findAnyCycleInScc(Graph graph, List<GroupNode> scc, Set<GroupNode> inScc) {
		final Map<GroupNode, VisitState> state = new HashMap<>();
		final Deque<GraphEdge> stack = new ArrayDeque<>();

		for (GroupNode start : scc) {
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
			if (!isNullPatchableBreakCandidate( e )) {
				continue;
			}

			if (best == null) {
				best = e;
			}
			else if (e.getBreakCost() < best.getBreakCost()) {
				best = e;
			}
			else if (e.getBreakCost() == best.getBreakCost()) {
				// Prefer not to break backward-flowing edges (preserve action queue order)
				// Backward flow: toOrdinal < fromOrdinal (e.g., cascaded delete -> root delete)
				// Forward flow: toOrdinal >= fromOrdinal (e.g., root delete -> cascaded delete)
				final boolean eBackward = e.getTo().group().ordinal() < e.getFrom().group().ordinal();
				final boolean bestBackward = best.getTo().group().ordinal() < best.getFrom().group().ordinal();

				if (bestBackward && !eBackward) {
					// Current best flows backward, e flows forward - prefer e
					best = e;
				}
				else if (eBackward == bestBackward && e.getStableId() < best.getStableId()) {
					// Both flow same direction - use stableId as tiebreaker
					best = e;
				}
			}
		}
		return best;
	}

	private boolean isNullPatchableBreakCandidate(GraphEdge edge) {
		return edge.isNullPatchable();
	}

	private GraphEdge findPreferredOrderEdge(List<GraphEdge> cycle) {
		for (GraphEdge e : cycle) {
			if (isPreferredBreakCandidate( e )) {
				return e;
			}
		}
		return null;
	}

	private boolean isPreferredBreakCandidate(GraphEdge edge) {
		return !edge.isBroken() && edge.isPreferredOrder();
	}

	private boolean hasAnyBreakableEdge(List<GraphEdge> cycle) {
		for (GraphEdge e : cycle) {
			if (isNullPatchableBreakCandidate( e )) {
				return true;
			}
		}
		return false;
	}

	private void installPatchForEdge(GraphEdge chosen) {
		// Table-level edges (e.g., DELETE->INSERT for same table) don't have patch nodes
		// They can only be broken, not patched
		if (chosen.getPatchNode() == null) {
			return;  // Edge is already marked as broken, no patch needed
		}

		final FlushOperationGroup patchGroup = chosen.getPatchNode().group();

		final BindingPatch.CycleType cycleType = chosen.getPatchCycleType();
		if ( cycleType == null ) {
			return;
		}

		// Foreign key cycles patch INSERTs; unique swaps patch UPDATEs.
		if (cycleType == BindingPatch.CycleType.UNIQUE_SWAP) {
			if ( patchGroup.kind() != MutationKind.UPDATE)  {
				return;
			}
		}
		else {
			if ( patchGroup.kind() != MutationKind.INSERT)  {
				return;
			}
		}

		final String table = patchGroup.tableExpression();

		for ( FlushOperation op : patchGroup.operations()) {
			if (op.getBindingPatch() == null) {
				op.setBindingPatch( new BindingPatch(table, toSet(chosen.getColumnsToNull()), cycleType) );
			}
			else {
				final Set<SelectableMapping> merged = new LinkedHashSet<>(op.getBindingPatch().fkColumnsToNull());
				apply( chosen.getColumnsToNull(), merged );
				op.setBindingPatch( new BindingPatch(op.getBindingPatch().tableName(), merged, cycleType) );
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
			final FlushOperationGroup fromGroup = e.getFrom().group();
			final FlushOperationGroup toGroup = e.getTo().group();

			if (fromGroup.kind() != MutationKind.DELETE) {
				return false;
			}
			if (toGroup.kind() != MutationKind.DELETE) {
				return false;
			}
		}
		return true;
	}

	/// Break a DELETE-only cycle, where the temporary-null INSERT/UPDATE strategies
	/// do not apply. The database must tolerate the chosen break through deferrable
	/// constraints, cascade behavior, or the fact that the broken edge represented a
	/// table-level dependency that is not populated by the concrete rows being deleted.
	///
	/// Prefer deferrable edges, then edges that flow backward relative to operation
	/// ordinal so cascade delete order is preserved where possible.
	private void breakArbitraryDeleteEdge(
			List<GraphEdge> cycle,
			DeferrableConstraintMode deferrableConstraintMode) {
		GraphEdge toBreak = null;

		for (GraphEdge e : cycle) {
			if (isDeferrableBreakCandidate( e, deferrableConstraintMode )) {
				toBreak = e;
				break;
			}
		}

		if (toBreak == null) {
			for (GraphEdge e : cycle) {
				if (e.isBroken()) continue;
				int fromOrdinal = e.getFrom().group().ordinal();
				int toOrdinal = e.getTo().group().ordinal();
				if (toOrdinal < fromOrdinal) {
					toBreak = e;
					break;
				}
			}
		}

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

	/// Find an UPDATE edge in the cycle. UPDATEs reference existing rows, so this
	/// can preserve progress when no temporary-null candidate exists.
	private GraphEdge findUpdateEdge(List<GraphEdge> cycle) {
		for (GraphEdge e : cycle) {
			if (e.isBroken()) {
				continue;
			}
			if (e.getFrom().group().kind() == MutationKind.UPDATE ||
				e.getTo().group().kind() == MutationKind.UPDATE) {
				return e;
			}
		}
		return null;
	}

	/// Find an effectively deferred edge in the cycle. Deferred constraints are
	/// checked at transaction commit.
	private GraphEdge findEffectivelyDeferredEdge(
			List<GraphEdge> cycle,
			DeferrableConstraintMode deferrableConstraintMode) {
		for (GraphEdge e : cycle) {
			if (isDeferrableBreakCandidate( e, deferrableConstraintMode )) {
				return e;
			}
		}
		return null;
	}

	private boolean isDeferrableBreakCandidate(GraphEdge edge, DeferrableConstraintMode deferrableConstraintMode) {
		return !edge.isBroken() && edge.isEffectivelyDeferred( deferrableConstraintMode );
	}

	/// Find a non-DELETE edge in the cycle as a last resort.
	private GraphEdge findNonDeleteEdge(List<GraphEdge> cycle) {
		for (GraphEdge e : cycle) {
			if (!isLastResortBreakCandidate( e )) {
				continue;
			}
			if (e.getFrom().group().kind() == MutationKind.INSERT &&
				e.getTo().group().kind() == MutationKind.INSERT) {
				return e;
			}
		}

		for (GraphEdge e : cycle) {
			if (isLastResortBreakCandidate( e )) {
				return e;
			}
		}

		return null;
	}

	private boolean isLastResortBreakCandidate(GraphEdge edge) {
		return !edge.isBroken()
			&& (edge.getFrom().group().kind() != MutationKind.DELETE
				|| edge.getTo().group().kind() != MutationKind.DELETE);
	}


	private GraphEdge findUniquePatchCandidate(List<GraphEdge> cycle) {
		for (GraphEdge e : cycle) {
			if (!hasPatchColumns( e )) {
				continue;
			}
			if (e.isNullPatchableUnique()) {
				return e;
			}
		}
		return null;
	}

	private boolean hasPatchColumns(GraphEdge edge) {
		return edge.getPatchNode() != null
			&& edge.getColumnsToNull() != null
			&& edge.getColumnsToNull().getJdbcTypeCount() > 0;
	}

	/// Check whether a cycle is made only of unique-slot ordering edges and involves
	/// UPDATEs. This classification says what kind of ordering problem we found; it
	/// does not mean the cycle is patchable.
	private boolean isUniqueUpdateOrderingCycle(List<GraphEdge> cycle) {
		// Required unique edges are ordering edges, but not patch candidates. Nullable
		// unique swaps are represented explicitly by GraphEdgeKind.NULL_PATCHABLE_UNIQUE.
		boolean allUniqueEdges = true;
		boolean hasUpdate = false;

		for (GraphEdge e : cycle) {
			if (!e.isUniqueCycleEdge()) {
				allUniqueEdges = false;
				break;
			}

			if (e.getFrom().group().kind() == MutationKind.UPDATE ||
				e.getTo().group().kind() == MutationKind.UPDATE) {
				hasUpdate = true;
			}
		}

		return allUniqueEdges && hasUpdate;
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
