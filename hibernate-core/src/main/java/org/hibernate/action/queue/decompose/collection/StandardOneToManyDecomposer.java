/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.decompose.collection;

import org.hibernate.action.internal.CollectionRemoveAction;
import org.hibernate.action.queue.MutationKind;
import org.hibernate.action.queue.decompose.DecompositionContext;
import org.hibernate.action.queue.plan.PlannedOperation;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.persister.collection.OneToManyPersister;

import java.util.List;

/**
 * Standard one-to-many decomposer for single-table and simple joined inheritance
 * @author Steve Ebersole
 */
public class StandardOneToManyDecomposer extends AbstractOneToManyDecomposer {
	private final CollectionJdbcOperations jdbcOperations;

	public StandardOneToManyDecomposer(OneToManyPersister persister, SessionFactoryImplementor factory) {
		super( persister, factory );
		this.jdbcOperations = buildJdbcOperations( persister.getCollectionTableDescriptor(), factory );
	}

	@Override
	protected CollectionJdbcOperations selectJdbcOperations(Object entry, SharedSessionContractImplementor session) {
		return jdbcOperations;
	}

	@Override
	public List<PlannedOperation> decomposeRemove(
			CollectionRemoveAction action,
			int ordinalBase,
			SharedSessionContractImplementor session,
			DecompositionContext decompositionContext) {
		// Always fire PRE event, even if no SQL operations will be needed
		DecompositionSupport.firePreRemove( persister, action.getCollection(), action.getAffectedOwner(), session );

		// Create post-execution callback to handle post-execution work (afterAction, cache, events, stats)
		var postRemoveHandling = new PostCollectionRemoveHandling(
				persister,
				action.getCollection(),
				action.getAffectedOwner(),
				action.getAffectedOwnerId(),
				DecompositionSupport.generateCacheKey( action, session )
		);

		final var jdbcOperation = jdbcOperations.removeOperation();
		if ( jdbcOperation == null || action.isEmptySnapshot() ) {
			// No remove operation or collection is UNEQUIVOCALLY empty - create no-op to defer POST callback
			return List.of( DecompositionSupport.createNoOpCallbackCarrier(
					persister.getCollectionTableDescriptor(),
					ordinalBase * 1_000,
					postRemoveHandling
			) );
		}

		final PlannedOperation plannedOp = new PlannedOperation(
				persister.getCollectionTableDescriptor(),
				// technically an UPDATE
				MutationKind.UPDATE,
				jdbcOperation,
				new RemoveBindPlan( action.getKey(), persister ),
				ordinalBase * 1_000,
				"RemoveAllRows(" + persister.getRolePath() + ")"
		);

		// and attach to the operation
		plannedOp.setPostExecutionCallback( postRemoveHandling );

		return List.of( plannedOp );
	}
}
