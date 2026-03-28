/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.persister.collection.mutation;

import org.hibernate.action.internal.CollectionRemoveAction;
import org.hibernate.action.queue.MutationKind;
import org.hibernate.action.queue.plan.PlannedOperation;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.persister.collection.OneToManyPersister;

import java.util.List;

/**
 * Standard one-to-many decomposer for single-table and simple joined inheritance
 * @author Steve Ebersole
 */
public class StandardOneToManyDecomposer extends AbstractNonBundledOneToManyDecomposer {
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
			SharedSessionContractImplementor session) {
		// Create callback to handle post-execution work (afterAction, cache, events, stats)
		final Object cacheKey = lockCacheItem( action, session );
		final PostCollectionRemoveHandling postCollectionRemoveHandling = new PostCollectionRemoveHandling( action, cacheKey );

		final var jdbcOperation = jdbcOperations.removeOperation();
		if ( jdbcOperation == null ) {
			return List.of();
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

		// Attach post-execution callback to the operation
		plannedOp.setPostExecutionCallback( postCollectionRemoveHandling );

		return List.of( plannedOp );
	}
}
