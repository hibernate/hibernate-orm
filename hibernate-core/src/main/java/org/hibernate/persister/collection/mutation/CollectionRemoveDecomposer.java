/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.persister.collection.mutation;

import org.hibernate.action.internal.CollectionRemoveAction;
import org.hibernate.action.queue.graph.MutationDecomposer;
import org.hibernate.action.queue.plan.PlannedOperationGroup;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.persister.collection.BasicCollectionPersister;
import org.hibernate.persister.collection.OneToManyPersister;

import java.util.List;

/**
 * Decomposes a collection remove action into planned operations by delegating to
 * the {@link RemoveCoordinator}.
 * <p>
 * Collection removal deletes all entries in the collection by the collection key.
 *
 * @author Steve Ebersole
 */
public class CollectionRemoveDecomposer implements MutationDecomposer<CollectionRemoveAction> {
	@Override
	public List<PlannedOperationGroup> decompose(
			CollectionRemoveAction action,
			int ordinalBase,
			SharedSessionContractImplementor session) {
		final var persister = action.getPersister();
		final var key = action.getKey();

		// Delegate to the RemoveCoordinator for decomposition
		final RemoveCoordinator coordinator = getRemoveCoordinator( persister );
		if ( coordinator == null ) {
			return List.of();
		}

		return coordinator.decomposeRemove( key, ordinalBase, session );
	}

	/**
	 * Get the RemoveCoordinator from the persister.
	 */
	private static RemoveCoordinator getRemoveCoordinator(
			org.hibernate.persister.collection.CollectionPersister persister) {
		if ( persister instanceof OneToManyPersister oneToMany ) {
			return oneToMany.getRemoveCoordinator();
		}
		else if ( persister instanceof BasicCollectionPersister basic ) {
			return basic.getRemoveCoordinator();
		}
		return null;
	}
}
