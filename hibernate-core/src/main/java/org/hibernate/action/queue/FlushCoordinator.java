/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue;

import org.hibernate.action.queue.exec.PlannedOperationExecutor;
import org.hibernate.action.queue.exec.PostExecutionCallback;
import org.hibernate.action.queue.exec.StandardPlannedOperationExecutor;
import org.hibernate.action.queue.plan.PlanStep;
import org.hibernate.action.queue.plan.PlannedOperation;
import org.hibernate.action.queue.plan.PlannedOperationGroup;
import org.hibernate.action.queue.fk.ForeignKeyModel;
import org.hibernate.action.queue.graph.Decomposer;
import org.hibernate.action.queue.graph.GraphBuilder;
import org.hibernate.action.queue.graph.StandardGraphBuilder;
import org.hibernate.action.queue.plan.FlushPlan;
import org.hibernate.action.queue.plan.FlushPlanner;
import org.hibernate.action.queue.plan.StandardFlushPlanner;
import org.hibernate.action.spi.Executable;
import org.hibernate.engine.spi.SessionImplementor;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static org.hibernate.internal.util.collections.CollectionHelper.arrayList;

/// Orchestrates the steps needed to flush a Session, using a graph-based approach to
/// model mutation scheduling that automatically handles foreign key dependencies,
/// cycle detection, and proper execution ordering.
///
/// As it coordinates all phases of flush execution, its responsibilities include -
///
/// - Manage decomposition → graph → planning → execution flow
/// - Tracks newly managed entities for unresolved insert resolution
/// - Handles recursive flush for resolved inserts
/// - Validates no unresolved inserts remain
/// - Manages post-execution callbacks
///
/// An important input to this process is the new [ForeignKeyModel] which describes
/// details about all foreign keys defined by the domain model.  Flush coordination
/// leverages this at many points.
///
/// The basic flow is initiated by a call to [#executeFlush(List)] which is passed a
/// list of [actions][Executable].
///
/// - First, these actions (EntityInsertAction, etc.) are decomposed into low-level
/// 	[PlannedOperation] references by the [Decomposer] delegate.
/// - The [PlannedOperation] references are then arranged into a directed dependency `Graph`
/// 	(using the [ForeignKeyModel]) by [GraphBuilder].
/// - [FlushPlanner] then creates an executable plan from the dependency `Graph`..
///
/// Some important concepts for this coordination include -
///
/// - `PlannedOperation` - a single operation against a single table
/// - `PlannedOperationGroup` - multiple operations (of the same kind) against a single table
/// -
///
/// @author Steve Ebersole
public class FlushCoordinator {
	private final SessionImplementor session;
	private final GraphBuilder graphBuilder;

	private final Decomposer decomposer;
	private final FlushPlanner flushPlanner;
	private final PlannedOperationExecutor executor;

	// Track entities that became managed during the current flush
	private final List<Object> newlyManagedEntities = new ArrayList<>();

	public FlushCoordinator(SessionImplementor session) {
		this.session = session;

		// for now
		boolean avoidBreakingDeferrable = false;
		boolean ignoreDeferrableForOrdering = true;

		ForeignKeyModel foreignKeyModel = session.getFactory().getForeignKeyModel();

		decomposer = new Decomposer( session );
		graphBuilder = new StandardGraphBuilder(
				foreignKeyModel,
				avoidBreakingDeferrable,
				ignoreDeferrableForOrdering
		);
		flushPlanner = new StandardFlushPlanner();
		executor = new StandardPlannedOperationExecutor(
				null,
				!ignoreDeferrableForOrdering,
				session
		);
	}

	/**
	 * Get the Decomposer (for accessing unresolved insert tracking).
	 *
	 * @return the decomposer
	 */
	public Decomposer getDecomposer() {
		return decomposer;
	}

	public void executeFlush(List<Executable> actions) {
		if ( actions.isEmpty() ) {
			return;
		}

		final List<PostExecutionCallback> postExecutionCallbacks = new ArrayList<>();

		var operationGroups = decomposeExecutables( actions, postExecutionCallbacks::add );
		if ( operationGroups.isEmpty() ) {
			// No operations decomposed - check if we have unresolved inserts that can never resolve
			decomposer.validateNoUnresolvedInserts();
			return;
		}

		var graph = graphBuilder.build( operationGroups );

		var plan = flushPlanner.plan( graph );

		try {
			executePlan( plan );
			// Post-execution phase: finalize actions after all their operations completed
			afterAllOperationsExecuted( actions, postExecutionCallbacks );
		}
		finally {
			postExecutionCallbacks.clear();
		}

		// After execution, try to resolve any deferred inserts
		resolveUnresolvedInserts();

		// Final validation - any remaining unresolved inserts are an error
		decomposer.validateNoUnresolvedInserts();
	}

	private List<PlannedOperationGroup> decomposeExecutables(
			List<Executable> executables,
			Consumer<PostExecutionCallback> postExecCallbackRegistry) {
		final ArrayList<PlannedOperationGroup> out = arrayList( executables.size() * 2);
		int ordinalBase = 0;
		for (Executable e : executables) {
			var operationGroups = decomposer.decompose( e, ordinalBase++, postExecCallbackRegistry );
			out.addAll( operationGroups );
		}
		return out;
	}

	/**
	 * Executes planned operations in order. Handles fixups emitted from cycle breaks.
	 *
	 * Policy here:
	 *  - run all base steps first
	 *  - enqueue fixups as we go
	 *  - run fixups after base plan finishes (simple + safe)
	 */
	private void executePlan(FlushPlan plan) {
		for ( PlanStep step : plan.steps() ) {
			executeStep(step, plan);
		}

		// Execute deferred FK fixups
		for (PlannedOperation fixup : plan.drainFixupsInOrder()) {
			executor.executePlannedOperation( fixup );
		}
	}

	private void executeStep(PlanStep step, FlushPlan plan) {
		for (PlannedOperation op : step.operations()) {
			executor.executePlannedOperation(op);

			// Track entities that became managed (for unresolved insert resolution)
			if (op.getKind() == MutationKind.INSERT) {
				final Object entity = op.getBindPlan().getEntityInstance();
				if (entity != null) {
					newlyManagedEntities.add(entity);
				}
			}

			// If this op was cycle-broken, the patcher stored intended FK values in op.intendedFkValues.
			if (!op.getIntendedFkValues().isEmpty()) {
				final Object entityId = op.getBindPlan().getEntityId();

				final PlannedOperation fix = executor.synthesizeFixupUpdateIfNeeded( op, entityId);
				if (fix != null) {
					plan.enqueueFixup(fix, step);
				}
			}
		}
	}

	/**
	 * Post-execution phase: finalize actions after all their PlannedOperations have executed.
	 * All finalization work is handled by the post-execution callbacks.
	 *
	 * @param actions The original list of Executable actions (unused - kept for potential future use)
	 * @param callbacks The callbacks to be triggered
	 */
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

	/**
	 * After executing a flush, check if any unresolved inserts can now be resolved.
	 * This handles the case where an entity is persisted after another entity that references it.
	 */
	private void resolveUnresolvedInserts() {
		if (!decomposer.hasUnresolvedInserts()) {
			return;
		}

		// Try to resolve inserts for each entity that became managed
		for (Object managedEntity : newlyManagedEntities) {
			final List<PlannedOperationGroup> resolvedGroups = decomposer.resolveAndDecompose(managedEntity);

			if (!resolvedGroups.isEmpty()) {
				// Recursively flush resolved operations
				final var graph = graphBuilder.build(resolvedGroups);
				final var plan = flushPlanner.plan(graph);
				executePlan(plan);

				// After recursive execution, try again (might have resolved more dependencies)
				resolveUnresolvedInserts();
				return;
			}
		}

		// Clear the list for the next flush
		newlyManagedEntities.clear();
	}

}
