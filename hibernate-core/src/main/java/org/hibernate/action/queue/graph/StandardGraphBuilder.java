/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.graph;

import org.hibernate.action.queue.MutationKind;
import org.hibernate.action.queue.PlanningOptions;
import org.hibernate.action.queue.constraint.Constraint;
import org.hibernate.action.queue.constraint.DeferrableConstraintMode;
import org.hibernate.action.queue.constraint.ForeignKey;
import org.hibernate.action.queue.plan.FlushOperation;
import org.hibernate.action.queue.plan.FlushOperationGroup;
import org.hibernate.action.queue.constraint.ConstraintModel;
import org.hibernate.action.queue.constraint.UniqueConstraint;
import org.hibernate.action.queue.constraint.UniqueSlot;
import org.hibernate.action.queue.constraint.UniqueSlotExtractor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
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

		// Persister lookup used for runtime unique-slot extraction.
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
	public Graph build(List<FlushOperationGroup> groups, DeferrableConstraintMode deferrableConstraintMode) {
		// Split UPDATE groups that need per-row unique-slot ordering into separate nodes.
		List<FlushOperationGroup> expandedGroups = planningOptions.orderByUniqueKeySlots()
				? splitUpdateGroupsForUniqueSlotOrdering( groups, deferrableConstraintMode )
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
		final ArrayList<FlushOperationGroup> sortedGroups = new ArrayList<>( expandedGroups );
		sortedGroups.sort( comparingInt( FlushOperationGroup::ordinal ) );

		long nodeId = 1;
		for ( FlushOperationGroup g : sortedGroups ) {
			final GroupNode n = new GroupNode( g, nodeId++ );
			nodes.add( n );

			// Pre-initialize edge lists for all nodes to avoid computeIfAbsent overhead
			outgoing.put( n, new ArrayList<>() );

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

		long edgeId = 1;
		for ( ForeignKey foreignKey : constraintModel.foreignKeys() ) {
			if ( ignoreDeferrableForOrdering() && isEffectivelyDeferred( foreignKey, deferrableConstraintMode ) ) {
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
			edgeId = createInsertToInsertEdges(
					foreignKey,
					childTable,
					parentTable,
					parentInserts,
					childInserts,
					outgoing,
					edgeId,
					deferrableConstraintMode
			);
			edgeId = createDeleteToDeleteEdges( foreignKey, parentDeletes, childDeletes, outgoing, edgeId );
			edgeId = createInsertToUpdateEdges( foreignKey, parentInserts, childUpdates, outgoing, edgeId );
			edgeId = createUpdateToDeleteEdges( foreignKey, childUpdates, parentDeletes, outgoing, edgeId );
			edgeId = createUpdateToDeleteEdges( foreignKey, parentUpdates, childDeletes, outgoing, edgeId );
		}

		edgeId = createInsertToOrderUpdateEdges( insertNodeByTable, updateNodeByTable, outgoing, edgeId );

		// Create unique-slot ordering edges from runtime release/occupy facts.
		if ( planningOptions.orderByUniqueKeySlots() ) {
			edgeId = addUniqueSlotEdges(
					nodes,
					expandedGroups,
					deleteNodeByTable,
					insertNodeByTable,
					outgoing,
					edgeId,
					deferrableConstraintMode
			);
		}
		else {
			// Fallback: Create table-level DELETE -> INSERT edges to prevent unique constraint violations
			// For tables with both DELETE and INSERT operations, ensure DELETEs execute first
			// to avoid conflicts (e.g., moving an element between collections on a join table with unique constraints)
			// NOTE: This is less precise than addUniqueSlotEdges() and can create unnecessary edges
			// These edges are breakable to allow cycles to be resolved (e.g., orphan removal scenarios)
			edgeId = addTableLevelDeleteInsertEdges( deleteNodeByTable, insertNodeByTable, outgoing, edgeId );
		}

		for ( List<GraphEdge> es : outgoing.values() ) {
			es.sort( Comparator.comparingLong( x -> x.stableId ) );
		}

		return new Graph( nodes, outgoing );
	}

	/**
	 * Create same-table INSERT -> UPDATE_ORDER edges.
	 * Order-column updates target rows by their element identifier, so a row inserted during the same
	 * flush must exist before its order column can be written.
	 */
	private long createInsertToOrderUpdateEdges(
			Map<String, List<GroupNode>> insertNodeByTable,
			Map<String, List<GroupNode>> updateNodeByTable,
			Map<GroupNode, List<GraphEdge>> outgoing,
			long edgeId) {
		for ( Map.Entry<String, List<GroupNode>> entry : insertNodeByTable.entrySet() ) {
			final List<GroupNode> updates = updateNodeByTable.get( entry.getKey() );
			if ( updates == null ) {
				continue;
			}

			for ( GroupNode insert : entry.getValue() ) {
				for ( GroupNode update : updates ) {
					if ( update.group().kind() != MutationKind.UPDATE_ORDER ) {
						continue;
					}
					if ( !constraintModel.getUniqueConstraintsForTable( entry.getKey() ).isEmpty() ) {
						continue;
					}

					outgoing.get( insert ).add( GraphEdge.requiredOrder(
							insert,
							update,
							insert,
							update,
							Util.EMPTY_SELECTABLES,
							null,
							edgeId++
					) );
				}
			}
		}

		return edgeId;
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
			long edgeId,
			DeferrableConstraintMode deferrableConstraintMode) {
		if ( parentInserts == null || childInserts == null ) {
			return edgeId;
		}

		final boolean isSelfReferencing = childTable.equals(parentTable);
		for ( GroupNode parentInsert : parentInserts ) {
			for ( GroupNode childInsert : childInserts ) {
				// For self-referencing FKs, only create edges where parent ordinal < child ordinal
				// This represents parent entities (lower ordinal) must be inserted before child entities (higher ordinal)
				// Skip edges where parent ordinal >= child ordinal as these are false dependencies
				if (isSelfReferencing && parentInsert.group().ordinal() >= childInsert.group().ordinal()) {
					continue;
				}
				// Edge direction for ordering: parent -> child
				// Self-referencing FKs must be breakable (otherwise first INSERT is impossible)
				final boolean breakable;
				if ( childTable.equals( parentTable ) ) {
					// Self-referential FK - always breakable
					breakable = true;
				}
				else {
					breakable = foreignKey.nullable()
								&& (!avoidBreakingDeferrable()
										|| !isEffectivelyDeferred( foreignKey, deferrableConstraintMode ));
				}
				final int breakCost = computeBreakCost( foreignKey, deferrableConstraintMode );

				final GraphEdge edge = breakable
						? GraphEdge.nullPatchableFk(
								parentInsert,
								childInsert,
								parentInsert,
								childInsert,
								foreignKey.keyColumns(),
								foreignKey,
								breakCost,
								edgeId++
						)
						: GraphEdge.requiredOrder(
								parentInsert,
								childInsert,
								parentInsert,
								childInsert,
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
				// DELETE edges can't be broken with NULL strategy (would need to re-insert row)
				// Breaking relies on deferrable constraints or action queue ordering
				// Ordinal-based cycle breaking in CycleBreaker will prefer to keep forward-flowing edges
				// Edge direction for DELETE: child -> parent (reversed!)
				final GraphEdge edge = GraphEdge.requiredOrder(
						parentDelete,
						childDelete,
						childDelete,
						parentDelete,
						foreignKey.keyColumns(),
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
				final GraphEdge edge = GraphEdge.requiredOrder(
						parentInsert,
						childUpdate,
						parentInsert,
						childUpdate,
						Util.EMPTY_SELECTABLES,
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
				final GraphEdge edge = GraphEdge.requiredOrder(
						delete,
						update,
						update,
						delete,
						Util.EMPTY_SELECTABLES,
						foreignKey,
						edgeId++
				);

				outgoing.get( update ).add( edge );
			}
		}

		return edgeId;
	}

	private static int computeBreakCost(ForeignKey e, DeferrableConstraintMode deferrableConstraintMode) {
		// simple heuristic; tune later:
		// - deferrable edges get higher cost if we try to avoid breaking them
		// - nullable edges are break candidates (already filtered)
		int cost = 100;
		if ( isEffectivelyDeferred( e, deferrableConstraintMode ) ) {
			cost += 50;
		}
		// If your ForeignKeyDescriptor has useful metadata, incorporate it here.
		return cost;
	}

	private static boolean isEffectivelyDeferred(
			Constraint constraint,
			DeferrableConstraintMode deferrableConstraintMode) {
		return constraint != null && deferrableConstraintMode.isDeferred( constraint.getDeferrability() );
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
						final GraphEdge edge = GraphEdge.preferredOrder(
								null,
								null,
								deleteNode,
								insertNode,
								Util.EMPTY_SELECTABLES,
								null,
								1000,
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
	 * Add ordering edges for unique slot conflicts using concrete runtime values.
	 * <p>
	 * Each DELETE or UPDATE can release a unique slot, and each INSERT or UPDATE can
	 * occupy a unique slot. Edges are added only when those facts conflict on the
	 * same slot value. This also handles UPDATE-to-UPDATE unique conflicts, unique
	 * value swaps, and table-level DELETE-to-INSERT fallback for tables that have no
	 * modeled unique constraints.
	 */
	private long addUniqueSlotEdges(
			ArrayList<GroupNode> nodes,
			List<FlushOperationGroup> groups,
			Map<String, List<GroupNode>> deleteNodeByTable,
			Map<String, List<GroupNode>> insertNodeByTable,
			Map<GroupNode, List<GraphEdge>> outgoing,
			long edgeId,
			DeferrableConstraintMode deferrableConstraintMode) {

		final UniqueSlotFacts facts = collectUniqueSlotFacts(
				nodes,
				groups,
				deleteNodeByTable,
				insertNodeByTable,
				deferrableConstraintMode
		);

		edgeId = addReleaseToOccupyEdges(
				facts.releasesBySlot(),
				facts.occupiesBySlot(),
				outgoing,
				edgeId
		);
		edgeId = addUpdateSwapEdges(
				facts.updateSlotChanges(),
				outgoing,
				edgeId
		);
		edgeId = addTableLevelDeleteInsertEdges(
				facts.tablesNeedingTableLevelFallback(),
				deleteNodeByTable,
				insertNodeByTable,
				outgoing,
				edgeId
		);

		return edgeId;
	}

	private UniqueSlotFacts collectUniqueSlotFacts(
			ArrayList<GroupNode> nodes,
			List<FlushOperationGroup> groups,
			Map<String, List<GroupNode>> deleteNodeByTable,
			Map<String, List<GroupNode>> insertNodeByTable,
			DeferrableConstraintMode deferrableConstraintMode) {
		final Map<UniqueSlot, List<UniqueSlotRelease>> releasesBySlot = new HashMap<>();
		final Map<UniqueSlot, List<UniqueSlotOccupy>> occupiesBySlot = new HashMap<>();
		final List<UpdateSlotChange> updateSlotChanges = new ArrayList<>();
		final Set<String> tablesNeedingTableLevelFallback = new HashSet<>();

		for ( Map.Entry<String, List<GroupNode>> entry : deleteNodeByTable.entrySet() ) {
			String tableName = entry.getKey();
			var constraints = constraintModel.getUniqueConstraintsForTable( (tableName) );

			if ( constraints.isEmpty() ) {
				if ( insertNodeByTable.containsKey( tableName ) ) {
					tablesNeedingTableLevelFallback.add( tableName );
				}
				continue;
			}

			for ( GroupNode deleteNode : entry.getValue() ) {
				final int releaseCount = countReleases( releasesBySlot );
				extractUniqueSlotFactsFromGroup(
						deleteNode,
						releasesBySlot,
						occupiesBySlot,
						deferrableConstraintMode
				);
				if ( insertNodeByTable.containsKey( tableName ) && releaseCount == countReleases( releasesBySlot ) ) {
					tablesNeedingTableLevelFallback.add( tableName );
				}
			}
		}

		for ( Map.Entry<String, List<GroupNode>> entry : insertNodeByTable.entrySet() ) {
			String tableName = entry.getKey();
			var constraints = constraintModel.getUniqueConstraintsForTable( (tableName) );

			if ( constraints.isEmpty() ) {
				continue;
			}

			for ( GroupNode insertNode : entry.getValue() ) {
				extractUniqueSlotFactsFromGroup(
						insertNode,
						releasesBySlot,
						occupiesBySlot,
						deferrableConstraintMode
				);
			}
		}

		for ( FlushOperationGroup group : groups ) {
			if ( group.kind() == MutationKind.UPDATE ) {
				String tableName = (group.tableExpression());
				if ( !constraintModel.getUniqueConstraintsForTable( tableName ).isEmpty() ) {
					GroupNode updateNode = findGroupNode( nodes, group );
					if ( updateNode != null ) {
						extractUpdateSlotChanges(
								updateNode,
								updateSlotChanges,
								releasesBySlot,
								occupiesBySlot,
								deferrableConstraintMode
						);
					}
				}
			}
		}

		return new UniqueSlotFacts(
				releasesBySlot,
				occupiesBySlot,
				updateSlotChanges,
				tablesNeedingTableLevelFallback
		);
	}

	private int countReleases(Map<UniqueSlot, List<UniqueSlotRelease>> releasesBySlot) {
		int count = 0;
		for ( List<UniqueSlotRelease> releases : releasesBySlot.values() ) {
			count += releases.size();
		}
		return count;
	}

	private long addReleaseToOccupyEdges(
			Map<UniqueSlot, List<UniqueSlotRelease>> releasesBySlot,
			Map<UniqueSlot, List<UniqueSlotOccupy>> occupiesBySlot,
			Map<GroupNode, List<GraphEdge>> outgoing,
			long edgeId) {
		for ( Map.Entry<UniqueSlot, List<UniqueSlotOccupy>> entry : occupiesBySlot.entrySet() ) {
			final UniqueSlot slot = entry.getKey();
			final UniqueSlotParticipants participants = classifyUniqueSlotParticipants(
					releasesBySlot.get( slot ),
					entry.getValue()
			);
			edgeId = addDeleteReleaseToOccupyEdges( slot, participants, outgoing, edgeId );
			edgeId = addUpdateReleaseToInsertEdges( slot, participants, outgoing, edgeId );
			edgeId = addSameSlotUpdateConflictEdges( slot, participants, outgoing, edgeId );
		}
		return edgeId;
	}

	private UniqueSlotParticipants classifyUniqueSlotParticipants(
			List<UniqueSlotRelease> releases,
			List<UniqueSlotOccupy> occupies) {
		final List<GroupNode> deleteReleasers = new ArrayList<>();
		final List<GroupNode> updateReleasers = new ArrayList<>();
		final List<GroupNode> insertOccupiers = new ArrayList<>();
		final List<GroupNode> updateOccupiers = new ArrayList<>();

		if ( releases != null ) {
			for ( UniqueSlotRelease release : releases ) {
				if ( release.kind() == MutationKind.DELETE ) {
					addIfAbsent( deleteReleasers, release.node() );
				}
				else if ( release.kind() == MutationKind.UPDATE ) {
					addIfAbsent( updateReleasers, release.node() );
				}
			}
		}

		for ( UniqueSlotOccupy occupy : occupies ) {
			if ( occupy.kind() == MutationKind.INSERT ) {
				addIfAbsent( insertOccupiers, occupy.node() );
			}
			else if ( occupy.kind() == MutationKind.UPDATE ) {
				addIfAbsent( updateOccupiers, occupy.node() );
			}
		}

		return new UniqueSlotParticipants(
				deleteReleasers,
				updateReleasers,
				insertOccupiers,
				updateOccupiers
		);
	}

	private long addDeleteReleaseToOccupyEdges(
			UniqueSlot slot,
			UniqueSlotParticipants participants,
			Map<GroupNode, List<GraphEdge>> outgoing,
			long edgeId) {
		final List<GroupNode> allOccupiers = participants.allOccupiers();
		if ( participants.deleteReleasers().isEmpty() || allOccupiers.isEmpty() ) {
			return edgeId;
		}

		final UniqueConstraint constraint = slot.constraint();
		for ( GroupNode releaser : participants.deleteReleasers() ) {
			for ( GroupNode occupier : allOccupiers ) {
				if ( releaser == occupier ) {
					continue;
				}

				final GraphEdge edge = createUniqueReleaseToOccupyEdge(
						releaser,
						occupier,
						constraint,
						edgeId++
				);
				outgoing.get( releaser ).add( edge );
			}
		}
		return edgeId;
	}

	private long addUpdateReleaseToInsertEdges(
			UniqueSlot slot,
			UniqueSlotParticipants participants,
			Map<GroupNode, List<GraphEdge>> outgoing,
			long edgeId) {
		if ( participants.updateReleasers().isEmpty() || participants.insertOccupiers().isEmpty() ) {
			return edgeId;
		}

		final UniqueConstraint constraint = slot.constraint();
		final SelectableMappings columnsToNull = getUniqueConstraintColumns( constraint );
		for ( GroupNode releaser : participants.updateReleasers() ) {
			for ( GroupNode insertNode : participants.insertOccupiers() ) {
				if ( releaser == insertNode ) {
					continue;
				}

				final GraphEdge edge = GraphEdge.preferredOrder(
						null,
						insertNode,
						releaser,
						insertNode,
						columnsToNull,
						constraint,
						1000,
						edgeId++
				);
				outgoing.get( releaser ).add( edge );
			}
		}
		return edgeId;
	}

	private long addSameSlotUpdateConflictEdges(
			UniqueSlot slot,
			UniqueSlotParticipants participants,
			Map<GroupNode, List<GraphEdge>> outgoing,
			long edgeId) {
		final List<GroupNode> updateNodes = participants.updateOccupiers();
		if ( updateNodes.size() <= 1 ) {
			return edgeId;
		}

		final UniqueConstraint constraint = slot.constraint();
		final SelectableMappings columnsToNull = getUniqueConstraintColumns( constraint );
		for ( int i = 0; i < updateNodes.size(); i++ ) {
			for ( int j = i + 1; j < updateNodes.size(); j++ ) {
				final GroupNode update1 = updateNodes.get( i );
				final GroupNode update2 = updateNodes.get( j );

				final GraphEdge edge1 = createUniqueUpdateConflictEdge(
						update1,
						update2,
						update1,
						constraint,
						columnsToNull,
						edgeId++
				);

				final GraphEdge edge2 = createUniqueUpdateConflictEdge(
						update2,
						update1,
						update2,
						constraint,
						columnsToNull,
						edgeId++
				);

				outgoing.get( update1 ).add( edge1 );
				outgoing.get( update2 ).add( edge2 );
			}
		}
		return edgeId;
	}

	private long addUpdateSwapEdges(
			List<UpdateSlotChange> updateSlotChanges,
			Map<GroupNode, List<GraphEdge>> outgoing,
			long edgeId) {
		final Map<UniqueSlot, List<UpdateSlotChange>> changesByOldSlot = new HashMap<>();
		for ( UpdateSlotChange change : updateSlotChanges ) {
			changesByOldSlot.computeIfAbsent( change.oldSlot(), k -> new ArrayList<>() ).add( change );
		}

		for ( UpdateSlotChange change1 : updateSlotChanges ) {
			final List<UpdateSlotChange> conflictingChanges = changesByOldSlot.get( change1.newSlot() );
			if ( conflictingChanges != null ) {
				for ( UpdateSlotChange change2 : conflictingChanges ) {
					if ( change1 == change2 ) {
						continue;
					}

					final UniqueConstraint constraint = change1.newSlot().constraint();
					final SelectableMappings columnsToNull = getUniqueConstraintColumns( constraint );

					final GraphEdge edge = createUniqueUpdateConflictEdge(
							change2.node(),
							change1.node(),
							change1.node(),
							constraint,
							columnsToNull,
							edgeId++
					);
					outgoing.get( change2.node() ).add( edge );
				}
			}
		}
		return edgeId;
	}

	private long addTableLevelDeleteInsertEdges(
			Set<String> tablesNeedingTableLevelFallback,
			Map<String, List<GroupNode>> deleteNodeByTable,
			Map<String, List<GroupNode>> insertNodeByTable,
			Map<GroupNode, List<GraphEdge>> outgoing,
			long edgeId) {
		for ( String tableName : tablesNeedingTableLevelFallback ) {
			final List<GroupNode> deleteNodes = deleteNodeByTable.get( tableName );
			final List<GroupNode> insertNodes = insertNodeByTable.get( tableName );

			for ( GroupNode deleteNode : deleteNodes ) {
				for ( GroupNode insertNode : insertNodes ) {
					final GraphEdge edge = GraphEdge.preferredOrder(
							null,
							null,
							deleteNode,
							insertNode,
							Util.EMPTY_SELECTABLES,
							null,
							1000,
							edgeId++
					);

					outgoing.get( deleteNode ).add( edge );
				}
			}
		}
		return edgeId;
	}

	private void addIfAbsent(List<GroupNode> nodes, GroupNode node) {
		if ( !nodes.contains( node ) ) {
			nodes.add( node );
		}
	}

	private GraphEdge createUniqueReleaseToOccupyEdge(
			GroupNode releaser,
			GroupNode occupier,
			UniqueConstraint constraint,
			long edgeId) {
		return GraphEdge.requiredOrder(
				null,
				occupier,
				releaser,
				occupier,
				getUniqueConstraintColumns( constraint ),
				constraint,
				edgeId
		);
	}

	private GraphEdge createUniqueUpdateConflictEdge(
			GroupNode from,
			GroupNode to,
			GroupNode keyNode,
			UniqueConstraint constraint,
			SelectableMappings columnsToNull,
			long edgeId) {
		if ( isNullPatchableUniqueConstraint( constraint ) ) {
			return GraphEdge.nullPatchableUnique(
					null,
					keyNode,
					from,
					to,
					columnsToNull,
					constraint,
					100,
					edgeId
			);
		}

		return GraphEdge.requiredOrder(
				null,
				keyNode,
				from,
				to,
				columnsToNull,
				constraint,
				edgeId
		);
	}

	private boolean isNullPatchableUniqueConstraint(UniqueConstraint constraint) {
		return constraint != null && constraint.nullable();
	}

	private SelectableMappings getUniqueConstraintColumns(UniqueConstraint constraint) {
		return constraint != null ? constraint.columns() : Util.EMPTY_SELECTABLES;
	}

	/**
	 * Find the GroupNode corresponding to a FlushOperationGroup.
	 */
	private GroupNode findGroupNode(ArrayList<GroupNode> nodes, FlushOperationGroup group) {
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
			Map<UniqueSlot, List<UniqueSlotRelease>> releasesBySlot,
			Map<UniqueSlot, List<UniqueSlotOccupy>> occupiesBySlot,
			DeferrableConstraintMode deferrableConstraintMode) {
		FlushOperationGroup group = node.group();
		UniqueSlotExtractor extractor = new UniqueSlotExtractor( constraintModel, session, entityPersistersByTable );

		for ( FlushOperation operation : group.operations() ) {
			List<UniqueSlot> oldSlots = removeEffectivelyDeferredSlots(
					extractor.extractOldSlots( operation ),
					deferrableConstraintMode
			);
			List<UniqueSlot> newSlots = removeEffectivelyDeferredSlots(
					extractor.extractSlots( operation ),
					deferrableConstraintMode
			);

			final Map<UniqueConstraintKey, UniqueSlot> oldSlotsByConstraint = uniqueSlotsByConstraint( oldSlots );
			for ( UniqueSlot newSlot : newSlots ) {
				final UniqueSlot oldSlot = oldSlotsByConstraint.get( UniqueConstraintKey.from( newSlot ) );
				if ( oldSlot == null ) {
					continue;
				}

				updateSlotChanges.add( new UpdateSlotChange( node, oldSlot, newSlot ) );
				if ( !hasNullValue( oldSlot.keyValues() ) ) {
					addUniqueSlotRelease( releasesBySlot, oldSlot, node, MutationKind.UPDATE );
				}
			}

			// Add new slots as occupy facts.
			for ( UniqueSlot slot : newSlots ) {
				addUniqueSlotOccupy( occupiesBySlot, slot, node, MutationKind.UPDATE );
			}
		}
	}

	private void addUniqueSlotRelease(
			Map<UniqueSlot, List<UniqueSlotRelease>> releasesBySlot,
			UniqueSlot slot,
			GroupNode node,
			MutationKind kind) {
		final UniqueSlotRelease release = new UniqueSlotRelease( slot, node, kind );
		final List<UniqueSlotRelease> releases = releasesBySlot.computeIfAbsent( slot, k -> new ArrayList<>() );
		if ( !releases.contains( release ) ) {
			releases.add( release );
		}
	}

	private void addUniqueSlotOccupy(
			Map<UniqueSlot, List<UniqueSlotOccupy>> occupiesBySlot,
			UniqueSlot slot,
			GroupNode node,
			MutationKind kind) {
		final UniqueSlotOccupy occupy = new UniqueSlotOccupy( slot, node, kind );
		final List<UniqueSlotOccupy> occupies = occupiesBySlot.computeIfAbsent( slot, k -> new ArrayList<>() );
		if ( !occupies.contains( occupy ) ) {
			occupies.add( occupy );
		}
	}

	private boolean hasNullValue(Object[] values) {
		for ( Object value : values ) {
			if ( value == null ) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Extract unique slots from all operations in a group.
	 */
	private void extractUniqueSlotFactsFromGroup(
			GroupNode node,
			Map<UniqueSlot, List<UniqueSlotRelease>> releasesBySlot,
			Map<UniqueSlot, List<UniqueSlotOccupy>> occupiesBySlot,
			DeferrableConstraintMode deferrableConstraintMode) {
		FlushOperationGroup group = node.group();

		// Create slot extractor
		UniqueSlotExtractor extractor = new UniqueSlotExtractor(
				constraintModel,
				session,
				entityPersistersByTable
		);

		// Extract slots from each operation in the group
		for ( FlushOperation operation : group.operations() ) {
			List<UniqueSlot> slots = removeEffectivelyDeferredSlots(
					extractor.extractSlots( operation ),
					deferrableConstraintMode
			);

			for ( UniqueSlot slot : slots ) {
				if ( group.kind() == MutationKind.DELETE ) {
					addUniqueSlotRelease( releasesBySlot, slot, node, MutationKind.DELETE );
				}
				else if ( group.kind() == MutationKind.INSERT ) {
					addUniqueSlotOccupy( occupiesBySlot, slot, node, MutationKind.INSERT );
				}
			}
		}
	}

	private List<UniqueSlot> removeEffectivelyDeferredSlots(
			List<UniqueSlot> slots,
			DeferrableConstraintMode deferrableConstraintMode) {
		if ( slots.isEmpty() ) {
			return slots;
		}

		final List<UniqueSlot> filteredSlots = new ArrayList<>( slots.size() );
		for ( UniqueSlot slot : slots ) {
			if ( !isEffectivelyDeferred( slot.constraint(), deferrableConstraintMode ) ) {
				filteredSlots.add( slot );
			}
		}
		return filteredSlots;
	}

	private Map<UniqueConstraintKey, UniqueSlot> uniqueSlotsByConstraint(List<UniqueSlot> slots) {
		final Map<UniqueConstraintKey, UniqueSlot> slotsByConstraint = new HashMap<>();
		for ( UniqueSlot slot : slots ) {
			slotsByConstraint.put( UniqueConstraintKey.from( slot ), slot );
		}
		return slotsByConstraint;
	}

	/**
	 * Builder-local fact: an operation releases a unique slot.
	 */
	private record UniqueSlotRelease(UniqueSlot slot, GroupNode node, MutationKind kind) {
	}

	/**
	 * Builder-local fact: an operation occupies a unique slot.
	 */
	private record UniqueSlotOccupy(UniqueSlot slot, GroupNode node, MutationKind kind) {
	}

	private record UniqueSlotFacts(
			Map<UniqueSlot, List<UniqueSlotRelease>> releasesBySlot,
			Map<UniqueSlot, List<UniqueSlotOccupy>> occupiesBySlot,
			List<UpdateSlotChange> updateSlotChanges,
			Set<String> tablesNeedingTableLevelFallback) {
	}

	private record UniqueSlotParticipants(
			List<GroupNode> deleteReleasers,
			List<GroupNode> updateReleasers,
			List<GroupNode> insertOccupiers,
			List<GroupNode> updateOccupiers) {
		private List<GroupNode> allOccupiers() {
			final List<GroupNode> allOccupiers = new ArrayList<>();
			allOccupiers.addAll( insertOccupiers );
			allOccupiers.addAll( updateOccupiers );
			return allOccupiers;
		}
	}

	/**
	 * Helper record to track UPDATE operations with their old and new unique slot values.
	 */
	private record UpdateSlotChange(GroupNode node, UniqueSlot oldSlot, UniqueSlot newSlot) {
	}

	private record UniqueConstraintKey(String tableName, String constraintName) {
		private static UniqueConstraintKey from(UniqueSlot slot) {
			return new UniqueConstraintKey( slot.tableName(), slot.constraintName() );
		}
	}

	/**
	 * Split UPDATE groups that need per-row unique-slot ordering.
	 * <p>
	 * Groups are normally table/shape batches, but row-to-row unique-slot cycles
	 * need separate graph nodes so the planner can see and resolve the cycle.
	 */
	private List<FlushOperationGroup> splitUpdateGroupsForUniqueSlotOrdering(
			List<FlushOperationGroup> groups,
			DeferrableConstraintMode deferrableConstraintMode) {
		List<FlushOperationGroup> result = new ArrayList<>();

		UniqueSlotExtractor extractor = new UniqueSlotExtractor( constraintModel, session, entityPersistersByTable );

		for ( FlushOperationGroup group : groups ) {
			if ( group.kind() != MutationKind.UPDATE || group.operations().size() <= 1 ) {
				// Not an UPDATE or only one operation - no need to split
				result.add( group );
				continue;
			}

			String tableName = (group.tableExpression());
			List<UniqueConstraint> constraints = constraintModel.getUniqueConstraintsForTable( tableName );

			if ( constraints.isEmpty() ) {
				// No unique constraints - no unique-slot cycles possible.
				result.add( group );
				continue;
			}

			Map<FlushOperation, Map<UniqueConstraintKey, UniqueSlot>> newSlotsByOperation = new HashMap<>();
			Map<FlushOperation, Map<UniqueConstraintKey, UniqueSlot>> oldSlotsByOperation = new HashMap<>();

			for ( FlushOperation operation : group.operations() ) {
				List<UniqueSlot> oldSlotList = removeEffectivelyDeferredSlots(
						extractor.extractOldSlots( operation ),
						deferrableConstraintMode
				);
				List<UniqueSlot> newSlotList = removeEffectivelyDeferredSlots(
						extractor.extractSlots( operation ),
						deferrableConstraintMode
				);

				if ( !oldSlotList.isEmpty() && !newSlotList.isEmpty() ) {
					oldSlotsByOperation.put( operation, uniqueSlotsByConstraint( oldSlotList ) );
					newSlotsByOperation.put( operation, uniqueSlotsByConstraint( newSlotList ) );
				}
			}

			// A unique-slot cycle is possible if any operation's new slot is an old
			// slot of another operation in the same group.
			Set<UniqueSlot> oldSlotSet = new HashSet<>();
			for ( Map<UniqueConstraintKey, UniqueSlot> oldSlots : oldSlotsByOperation.values() ) {
				oldSlotSet.addAll( oldSlots.values() );
			}
			boolean hasUniqueSlotCycle = false;
			for ( Map<UniqueConstraintKey, UniqueSlot> newSlots : newSlotsByOperation.values() ) {
				for ( UniqueSlot newSlot : newSlots.values() ) {
					if ( oldSlotSet.contains( newSlot ) ) {
						hasUniqueSlotCycle = true;
						break;
					}
				}
				if ( hasUniqueSlotCycle ) {
					break;
				}
			}

			if ( !hasUniqueSlotCycle ) {
				// No row-to-row unique-slot cycle - keep as single group.
				result.add( group );
			}
			else {
				// Split so the graph can represent row-to-row unique-slot ordering.
				int ordinalOffset = 0;
				for ( FlushOperation operation : group.operations() ) {
					FlushOperationGroup singleOpGroup = new FlushOperationGroup(
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

}
