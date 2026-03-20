/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.graph;

import org.hibernate.action.queue.MutationKind;
import org.hibernate.action.queue.PlanningOptions;
import org.hibernate.action.queue.bind.EntityUpdateBindPlan;
import org.hibernate.action.queue.constraint.ForeignKey;
import org.hibernate.action.queue.op.PlannedOperation;
import org.hibernate.action.queue.plan.PlannedOperationGroup;
import org.hibernate.action.queue.constraint.ConstraintModel;
import org.hibernate.action.queue.constraint.UniqueConstraint;
import org.hibernate.action.queue.constraint.UniqueSlot;
import org.hibernate.action.queue.constraint.UniqueSlotExtractor;
import org.hibernate.metamodel.mapping.SelectableConsumer;
import org.hibernate.metamodel.mapping.SelectableMapping;
import org.hibernate.metamodel.mapping.SelectableMappings;
import org.hibernate.persister.entity.EntityPersister;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Comparator.comparingInt;
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

	public StandardGraphBuilder(
			ConstraintModel constraintModel,
			PlanningOptions planningOptions,
			org.hibernate.engine.spi.SharedSessionContractImplementor session) {
		super(constraintModel, planningOptions, session);
	}

	@Override
	public Graph build(List<PlannedOperationGroup> groups) {
		// Phase 3: Split UPDATE groups that contain swap conflicts into separate nodes
		List<PlannedOperationGroup> expandedGroups = expandSwapConflicts(groups);

		final ArrayList<GroupNode> nodes = arrayList(expandedGroups.size());
		// When tables have self-referential FKs, they can have multiple groups per kind
		// (split by ordinalBase to avoid false cycles)
		final Map<String, List<GroupNode>> insertNodeByTable = new HashMap<>();
		final Map<String, List<GroupNode>> updateNodeByTable = new HashMap<>();
		final Map<String, List<GroupNode>> deleteNodeByTable = new HashMap<>();
		final Map<GroupNode, List<GraphEdge>> outgoing = new HashMap<>();

		// stable ids based on group.ordinal (if provided), else encounter order
		// (we assume group.ordinal is stable in decomposer)
		final ArrayList<PlannedOperationGroup> sortedGroups = new ArrayList<>(expandedGroups);
		sortedGroups.sort( comparingInt( PlannedOperationGroup::ordinal ));

		long nodeId = 1;
		for ( PlannedOperationGroup g : sortedGroups) {
			final GroupNode n = new GroupNode(g, nodeId++);
			nodes.add(n);

			if ( g.kind() == MutationKind.INSERT) {
				insertNodeByTable.computeIfAbsent((g.tableExpression()), k -> new ArrayList<>()).add(n);
			}
			else if ( g.kind() == MutationKind.UPDATE ) {
				updateNodeByTable.computeIfAbsent((g.tableExpression()), k -> new ArrayList<>()).add(n);
			}
			else if ( g.kind() == MutationKind.DELETE ) {
				deleteNodeByTable.computeIfAbsent((g.tableExpression()), k -> new ArrayList<>()).add(n);
			}
		}

		long edgeId = 1;
		for ( ForeignKey foreignKey : constraintModel.foreignKeys() ) {
			if (ignoreDeferrableForOrdering() && foreignKey.deferrable()) {
				// if the foreign-key is known to be deferrable in the database,
				// and we are allowed to take advantage of that, then there is no
				// need to break.
				continue;
			}

			final String childTable = (foreignKey.keyTable());
			final String parentTable = (foreignKey.targetTable());

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
							(!avoidBreakingDeferrable() || !foreignKey.deferrable());
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

			// DISABLED: Create UPDATE -> INSERT edges: parent INSERT -> child UPDATE
			// This creates extra GraphEdge objects that contribute to allocation overhead.
			// The scenario (UPDATE setting FK to newly inserted parent) is uncommon and
			// is typically handled by the fixup pattern (INSERT with NULL FK, then UPDATE).
			// Re-enable if this edge case is needed.
			/*
			if (childUpdates != null && parentInserts != null) {
				for (GroupNode parentInsert : parentInserts) {
					for (GroupNode childUpdate : childUpdates) {
						// Edge direction: parent INSERT -> child UPDATE
						// When an UPDATE sets a foreign key, it must happen AFTER the referenced row is inserted
						// Example: INSERT Node(id=1), then UPDATE Node SET parent_id=1 WHERE id=2
						final boolean breakable;
						if (childTable.equals(parentTable)) {
							// Self-referential FK - always breakable
							breakable = true;
						}
						else {
							breakable = foreignKey.nullable() &&
								(!avoidBreakingDeferrable() || !foreignKey.deferrable());
						}
						final int breakCost = computeBreakCost(foreignKey);

						final GraphEdge edge = new GraphEdge(
								// FK target (parent)
								parentInsert,
								// FK key (child)
								childUpdate,
								// FROM parent INSERT (graphing)
								parentInsert,
								// TO child UPDATE (graphing)
								childUpdate,
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
			*/
		}

		// Create DELETE -> INSERT edges based on unique constraint slot conflicts
		// Phase 2: Runtime value tracking - creates edges only when DELETE and INSERT
		// target the same unique constraint value (not just same table)
		if (planningOptions.orderByUniqueKeySlots()) {
			edgeId = addUniqueSlotEdges(nodes, expandedGroups, deleteNodeByTable, insertNodeByTable, outgoing, edgeId);
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

	/**
	 * Phase 2: Add unique slot-based DELETE → INSERT edges.
	 * Phase 3: Enhanced to handle UPDATE operations that change unique constraint values.
	 * Extracts actual unique constraint values from operations and creates edges
	 * only when operations conflict on the same unique slot value.
	 */
	private long addUniqueSlotEdges(
			ArrayList<GroupNode> nodes,
			List<PlannedOperationGroup> groups,
			Map<String, List<GroupNode>> deleteNodeByTable,
			Map<String, List<GroupNode>> insertNodeByTable,
			Map<GroupNode, List<GraphEdge>> outgoing,
			long edgeId) {

		// Build map of slots to operations (DELETE, INSERT, and UPDATE)
		Map<UniqueSlot, List<OperationWithNode>> slotMap = new HashMap<>();

		// Extract slots from DELETE operations
		for (Map.Entry<String, List<GroupNode>> entry : deleteNodeByTable.entrySet()) {
			String tableName = entry.getKey();

			// Skip if table has no unique constraints
			if (constraintModel.getUniqueConstraintsForTable((tableName)).isEmpty()) {
				continue;
			}

			for (GroupNode deleteNode : entry.getValue()) {
				extractSlotsFromGroup(deleteNode, slotMap);
			}
		}

		// Extract slots from INSERT operations
		for (Map.Entry<String, List<GroupNode>> entry : insertNodeByTable.entrySet()) {
			String tableName = entry.getKey();

			// Skip if table has no unique constraints
			if (constraintModel.getUniqueConstraintsForTable((tableName)).isEmpty()) {
				continue;
			}

			for (GroupNode insertNode : entry.getValue()) {
				extractSlotsFromGroup(insertNode, slotMap);
			}
		}

		// Phase 3: Extract slots from UPDATE operations and track old->new changes
		// IMPORTANT: Collect ALL update slot changes FIRST before creating any edges
		List<UpdateSlotChange> allUpdateSlotChanges = new ArrayList<>();
		for (PlannedOperationGroup group : groups) {
			if (group.kind() == MutationKind.UPDATE) {
				String tableName = (group.tableExpression());
				if (!constraintModel.getUniqueConstraintsForTable(tableName).isEmpty()) {
					// Find the corresponding GroupNode
					GroupNode updateNode = findGroupNode(nodes, group);
					if (updateNode != null) {
						extractUpdateSlotChanges(updateNode, allUpdateSlotChanges, slotMap);
					}
				}
			}
		}

		// For each slot with operations, create edges if operations conflict
		for (Map.Entry<UniqueSlot, List<OperationWithNode>> entry : slotMap.entrySet()) {
			List<OperationWithNode> operations = entry.getValue();

			// Separate DELETEs, INSERTs, and UPDATEs
			List<GroupNode> deleteNodes = new ArrayList<>();
			List<GroupNode> insertNodes = new ArrayList<>();
			List<GroupNode> updateNodes = new ArrayList<>();

			for (OperationWithNode opWithNode : operations) {
				MutationKind kind = opWithNode.operation.getKind();
				if (kind == MutationKind.DELETE) {
					if (!deleteNodes.contains(opWithNode.node)) {
						deleteNodes.add(opWithNode.node);
					}
				}
				else if (kind == MutationKind.INSERT) {
					if (!insertNodes.contains(opWithNode.node)) {
						insertNodes.add(opWithNode.node);
					}
				}
				else if (kind == MutationKind.UPDATE) {
					if (!updateNodes.contains(opWithNode.node)) {
						updateNodes.add(opWithNode.node);
					}
				}
			}

			// Create edges for conflicting operations
			// Phase 3: UPDATEs that set the same unique value conflict with each other
			List<GroupNode> allOccupiers = new ArrayList<>();
			allOccupiers.addAll(insertNodes);
			allOccupiers.addAll(updateNodes);

			// DELETE/UPDATE (releasing) must happen before INSERT/UPDATE (occupying) same slot
			List<GroupNode> allReleasers = new ArrayList<>();
			allReleasers.addAll(deleteNodes);
			// UPDATEs both release and occupy - but we handle release first

			if (!allReleasers.isEmpty() && !allOccupiers.isEmpty()) {
				for (GroupNode releaser : allReleasers) {
					for (GroupNode occupier : allOccupiers) {
						// Don't create self-edges
						if (releaser == occupier) {
							continue;
						}

						final GraphEdge edge = new GraphEdge(
								// No specific FK (this is a unique-slot-level dependency)
								null,
								null,
								// FROM releaser (graphing)
								releaser,
								// TO occupier (graphing)
								occupier,
								// NOT breakable - must maintain this order to avoid unique violations
								false,
								// No break cost
								0,
								// No columns
								EMPTY_SELECTABLES,
								null,  // No FK - this is a unique constraint edge
								edgeId++
						);

						outgoing.computeIfAbsent(releaser, k -> new ArrayList<>()).add(edge);
					}
				}
			}

			// Phase 3: UPDATEs can also conflict with each other (one-to-one swaps)
			if (updateNodes.size() > 1) {
				// Create edges between UPDATEs targeting the same slot
				// This creates cycles that will be detected and ignored as false positives
				for (int i = 0; i < updateNodes.size(); i++) {
					for (int j = i + 1; j < updateNodes.size(); j++) {
						GroupNode update1 = updateNodes.get(i);
						GroupNode update2 = updateNodes.get(j);

						// Create bidirectional edges (this will create a cycle)
						final GraphEdge edge1 = new GraphEdge(
								null, null,
								update1, update2,
								false, 0,
								EMPTY_SELECTABLES,
								null,
								edgeId++
						);

						final GraphEdge edge2 = new GraphEdge(
								null, null,
								update2, update1,
								false, 0,
								EMPTY_SELECTABLES,
								null,
								edgeId++
						);

						outgoing.computeIfAbsent(update1, k -> new ArrayList<>()).add(edge1);
						outgoing.computeIfAbsent(update2, k -> new ArrayList<>()).add(edge2);
					}
				}
			}
		}

		// Phase 3: Handle UPDATE swap cycles
		// For each UPDATE that changes from oldValue to newValue,
		// create an edge from any UPDATE that currently holds newValue (and is changing it)
		for (UpdateSlotChange change1 : allUpdateSlotChanges) {
			for (UpdateSlotChange change2 : allUpdateSlotChanges) {
				if (change1 == change2) {
					continue;
				}

				// If change1 wants the value that change2 currently has (is releasing),
				// then change2 must execute before change1
				if (change1.newSlot().equals(change2.oldSlot())) {
					// Try to find the FK that corresponds to this unique constraint
					// so we can install patches if needed
					ForeignKey correspondingFk = findForeignKeyForUniqueSlot(change1.newSlot());
					SelectableMappings columnsToNull = (correspondingFk != null) ?
							correspondingFk.keyColumns() : EMPTY_SELECTABLES;

					final GraphEdge edge = new GraphEdge(
							null,  // No target node for unique constraint edges
							change1.node(),  // Key node (will receive patch if edge is broken)
							change2.node(),  // FROM: node releasing the value
							change1.node(),  // TO: node wanting the value
							false, 0,
							columnsToNull,  // Columns to NULL if this edge is broken
							null,  // Keep FK null to mark this as a unique constraint edge
							edgeId++
					);
					outgoing.computeIfAbsent(change2.node(), k -> new ArrayList<>()).add(edge);

					// DEBUG: Log swap edge creation
					try {
						String msg = String.format("SWAP edge: node%d (releasing %s) → node%d (wanting %s)\\n",
								change2.node().stableId(),
								change2.oldSlot().keyValues()[0],
								change1.node().stableId(),
								change1.newSlot().keyValues()[0]);
						java.nio.file.Files.write(
								java.nio.file.Paths.get("/tmp/swap-edges-debug.log"),
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

		return edgeId;
	}

	/**
	 * Find the GroupNode corresponding to a PlannedOperationGroup.
	 */
	private GroupNode findGroupNode(ArrayList<GroupNode> nodes, PlannedOperationGroup group) {
		for (GroupNode node : nodes) {
			if (node.group() == group) {
				return node;
			}
		}
		return null;
	}

	/**
	 * Extract unique slot changes from UPDATE operations.
	 * For UPDATEs, we need to track BOTH old and new values to detect swap cycles.
	 */
	private void extractUpdateSlotChanges(
			GroupNode node,
			List<UpdateSlotChange> updateSlotChanges,
			Map<UniqueSlot, List<OperationWithNode>> slotMap) {
		PlannedOperationGroup group = node.group();
		Map<String, EntityPersister> persisterMap = UniqueSlotExtractor.buildPersisterMap(session);
		UniqueSlotExtractor extractor = new UniqueSlotExtractor(constraintModel, session, persisterMap);

		// DEBUG
		try {
			String msg = String.format("extractUpdateSlotChanges called for table %s with %d operations\\n",
					group.tableExpression(), group.operations().size());
			java.nio.file.Files.write(
					java.nio.file.Paths.get("/tmp/swap-edges-debug.log"),
					msg.getBytes(),
					java.nio.file.StandardOpenOption.CREATE,
					java.nio.file.StandardOpenOption.APPEND
			);
		} catch (Exception e) {
			// ignore
		}

		for (PlannedOperation operation : group.operations()) {
			// Extract both old and new slots for this UPDATE
			List<UniqueSlot> oldSlots = extractOldSlotsFromUpdate(operation, persisterMap);
			List<UniqueSlot> newSlots = extractor.extractSlots(operation);

			// DEBUG
			try {
				String msg = String.format("  Operation: oldSlots=%d, newSlots=%d\\n", oldSlots.size(), newSlots.size());
				if (!oldSlots.isEmpty()) msg += String.format("    Old: %s\\n", oldSlots.get(0));
				if (!newSlots.isEmpty()) msg += String.format("    New: %s\\n", newSlots.get(0));
				java.nio.file.Files.write(
						java.nio.file.Paths.get("/tmp/swap-edges-debug.log"),
						msg.getBytes(),
						java.nio.file.StandardOpenOption.CREATE,
						java.nio.file.StandardOpenOption.APPEND
				);
			} catch (Exception e) {
				// ignore
			}

			// Track the change for swap detection
			if (!oldSlots.isEmpty() && !newSlots.isEmpty()) {
				// For simplicity, assume one unique constraint per entity
				// (can be enhanced later for multiple constraints)
				for (int i = 0; i < Math.min(oldSlots.size(), newSlots.size()); i++) {
					updateSlotChanges.add(new UpdateSlotChange(node, oldSlots.get(i), newSlots.get(i)));
				}
			}

			// Add new slots to the slotMap (for occupier tracking)
			for (UniqueSlot slot : newSlots) {
				slotMap.computeIfAbsent(slot, k -> new ArrayList<>())
						.add(new OperationWithNode(operation, node));
			}
		}
	}

	/**
	 * Extract unique slots from all operations in a group.
	 */
	private void extractSlotsFromGroup(GroupNode node, Map<UniqueSlot, List<OperationWithNode>> slotMap) {
		PlannedOperationGroup group = node.group();

		// Build persister map for value extraction
		Map<String, EntityPersister> persisterMap = UniqueSlotExtractor.buildPersisterMap(session);

		// Create slot extractor
		UniqueSlotExtractor extractor = new UniqueSlotExtractor(
				constraintModel,
				session,
				persisterMap
		);

		// Extract slots from each operation in the group
		for (PlannedOperation operation : group.operations()) {
			List<UniqueSlot> slots = extractor.extractSlots(operation);

			// Add operation to slot map for each extracted slot
			for (UniqueSlot slot : slots) {
				slotMap.computeIfAbsent(slot, k -> new ArrayList<>())
						.add(new OperationWithNode(operation, node));
			}
		}
	}

	/**
	 * Helper record to track operation with its node.
	 */
	private record OperationWithNode(PlannedOperation operation, GroupNode node) {}

	/**
	 * Helper record to track UPDATE operations with their old and new unique slot values.
	 */
	private record UpdateSlotChange(GroupNode node, UniqueSlot oldSlot, UniqueSlot newSlot) {}

	/**
	 * Phase 3: Expand UPDATE groups that contain swap conflicts into separate groups.
	 * When multiple UPDATEs in the same group swap unique constraint values, they need
	 * to be in separate graph nodes to create a detectable cycle.
	 */
	private List<PlannedOperationGroup> expandSwapConflicts(List<PlannedOperationGroup> groups) {
		List<PlannedOperationGroup> result = new ArrayList<>();

		// DEBUG
		try {
			String msg = String.format("expandSwapConflicts: input %d groups\\n", groups.size());
			java.nio.file.Files.write(
					java.nio.file.Paths.get("/tmp/expand-debug.log"),
					msg.getBytes(),
					java.nio.file.StandardOpenOption.CREATE,
					java.nio.file.StandardOpenOption.APPEND
			);
		} catch (Exception e) {
			// ignore
		}

		for (PlannedOperationGroup group : groups) {
			if (group.kind() != MutationKind.UPDATE || group.operations().size() <= 1) {
				// Not an UPDATE or only one operation - no need to split
				result.add(group);
				continue;
			}

			// Check if this UPDATE group has swap conflicts
			String tableName = (group.tableExpression());
			List<UniqueConstraint> constraints = constraintModel.getUniqueConstraintsForTable(tableName);

			if (constraints.isEmpty()) {
				// No unique constraints - no swaps possible
				result.add(group);
				continue;
			}

			// Extract slots for each operation
			Map<String, EntityPersister> persisterMap = UniqueSlotExtractor.buildPersisterMap(session);
			UniqueSlotExtractor extractor = new UniqueSlotExtractor(constraintModel, session, persisterMap);

			Map<PlannedOperation, UniqueSlot> newSlots = new HashMap<>();
			Map<PlannedOperation, UniqueSlot> oldSlots = new HashMap<>();

			for (PlannedOperation operation : group.operations()) {
				List<UniqueSlot> oldSlotList = extractOldSlotsFromUpdate(operation, persisterMap);
				List<UniqueSlot> newSlotList = extractor.extractSlots(operation);

				if (!oldSlotList.isEmpty() && !newSlotList.isEmpty()) {
					oldSlots.put(operation, oldSlotList.get(0));
					newSlots.put(operation, newSlotList.get(0));
				}
			}

			// Check for swap conflicts
			boolean hasSwap = false;
			for (PlannedOperation op1 : group.operations()) {
				for (PlannedOperation op2 : group.operations()) {
					if (op1 == op2) continue;

					UniqueSlot new1 = newSlots.get(op1);
					UniqueSlot old2 = oldSlots.get(op2);

					if (new1 != null && old2 != null && new1.equals(old2)) {
						hasSwap = true;
						break;
					}
				}
				if (hasSwap) break;
			}

			if (!hasSwap) {
				// No swap conflicts - keep as single group
				result.add(group);
			} else {
				// DEBUG
				try {
					String msg = String.format("  Group %s has swap - splitting %d operations\\n",
							group.tableExpression(), group.operations().size());
					java.nio.file.Files.write(
							java.nio.file.Paths.get("/tmp/expand-debug.log"),
							msg.getBytes(),
							java.nio.file.StandardOpenOption.CREATE,
							java.nio.file.StandardOpenOption.APPEND
					);
				} catch (Exception e) {
					// ignore
				}

				// Has swap conflicts - split into individual operation groups
				int ordinalOffset = 0;
				for (PlannedOperation operation : group.operations()) {
					PlannedOperationGroup singleOpGroup = new PlannedOperationGroup(
							group.tableExpression(),
							group.kind(),
							group.shapeKey(),
							List.of(operation),
							group.needsIdPrePhase(),
							group.ordinal() * 1000 + ordinalOffset++,  // Ensure unique ordinals
							group.origin()
					);
					result.add(singleOpGroup);
				}
			}
		}

		// DEBUG
		try {
			String msg = String.format("expandSwapConflicts: output %d groups\\n", result.size());
			java.nio.file.Files.write(
					java.nio.file.Paths.get("/tmp/expand-debug.log"),
					msg.getBytes(),
					java.nio.file.StandardOpenOption.CREATE,
					java.nio.file.StandardOpenOption.APPEND
			);
		} catch (Exception e) {
			// ignore
		}

		return result;
	}

	/**
	 * Extract old unique slot values from an UPDATE operation.
	 */
	private List<UniqueSlot> extractOldSlotsFromUpdate(
			PlannedOperation operation,
			Map<String, EntityPersister> persisterMap) {
		List<UniqueSlot> slots = new ArrayList<>();

		if (operation.getKind() != MutationKind.UPDATE) {
			return slots;
		}

		String tableName = operation.getTableExpression();
		List<UniqueConstraint> constraints = constraintModel.getUniqueConstraintsForTable(tableName);

		if (constraints.isEmpty()) {
			return slots;
		}

		var bindPlan = operation.getBindPlan();
		if (!(bindPlan instanceof EntityUpdateBindPlan updateBindPlan)) {
			return slots;
		}

		EntityPersister persister = persisterMap.get(tableName);
		if (persister == null) {
			return slots;
		}

		// Extract old values from previousState
		for (UniqueConstraint constraint : constraints) {
			Object[] oldValues = extractValuesFromState(persister, updateBindPlan.getPreviousState(), constraint);
			if (oldValues != null) {
				slots.add(new UniqueSlot(tableName, constraint.constraintName(), oldValues));
			}
		}

		return slots;
	}

	/**
	 * Extract values from entity state array for a unique constraint.
	 */
	private Object[] extractValuesFromState(
			EntityPersister persister,
			Object[] state,
			UniqueConstraint constraint) {
		if (state == null) {
			return null;
		}

		try {
			// Handle primary key
			if (constraint.isPrimaryKey()) {
				// PKs don't change in UPDATEs, skip
				return null;
			}

			// Handle other unique constraints using property names
			if (constraint.propertyNames() != null && constraint.propertyNames().length > 0) {
				Object[] values = new Object[constraint.propertyNames().length];
				for (int i = 0; i < constraint.propertyNames().length; i++) {
					String propertyName = constraint.propertyNames()[i];
					int propertyIndex = persister.getEntityMetamodel().getPropertyIndex(propertyName);
					if (propertyIndex >= 0 && propertyIndex < state.length) {
						Object value = state[propertyIndex];

						// If the value is an entity (association), extract its ID
						if (value != null && persister.getEntityMetamodel().getPropertyTypes()[propertyIndex].isEntityType()) {
							var entityType = (org.hibernate.type.EntityType) persister.getEntityMetamodel().getPropertyTypes()[propertyIndex];
							var associatedPersister = session.getFactory().getMappingMetamodel().getEntityDescriptor(entityType.getAssociatedEntityName());
							if (associatedPersister != null) {
								value = associatedPersister.getIdentifier(value, session);
							}
						}

						values[i] = value;
					} else {
						return null;
					}
				}
				return values;
			}

			return null;
		}
		catch (Exception e) {
			return null;
		}
	}

	/**
	 * Find a foreign key that uses the same columns as the given unique constraint.
	 * This is needed to install patches for UPDATE swap cycles.
	 */
	private ForeignKey findForeignKeyForUniqueSlot(UniqueSlot slot) {
		String tableName = (slot.tableName());

		// Get the unique constraint for this slot
		List<UniqueConstraint> constraints = constraintModel.getUniqueConstraintsForTable(tableName);
		UniqueConstraint matchingConstraint = null;
		for (UniqueConstraint constraint : constraints) {
			if (constraint.constraintName().equals(slot.constraintName())) {
				matchingConstraint = constraint;
				break;
			}
		}

		if (matchingConstraint == null || matchingConstraint.propertyNames() == null) {
			return null;
		}

		// Find an FK that uses the same columns
		// For now, return the first FK on this table (works for simple cases like dept_id)
		for (ForeignKey fk : constraintModel.foreignKeys()) {
			String fkKeyTable = (fk.keyTable());
			if (fkKeyTable.equals(tableName)) {
				return fk;
			}
		}

		return null;
	}
}
