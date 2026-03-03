/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue;

import org.hibernate.action.queue.exec.PlannedOperationExecutor;
import org.hibernate.action.queue.exec.StandardPlannedOperationExecutor;
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
import java.util.function.Supplier;

import static org.hibernate.internal.util.collections.CollectionHelper.arrayList;

/**
 * Sort of temporary reenvisioning of ActionQueue using this new approach.
 *
 * @author Steve Ebersole
 */
public class FlushCoordinator {
	private final GraphBuilder graphBuilder;

	private final Decomposer decomposer;
	private final FlushPlanner flushPlanner;
	private final PlannedOperationExecutor executor;

	public FlushCoordinator(SessionImplementor session) {
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

	public void executeFlush(List<Executable> actions) {
		if ( actions.isEmpty() ) {
			return;
		}

		var operationGroups = decomposeExecutables( actions );
		if ( operationGroups.isEmpty() ) {
			return;
		}

		var graph = graphBuilder.build( operationGroups );

		var plan = flushPlanner.plan( graph );
		executePlan( plan );
	}

	private List<PlannedOperationGroup> decomposeExecutables(List<Executable> executables) {
		final ArrayList<PlannedOperationGroup> out = arrayList( executables.size() * 2);
		int ordinalBase = 0;
		for (Executable e : executables) {
			out.addAll( decomposer.decompose( e, ordinalBase++ ) );
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
		for ( FlushPlan.PlanStep step : plan.steps() ) {
			executeStep(step, plan);
		}

		// Execute deferred FK fixups
		for (PlannedOperation fixup : plan.drainFixupsInOrder()) {
			executor.executePlannedOperation( fixup );
		}
	}

	private void executeStep(FlushPlan.PlanStep step, FlushPlan plan) {
		for (PlannedOperation op : step.ops()) {
			executor.executePlannedOperation(op);

			// If this op was cycle-broken, the patcher stored intended FK values in op.intendedFkValues.
			if (!op.getIntendedFkValues().isEmpty()) {
				final Object entityId = op.getBindPlan().getEntityIdAccess().get();

				final PlannedOperation fix = executor.synthesizeFixupUpdateIfNeeded( op, entityId);
				if (fix != null) {
					plan.enqueueFixup(fix, step);
				}
			}
		}
	}

	private Object determineEntityId(PlannedOperation op) {
		final Supplier<Object> entityIdAccess = op.getBindPlan().getEntityIdAccess();
		Object id =  entityIdAccess == null ? null : entityIdAccess.get();
		if (id == null) {
			throw new IllegalStateException(
					"Cycle-break fixup requires entity id, but BindPlan returned null. " +
					"Ensure INSERT has executed and the bind plan is backed by EntityInsertAction."
			);
		}
		return id;

	}
}
