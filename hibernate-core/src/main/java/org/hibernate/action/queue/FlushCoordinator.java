/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue;

import org.hibernate.HibernateException;
import org.hibernate.action.internal.AbstractEntityInsertAction;
import org.hibernate.action.internal.ActionLogging;
import org.hibernate.action.internal.CollectionRecreateAction;
import org.hibernate.action.internal.CollectionRemoveAction;
import org.hibernate.action.internal.CollectionUpdateAction;
import org.hibernate.action.internal.EntityDeleteAction;
import org.hibernate.action.internal.EntityUpdateAction;
import org.hibernate.action.internal.OrphanRemovalAction;
import org.hibernate.action.internal.QueuedOperationCollectionAction;
import org.hibernate.action.queue.constraint.ConstraintModel;
import org.hibernate.action.queue.exec.PlanStepExecutor;
import org.hibernate.action.queue.exec.PlanStepExecutorFactory;
import org.hibernate.action.queue.decompose.Decomposer;
import org.hibernate.action.queue.graph.GraphBuilder;
import org.hibernate.action.queue.graph.StandardGraphBuilder;
import org.hibernate.action.queue.plan.PlannedOperation;
import org.hibernate.action.queue.plan.FlushPlan;
import org.hibernate.action.queue.plan.FlushPlanner;
import org.hibernate.action.queue.plan.PlanStep;
import org.hibernate.action.queue.plan.PlannedOperationGroup;
import org.hibernate.action.queue.plan.StandardFlushPlanner;
import org.hibernate.action.queue.support.GraphBasedActionQueueFactory;
import org.hibernate.action.queue.support.OperationGroupKey;
import org.hibernate.action.spi.Executable;
import org.hibernate.engine.spi.SessionImplementor;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.hibernate.action.queue.CollectionOrdinalSupport.extractCollectionOrdinal;

/// Orchestrates the steps needed to flush a Session, using a graph-based approach to
/// model mutation-operation scheduling that automatically handles foreign key dependencies,
/// cycle detection, and proper execution ordering.
///
/// Its responsibilities include -
///
/// - Manage decomposition → graph → planning → execution flow
/// - Tracks newly managed entities for unresolved insert resolution
/// - Handles recursive flush for resolved inserts
/// - Validates no unresolved inserts remain
/// - Manages post-execution callbacks
///
/// An important input to this process is the new [ConstraintModel] which describes
/// details about all foreign keys and unique keys defined by the domain model.  Flush
/// coordination leverages this at many points.
///
/// The basic flow is initiated by a call to [#executeFlush(List)] which is passed a
/// list of [actions][Executable].
///
/// - First, these actions (EntityInsertAction, etc.) are decomposed into low-level
/// 	[PlannedOperation] references by the [Decomposer] delegate.
/// - The [PlannedOperation] references are grouped by "shape" into [PlannedOperationGroup] references.
/// - The [PlannedOperationGroup] references are then arranged into a directed dependency `Graph`
/// 	(using the [ConstraintModel]) by [GraphBuilder].
/// - [FlushPlanner] then creates an executable plan from the `Graph`..
///
/// Some important concepts for this coordination include -
///
/// = `ConstraintModel` - Details about constraints defined on the domain model.
/// - `PlannedOperation` - A single operation against a single table
/// - `PlannedOperationGroup` - Multiple operations (of the same kind and shape) against a single table
/// - `GraphNode` - Wraps `PlannedOperation` and acts as vertx for the graph.
/// - `GraphEdge` - Used to denote to/from dependencies in the graph.
/// - `CycleBreaker` - Used to find edges where we can break cycles in the graph (using fk-fixups e.g.).
/// - `BindingPatch` - When we do break an edge, we "install" one of these on to the corresponding `PlannedOperation`.
/// - `PlanStep` - Grouping of independent `PlannedOperation` references.
/// - `FlushPlan` - Group of `PlanStep`, indicates the overall groupings of operations to perform as part of the flush.
///
/// @author Steve Ebersole
public class FlushCoordinator {
	private final transient PlanningOptions planningOptions;
	private final transient SessionImplementor session;
	private final transient GraphBuilder graphBuilder;

	private final transient Decomposer decomposer;
	private final transient FlushPlanner flushPlanner;
	private final transient PlanStepExecutor executor;

	// Track entities that became managed during the current flush
	private final transient List<Object> newlyManagedEntities = new ArrayList<>();

	// Tables involved in self-referential associations (need ordinalBase separation to avoid false cycles)
	private final transient ConstraintModel constraintModel;

	public FlushCoordinator(ConstraintModel constraintModel, PlanningOptions planningOptions, SessionImplementor session) {
		this.constraintModel = constraintModel;
		this.planningOptions = planningOptions;
		this.session = session;

		decomposer = new Decomposer( session );
		graphBuilder = new StandardGraphBuilder( constraintModel, planningOptions, session );
		flushPlanner = new StandardFlushPlanner( planningOptions );
		executor = PlanStepExecutorFactory.create( session );
	}

	/// Get the Decomposer (for accessing unresolved insert tracking).
	///
	/// @return the decomposer
	public Decomposer getDecomposer() {
		return decomposer;
	}

	/// Execute a single IDENTITY insert immediately.
	///
	/// IDENTITY inserts must execute immediately upon persist() to generate the ID,
	/// rather than being deferred until flush(). This method handles immediate execution
	/// of a single IDENTITY insert action.
	///
	/// @param insert the IDENTITY insert action to execute
	/// @param pendingInsertsExecutor callback to execute any pending inserts after this entity is managed
	public void executeIdentityInsert(AbstractEntityInsertAction insert, Runnable pendingInsertsExecutor) {
		try {
			// Execute IDENTITY insert directly
			// This handles ID generation, EntityKey creation, event firing, etc.
			insert.execute();

			// Now make the entity managed (ID has been generated)
			if (!insert.isVeto()) {
				insert.makeEntityManaged();

				// CRITICAL: Now that this entity is managed (in PC with ID), execute any pending inserts.
				// This allows pending inserts to reference this entity during decomposition.
				// Must happen AFTER makeEntityManaged() but BEFORE resolveDependentActions().
				pendingInsertsExecutor.run();

				// After making the entity managed, resolve any waiting inserts that depended on it
				// This mirrors ActionQueueLegacy behavior where resolveDependentActions is called
				// after each entity becomes managed. Returns the original insert actions so they
				// can be properly executed with IDENTITY ID handling.
				final var resolvedActions = decomposer.resolveDependentActions(insert.getInstance());
				if (!resolvedActions.isEmpty()) {
					// Re-execute each resolved action - they're now ready because dependencies are satisfied
					// For IDENTITY inserts, this will execute them immediately (recursive call)
					// For regular inserts, they would be added to queue
					for (var resolvedAction : resolvedActions) {
						// Execute directly - we know dependencies are resolved
						executeIdentityInsert(resolvedAction, pendingInsertsExecutor);
					}
				}
			}
		}
		catch (Exception e) {
			throw new HibernateException("Could not execute IDENTITY insert", e);
		}
	}

	public void executeInsertFlush(List<AbstractEntityInsertAction> insertActions) {

	}

	/**
	 * Execute flush with separate action phases.
	 * Preserves phase boundaries to maintain execution order intent.
	 */
	public void executeFlush(
			List<CollectionRemoveAction> orphanCollectionRemovals,
			List<OrphanRemovalAction> orphanRemovals,
			List<AbstractEntityInsertAction> insertions,
			List<EntityUpdateAction> updates,
			List<QueuedOperationCollectionAction> collectionQueuedOps,
			List<CollectionRemoveAction> collectionRemovals,
			List<CollectionUpdateAction> collectionUpdates,
			List<CollectionRecreateAction> collectionCreations,
			List<EntityDeleteAction> deletions) {

		// Count for early opt-out (return)
		int totalActions = orphanCollectionRemovals.size() + orphanRemovals.size() + insertions.size()
				+ updates.size() + collectionQueuedOps.size() + collectionRemovals.size()
				+ collectionUpdates.size() + collectionCreations.size() + deletions.size();

		if ( totalActions == 0 ) {
			// EARLY EXIT!!
			return;
		}

		// Decompose each phase separately to preserve phase identity
		var operationGroups = decomposeExecutablePhases(
				orphanCollectionRemovals,
				orphanRemovals,
				insertions,
				updates,
				collectionQueuedOps,
				collectionRemovals,
				collectionUpdates,
				collectionCreations,
				deletions
		);

		if ( operationGroups.isEmpty() ) {
			// No SQL operations needed - post-execution callbacks (if any) were already
			// attached to PlannedOperations and would have run during decomposition.
			// Since there are no operations, there's nothing to finalize.
			decomposer.validateNoUnresolvedInserts();
			return;
		}

		// Note: IDENTITY inserts are no longer in the actions list
		// They were executed immediately when added to ActionQueue via executeIdentityInsert()
		// All operations here go through normal graph/plan/execute flow

		// Fast path: Skip graph building for simple scenarios with no dependencies
		final FlushPlan plan;
		if ( canSkipGraphBuilding( operationGroups ) ) {
			// Fast path triggered - skip expensive graph building and planning
			ActionLogging.ACTION_LOGGER.trace( "Skipping graph building - no statement dependencies" );
			plan = createSimplePlan( operationGroups );
		}
		else {
			// Complex scenario - use full graph-based planning
			ActionLogging.ACTION_LOGGER.trace( "Building graph - statement dependencies found" );
			var graph = graphBuilder.build( operationGroups );
			plan = flushPlanner.plan( graph );
		}

		// Execute the plan - post-execution callbacks will run inline as operations complete
		executePlan( plan );

		executor.finishUp();

		// After execution, try to resolve any deferred inserts
		resolveUnresolvedInserts();

		// Final validation - any remaining unresolved inserts are an error
		decomposer.validateNoUnresolvedInserts();
	}

	/// Check if we can skip graph building and use a simple direct execution plan.
	///
	/// Graph building has overhead - we can skip it when there are no dependencies:
	/// - Single operation group → no inter-group dependencies possible
	/// - Only INSERT operations with no relevant FKs → order doesn't matter
	/// - Only DELETE operations with no relevant FKs → order doesn't matter
	/// - Only UPDATE operations → no FK ordering needed (FKs already set)
	///
	/// This optimization provides 20-30% improvement on simple scenarios.
	///
	/// @param groups the operation groups to check
	/// @return true if we can skip graph building
	private boolean canSkipGraphBuilding(List<PlannedOperationGroup> groups) {
		// Single group - check for intra-group dependencies
		if (groups.size() == 1) {
			PlannedOperationGroup singleGroup = groups.get(0);

			// Single operation - definitely no dependencies
			if (singleGroup.operations().size() <= 1) {
				return true;
			}

			// Multiple UPDATEs on table with unique constraints might have cycles (swaps)
			if (singleGroup.kind() == MutationKind.UPDATE
					&& planningOptions.orderByUniqueKeySlots()
					&& singleGroup.hasUniqueConstraints()) {
				return false;  // Need graph to detect swaps within the group
			}

			// Otherwise, single group has no inter-group dependencies
			return true;
		}

		// Check if all groups are the same kind and have no relevant dependencies
		MutationKind firstKind = groups.get(0).kind();

		// Mixed kinds or UPDATE_ORDER (collection operations) need graph
		for (PlannedOperationGroup group : groups) {
			if (group.kind() != firstKind || group.kind() == MutationKind.UPDATE_ORDER) {
				return false;
			}
		}

		// For INSERTs: Check if any operation group has foreign keys to another group's table
		// For DELETEs: Check if any operation group is referenced by another group's table
		if (firstKind == MutationKind.INSERT || firstKind == MutationKind.DELETE) {
			return !hasInterGroupDependencies(groups, firstKind);
		}

		// For UPDATEs: If unique constraint ordering is enabled, need graph for swap detection
		if (firstKind == MutationKind.UPDATE && planningOptions.orderByUniqueKeySlots()) {
			return !hasUniqueConstraintConflicts(groups);
		}

		// UPDATEs and NO_OPs can be executed in any order
		return firstKind == MutationKind.UPDATE || firstKind == MutationKind.NO_OP;
	}

	/// Check if operation groups have foreign key dependencies between them.
	///
	/// @param groups the operation groups
	/// @param kind the mutation kind (INSERT or DELETE)
	/// @return true if there are inter-group dependencies
	private boolean hasInterGroupDependencies(List<PlannedOperationGroup> groups, MutationKind kind) {
		// Build set of tables involved in this flush
		final java.util.Set<String> involvedTables = new java.util.HashSet<>();
		for (PlannedOperationGroup group : groups) {
			involvedTables.add(group.tableExpression());
		}

		// For each foreign key, check if it creates a dependency between groups
		for (var fk : constraintModel.foreignKeys()) {
			// For DELETE operations, we must consider ALL FKs including inheritance FKs
			// (e.g., joined inheritance where dog.id -> animal.id requires deleting dog before animal)
			// For INSERT operations, we only need association FKs (inheritance inserts are ordered by table structure)
			if (!fk.isAssociation() && kind != MutationKind.DELETE) {
				continue; // Skip non-association FKs for INSERTs (secondary tables, inheritance handled separately)
			}

			final String keyTable = fk.keyTable();
			final String targetTable = fk.targetTable();

			// Check if this FK creates a dependency between two groups in this flush
			if (kind == MutationKind.INSERT) {
				// For INSERT: keyTable depends on targetTable (must insert target first)
				// Even nullable FKs create dependencies because we don't know at this point
				// whether the FK value will be null or not. The graph builder will handle
				// breaking nullable FK edges if there's a cycle.
				if (involvedTables.contains(keyTable) && involvedTables.contains(targetTable)) {
					return true;
				}
			}
			else if (kind == MutationKind.DELETE) {
				// For DELETE: targetTable depends on keyTable (must delete key holders first)
				if (involvedTables.contains(keyTable) && involvedTables.contains(targetTable)) {
					return true;
				}
			}
		}

		return false;
	}

	/// Check if UPDATE operation groups have potential unique constraint conflicts.
	/// If any UPDATEs are on tables with unique constraints, we need graph building
	/// to detect and handle swaps.
	///
	/// @param groups the UPDATE operation groups
	/// @return true if there are potential unique constraint conflicts
	private boolean hasUniqueConstraintConflicts(List<PlannedOperationGroup> groups) {
		// Check if any group operates on a table with unique constraints
		for (PlannedOperationGroup group : groups) {
			if ( group.hasUniqueConstraints() ) {
				// This table has unique constraints - need graph to detect swaps
				return true;
			}
		}
		return false;
	}

	/// Create a simple execution plan without graph building.
	/// All operation groups are independent, so we can execute them in a single step.
	///
	/// @param groups the operation groups
	/// @return a simple flush plan
	private FlushPlan createSimplePlan(List<PlannedOperationGroup> groups) {
		// Collect all operations from all groups, maintaining their order
		final List<PlannedOperation> allOperations = new ArrayList<>();
		for (PlannedOperationGroup group : groups) {
			allOperations.addAll(group.operations());
		}

		// Create a single step with all operations
		final PlanStep step = new SimplePlanStep(allOperations);
		return new FlushPlan(List.of(step));
	}

	/// Simple implementation of PlanStep for fast path execution.
	private static class SimplePlanStep implements PlanStep {
		private final List<PlannedOperation> operations;

		SimplePlanStep(List<PlannedOperation> operations) {
			this.operations = List.copyOf(operations);
		}

		@Override
		public List<PlannedOperation> operations() {
			return operations;
		}

	}

	/**
	 * Decompose executables from separate phases, preserving phase boundaries.
	 */
	private List<PlannedOperationGroup> decomposeExecutablePhases(
			List<CollectionRemoveAction> orphanCollectionRemovals,
			List<OrphanRemovalAction> orphanRemovals,
			List<AbstractEntityInsertAction> insertions,
			List<EntityUpdateAction> updates,
			List<QueuedOperationCollectionAction> collectionQueuedOps,
			List<CollectionRemoveAction> collectionRemovals,
			List<CollectionUpdateAction> collectionUpdates,
			List<CollectionRecreateAction> collectionCreations,
			List<EntityDeleteAction> deletions) {

		decomposer.beginFlush( insertions, deletions );

		final ArrayList<PlannedOperation> operations = new ArrayList<>();
		int ordinalBase = 0;

		// Decompose each phase separately, maintaining ordinal ordering
		ordinalBase = decomposePhase(orphanCollectionRemovals, ordinalBase, operations);
		ordinalBase = decomposePhase(orphanRemovals, ordinalBase, operations);
		ordinalBase = decomposePhase(insertions, ordinalBase, operations);
		ordinalBase = decomposePhase(updates, ordinalBase, operations);
		ordinalBase = decomposePhase(collectionQueuedOps, ordinalBase, operations);
		ordinalBase = decomposePhase(collectionRemovals, ordinalBase, operations);
		ordinalBase = decomposePhase(collectionUpdates, ordinalBase, operations);
		ordinalBase = decomposePhase(collectionCreations, ordinalBase, operations);
		ordinalBase = decomposePhase(deletions, ordinalBase, operations);

		decomposer.endFlush();

		// Group operations by shape
		return groupOperations(operations);
	}

	private int decomposePhase(List<? extends Executable> executables, int ordinalBase, List<PlannedOperation> operations) {
		for (Executable e : executables) {
			var ops = decomposer.decompose(e, ordinalBase++);
			operations.addAll(ops);
		}
		return ordinalBase;
	}

	/// Groups PlannedOperations by their StatementShapeKey (table + kind + SQL shape).
	///
	/// Uses a hybrid grouping strategy:
	/// - For tables with self-referential associations: groups are split by ordinalBase
	///   to avoid creating false cycles in the dependency graph
	/// - For other tables: operations from different entities are merged for better JDBC batching
	///
	/// This optimizes batching while preventing unnecessary cycle breaking and fixup UPDATEs
	/// for self-referential foreign keys.
	///
	/// Example with self-referential FK (person.parent_id → person.id):
	/// - Parent person (ordinalBase=0) and child person (ordinalBase=1) stay in separate groups
	/// - Graph has edge Group0→Group1 (no cycle, FK preserved in INSERT)
	/// - If merged: self-edge → cycle → null FK + fixup UPDATE (worse performance)
	///
	/// @param operations the raw operations from decomposition
	/// @return grouped operations
	private List<PlannedOperationGroup> groupOperations(List<PlannedOperation> operations) {
		if (operations.isEmpty()) {
			return List.of();
		}

		// Group operations by their shape, optionally including ordinalBase for self-referential tables
		// Also separate by cascade source to preserve cascade metadata fidelity
		final Map<OperationGroupKey, OperationGroupBuilder> builders = new LinkedHashMap<>();

		for (PlannedOperation operation : operations) {
			final StatementShapeKey shapeKey = operation.getShapeKey();

			// Build composite key considering self-referential tables
			// Self-referential tables need ordinalBase to avoid false cycles
			final OperationGroupKey key;
			if (operation.getMutatingTableDescriptor().isSelfReferential()) {
				key = new OrdinalAwareOperationGroupKey(
						shapeKey,
						extractCollectionOrdinal(operation.getOrdinal())
				);
			}
			else {
				// Non-self-referential: use simple shape key
				key = shapeKey;
			}

			var builder = builders.get(key);
			if (builder == null) {
				// First operation for this key - create new builder (which adds the operation in constructor)
				builder = new OperationGroupBuilder(operation, shapeKey);
				builders.put(key, builder);
			}
			else {
				// Subsequent operation for this key - add to existing builder
				builder.addOperation(operation);
			}
		}

		// Build groups
		final List<PlannedOperationGroup> groups = new ArrayList<>(builders.size());
		for (OperationGroupBuilder builder : builders.values()) {
			groups.add(builder.build());
		}

		return groups;
	}

	/// Helper class to build a PlannedOperationGroup from multiple PlannedOperations.
	/// When merging operations from multiple entities, tracks the minimum ordinal for proper ordering.
	private static class OperationGroupBuilder {
		private final String tableExpression;
		private final MutationKind kind;
		private final StatementShapeKey shapeKey;
		private int ordinal;
		private final String origin;
		private final boolean hasUniqueConstraints;
		private final List<PlannedOperation> operations = new ArrayList<>();

		OperationGroupBuilder(PlannedOperation firstOperation, StatementShapeKey shapeKey) {
			this.tableExpression = firstOperation.getTableExpression();
			this.kind = firstOperation.getKind();
			this.shapeKey = shapeKey;
			this.ordinal = firstOperation.getOrdinal();
			this.origin = firstOperation.getOrigin();
			this.operations.add(firstOperation);

			hasUniqueConstraints = firstOperation.getMutatingTableDescriptor().hasUniqueConstraints();
		}

		void addOperation(PlannedOperation op) {
			this.operations.add(op);
			// When merging operations from different entities, use the minimum ordinal
			// to ensure the group executes at the earliest required point
			this.ordinal = Math.min(this.ordinal, op.getOrdinal());
		}

		PlannedOperationGroup build() {
			// Compute group's needsIdPrePhase as true if ANY operation needs it
			final boolean needsIdPrePhase = operations.stream()
					.anyMatch(PlannedOperation::needsIdPrePhase);

			return new PlannedOperationGroup(
					tableExpression,
					kind,
					shapeKey,
					operations,
					needsIdPrePhase,
					hasUniqueConstraints,
					ordinal,
					origin
			);
		}
	}

	/// Executes planned operations in order. Handles fixups emitted from cycle breaks.
	private void executePlan(FlushPlan plan) {
		for ( PlanStep step : plan.steps() ) {
			executeStep(step, plan);
		}

		// Execute all fixups after all regular operations
		final List<PlannedOperation> fixups = plan.drainFixupsInOrder();
		if (!fixups.isEmpty()) {
			executor.execute( fixups, null, null );
		}
	}

	private void executeStep(PlanStep step, FlushPlan plan) {
		executor.execute(
				step.operations(),
				newlyManagedEntities::add,
				plan::enqueueFixup
		);
	}

	/// After executing a flush, check if any unresolved inserts can now be resolved.
	/// This handles the case where an entity is persisted after another entity that references it.
	private void resolveUnresolvedInserts() {
		if (!decomposer.hasUnresolvedInserts()) {
			return;
		}

		// Try to resolve inserts for each entity that became managed
		for (Object managedEntity : newlyManagedEntities) {
			final List<PlannedOperation> resolvedOperations = decomposer.resolveAndDecompose(managedEntity);

			if (!resolvedOperations.isEmpty()) {
				// Group resolved operations and recursively flush
				final var resolvedGroups = groupOperations(resolvedOperations);
				final var graph = graphBuilder.build(resolvedGroups);
				final var plan = flushPlanner.plan(graph);
				executePlan(plan);

				// After recursive execution, try again (might have resolved more dependencies)
				resolveUnresolvedInserts();
				return;
			}
		}

		executor.finishUp();

		// Clear the list for the next flush
		newlyManagedEntities.clear();
	}

	/// Composite key for grouping operations by shape and ordinalBase.
	private record CompositeOperationGroupKey(
			StatementShapeKey shapeKey,
			int ordinalBase) implements OperationGroupKey {
	}

	/**
	 * Operation group key that includes ordinal information for self-referential tables.
	 */
	private record OrdinalAwareOperationGroupKey(
			StatementShapeKey shapeKey,
			Integer ordinalBase) implements OperationGroupKey {
	}


	public void serialize(ObjectOutputStream oos) throws IOException {
		decomposer.serialize(oos);
	}

	public static FlushCoordinator deserialize(
			ObjectInputStream ois,
			GraphBasedActionQueueFactory actionQueueFactory,
			SessionImplementor session) throws IOException, ClassNotFoundException {
		var decomposer = Decomposer.deserialize(  ois, actionQueueFactory, session );
		return new FlushCoordinator( decomposer, actionQueueFactory, session );
	}

	/// Deserialization constructor.
	/// @see GraphBasedActionQueue#deserialize(ObjectInputStream, org.hibernate.action.queue.support.GraphBasedActionQueueFactory, SessionImplementor)
	public FlushCoordinator(
			Decomposer decomposer,
			GraphBasedActionQueueFactory actionQueueFactory,
			SessionImplementor session) {
		this.constraintModel = actionQueueFactory.getConstraintModel();
		planningOptions = actionQueueFactory.getPlanningOptions();
		this.decomposer = decomposer;
		this.session = session;

		graphBuilder = new StandardGraphBuilder( actionQueueFactory.getConstraintModel(), actionQueueFactory.getPlanningOptions(), session );
		flushPlanner = new StandardFlushPlanner( actionQueueFactory.getPlanningOptions() );
		executor = PlanStepExecutorFactory.create( session );
	}
}
