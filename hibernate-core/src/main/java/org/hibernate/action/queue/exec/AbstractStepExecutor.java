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

		// grab a reference to the physical connection and hold it for the flush processing
		var physicalConnection = session.getJdbcCoordinator().getLogicalConnection().getPhysicalConnection();
		try {
			doExecution(
					physicalConnection,
					plannedOperations,
					newlyManagedEntityConsumer,
					fixupOperationConsumer
			);
		}
		finally {
			session.getJdbcCoordinator().getLogicalConnection().afterStatement();
		}
	}

	private void doExecution(Connection physicalConnection, List<PlannedOperation> plannedOperations, Consumer<Object> newlyManagedEntityConsumer, Consumer<PlannedOperation> fixupOperationConsumer) {
		for ( int i = 0; i < plannedOperations.size(); i++ ) {
			var plannedOperation = plannedOperations.get( i );

			// No-op operations: only carry post-execution callback, skip SQL execution
			if ( plannedOperation.getKind() != MutationKind.NO_OP ) {
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
						// todo : need to figure out how to get these (currently null) values here
						selfExecuting.performMutation( null, null, session );
					}
					else {
						throw new IllegalStateException(
								"Unsupported JdbcOperation type: " + jdbcOperation.getClass().getName() );
					}
				}
			}

			// Execute post-execution callback immediately after operation completes (for both no-op and real operations)
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
	}

	private void executeWithGeneratedValues(PlannedOperation plannedOperation) {
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

	protected abstract void executePreparable(PreparableMutationOperation preparable, PlannedOperation plannedOperation);

	@Override
	public void finishUp() {
		// nothing to do by default
	}

}
