/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.persister.collection.mutation;

import org.hibernate.action.internal.CollectionUpdateAction;
import org.hibernate.action.queue.exec.PostExecutionCallback;
import org.hibernate.cache.spi.access.CollectionDataAccess;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.event.spi.PostCollectionUpdateEvent;
import org.hibernate.event.spi.PostCollectionUpdateEventListener;

/// Post-execution callback for collection update actions.
///
/// This handles all the finalization work that needs to happen after all SQL statements
/// for the collection update have been executed, including:
///
///   - Updating CollectionEntry state (afterAction)
///   - Removing/evicting item from cache
///   - Firing POST_COLLECTION_UPDATE event listeners
///   - Updating statistics
///
/// @see CollectionUpdateAction
///
/// @author Steve Ebersole
public class PostCollectionUpdateHandling implements PostExecutionCallback {
	private final CollectionUpdateAction action;
	private final Object cacheKey;

	public PostCollectionUpdateHandling(CollectionUpdateAction action, Object cacheKey) {
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

		// 3. Fire POST_COLLECTION_UPDATE event
		postUpdate(session);

		// 4. Update statistics
		final var statistics = session.getFactory().getStatistics();
		if (statistics.isStatisticsEnabled()) {
			statistics.updateCollection(persister.getRole());
		}
	}

	private void evict(SessionImplementor session, Object cacheKey) {
		if (action.getPersister().hasCache() && cacheKey != null) {
			final CollectionDataAccess cache = action.getPersister().getCacheAccessStrategy();
			cache.remove(session, cacheKey);
		}
	}

	private void postUpdate(SessionImplementor session) {
		session.getFactory()
				.getEventListenerGroups()
				.eventListenerGroup_POST_COLLECTION_UPDATE
				.fireLazyEventOnEachListener(
						() -> newPostCollectionUpdateEvent(session),
						PostCollectionUpdateEventListener::onPostUpdateCollection
				);
	}

	private PostCollectionUpdateEvent newPostCollectionUpdateEvent(SessionImplementor session) {
		return new PostCollectionUpdateEvent(
				action.getPersister(),
				action.getCollection(),
				(org.hibernate.event.spi.EventSource) session,
				action.getAffectedOwner(),
				action.getAffectedOwnerId()
		);
	}
}
