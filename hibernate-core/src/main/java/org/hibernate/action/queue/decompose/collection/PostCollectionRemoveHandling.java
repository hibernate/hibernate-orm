/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.decompose.collection;

import org.hibernate.action.internal.CollectionRemoveAction;
import org.hibernate.action.queue.exec.PostExecutionCallback;
import org.hibernate.cache.spi.access.CollectionDataAccess;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.persister.collection.CollectionPersister;

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
	private final CollectionPersister persister;
	private final PersistentCollection<?> collection;
	private final Object affectedOwner;
	private final Object affectedOwnerId;
	private final Object cacheKey;

	public PostCollectionRemoveHandling(
			CollectionPersister persister,
			PersistentCollection<?> collection,
			Object affectedOwner,
			Object affectedOwnerId,
			Object cacheKey) {
		this.persister = persister;
		this.collection = collection;
		this.affectedOwner = affectedOwner;
		this.affectedOwnerId = affectedOwnerId;
		this.cacheKey = cacheKey;
	}

	@Override
	public void handle(SessionImplementor session) {
		// 1. Update CollectionEntry state
		if (collection != null) {
			session.getPersistenceContextInternal()
					.getCollectionEntry(collection)
					.afterAction(collection);
		}

		// 2. Evict from cache
		evict(session, cacheKey);

		// 3. Fire POST_COLLECTION_REMOVE event
		DecompositionSupport.firePostRemove(
				persister,
				collection,
				affectedOwner,
				affectedOwnerId,
				session
		);

		// 4. Update statistics
		final var statistics = session.getFactory().getStatistics();
		if (statistics.isStatisticsEnabled()) {
			statistics.removeCollection(persister.getRole());
		}
	}

	private void evict(SessionImplementor session, Object cacheKey) {
		if (persister.hasCache() && cacheKey != null) {
			final CollectionDataAccess cache = persister.getCacheAccessStrategy();
			cache.remove(session, cacheKey);
		}
	}
}
