/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.persister.collection.mutation;

import org.hibernate.action.internal.CollectionRecreateAction;
import org.hibernate.action.queue.graph.MutationDecomposer;
import org.hibernate.action.queue.plan.PlannedOperationGroup;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.persister.collection.BasicCollectionPersister;
import org.hibernate.persister.collection.OneToManyPersister;

import java.util.List;

/**
 * Decomposes a collection recreate action into planned operations.
 * Collection recreation inserts all entries in the collection by delegating to
 * the {@link InsertRowsCoordinator}.
 *
 * @author Steve Ebersole
 */
public class CollectionRecreateDecomposer implements MutationDecomposer<CollectionRecreateAction> {
	@Override
	public List<PlannedOperationGroup> decompose(
			CollectionRecreateAction action,
			int ordinalBase,
			SharedSessionContractImplementor session) {
		final var persister = action.getPersister();
		final var collection = action.getCollection();
		final var key = action.getKey();

		// Delegate to the InsertRowsCoordinator for decomposition
		final InsertRowsCoordinator coordinator = getInsertRowsCoordinator( persister );
		if ( coordinator == null ) {
			return List.of();
		}

		return coordinator.decomposeInsertRows(
				collection,
				key,
				collection::includeInRecreate,
				ordinalBase,
				session
		);
	}

	/**
	 * Get the InsertRowsCoordinator from the persister.
	 */
	private static InsertRowsCoordinator getInsertRowsCoordinator(
			org.hibernate.persister.collection.CollectionPersister persister) {
		if ( persister instanceof OneToManyPersister oneToMany ) {
			return oneToMany.getInsertRowsCoordinator();
		}
		else if ( persister instanceof BasicCollectionPersister basic ) {
			return basic.getInsertRowsCoordinator();
		}
		return null;
	}
}
