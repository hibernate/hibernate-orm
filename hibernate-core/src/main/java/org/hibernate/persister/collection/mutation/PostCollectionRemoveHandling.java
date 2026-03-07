/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.persister.collection.mutation;

import org.hibernate.action.internal.CollectionRemoveAction;
import org.hibernate.action.queue.exec.PostExecutionCallback;
import org.hibernate.cache.spi.access.CollectionDataAccess;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.event.spi.PostCollectionRemoveEvent;
import org.hibernate.event.spi.PostCollectionRemoveEventListener;

/// Post-execution callback for collection remove actions.
///
/// This handles all the finalization work that needs to happen after all SQL statements
/// for the collection removal have been executed, including:
///
///   - Updating CollectionEntry state (afterAction)
///   - Removing/evicting item from cache
///   - Firing POST_COLLECTION_REMOVE event listeners
///   - Updating statistics
///
/// @see CollectionRemoveAction
///
/// @author Steve Ebersole
public class PostCollectionRemoveHandling implements PostExecutionCallback {
	private final CollectionRemoveAction action;
	private final Object cacheKey;

	public PostCollectionRemoveHandling(CollectionRemoveAction action, Object cacheKey) {
		this.action = action;
		this.cacheKey = cacheKey;
	}

	@Override
	public void handle(SessionImplementor session) {
		final var collection = action.getCollection();
		final var persister = action.getPersister();

		// 1. Update CollectionEntry state
		if (collection != null) {
			session.getPersistenceContextInternal()
					.getCollectionEntry(collection)
					.afterAction(collection);
		}

		// 2. Evict from cache
		evict(session, cacheKey);

		// 3. Fire POST_COLLECTION_REMOVE event
		postRemove(session);

		// 4. Update statistics
		final var statistics = session.getFactory().getStatistics();
		if (statistics.isStatisticsEnabled()) {
			statistics.removeCollection(persister.getRole());
		}
	}

	private void evict(SessionImplementor session, Object cacheKey) {
		if (action.getPersister().hasCache() && cacheKey != null) {
			final CollectionDataAccess cache = action.getPersister().getCacheAccessStrategy();
			cache.remove(session, cacheKey);
		}
	}

	private void postRemove(SessionImplementor session) {
		session.getFactory()
				.getEventListenerGroups()
				.eventListenerGroup_POST_COLLECTION_REMOVE
				.fireLazyEventOnEachListener(
						() -> newPostCollectionRemoveEvent(session),
						PostCollectionRemoveEventListener::onPostRemoveCollection
				);
	}

	private PostCollectionRemoveEvent newPostCollectionRemoveEvent(SessionImplementor session) {
		return new PostCollectionRemoveEvent(
				action.getPersister(),
				action.getCollection(),
				(org.hibernate.event.spi.EventSource) session,
				action.getAffectedOwner(),
				action.getAffectedOwnerId()
		);
	}
}
