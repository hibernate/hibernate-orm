/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.persister.collection.mutation;

import org.hibernate.action.internal.CollectionRemoveAction;
import org.hibernate.action.queue.exec.PostExecutionCallback;
import org.hibernate.action.queue.graph.MutationDecomposer;
import org.hibernate.action.queue.plan.PlannedOperation;
import org.hibernate.cache.spi.access.CollectionDataAccess;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.event.spi.PreCollectionRemoveEvent;
import org.hibernate.event.spi.PreCollectionRemoveEventListener;

import java.util.function.Consumer;
import org.hibernate.persister.collection.BasicCollectionPersister;
import org.hibernate.persister.collection.OneToManyPersister;

import java.util.List;

/// Decomposes a collection remove action into planned operations.
///
/// Manages pre-execution phase and registering [PostCollectionRemoveHandling] callback
/// for post-execution phase.  Delegates to [RemoveCoordinator] to create the needed
/// PlannedOperations.
///
/// @author Steve Ebersole
public class CollectionRemoveDecomposer implements MutationDecomposer<CollectionRemoveAction> {
	@Override
	public List<PlannedOperation> decompose(
			CollectionRemoveAction action,
			int ordinalBase,
			Consumer<PostExecutionCallback> postExecutionCallbackRegistry,
			SharedSessionContractImplementor session) {
		final var persister = action.getPersister();
		final var key = action.getKey();

		preRemove(action, session);

		final Object cacheKey = lockCacheItem(action, session);

		// Register callback even if no operations (for event firing, statistics)
		postExecutionCallbackRegistry.accept(new PostCollectionRemoveHandling(action, cacheKey));

		final RemoveCoordinator coordinator = getRemoveCoordinator(persister);
		if (coordinator == null) {
			return List.of();
		}

		return coordinator.decomposeRemove(key, ordinalBase, session);
	}

	private void preRemove(CollectionRemoveAction action, SharedSessionContractImplementor session) {
		session.getFactory()
				.getEventListenerGroups()
				.eventListenerGroup_PRE_COLLECTION_REMOVE
				.fireLazyEventOnEachListener(
						() -> new PreCollectionRemoveEvent(
								action.getPersister(),
								action.getCollection(),
								(org.hibernate.event.spi.EventSource) session,
								action.getAffectedOwner()
						),
						PreCollectionRemoveEventListener::onPreRemoveCollection
				);
	}

	private Object lockCacheItem(CollectionRemoveAction action, SharedSessionContractImplementor session) {
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
