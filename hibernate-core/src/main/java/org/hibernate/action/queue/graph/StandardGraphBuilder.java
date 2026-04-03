/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.graph;

import org.hibernate.action.queue.MutationKind;
import org.hibernate.action.queue.PlanningOptions;
import org.hibernate.action.queue.bind.EntityUpdateBindPlan;
import org.hibernate.action.queue.constraint.ForeignKey;
import org.hibernate.action.queue.plan.PlannedOperation;
import org.hibernate.action.queue.plan.PlannedOperationGroup;
import org.hibernate.action.queue.constraint.ConstraintModel;
import org.hibernate.action.queue.constraint.UniqueConstraint;
import org.hibernate.action.queue.constraint.UniqueSlot;
import org.hibernate.action.queue.constraint.UniqueSlotExtractor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.mapping.SelectableConsumer;
import org.hibernate.metamodel.mapping.SelectableMapping;
import org.hibernate.metamodel.mapping.SelectableMappings;
import org.hibernate.persister.entity.EntityPersister;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.util.Comparator.comparingInt;
import static org.hibernate.internal.util.collections.CollectionHelper.arrayList;

/**
 * @author Steve Ebersole
 */
public class StandardGraphBuilder implements GraphBuilder {
	// Empty SelectableMappings for non-breakable DELETE edges
	private static final SelectableMappings EMPTY_SELECTABLES = new SelectableMappings() {
		@Override
		public int getJdbcTypeCount() {
			return 0;
		}

		@Override
		public SelectableMapping getSelectable(int columnIndex) {
			throw new IndexOutOfBoundsException( "No selectables in empty instance" );
		}

		@Override
		public int forEachSelectable(int offset, SelectableConsumer consumer) {
			return 0;
		}
	};

	private final ConstraintModel constraintModel;
	private final PlanningOptions planningOptions;
	private final SharedSessionContractImplementor session;
	private final Map<String, EntityPersister> entityPersistersByTable;

	public StandardGraphBuilder(
			ConstraintModel constraintModel,
			PlanningOptions planningOptions,
			SharedSessionContractImplementor session) {
		this.constraintModel = constraintModel;
		this.planningOptions = planningOptions;
		this.session = session;

		// this map is only used with unique-key cycle detection
		entityPersistersByTable = planningOptions.orderByUniqueKeySlots()
				? UniqueSlotExtractor.buildPersisterMap( session )
				: Map.of();
	}

	// Convenience accessors for planning options
	private boolean avoidBreakingDeferrable() {
		return planningOptions.avoidBreakingDeferrable();
	}

	private boolean ignoreDeferrableForOrdering() {
		return planningOptions.ignoreDeferrableForOrdering();
	}

	@Override
	public Graph build(List<PlannedOperationGroup> groups) {
		// Split UPDATE groups that contain swap conflicts into separate nodes
		// Only do this if unique slot ordering is enabled
		List<PlannedOperationGroup> expandedGroups = planningOptions.orderByUniqueKeySlots()
				? expandSwapConflicts( groups )
				: groups;

		final ArrayList<GroupNode> nodes = arrayList( expandedGroups.size() );
		// When tables have self-referential FKs, they can have multiple groups per kind
		// (split by ordinalBase to avoid false cycles)
		final Map<String, List<GroupNode>> insertNodeByTable = new HashMap<>();
		final Map<String, List<GroupNode>> updateNodeByTable = new HashMap<>();
		final Map<String, List<GroupNode>> deleteNodeByTable = new HashMap<>();
		final Map<GroupNode, List<GraphEdge>> outgoing = new HashMap<>();

		// stable ids based on group.ordinal (if provided), else encounter order
		// (we assume group.ordinal is stable in decomposer)
		final ArrayList<PlannedOperationGroup> sortedGroups = new ArrayList<>( expandedGroups );
		sortedGroups.sort( comparingInt( PlannedOperationGroup::ordinal ) );

		long nodeId = 1;
		for ( PlannedOperationGroup g : sortedGroups ) {
			final GroupNode n = new GroupNode( g, nodeId++ );
			nodes.add( n );

			if ( g.kind() == MutationKind.INSERT ) {
				insertNodeByTable.computeIfAbsent( (g.tableExpression()), k -> new ArrayList<>() ).add( n );
			}
			else if ( g.kind() == MutationKind.UPDATE || g.kind() == MutationKind.UPDATE_ORDER ) {
				updateNodeByTable.computeIfAbsent( (g.tableExpression()), k -> new ArrayList<>() ).add( n );
			}
			else if ( g.kind() == MutationKind.DELETE ) {
				deleteNodeByTable.computeIfAbsent( (g.tableExpression()), k -> new ArrayList<>() ).add( n );
			}
		}

		// Pre-initialize edge lists for all nodes to avoid computeIfAbsent overhead
		for ( GroupNode n : nodes ) {
			outgoing.put( n, new ArrayList<>() );
		}

		long edgeId = 1;
		for ( ForeignKey foreignKey : constraintModel.foreignKeys() ) {
			if ( ignoreDeferrableForOrdering() && foreignKey.deferrable() ) {
				// if the foreign-key is known to be deferrable in the database,
				// and we are allowed to take advantage of that, then there is no
				// need to break.
				continue;
			}

			final String childTable = (foreignKey.keyTable());
			final String parentTable = (foreignKey.targetTable());

			// Hoist all map lookups to avoid redundant lookups in multiple sections below
			final List<GroupNode> parentInserts = insertNodeByTable.get( parentTable );
			final List<GroupNode> childInserts = insertNodeByTable.get( childTable );
			final List<GroupNode> parentDeletes = deleteNodeByTable.get( parentTable );
			final List<GroupNode> childDeletes = deleteNodeByTable.get( childTable );
			final List<GroupNode> parentUpdates = updateNodeByTable.get( parentTable );
			final List<GroupNode> childUpdates = updateNodeByTable.get( childTable );

			// Early exit if this FK doesn't involve any operations in this flush
			if ( parentInserts == null && childInserts == null
					&& parentDeletes == null && childDeletes == null
					&& parentUpdates == null && childUpdates == null ) {
				continue;
			}

			// Create all FK-based edges using helper methods
			edgeId = createInsertToInsertEdges( foreignKey, childTable, parentTable, parentInserts, childInserts, outgoing, edgeId );
			edgeId = createDeleteToDeleteEdges( foreignKey, parentDeletes, childDeletes, outgoing, edgeId );
			edgeId = createInsertToUpdateEdges( foreignKey, parentInserts, childUpdates, outgoing, edgeId );
			edgeId = createUpdateToDeleteEdges( foreignKey, childUpdates, parentDeletes, outgoing, edgeId );
			edgeId = createUpdateToDeleteEdges( foreignKey, parentUpdates, childDeletes, outgoing, edgeId );
		}

		// Create table-level DELETE -> INSERT edges to prevent unique constraint violations
		// For tables with both DELETE and INSERT operations, ensure DELETEs execute first
		// to avoid conflicts (e.g., moving an element between collections on a join table with unique constraints)
		edgeId = addTableLevelDeleteInsertEdges( deleteNodeByTable, insertNodeByTable, outgoing, edgeId );

		// Create DELETE -> INSERT edges based on unique constraint slot conflicts
		// Phase 2: Runtime value tracking - creates edges only when DELETE and INSERT
		// target the same unique constraint value (not just same table)
		if ( planningOptions.orderByUniqueKeySlots() ) {
			edgeId = addUniqueSlotEdges( nodes, expandedGroups, deleteNodeByTable, insertNodeByTable, outgoing,	edgeId );
		}

		for ( List<GraphEdge> es : outgoing.values() ) {
			es.sort( Comparator.comparingLong( x -> x.stableId ) );
		}

		return new Graph( nodes, outgoing );
	}

	/**
	 * Create INSERT -> INSERT edges: parent -> child (insert parent before child).
	 * When a table has multiple groups (self-referential FK), create edges for all combinations.
	 */
	private long createInsertToInsertEdges(
			ForeignKey foreignKey,
			String childTable,
			String parentTable,
			List<GroupNode> parentInserts,
			List<GroupNode> childInserts,
			Map<GroupNode, List<GraphEdge>> outgoing,
			long edgeId) {
		if ( parentInserts == null || childInserts == null ) {
			return edgeId;
		}

		for ( GroupNode parentInsert : parentInserts ) {
			for ( GroupNode childInsert : childInserts ) {
				// Edge direction for ordering: parent -> child
				// Self-referencing FKs must be breakable (otherwise first INSERT is impossible)
				final boolean breakable;
				if ( childTable.equals( parentTable ) ) {
					// Self-referential FK - always breakable
					breakable = true;
				}
				else {
					breakable = foreignKey.nullable()
								&& (!avoidBreakingDeferrable() || !foreignKey.deferrable());
				}
				final int breakCost = computeBreakCost( foreignKey );

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

				outgoing.get( parentInsert ).add( edge );
			}
		}

		return edgeId;
	}

	/**
	 * Create DELETE -> DELETE edges: child -> parent (delete child before parent).
	 * When a table has multiple groups (self-referential FK), create edges for all combinations.
	 */
	private long createDeleteToDeleteEdges(
			ForeignKey foreignKey,
			List<GroupNode> parentDeletes,
			List<GroupNode> childDeletes,
			Map<GroupNode, List<GraphEdge>> outgoing,
			long edgeId) {
		if ( parentDeletes == null || childDeletes == null ) {
			return edgeId;
		}

		for ( GroupNode parentDelete : parentDeletes ) {
			for ( GroupNode childDelete : childDeletes ) {
				// DELETE edges can be breakable if FK is nullable
				// This allows handling circular FK dependencies by nullifying one FK first
				final boolean breakable = foreignKey.nullable();
				final int breakCost = breakable ? computeBreakCost( foreignKey ) : 0;
				final SelectableMappings columnsToNull = breakable ? foreignKey.keyColumns() : EMPTY_SELECTABLES;

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
						// Breakable if FK is nullable - allows UPDATE to null FK before DELETE
						breakable,
						// Break cost
						breakCost,
						// Columns to null if edge is broken
						columnsToNull,
						foreignKey,
						edgeId++
				);

				outgoing.get( childDelete ).add( edge );
			}
		}

		return edgeId;
	}

	/**
	 * Create INSERT -> UPDATE edges: insert parent before updating child FK.
	 * This is critical for one-to-many collections where collection operations UPDATE child FK.
	 * SKIP UPDATE_ORDER groups as they only update order columns, not FKs.
	 */
	private long createInsertToUpdateEdges(
			ForeignKey foreignKey,
			List<GroupNode> parentInserts,
			List<GroupNode> childUpdates,
			Map<GroupNode, List<GraphEdge>> outgoing,
			long edgeId) {
		if ( parentInserts == null || childUpdates == null ) {
			return edgeId;
		}

		for ( GroupNode parentInsert : parentInserts ) {
			for ( GroupNode childUpdate : childUpdates ) {
				// Skip order-only updates - they don't set FK values
				if ( childUpdate.group().kind() == MutationKind.UPDATE_ORDER ) {
					continue;
				}

				// INSERT parent must happen before UPDATE child
				// Example: INSERT Product, then UPDATE Category SET product_id=1
				final GraphEdge edge = new GraphEdge(
						// FK target (parent)
						parentInsert,
						// FK key (child)
						childUpdate,
						// FROM parent INSERT (graphing)
						parentInsert,
						// TO child UPDATE (graphing)
						childUpdate,
						// NOT breakable
						false,
						// No break cost
						0,
						// No columns
						EMPTY_SELECTABLES,
						foreignKey,
						edgeId++
				);

				outgoing.get( parentInsert ).add( edge );
			}
		}

		return edgeId;
	}

	/**
	 * Create UPDATE -> DELETE edges: update child/parent FK before deleting parent.
	 * Handles both Child UPDATE -> Parent DELETE and Parent UPDATE -> Child DELETE cases.
	 * SKIP UPDATE_ORDER groups as they only update order columns, not FKs.
	 */
	private long createUpdateToDeleteEdges(
			ForeignKey foreignKey,
			List<GroupNode> updates,
			List<GroupNode> deletes,
			Map<GroupNode, List<GraphEdge>> outgoing,
			long edgeId) {
		if ( updates == null || deletes == null ) {
			return edgeId;
		}

		for ( GroupNode update : updates ) {
			// Skip order-only updates - they don't change FK values
			if ( update.group().kind() == MutationKind.UPDATE_ORDER ) {
				continue;
			}

			for ( GroupNode delete : deletes ) {
				// UPDATE must happen before DELETE
				// Example: UPDATE Car SET engine_id=2, then DELETE Engine WHERE id=1
				final GraphEdge edge = new GraphEdge(
						// FK target (deleted node)
						delete,
						// FK key (updated node)
						update,
						// FROM update (graphing)
						update,
						// TO delete (graphing)
						delete,
						// NOT breakable
						false,
						// No break cost
						0,
						// No columns
						EMPTY_SELECTABLES,
						foreignKey,
						edgeId++
				);

				outgoing.get( update ).add( edge );
			}
		}

		return edgeId;
	}

	private static int computeBreakCost(ForeignKey e) {
		// simple heuristic; tune later:
		// - deferrable edges get higher cost if we try to avoid breaking them
		// - nullable edges are break candidates (already filtered)
		int cost = 100;
		if ( e.deferrable() ) {
			cost += 50;
		}
		// If your ForeignKeyDescriptor has useful metadata, incorporate it here.
		return cost;
	}

	/**
	 * Add table-level DELETE → INSERT edges to prevent unique constraint violations.
	 * For any table that has both DELETE and INSERT operations, create edges from all
	 * DELETE nodes to all INSERT nodes to ensure DELETEs execute first.
	 * <p>
	 * This is critical for join tables with unique constraints (e.g., unidirectional
	 * @OneToMany) where moving an element between collections requires DELETE-then-INSERT
	 * ordering to avoid constraint violations.
	 */
	private long addTableLevelDeleteInsertEdges(
			Map<String, List<GroupNode>> deleteNodeByTable,
			Map<String, List<GroupNode>> insertNodeByTable,
			Map<GroupNode, List<GraphEdge>> outgoing,
			long edgeId) {

		// Iterate through all tables that have DELETE operations
		for ( Map.Entry<String, List<GroupNode>> entry : deleteNodeByTable.entrySet() ) {
			String tableName = entry.getKey();
			List<GroupNode> deleteNodes = entry.getValue();
			List<GroupNode> insertNodes = insertNodeByTable.get( tableName );

			// Only create edges if this table also has INSERT operations
			if ( insertNodes != null && !insertNodes.isEmpty() ) {
				// Create DELETE -> INSERT edge for each combination
				for ( GroupNode deleteNode : deleteNodes ) {
					for ( GroupNode insertNode : insertNodes ) {
						final GraphEdge edge = new GraphEdge(
								// No FK target/key (this is table-level ordering)
								null,
								null,
								// FROM delete (graphing)
								deleteNode,
								// TO insert (graphing)
								insertNode,
								// NOT breakable - must maintain this order to avoid unique violations
								false,
								// No break cost (not breakable)
								0,
								// No columns to null (not breakable)
								EMPTY_SELECTABLES,
								// No FK (this is table-level ordering)
								null,
								edgeId++
						);

						outgoing.get( deleteNode ).add( edge );
					}
				}
			}
		}

		return edgeId;
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
		for ( Map.Entry<String, List<GroupNode>> entry : deleteNodeByTable.entrySet() ) {
			String tableName = entry.getKey();

			// Skip if table has no unique constraints
			if ( constraintModel.getUniqueConstraintsForTable( (tableName) ).isEmpty() ) {
				continue;
			}

			for ( GroupNode deleteNode : entry.getValue() ) {
				extractSlotsFromGroup( deleteNode, slotMap );
			}
		}

		// Extract slots from INSERT operations
		for ( Map.Entry<String, List<GroupNode>> entry : insertNodeByTable.entrySet() ) {
			String tableName = entry.getKey();

			// Skip if table has no unique constraints
			if ( constraintModel.getUniqueConstraintsForTable( (tableName) ).isEmpty() ) {
				continue;
			}

			for ( GroupNode insertNode : entry.getValue() ) {
				extractSlotsFromGroup( insertNode, slotMap );
			}
		}

		// Phase 3: Extract slots from UPDATE operations and track old->new changes
		// IMPORTANT: Collect ALL update slot changes FIRST before creating any edges
		List<UpdateSlotChange> allUpdateSlotChanges = new ArrayList<>();
		for ( PlannedOperationGroup group : groups ) {
			if ( group.kind() == MutationKind.UPDATE ) {
				String tableName = (group.tableExpression());
				if ( !constraintModel.getUniqueConstraintsForTable( tableName ).isEmpty() ) {
					// Find the corresponding GroupNode
					GroupNode updateNode = findGroupNode( nodes, group );
					if ( updateNode != null ) {
						extractUpdateSlotChanges( updateNode, allUpdateSlotChanges, slotMap );
					}
				}
			}
		}

		// For each slot with operations, create edges if operations conflict
		for ( Map.Entry<UniqueSlot, List<OperationWithNode>> entry : slotMap.entrySet() ) {
			List<OperationWithNode> operations = entry.getValue();

			// Separate DELETEs, INSERTs, and UPDATEs
			List<GroupNode> deleteNodes = new ArrayList<>();
			List<GroupNode> insertNodes = new ArrayList<>();
			List<GroupNode> updateNodes = new ArrayList<>();

			for ( OperationWithNode opWithNode : operations ) {
				MutationKind kind = opWithNode.operation.getKind();
				if ( kind == MutationKind.DELETE ) {
					if ( !deleteNodes.contains( opWithNode.node ) ) {
						deleteNodes.add( opWithNode.node );
					}
				}
				else if ( kind == MutationKind.INSERT ) {
					if ( !insertNodes.contains( opWithNode.node ) ) {
						insertNodes.add( opWithNode.node );
					}
				}
				else if ( kind == MutationKind.UPDATE ) {
					if ( !updateNodes.contains( opWithNode.node ) ) {
						updateNodes.add( opWithNode.node );
					}
				}
			}

			// Create edges for conflicting operations
			// Phase 3: UPDATEs that set the same unique value conflict with each other
			List<GroupNode> allOccupiers = new ArrayList<>();
			allOccupiers.addAll( insertNodes );
			allOccupiers.addAll( updateNodes );

			// DELETE/UPDATE (releasing) must happen before INSERT/UPDATE (occupying) same slot
			List<GroupNode> allReleasers = new ArrayList<>();
			allReleasers.addAll( deleteNodes );
			// UPDATEs both release and occupy - but we handle release first

			if ( !allReleasers.isEmpty() && !allOccupiers.isEmpty() ) {
				// Get unique constraint columns from the slot (needed for patch installation if cycles occur)
				UniqueSlot slot = entry.getKey();
				UniqueConstraint constraint = slot.constraint();
				SelectableMappings columnsToNull = (constraint != null) ?
						constraint.columns() : EMPTY_SELECTABLES;

				for ( GroupNode releaser : allReleasers ) {
					for ( GroupNode occupier : allOccupiers ) {
						// Don't create self-edges
						if ( releaser == occupier ) {
							continue;
						}

						final GraphEdge edge = new GraphEdge(
								// No specific FK (this is a unique-slot-level dependency)
								null,
								occupier,  // Key node if this edge needs breaking
								// FROM releaser (graphing)
								releaser,
								// TO occupier (graphing)
								occupier,
								// NOT breakable - must maintain this order to avoid unique violations
								false,
								// No break cost
								0,
								// Columns to NULL if edge needs breaking
								columnsToNull,
								null,  // No FK - this is a unique constraint edge
								edgeId++
						);

						outgoing.get( releaser ).add( edge );
					}
				}
			}

			// Phase 3: UPDATEs can also conflict with each other (one-to-one swaps)
			if ( updateNodes.size() > 1 ) {
				// Create edges between UPDATEs targeting the same slot
				// These edges may form cycles that need patch installation

				// Get unique constraint columns from the slot (needed for patch installation)
				UniqueSlot slot = entry.getKey();
				UniqueConstraint constraint = slot.constraint();
				SelectableMappings columnsToNull = (constraint != null) ?
						constraint.columns() : EMPTY_SELECTABLES;

				for ( int i = 0; i < updateNodes.size(); i++ ) {
					for ( int j = i + 1; j < updateNodes.size(); j++ ) {
						GroupNode update1 = updateNodes.get( i );
						GroupNode update2 = updateNodes.get( j );

						// UPDATE-to-UPDATE edges are breakable if we have nullable columns
						// This allows NULL-then-patch strategy for unique constraint swaps
						boolean edgeBreakable = constraint != null && constraint.nullable();

						// Create bidirectional edges (this will create a cycle)
						final GraphEdge edge1 = new GraphEdge(
								null,
								update1,  // Key node (will receive patch if edge is broken)
								update1, update2,
								edgeBreakable,
								edgeBreakable ? 100 : 0,
								columnsToNull,  // Columns to NULL for cycle breaking
								null,
								edgeId++
						);

						final GraphEdge edge2 = new GraphEdge(
								null,
								update2,  // Key node (will receive patch if edge is broken)
								update2, update1,
								edgeBreakable,
								edgeBreakable ? 100 : 0,
								columnsToNull,  // Columns to NULL for cycle breaking
								null,
								edgeId++
						);

						outgoing.get( update1 ).add( edge1 );
						outgoing.get( update2 ).add( edge2 );
					}
				}
			}
		}

		// Phase 3: Handle UPDATE swap cycles
		// For each UPDATE that changes from oldValue to newValue,
		// create an edge from any UPDATE that currently holds newValue (and is changing it)
		// Use HashMap to avoid O(n²) comparison - index by oldSlot for O(1) lookup
		Map<UniqueSlot, List<UpdateSlotChange>> changesByOldSlot = new HashMap<>();
		for ( UpdateSlotChange change : allUpdateSlotChanges ) {
			changesByOldSlot.computeIfAbsent( change.oldSlot(), k -> new ArrayList<>() ).add( change );
		}

		for ( UpdateSlotChange change1 : allUpdateSlotChanges ) {
			// Find all changes that are releasing the value that change1 wants
			List<UpdateSlotChange> conflictingChanges = changesByOldSlot.get( change1.newSlot() );
			if ( conflictingChanges != null ) {
				for ( UpdateSlotChange change2 : conflictingChanges ) {
					if ( change1 == change2 ) {
						continue;
					}

					// change2 currently holds the value that change1 wants,
					// so change2 must execute before change1
					UniqueConstraint constraint = change1.newSlot().constraint();
					SelectableMappings columnsToNull = (constraint != null) ?
							constraint.columns() : EMPTY_SELECTABLES;

					// UPDATE swap edges are breakable if we have nullable columns
					boolean edgeBreakable = constraint != null && constraint.nullable();

					final GraphEdge edge = new GraphEdge(
							null,  // No target node for unique constraint edges
							change1.node(),  // Key node (will receive patch if edge is broken)
							change2.node(),  // FROM: node releasing the value
							change1.node(),  // TO: node wanting the value
							edgeBreakable,
							edgeBreakable ? 100 : 0,
							columnsToNull,  // Columns to NULL if this edge is broken
							null,  // Keep FK null to mark this as a unique constraint edge
							edgeId++
					);
					outgoing.get( change2.node() ).add( edge );
				}
			}
		}

		return edgeId;
	}

	/**
	 * Find the GroupNode corresponding to a PlannedOperationGroup.
	 */
	private GroupNode findGroupNode(ArrayList<GroupNode> nodes, PlannedOperationGroup group) {
		for ( GroupNode node : nodes ) {
			if ( node.group() == group ) {
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
		UniqueSlotExtractor extractor = new UniqueSlotExtractor( constraintModel, session, entityPersistersByTable );

		for ( PlannedOperation operation : group.operations() ) {
			// Extract both old and new slots for this UPDATE
			List<UniqueSlot> oldSlots = extractOldSlotsFromUpdate( operation, entityPersistersByTable );
			List<UniqueSlot> newSlots = extractor.extractSlots( operation );

			// Track the change for swap detection
			if ( !oldSlots.isEmpty() && !newSlots.isEmpty() ) {
				// For simplicity, assume one unique constraint per entity
				// (can be enhanced later for multiple constraints)
				for ( int i = 0; i < Math.min( oldSlots.size(), newSlots.size() ); i++ ) {
					updateSlotChanges.add( new UpdateSlotChange( node, oldSlots.get( i ), newSlots.get( i ) ) );
				}
			}

			// Add new slots to the slotMap (for occupier tracking)
			for ( UniqueSlot slot : newSlots ) {
				slotMap.computeIfAbsent( slot, k -> new ArrayList<>() )
						.add( new OperationWithNode( operation, node ) );
			}
		}
	}

	/**
	 * Extract unique slots from all operations in a group.
	 */
	private void extractSlotsFromGroup(GroupNode node, Map<UniqueSlot, List<OperationWithNode>> slotMap) {
		PlannedOperationGroup group = node.group();

		// Create slot extractor
		UniqueSlotExtractor extractor = new UniqueSlotExtractor(
				constraintModel,
				session,
				entityPersistersByTable
		);

		// Extract slots from each operation in the group
		for ( PlannedOperation operation : group.operations() ) {
			List<UniqueSlot> slots = extractor.extractSlots( operation );

			// Add operation to slot map for each extracted slot
			for ( UniqueSlot slot : slots ) {
				slotMap.computeIfAbsent( slot, k -> new ArrayList<>() )
						.add( new OperationWithNode( operation, node ) );
			}
		}
	}

	/**
	 * Helper record to track operation with its node.
	 */
	private record OperationWithNode(PlannedOperation operation, GroupNode node) {
	}

	/**
	 * Helper record to track UPDATE operations with their old and new unique slot values.
	 */
	private record UpdateSlotChange(GroupNode node, UniqueSlot oldSlot, UniqueSlot newSlot) {
	}

	/**
	 * Phase 3: Expand UPDATE groups that contain swap conflicts into separate groups.
	 * When multiple UPDATEs in the same group swap unique constraint values, they need
	 * to be in separate graph nodes to create a detectable cycle.
	 */
	private List<PlannedOperationGroup> expandSwapConflicts(List<PlannedOperationGroup> groups) {
		List<PlannedOperationGroup> result = new ArrayList<>();

		UniqueSlotExtractor extractor = new UniqueSlotExtractor( constraintModel, session, entityPersistersByTable );

		for ( PlannedOperationGroup group : groups ) {
			if ( group.kind() != MutationKind.UPDATE || group.operations().size() <= 1 ) {
				// Not an UPDATE or only one operation - no need to split
				result.add( group );
				continue;
			}

			// Check if this UPDATE group has swap conflicts
			String tableName = (group.tableExpression());
			List<UniqueConstraint> constraints = constraintModel.getUniqueConstraintsForTable( tableName );

			if ( constraints.isEmpty() ) {
				// No unique constraints - no swaps possible
				result.add( group );
				continue;
			}

			// Extract slots for each operation
			Map<PlannedOperation, UniqueSlot> newSlots = new HashMap<>();
			Map<PlannedOperation, UniqueSlot> oldSlots = new HashMap<>();

			for ( PlannedOperation operation : group.operations() ) {
				List<UniqueSlot> oldSlotList = extractOldSlotsFromUpdate( operation, entityPersistersByTable );
				List<UniqueSlot> newSlotList = extractor.extractSlots( operation );

				if ( !oldSlotList.isEmpty() && !newSlotList.isEmpty() ) {
					oldSlots.put( operation, oldSlotList.get( 0 ) );
					newSlots.put( operation, newSlotList.get( 0 ) );
				}
			}

			// Check for swap conflicts
			// Use Set for O(n) lookup instead of O(n²) nested loop
			// A swap exists if any operation's newSlot matches another operation's oldSlot
			Set<UniqueSlot> oldSlotSet = new HashSet<>( oldSlots.values() );
			boolean hasSwap = false;
			for ( UniqueSlot newSlot : newSlots.values() ) {
				if ( oldSlotSet.contains( newSlot ) ) {
					hasSwap = true;
					break;
				}
			}

			if ( !hasSwap ) {
				// No swap conflicts - keep as single group
				result.add( group );
			}
			else {
				// Has swap conflicts - split into individual operation groups
				int ordinalOffset = 0;
				for ( PlannedOperation operation : group.operations() ) {
					PlannedOperationGroup singleOpGroup = new PlannedOperationGroup(
							group.tableExpression(),
							group.kind(),
							group.shapeKey(),
							List.of( operation ),
							group.needsIdPrePhase(),
							operation.getMutatingTableDescriptor().hasUniqueConstraints(),
							group.ordinal() * 1000 + ordinalOffset++,  // Ensure unique ordinals
							group.origin()
					);
					result.add( singleOpGroup );
				}
			}
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

		if ( operation.getKind() != MutationKind.UPDATE ) {
			return slots;
		}

		String tableName = operation.getTableExpression();
		List<UniqueConstraint> constraints = constraintModel.getUniqueConstraintsForTable( tableName );

		if ( constraints.isEmpty() ) {
			return slots;
		}

		var bindPlan = operation.getBindPlan();
		if ( !(bindPlan instanceof EntityUpdateBindPlan updateBindPlan) ) {
			return slots;
		}

		EntityPersister persister = persisterMap.get( tableName );
		if ( persister == null ) {
			return slots;
		}

		// Extract old values from previousState
		for ( UniqueConstraint constraint : constraints ) {
			Object[] oldValues = extractValuesFromState( persister, updateBindPlan.getPreviousState(), constraint );
			if ( oldValues != null ) {
				slots.add( new UniqueSlot( tableName, oldValues, constraint ) );
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
		if ( state == null ) {
			return null;
		}

		try {
			// Handle primary key
			if ( constraint.isPrimaryKey() ) {
				// PKs don't change in UPDATEs, skip
				return null;
			}

			// Handle other unique constraints using property names
			if ( constraint.propertyNames() != null && constraint.propertyNames().length > 0 ) {
				Object[] values = new Object[constraint.propertyNames().length];
				for ( int i = 0; i < constraint.propertyNames().length; i++ ) {
					String propertyName = constraint.propertyNames()[i];
					int propertyIndex = persister.getEntityMetamodel().getPropertyIndex( propertyName );
					if ( propertyIndex >= 0 && propertyIndex < state.length ) {
						Object value = state[propertyIndex];

						// If the value is an entity (association), extract its ID
						if ( value != null && persister.getEntityMetamodel()
								.getPropertyTypes()[propertyIndex].isEntityType() ) {
							var entityType = (org.hibernate.type.EntityType) persister.getEntityMetamodel()
									.getPropertyTypes()[propertyIndex];
							var associatedPersister = session.getFactory().getMappingMetamodel()
									.getEntityDescriptor( entityType.getAssociatedEntityName() );
							if ( associatedPersister != null ) {
								value = associatedPersister.getIdentifier( value, session );
							}
						}

						values[i] = value;
					}
					else {
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
		List<UniqueConstraint> constraints = constraintModel.getUniqueConstraintsForTable( tableName );
		UniqueConstraint matchingConstraint = null;
		for ( UniqueConstraint constraint : constraints ) {
			if ( constraint.constraintName().equals( slot.constraintName() ) ) {
				matchingConstraint = constraint;
				break;
			}
		}

		if ( matchingConstraint == null || matchingConstraint.propertyNames() == null ) {
			return null;
		}

		// Find an FK that uses the same columns
		// For now, return the first FK on this table (works for simple cases like dept_id)
		for ( ForeignKey fk : constraintModel.foreignKeys() ) {
			String fkKeyTable = (fk.keyTable());
			if ( fkKeyTable.equals( tableName ) ) {
				return fk;
			}
		}

		return null;
	}
}
