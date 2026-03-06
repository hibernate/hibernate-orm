/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.graph;

import org.hibernate.action.queue.MutationKind;
import org.hibernate.action.queue.fk.ForeignKey;
import org.hibernate.action.queue.plan.PlannedOperationGroup;
import org.hibernate.action.queue.fk.ForeignKeyModel;
import org.hibernate.metamodel.mapping.SelectableConsumer;
import org.hibernate.metamodel.mapping.SelectableMapping;
import org.hibernate.metamodel.mapping.SelectableMappings;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Comparator.comparingInt;
import static org.hibernate.action.queue.Helper.normalizeTableName;
import static org.hibernate.internal.util.collections.CollectionHelper.arrayList;

/**
 * @author Steve Ebersole
 */
public class StandardGraphBuilder extends AbstractGraphBuilder {
	// Empty SelectableMappings for non-breakable DELETE edges
	private static final SelectableMappings EMPTY_SELECTABLES = new SelectableMappings() {
		@Override
		public int getJdbcTypeCount() {
			return 0;
		}

		@Override
		public SelectableMapping getSelectable(int columnIndex) {
			throw new IndexOutOfBoundsException("No selectables in empty instance");
		}

		@Override
		public int forEachSelectable(int offset, SelectableConsumer consumer) {
			return 0;
		}
	};

	public StandardGraphBuilder(ForeignKeyModel fkModel, boolean avoidBreakingDeferrable, boolean ignoreDeferrableForOrdering) {
		super(fkModel, avoidBreakingDeferrable, ignoreDeferrableForOrdering);
	}

	@Override
	public Graph build(List<PlannedOperationGroup> groups) {
		final ArrayList<GroupNode> nodes = arrayList(groups.size());
		// After consolidation, each table has at most one node per kind+shape
		final Map<String, GroupNode> insertNodeByTable = new HashMap<>();
		final Map<String, GroupNode> updateNodeByTable = new HashMap<>();
		final Map<String, GroupNode> deleteNodeByTable = new HashMap<>();
		final Map<GroupNode, List<GraphEdge>> outgoing = new HashMap<>();

		// stable ids based on group.ordinal (if provided), else encounter order
		// (we assume group.ordinal is stable in decomposer)
		final ArrayList<PlannedOperationGroup> sortedGroups = new ArrayList<>(groups);
		sortedGroups.sort( comparingInt( PlannedOperationGroup::ordinal ));

		long nodeId = 1;
		for ( PlannedOperationGroup g : sortedGroups) {
			final GroupNode n = new GroupNode(g, nodeId++);
			nodes.add(n);

			if ( g.kind() == MutationKind.INSERT) {
				insertNodeByTable.put(normalizeTableName(g.tableExpression()), n);
			}
			else if ( g.kind() == MutationKind.UPDATE ) {
				updateNodeByTable.put(normalizeTableName(g.tableExpression()), n);
			}
			else if ( g.kind() == MutationKind.DELETE ) {
				deleteNodeByTable.put(normalizeTableName(g.tableExpression()), n);
			}
		}

		long edgeId = 1;
		for ( ForeignKey foreignKey : fkModel.foreignKeys() ) {
			if (ignoreDeferrableForOrdering && foreignKey.deferrable()) {
				// if the foreign-key is known to be deferrable in the database,
				// and we are allowed to take advantage of that, then there is no
				// need to break.
				continue;
			}

			final String childTable = normalizeTableName(foreignKey.keyTable());
			final String parentTable = normalizeTableName(foreignKey.targetTable());

		// Create INSERT edges: parent -> child (insert parent before child)
		final GroupNode parentInsert = insertNodeByTable.get(parentTable);
		final GroupNode childInsert = insertNodeByTable.get(childTable);

		if (parentInsert != null && childInsert != null) {
			// Edge direction for ordering: parent -> child
			final boolean breakable = foreignKey.nullable() &&
				(!avoidBreakingDeferrable || !foreignKey.deferrable());
			final int breakCost = computeBreakCost(foreignKey);

			final GraphEdge edge = new GraphEdge(
					// FK target
					parentInsert,
					// FK key
					childInsert,
					// FROM parent (graphing)
					parentInsert,
					// TO child (graphing)
					childInsert,
					breakable,
					breakCost,
					foreignKey.keyColumns(),
					foreignKey,
					edgeId++
			);

			outgoing.computeIfAbsent(parentInsert, k -> new ArrayList<>()).add(edge);
		}

			// Create DELETE edges: child -> parent (delete child before parent)
			final GroupNode parentDelete = deleteNodeByTable.get(parentTable);
			final GroupNode childDelete = deleteNodeByTable.get(childTable);

			if (parentDelete != null && childDelete != null) {
				// Edge direction for DELETE: child -> parent (reversed!)
				final GraphEdge edge = new GraphEdge(
						// FK target
						parentDelete,
						// FK key
						childDelete,
						// FROM child (graphing)
						childDelete,
						// TO parent (graphing)
						parentDelete,
						// NOT breakable - can't use NULL strategy for DELETE
						false,
						// No break cost (not breakable)
						0,
						// No columns to null (not breakable) - pass empty SelectableMappings
						EMPTY_SELECTABLES,
						foreignKey,
						edgeId++
				);

				outgoing.computeIfAbsent(childDelete, k -> new ArrayList<>()).add(edge);
			}

			// Create UPDATE -> DELETE edges: update child/parent FK before deleting parent
			// Case 1: Child UPDATE -> Parent DELETE (child FK changes, then orphan parent deleted)
			final GroupNode childUpdate = updateNodeByTable.get(childTable);
			if (childUpdate != null && parentDelete != null) {
				// UPDATE child must happen before DELETE parent
				// Example: UPDATE Car SET engine_id=2, then DELETE Engine WHERE id=1
				final GraphEdge edge = new GraphEdge(
						// FK target (parent)
						parentDelete,
						// FK key (child)
						childUpdate,
						// FROM child UPDATE (graphing)
						childUpdate,
						// TO parent DELETE (graphing)
						parentDelete,
						// NOT breakable
						false,
						// No break cost
						0,
						// No columns
						EMPTY_SELECTABLES,
						foreignKey,
						edgeId++
				);

				outgoing.computeIfAbsent(childUpdate, k -> new ArrayList<>()).add(edge);
			}

			// Case 2: Parent UPDATE -> Child DELETE (parent FK changes, then orphan child deleted)
			final GroupNode parentUpdate = updateNodeByTable.get(parentTable);
			if (parentUpdate != null && childDelete != null) {
				// UPDATE parent must happen before DELETE child
				final GraphEdge edge = new GraphEdge(
						// FK target (parent)
						parentUpdate,
						// FK key (child)
						childDelete,
						// FROM parent UPDATE (graphing)
						parentUpdate,
						// TO child DELETE (graphing)
						childDelete,
						// NOT breakable
						false,
						// No break cost
						0,
						// No columns
						EMPTY_SELECTABLES,
						foreignKey,
						edgeId++
				);

				outgoing.computeIfAbsent(parentUpdate, k -> new ArrayList<>()).add(edge);
			}
		}

		for (GroupNode n : nodes) {
			outgoing.computeIfAbsent(n, k -> new ArrayList<>());
		}

		for (List<GraphEdge> es : outgoing.values()) {
			es.sort(Comparator.comparingLong(x -> x.stableId));
		}

		return new Graph(nodes, outgoing);
	}

	private static int computeBreakCost(ForeignKey e) {
		// simple heuristic; tune later:
		// - deferrable edges get higher cost if we try to avoid breaking them
		// - nullable edges are break candidates (already filtered)
		int cost = 100;
		if (e.deferrable()) cost += 50;
		// If your ForeignKeyDescriptor has useful metadata, incorporate it here.
		return cost;
	}
}
