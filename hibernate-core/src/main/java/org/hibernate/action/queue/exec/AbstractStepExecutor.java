/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.exec;

import org.hibernate.action.queue.MutationKind;
import org.hibernate.action.queue.bind.JdbcValueBindings;
import org.hibernate.action.queue.cyclebreak.FixupSynthesizer;
import org.hibernate.action.queue.plan.FlushOperation;
import org.hibernate.engine.jdbc.mutation.internal.JdbcValueBindingsImpl;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.generator.values.GeneratedValuesMutationDelegate;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.sql.model.MutationOperation;
import org.hibernate.sql.model.PreparableMutationOperation;
import org.hibernate.sql.model.SelfExecutingUpdateOperation;

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
			List<FlushOperation> flushOperations,
			Consumer<Object> newlyManagedEntityConsumer,
			Consumer<FlushOperation> fixupOperationConsumer) {
		// todo : not a fan of this overall, but it largely fits the expectations of tests.

		// Grab a reference to the physical connection to ensure it's acquired before execution begins.
		// The connection will be maintained throughout the flush via the flush lifecycle
		// (flushBeginning/flushEnding), which disables aggressive connection release.
		// Individual statements are released from the resource registry as they complete,
		// but the connection itself is retained.
		var physicalConnection = session.getJdbcCoordinator().getLogicalConnection().getPhysicalConnection();
		doExecution(
				physicalConnection,
				flushOperations,
				newlyManagedEntityConsumer,
				fixupOperationConsumer
		);
	}

	/// There are 4 forms of FlushOperation handled here -
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
			List<FlushOperation> flushOperations,
			Consumer<Object> newlyManagedEntityConsumer,
			Consumer<FlushOperation> fixupOperationConsumer) {
		for ( int i = 0; i < flushOperations.size(); i++ ) {
			var flushOperation = flushOperations.get( i );
			final boolean execute = beforeOperationExecution( flushOperation );

			// No-op operations: only carry post-execution callback, skip SQL execution
			if ( flushOperation.getKind() != MutationKind.NO_OP && execute ) {
				final var bindPlan = flushOperation.getBindPlan();
				if ( bindPlan.getGeneratedValuesCollector() != null ) {
					// we need to execute these without batching
					executeWithGeneratedValues( flushOperation );
				}
				else {
					final MutationOperation jdbcOperation = flushOperation.getJdbcOperation();
					if ( jdbcOperation instanceof PreparableMutationOperation preparable ) {
						executePreparable( preparable, flushOperation );
					}
					else if ( jdbcOperation instanceof SelfExecutingUpdateOperation selfExecuting ) {
						executeSelfExecuting( selfExecuting, flushOperation );
					}
					else {
						throw new IllegalStateException(
								"Unsupported JdbcOperation type: " + jdbcOperation.getClass().getName() );
					}
				}
			}

			afterOperationExecution( flushOperation, newlyManagedEntityConsumer, fixupOperationConsumer );
		}
	}

	protected boolean beforeOperationExecution(FlushOperation flushOperation) {
		final var preExecutionCallback = flushOperation.getPreExecutionCallback();
		if ( preExecutionCallback == null ) {
			return true;
		}
		final boolean execute = preExecutionCallback.beforeExecution( (org.hibernate.engine.spi.SessionImplementor) session );
		flushOperation.setExecutionSkipped( !execute );
		return execute;
	}

	protected void afterOperationExecution(
			FlushOperation flushOperation,
			Consumer<Object> newlyManagedEntityConsumer,
			Consumer<FlushOperation> fixupOperationConsumer) {
		if ( flushOperation.getPostExecutionCallback() != null ) {
			flushOperation.getPostExecutionCallback().handle( (org.hibernate.engine.spi.SessionImplementor) session );
		}

		if ( newlyManagedEntityConsumer != null ) {
			if ( flushOperation.getKind() == MutationKind.INSERT ) {
				final Object entity = flushOperation.getBindPlan().getEntityInstance();
				if ( entity != null ) {
					newlyManagedEntityConsumer.accept( entity );
				}
			}
		}

		if ( fixupOperationConsumer != null ) {
			// If this op was cycle-broken, the patcher stored intended FK values in op.intendedFkValues.
			if ( !flushOperation.getIntendedFkValues().isEmpty() ) {
				final Object entityId = flushOperation.getBindPlan().getEntityId();

				final FlushOperation fix = fixupSynthesizer.synthesizeFixupOperationIfNeeded(
						flushOperation,
						entityId,
						session
				);
				if ( fix != null ) {
					fixupOperationConsumer.accept( fix );
				}
			}

			// If this op was cycle-broken for unique swap, the patcher stored intended values in op.intendedUniqueValues.
			if ( !flushOperation.getIntendedUniqueValues().isEmpty() ) {
				final Object entityId = flushOperation.getBindPlan().getEntityId();

				final FlushOperation fix = fixupSynthesizer.synthesizeFixupOperationIfNeeded(
						flushOperation,
						entityId,
						session
				);
				if ( fix != null ) {
					fixupOperationConsumer.accept( fix );
				}
			}
		}
	}

	protected abstract void executePreparable(PreparableMutationOperation preparable, FlushOperation flushOperation);

	protected void executeWithGeneratedValues(FlushOperation flushOperation) {
		var bindPlan = flushOperation.getBindPlan();
		var generatedValuesCollector = bindPlan.getGeneratedValuesCollector();

		final GeneratedValuesMutationDelegate generatedValuesDelegate;
		var mutationTarget = flushOperation.getJdbcOperation().getMutationTarget();
		if ( mutationTarget instanceof EntityPersister entityPersister ) {
			generatedValuesDelegate = flushOperation.getKind() == MutationKind.INSERT
					? entityPersister.getInsertDelegate()
					: entityPersister.getUpdateDelegate();
		}
		else {
			generatedValuesDelegate = null;
		}

		var generatedValues = generatedValuesDelegate.performGraphMutation(
				flushOperation,
				bindPlan.getEntityInstance(),
				session
		);

		generatedValuesCollector.apply( generatedValues );
	}

	protected void executeSelfExecuting(SelfExecutingUpdateOperation selfExecuting, FlushOperation flushOperation) {
		final var graphBindings = new JdbcValueBindings(
				flushOperation.getMutatingTableDescriptor(),
				selfExecuting
		);
		flushOperation.getBindPlan().bindValues( graphBindings, flushOperation, session );

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
				flushOperation.getBindPlan().getValuesAnalysis(),
				session
		);
	}

	@Override
	public void finishUp() {
		// nothing to do by default
	}

}
