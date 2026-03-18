/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.persister.collection.mutation;

import org.hibernate.action.internal.CollectionUpdateAction;
import org.hibernate.action.queue.exec.PostExecutionCallback;
import org.hibernate.cache.spi.access.CollectionDataAccess;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.event.spi.PostCollectionUpdateEvent;
import org.hibernate.event.spi.PostCollectionUpdateEventListener;
import org.hibernate.persister.collection.CollectionPersister;

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
	private final CollectionPersister persister;
	private final PersistentCollection<?> collection;
	private final Object key;

	private final Object affectedOwner;
	private final Object affectedOwnerId;

	public PostCollectionUpdateHandling(
			CollectionPersister persister,
			PersistentCollection<?> collection,
			Object key,
			Object affectedOwner,
			Object affectedOwnerId) {
		this.persister = persister;
		this.collection = collection;
		this.key = key;
		this.affectedOwner = affectedOwner;
		this.affectedOwnerId = affectedOwnerId;
	}

	@Override
	public void handle(SessionImplementor session) {
		// Update CollectionEntry state
		if (collection != null) {
			session.getPersistenceContextInternal()
					.getCollectionEntry(collection)
					.afterAction(collection);
		}

		// Evict from cache
		if ( persister.hasCache() ) {
			final CollectionDataAccess cache = persister.getCacheAccessStrategy();
			final Object cacheKey = cache.generateCacheKey(
					key,
					persister,
					session.getFactory(),
					session.getTenantIdentifier()
			);
			cache.remove(session, cacheKey);
		}

		// Fire POST_COLLECTION_UPDATE event
		postUpdate(session);

		// Update statistics
		final var statistics = session.getFactory().getStatistics();
		if (statistics.isStatisticsEnabled()) {
			statistics.updateCollection(persister.getRole());
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
				persister,
				collection,
				(org.hibernate.event.spi.EventSource) session,
				affectedOwner,
				affectedOwnerId
		);
	}
}
