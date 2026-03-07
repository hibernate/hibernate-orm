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
		// When tables have self-referential FKs, they can have multiple groups per kind
		// (split by ordinalBase to avoid false cycles)
		final Map<String, List<GroupNode>> insertNodeByTable = new HashMap<>();
		final Map<String, List<GroupNode>> updateNodeByTable = new HashMap<>();
		final Map<String, List<GroupNode>> deleteNodeByTable = new HashMap<>();
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
				insertNodeByTable.computeIfAbsent(normalizeTableName(g.tableExpression()), k -> new ArrayList<>()).add(n);
			}
			else if ( g.kind() == MutationKind.UPDATE ) {
				updateNodeByTable.computeIfAbsent(normalizeTableName(g.tableExpression()), k -> new ArrayList<>()).add(n);
			}
			else if ( g.kind() == MutationKind.DELETE ) {
				deleteNodeByTable.computeIfAbsent(normalizeTableName(g.tableExpression()), k -> new ArrayList<>()).add(n);
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
		// When a table has multiple groups (self-referential FK), create edges for all combinations
		final List<GroupNode> parentInserts = insertNodeByTable.get(parentTable);
		final List<GroupNode> childInserts = insertNodeByTable.get(childTable);

		if (parentInserts != null && childInserts != null) {
			for (GroupNode parentInsert : parentInserts) {
				for (GroupNode childInsert : childInserts) {
					// Edge direction for ordering: parent -> child
					// Self-referencing FKs must be breakable (otherwise first INSERT is impossible)
					final boolean breakable;
					if (childTable.equals(parentTable)) {
						// Self-referential FK - always breakable
						breakable = true;
					}
					else {
						breakable = foreignKey.nullable() &&
							(!avoidBreakingDeferrable || !foreignKey.deferrable());
					}
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
			}
		}

			// Create DELETE edges: child -> parent (delete child before parent)
			// When a table has multiple groups (self-referential FK), create edges for all combinations
			final List<GroupNode> parentDeletes = deleteNodeByTable.get(parentTable);
			final List<GroupNode> childDeletes = deleteNodeByTable.get(childTable);

			if (parentDeletes != null && childDeletes != null) {
				for (GroupNode parentDelete : parentDeletes) {
					for (GroupNode childDelete : childDeletes) {
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

						// DEBUG: Log DELETE edges
						try {
							String msg = String.format("DELETE edge: %s → %s (FK: %s.%s → %s)\n",
								childTable, parentTable,
								foreignKey.keyTable(), foreignKey.keyColumns(),
								foreignKey.targetTable());
							java.nio.file.Files.write(
								java.nio.file.Paths.get("/tmp/graph-edges-debug.log"),
								msg.getBytes(),
								java.nio.file.StandardOpenOption.CREATE,
								java.nio.file.StandardOpenOption.APPEND
							);
						} catch (Exception e) {
							// ignore
						}
					}
				}
			}

			// Create UPDATE -> DELETE edges: update child/parent FK before deleting parent
			// Case 1: Child UPDATE -> Parent DELETE (child FK changes, then orphan parent deleted)
			final List<GroupNode> childUpdates = updateNodeByTable.get(childTable);
			if (childUpdates != null && parentDeletes != null) {
				for (GroupNode childUpdate : childUpdates) {
					for (GroupNode parentDelete : parentDeletes) {
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
				}
			}

			// Case 2: Parent UPDATE -> Child DELETE (parent FK changes, then orphan child deleted)
			final List<GroupNode> parentUpdates = updateNodeByTable.get(parentTable);
			if (parentUpdates != null && childDeletes != null) {
				for (GroupNode parentUpdate : parentUpdates) {
					for (GroupNode childDelete : childDeletes) {
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
			}
		}

		// Create DELETE -> INSERT edges for same table
		// This ensures DELETE operations complete before INSERT operations to the same table,
		// avoiding unique constraint violations when replacing entities (e.g., orphan removal + insert)
		for (String tableName : deleteNodeByTable.keySet()) {
			final List<GroupNode> tableDeletes = deleteNodeByTable.get(tableName);
			final List<GroupNode> tableInserts = insertNodeByTable.get(tableName);

			if (tableInserts != null && tableDeletes != null) {
				for (GroupNode tableDelete : tableDeletes) {
					for (GroupNode tableInsert : tableInserts) {
						// DELETE must happen before INSERT to same table
						final GraphEdge edge = new GraphEdge(
								// No specific FK (this is a table-level dependency)
								null,
								null,
								// FROM delete (graphing)
								tableDelete,
								// TO insert (graphing)
								tableInsert,
								// NOT breakable - must maintain this order
								false,
								// No break cost
								0,
								// No columns
								EMPTY_SELECTABLES,
								null,
								edgeId++
						);

						outgoing.computeIfAbsent(tableDelete, k -> new ArrayList<>()).add(edge);
					}
				}
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
