/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.persister.collection.mutation;

import org.hibernate.action.internal.CollectionRecreateAction;
import org.hibernate.action.queue.exec.PostExecutionCallback;
import org.hibernate.action.queue.graph.MutationDecomposer;
import org.hibernate.action.queue.plan.PlannedOperation;
import org.hibernate.cache.spi.access.CollectionDataAccess;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.event.spi.PreCollectionRecreateEvent;
import org.hibernate.event.spi.PreCollectionRecreateEventListener;

import java.util.function.Consumer;
import org.hibernate.persister.collection.BasicCollectionPersister;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.collection.OneToManyPersister;

import java.util.List;

/// Decomposes a collection recreate action into planned operations.
///
/// Manages pre-execution phase and registering [PostCollectionRecreateHandling] callback
/// for post-execution phase.  Delegates to [InsertRowsCoordinator] to create the needed
/// PlannedOperations.
///
/// @author Steve Ebersole
public class CollectionRecreateDecomposer implements MutationDecomposer<CollectionRecreateAction> {
	@Override
	public List<PlannedOperation> decompose(
			CollectionRecreateAction action,
			int ordinalBase,
			Consumer<PostExecutionCallback> postExecutionCallbackRegistry,
			SharedSessionContractImplementor session) {
		final var persister = action.getPersister();
		final var collection = action.getCollection();
		final var key = action.getKey();

		preRecreate(action, session);

		final Object cacheKey = lockCacheItem(action, session);

		// Register callback even if no operations (for event firing, statistics)
		postExecutionCallbackRegistry.accept(new PostCollectionRecreateHandling(action, cacheKey));

		final InsertRowsCoordinator coordinator = getInsertRowsCoordinator(persister);
		if (coordinator == null) {
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

	private void preRecreate(CollectionRecreateAction action, SharedSessionContractImplementor session) {
		session.getFactory()
				.getEventListenerGroups()
				.eventListenerGroup_PRE_COLLECTION_RECREATE
				.fireLazyEventOnEachListener(
						() -> new PreCollectionRecreateEvent(
								action.getPersister(),
								action.getCollection(),
								(org.hibernate.event.spi.EventSource) session
						),
						PreCollectionRecreateEventListener::onPreRecreateCollection
				);
	}

	private Object lockCacheItem(CollectionRecreateAction action, SharedSessionContractImplementor session) {
		if (!action.getPersister().hasCache()) {
			return null;
		}

		final CollectionDataAccess cache = action.getPersister().getCacheAccessStrategy();
		return cache.generateCacheKey(
				action.getKey(),
				action.getPersister(),
				session.getFactory(),
				session.getTenantIdentifier()
		);
		// Note: The actual lock is obtained in CollectionAction.beforeExecutions()
		// We just generate the cache key here for use in post-execution
	}

	/**
	 * Get the InsertRowsCoordinator from the persister.
	 */
	private static InsertRowsCoordinator getInsertRowsCoordinator(CollectionPersister persister) {
		if ( persister instanceof OneToManyPersister oneToMany ) {
			return oneToMany.getInsertRowsCoordinator();
		}
		else if ( persister instanceof BasicCollectionPersister basic ) {
			return basic.getInsertRowsCoordinator();
		}
		return null;
	}
}
