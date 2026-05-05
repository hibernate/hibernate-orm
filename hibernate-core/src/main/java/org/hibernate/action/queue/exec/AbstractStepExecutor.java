/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.exec;

import org.hibernate.action.queue.MutationKind;
import org.hibernate.action.queue.cyclebreak.FixupSynthesizer;
import org.hibernate.action.queue.plan.PlannedOperation;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.generator.values.GeneratedValuesMutationDelegate;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.sql.model.MutationOperation;
import org.hibernate.sql.model.PreparableMutationOperation;
import org.hibernate.sql.model.SelfExecutingUpdateOperation;
import org.hibernate.engine.jdbc.mutation.internal.JdbcValueBindingsImpl;

import java.sql.Connection;
import java.util.List;
import java.util.function.Consumer;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractStepExecutor implements PlanStepExecutor {
	private final FixupSynthesizer fixupSynthesizer;

	protected final SharedSessionContractImplementor session;

	public AbstractStepExecutor(SharedSessionContractImplementor session) {
		this.session = session;

		fixupSynthesizer = new FixupSynthesizer();
	}

	@Override
	public void execute(
			List<PlannedOperation> plannedOperations,
			Consumer<Object> newlyManagedEntityConsumer,
			Consumer<PlannedOperation> fixupOperationConsumer) {
		// todo : not a fan of this overall, but it largely fits the expectations of tests.

		// Grab a reference to the physical connection to ensure it's acquired before execution begins.
		// The connection will be maintained throughout the flush via the flush lifecycle
		// (flushBeginning/flushEnding), which disables aggressive connection release.
		// Individual statements are released from the resource registry as they complete
		// (see StandardPlanStepExecutor.executeRow()), but the connection itself is retained.
		var physicalConnection = session.getJdbcCoordinator().getLogicalConnection().getPhysicalConnection();
		doExecution(
				physicalConnection,
				plannedOperations,
				newlyManagedEntityConsumer,
				fixupOperationConsumer
		);
	}

	/// There are 4 forms of PlannedOperation handled here -
	/// 	1. Operations with generated value handling.  At the moment these are handled separately to
	/// 		integrate the older delegate contract to allow new and legacy queue impls to continue
	/// 		working side-by-side.  I'd like to adjust this after we remove the legacy queue as we can
	/// 		very easily handle that here.
	/// 	2. Self-executing operations which are basically specialized handling for optional tables with
	/// 		databases which do not support proper SQL MERGE semantics.  This allows potential execution of
	/// 		multiple SQL statements.
	/// 	3. Standard preparable operations.  These are operations which can be handled via the normal
	/// 		"prepare, bind, execute" pattern.  These can also be potentially batched if batching is enabled.
	/// 	4. "No op" operations are a specialized to simply carry "post execution" callbacks;
	/// 		used from collection decomposers.
	private void doExecution(
			Connection physicalConnection,
			List<PlannedOperation> plannedOperations,
			Consumer<Object> newlyManagedEntityConsumer,
			Consumer<PlannedOperation> fixupOperationConsumer) {
		for ( int i = 0; i < plannedOperations.size(); i++ ) {
			var plannedOperation = plannedOperations.get( i );
			final boolean execute = beforeOperationExecution( plannedOperation );

			// No-op operations: only carry post-execution callback, skip SQL execution
			if ( plannedOperation.getKind() != MutationKind.NO_OP && execute ) {
				final var bindPlan = plannedOperation.getBindPlan();
				if ( bindPlan.getGeneratedValuesCollector() != null ) {
					// we need to execute these without batching
					executeWithGeneratedValues( plannedOperation );
				}
				else {
					final MutationOperation jdbcOperation = plannedOperation.getJdbcOperation();
					if ( jdbcOperation instanceof PreparableMutationOperation preparable ) {
						executePreparable( preparable, plannedOperation );
					}
					else if ( jdbcOperation instanceof SelfExecutingUpdateOperation selfExecuting ) {
						executeSelfExecuting( selfExecuting, plannedOperation );
					}
					else {
						throw new IllegalStateException(
								"Unsupported JdbcOperation type: " + jdbcOperation.getClass().getName() );
					}
				}
			}

			afterOperationExecution( plannedOperation, newlyManagedEntityConsumer, fixupOperationConsumer );
		}
	}

	protected boolean beforeOperationExecution(PlannedOperation plannedOperation) {
		final var preExecutionCallback = plannedOperation.getPreExecutionCallback();
		if ( preExecutionCallback == null ) {
			return true;
		}
		final boolean execute = preExecutionCallback.beforeExecution( (org.hibernate.engine.spi.SessionImplementor) session );
		plannedOperation.setExecutionSkipped( !execute );
		return execute;
	}

	protected void afterOperationExecution(
			PlannedOperation plannedOperation,
			Consumer<Object> newlyManagedEntityConsumer,
			Consumer<PlannedOperation> fixupOperationConsumer) {
		if ( plannedOperation.getPostExecutionCallback() != null ) {
			plannedOperation.getPostExecutionCallback().handle( (org.hibernate.engine.spi.SessionImplementor) session );
		}

		if ( newlyManagedEntityConsumer != null ) {
			if ( plannedOperation.getKind() == MutationKind.INSERT ) {
				final Object entity = plannedOperation.getBindPlan().getEntityInstance();
				if ( entity != null ) {
					newlyManagedEntityConsumer.accept( entity );
				}
			}
		}

		if ( fixupOperationConsumer != null ) {
			// If this op was cycle-broken, the patcher stored intended FK values in op.intendedFkValues.
			if ( !plannedOperation.getIntendedFkValues().isEmpty() ) {
				final Object entityId = plannedOperation.getBindPlan().getEntityId();

				final PlannedOperation fix = fixupSynthesizer.synthesizeFixupOperationIfNeeded(
						plannedOperation,
						entityId,
						session
				);
				if ( fix != null ) {
					fixupOperationConsumer.accept( fix );
				}
			}

			// If this op was cycle-broken for unique swap, the patcher stored intended values in op.intendedUniqueValues.
			if ( !plannedOperation.getIntendedUniqueValues().isEmpty() ) {
				final Object entityId = plannedOperation.getBindPlan().getEntityId();

				final PlannedOperation fix = fixupSynthesizer.synthesizeFixupOperationIfNeeded(
						plannedOperation,
						entityId,
						session
				);
				if ( fix != null ) {
					fixupOperationConsumer.accept( fix );
				}
			}
		}
	}

	protected abstract void executePreparable(PreparableMutationOperation preparable, PlannedOperation plannedOperation);

	protected void executeWithGeneratedValues(PlannedOperation plannedOperation) {
		var bindPlan = plannedOperation.getBindPlan();
		var generatedValuesCollector = bindPlan.getGeneratedValuesCollector();

		final GeneratedValuesMutationDelegate generatedValuesDelegate;
		var mutationTarget = plannedOperation.getJdbcOperation().getMutationTarget();
		if ( mutationTarget instanceof EntityPersister entityPersister ) {
			generatedValuesDelegate = plannedOperation.getKind() == MutationKind.INSERT
					? entityPersister.getInsertDelegate()
					: entityPersister.getUpdateDelegate();
		}
		else {
			generatedValuesDelegate = null;
		}

		var generatedValues = generatedValuesDelegate.performGraphMutation(
				plannedOperation,
				bindPlan.getEntityInstance(),
				session
		);

		generatedValuesCollector.apply( generatedValues );
	}

	protected void executeSelfExecuting(SelfExecutingUpdateOperation selfExecuting, PlannedOperation plannedOperation) {
		final var graphBindings = new JdbcValueBindings(
				plannedOperation.getMutatingTableDescriptor(),
				selfExecuting
		);
		plannedOperation.getBindPlan().execute(
				(operation, binder, resultChecker) -> binder.accept( graphBindings, session ),
				plannedOperation,
				session
		);

		final var jdbcValueBindings = new JdbcValueBindingsImpl(
				selfExecuting.getMutationType(),
				selfExecuting.getMutationTarget(),
				selfExecuting,
				session
		);
		graphBindings.getBindingGroup().forEachBinding( binding ->
				jdbcValueBindings.bindValue(
						binding.getValue(),
						graphBindings.getBindingGroup().getTableName(),
						binding.getColumnName(),
						binding.getValueDescriptor().getUsage()
				)
		);

		selfExecuting.performMutation(
				jdbcValueBindings,
				plannedOperation.getBindPlan().getValuesAnalysis(),
				session
		);
	}

	@Override
	public void finishUp() {
		// nothing to do by default
	}

}
