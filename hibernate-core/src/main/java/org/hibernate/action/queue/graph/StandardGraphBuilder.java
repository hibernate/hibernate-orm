/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.graph;

import org.hibernate.action.queue.MutationKind;
import org.hibernate.action.queue.fk.ForeignKey;
import org.hibernate.action.queue.plan.PlannedOperationGroup;
import org.hibernate.action.queue.fk.ForeignKeyModel;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.util.Comparator.comparingInt;
import static org.hibernate.action.queue.Helper.normalizeColumnName;
import static org.hibernate.action.queue.Helper.normalizeTableName;
import static org.hibernate.internal.util.collections.CollectionHelper.arrayList;

/**
 * @author Steve Ebersole
 */
public class StandardGraphBuilder extends AbstractGraphBuilder {
	public StandardGraphBuilder(ForeignKeyModel fkModel, boolean avoidBreakingDeferrable, boolean ignoreDeferrableForOrdering) {
		super(fkModel, avoidBreakingDeferrable, ignoreDeferrableForOrdering);
	}

	@Override
	public Graph build(List<PlannedOperationGroup> groups) {
		final ArrayList<GroupNode> nodes = arrayList(groups.size());
		final Map<String, GroupNode> insertNodeByTable = new HashMap<>();
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
		}

		long edgeId = 1;
		for ( ForeignKey foreignKey : fkModel.foreignKeys() ) {
			if (ignoreDeferrableForOrdering && foreignKey.deferrable()) {
				continue;
			}

			final String childTable = normalizeTableName(foreignKey.keyTable());
			final String parentTable = normalizeTableName(foreignKey.targetTable());

			final GroupNode parentInsert = insertNodeByTable.get(parentTable);
			final GroupNode childInsert = insertNodeByTable.get(childTable);

			if (parentInsert == null || childInsert == null) {
				continue; // only order inserts for tables involved in this flush
			}

			// Edge direction for ordering: parent -> child
			final boolean breakable = foreignKey.nullable() && (!avoidBreakingDeferrable || !foreignKey.deferrable());
			final int breakCost = computeBreakCost(foreignKey);

			final Set<String> colsToNull = new LinkedHashSet<>();
			for (String col : foreignKey.keyColumns()) {
				colsToNull.add(normalizeColumnName(col));
			}

			final GraphEdge edge = new GraphEdge(
					parentInsert,
					childInsert,
					breakable,
					breakCost,
					colsToNull,
					foreignKey.deferrable(),
					edgeId++
			);

			outgoing.computeIfAbsent(parentInsert, k -> new ArrayList<>()).add(edge);
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
