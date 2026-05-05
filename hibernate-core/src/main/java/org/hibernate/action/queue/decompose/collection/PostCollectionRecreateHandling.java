/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.decompose.collection;

import org.hibernate.action.internal.CollectionRecreateAction;
import org.hibernate.action.queue.bind.PostExecutionCallback;
import org.hibernate.cache.spi.access.CollectionDataAccess;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.persister.collection.CollectionPersister;

/// Post-execution callback for collection recreate actions.
///
/// This handles all the finalization work that needs to happen after all SQL statements
/// for the collection recreation have been executed, including:
///
///   - Updating CollectionEntry state (afterAction)
///   - Removing/evicting item from cache
///   - Firing POST_COLLECTION_RECREATE event listeners
///   - Updating statistics
///
/// @see CollectionRecreateAction
///
/// @author Steve Ebersole
public class PostCollectionRecreateHandling implements PostExecutionCallback {
	private final CollectionPersister persister;
	private final PersistentCollection<?> collection;
	private final Object affectedOwner;
	private final Object affectedOwnerId;
	private final Object cacheKey;

	public PostCollectionRecreateHandling(
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
		DecompositionSupport.syncOwnerCollectionLoadedState( persister, affectedOwner, session );

		// 2. Evict from cache
		evict(session, cacheKey);

		// 3. Fire POST_COLLECTION_RECREATE event
		DecompositionSupport.firePostRecreate(
				persister,
				collection,
				affectedOwner,
				affectedOwnerId,
				session
		);

		// 4. Update statistics
		final var statistics = session.getFactory().getStatistics();
		if (statistics.isStatisticsEnabled()) {
			statistics.recreateCollection(persister.getRole());
		}
	}

	private void evict(SessionImplementor session, Object cacheKey) {
		if (persister.hasCache() && cacheKey != null) {
			final CollectionDataAccess cache = persister.getCacheAccessStrategy();
			cache.remove(session, cacheKey);
		}
	}
}
