/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.exec;

import org.hibernate.action.queue.MutationKind;
import org.hibernate.action.queue.cyclebreak.FixupSynthesizer;
import org.hibernate.action.queue.mutation.jdbc.JdbcOperation;
import org.hibernate.action.queue.mutation.jdbc.PreparableJdbcOperation;
import org.hibernate.action.queue.mutation.jdbc.SelfExecutingJdbcOperation;
import org.hibernate.action.queue.op.PlannedOperation;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.generator.values.GeneratedValuesMutationDelegate;

import java.util.List;
import java.util.function.Consumer;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractStepPlanner implements PlanStepExecutor {
	private final FixupSynthesizer fixupSynthesizer;
	private final GeneratedValuesMutationDelegate generatedValuesDelegate;

	protected final SharedSessionContractImplementor session;

	public AbstractStepPlanner(SharedSessionContractImplementor session) {
		this.session = session;

		fixupSynthesizer = new FixupSynthesizer();
		generatedValuesDelegate = null;
	}

	@Override
	public void execute(
			List<PlannedOperation> plannedOperations,
			Consumer<Object> newlyManagedEntityConsumer,
			Consumer<PlannedOperation> fixupOperationConsumer) {
		for ( int i = 0; i < plannedOperations.size(); i++ ) {
			var plannedOperation = plannedOperations.get( i );

			if ( plannedOperation.getBindPlan().getGeneratedValuesCollector() != null ) {
				// we need to execute these without batching
				executeWithGeneratedValues( plannedOperation );
			}
			else {
				final JdbcOperation jdbcOperation = plannedOperation.getJdbcOperation();
				if ( jdbcOperation instanceof PreparableJdbcOperation preparable ) {
					executePreparable( preparable, plannedOperation );
				}
				else if ( jdbcOperation instanceof SelfExecutingJdbcOperation selfExecuting ) {
					selfExecuting.execute( session );
				}
				else {
					throw new IllegalStateException(
							"Unsupported JdbcOperation type: " + jdbcOperation.getClass().getName() );
				}
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

		var generatedValues = generatedValuesDelegate.performGraphMutation(
				plannedOperation,
				bindPlan.getEntityInstance(),
				session
		);

		generatedValuesCollector.apply( generatedValues );
	}

	protected abstract void executePreparable(PreparableJdbcOperation preparable, PlannedOperation plannedOperation);

	@Override
	public void finishUp() {
		// nothing to do by default
	}
}
