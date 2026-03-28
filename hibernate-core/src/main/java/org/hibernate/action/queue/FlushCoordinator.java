/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue;

import org.hibernate.HibernateException;
import org.hibernate.action.internal.AbstractEntityInsertAction;
import org.hibernate.action.queue.constraint.ConstraintModel;
import org.hibernate.action.queue.exec.PlanStepExecutor;
import org.hibernate.action.queue.exec.PlanStepExecutorFactory;
import org.hibernate.action.queue.exec.PostExecutionCallback;
import org.hibernate.action.queue.graph.Decomposer;
import org.hibernate.action.queue.graph.GraphBuilder;
import org.hibernate.action.queue.graph.StandardGraphBuilder;
import org.hibernate.action.queue.plan.PlannedOperation;
import org.hibernate.action.queue.plan.FlushPlan;
import org.hibernate.action.queue.plan.FlushPlanner;
import org.hibernate.action.queue.plan.PlanStep;
import org.hibernate.action.queue.plan.PlannedOperationGroup;
import org.hibernate.action.queue.plan.StandardFlushPlanner;
import org.hibernate.action.queue.support.GraphBasedActionQueueFactory;
import org.hibernate.action.spi.Executable;
import org.hibernate.engine.spi.SessionImplementor;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.hibernate.internal.util.collections.CollectionHelper.arrayList;

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
	private final SessionImplementor session;
	private final GraphBuilder graphBuilder;

	private final Decomposer decomposer;
	private final FlushPlanner flushPlanner;
	private final PlanStepExecutor executor;

	// Track entities that became managed during the current flush
	private final List<Object> newlyManagedEntities = new ArrayList<>();

	// Tables involved in self-referential associations (need ordinalBase separation to avoid false cycles)
	private final java.util.Set<String> selfReferentialTables;

	public FlushCoordinator(ConstraintModel constraintModel, PlanningOptions planningOptions, SessionImplementor session) {
		this.session = session;

		// Identify tables with self-referential associations
		selfReferentialTables = identifySelfReferentialTables(constraintModel);

		decomposer = new Decomposer( session );
		graphBuilder = new StandardGraphBuilder( constraintModel, planningOptions, session );
		flushPlanner = new StandardFlushPlanner( planningOptions );
		executor = PlanStepExecutorFactory.create( session );
	}

	/// Deserialization constructor.
	/// @see GraphBasedActionQueue#deserialize(ObjectInputStream, org.hibernate.action.queue.support.GraphBasedActionQueueFactory, SessionImplementor)
	public FlushCoordinator(
			Decomposer decomposer,
			GraphBasedActionQueueFactory actionQueueFactory,
			SessionImplementor session) {
		this.session = session;
		this.decomposer = decomposer;

		// Identify tables with self-referential associations
		selfReferentialTables = identifySelfReferentialTables(actionQueueFactory.getConstraintModel());

		graphBuilder = new StandardGraphBuilder( actionQueueFactory.getConstraintModel(), actionQueueFactory.getPlanningOptions(), session );
		flushPlanner = new StandardFlushPlanner( actionQueueFactory.getPlanningOptions() );
		executor = PlanStepExecutorFactory.create( session );
	}


	/// Identifies tables that have self-referential associations (FK from table to itself).
	/// These tables need special grouping treatment to avoid creating false cycles in the graph.
	private java.util.Set<String> identifySelfReferentialTables(ConstraintModel constraintModel) {
		final java.util.Set<String> tables = new java.util.HashSet<>();
		for (var fk : constraintModel.foreignKeys()) {
			if (fk.isAssociation()) {
				String keyTable = (fk.keyTable());
				String targetTable = (fk.targetTable());
				if (keyTable.equals(targetTable)) {
					tables.add(keyTable);
				}
			}
		}
		return tables;
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

	public void executeFlush(List<? extends Executable> actions) {
		if ( actions.isEmpty() ) {
			return;
		}

		var operationGroups = decomposeExecutables( actions );
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
		var graph = graphBuilder.build( operationGroups );
		var plan = flushPlanner.plan( graph );

		// Execute the plan - post-execution callbacks will run inline as operations complete
		executePlan( plan );

		executor.finishUp();

		// After execution, try to resolve any deferred inserts
		resolveUnresolvedInserts();

		// Final validation - any remaining unresolved inserts are an error
		decomposer.validateNoUnresolvedInserts();
	}


	private List<PlannedOperationGroup> decomposeExecutables(
			List<? extends Executable> executables) {
		final ArrayList<PlannedOperation> operations = arrayList( executables.size() * 2);
		int ordinalBase = 0;
		for (Executable e : executables) {
			var ops = decomposer.decompose( e, ordinalBase++ );
			operations.addAll( ops );
		}
		// Group operations by shape
		return groupOperations(operations);
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
		final Map<OperationGroupKey, OperationGroupBuilder> builders = new LinkedHashMap<>();

		// Cache: avoid repeated lookups of selfReferentialTables for same table
		// In batch workloads, many operations target the same table(s)
		final Map<String, Boolean> isSelfReferential = new java.util.HashMap<>();

		for (PlannedOperation operation : operations) {
			final StatementShapeKey shapeKey = operation.getShapeKey();
			final String normalizedTable = (operation.getTableExpression());

			// For self-referential tables, include ordinalBase to avoid false cycles
			// For other tables, merge across entities for better batching
			// Cache the lookup result to avoid redundant set.contains() calls
			final boolean selfRef = isSelfReferential.computeIfAbsent(
					normalizedTable,
					selfReferentialTables::contains
			);

			final int ordinalBase = selfRef
					? CollectionOrdinalSupport.extractCollectionOrdinal( operation.getOrdinal() )
					: -1; // -1 means "merge all ordinalBases together"

			final OperationGroupKey key = new OperationGroupKey(shapeKey, ordinalBase);
			var builder = builders.get(key);
			if (builder == null) {
				// First operation for this key - create new builder (which adds the operation in constructor)
				builder = new OperationGroupBuilder(operation, shapeKey);
				builders.put(key, builder);
			} else {
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
		private final List<PlannedOperation> operations = new ArrayList<>();

		OperationGroupBuilder(PlannedOperation firstOperation, StatementShapeKey shapeKey) {
			this.tableExpression = firstOperation.getTableExpression();
			this.kind = firstOperation.getKind();
			this.shapeKey = shapeKey;
			this.ordinal = firstOperation.getOrdinal();
			this.origin = firstOperation.getOrigin();
			this.operations.add(firstOperation);
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
					ordinal,
					origin
			);
		}
	}

	/// Executes planned operations in order. Handles fixups emitted from cycle breaks.
	/// Policy here:
	///  - run all base steps first
	///  - enqueue fixups as we go
	///  - run fixups after base plan finishes (simple + safe)
	private void executePlan(FlushPlan plan) {
		for ( PlanStep step : plan.steps() ) {
			executeStep(step, plan);
		}

		// Execute deferred FK fixups
		// todo: worth it to group these by shape?
		executor.execute( plan.drainFixupsInOrder(), null, null );
	}

	private void executeStep(PlanStep step, FlushPlan plan) {
		executor.execute(
				step.operations(),
				newlyManagedEntities::add,
				plan::enqueueFixup
		);
	}

	/// Post-execution phase: finalize actions after all their PlannedOperations have executed.
	/// All finalization work is handled by the post-execution callbacks.
	///
	/// @param actions The original list of Executable actions (unused - kept for potential future use)
	/// @param callbacks The callbacks to be triggered
	private void afterAllOperationsExecuted(List<Executable> actions, List<PostExecutionCallback> callbacks) {
		// Execute all post-execution callbacks - these handle ALL finalization:
		// - PostInsertHandling: handles all insert finalization (values, cache, events, stats)
		// - PostUpdateHandling: handles all update finalization (values, version, cache, events, stats)
		callbacks.forEach(callback -> {
			callback.handle( session );
		} );

		// Note: All entity action finalization is now handled by callbacks.
		// This method remains for potential future use with action types that don't use callbacks.
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
	private static class OperationGroupKey {
		private final StatementShapeKey shapeKey;
		private final int ordinalBase;

		OperationGroupKey(StatementShapeKey shapeKey, int ordinalBase) {
			this.shapeKey = shapeKey;
			this.ordinalBase = ordinalBase;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (!(o instanceof OperationGroupKey)) return false;
			OperationGroupKey that = (OperationGroupKey) o;
			return ordinalBase == that.ordinalBase && shapeKey.equals(that.shapeKey);
		}

		@Override
		public int hashCode() {
			return 31 * shapeKey.hashCode() + ordinalBase;
		}
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
}
