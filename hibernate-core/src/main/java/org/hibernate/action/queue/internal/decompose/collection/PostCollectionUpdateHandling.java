/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.internal.decompose.collection;


import org.hibernate.action.queue.spi.bind.PostExecutionCallback;
import org.hibernate.cache.spi.access.CollectionDataAccess;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.persister.collection.CollectionPersister;

/// Post-execution callback for collection update actions.
///
/// This handles all the finalization work that needs to happen after all SQL statements
/// for the collection update have been executed, including:
///
/// See [ - Updating CollectionEntry state (afterAction)].
/// See [ - Removing/evicting item from cache].
/// See [ - Firing POST_COLLECTION_UPDATE event listeners].
/// See [ - Updating statistics].
///
/// See [CollectionUpdateAction].
///
/// @author Steve Ebersole
public class PostCollectionUpdateHandling implements PostExecutionCallback {
	private final CollectionPersister persister;
	private final PersistentCollection<?> collection;
	private final Object key;
	private final Object cacheKey;

	private final Object affectedOwner;
	private final Object affectedOwnerId;

	public PostCollectionUpdateHandling(
			CollectionPersister persister,
			PersistentCollection<?> collection,
			Object key,
			Object affectedOwner,
			Object affectedOwnerId,
			Object cacheKey) {
		this.persister = persister;
		this.collection = collection;
		this.key = key;
		this.affectedOwner = affectedOwner;
		this.affectedOwnerId = affectedOwnerId;
		this.cacheKey = cacheKey;
	}

	@Override
	public void handle(SessionImplementor session) {
		// Update CollectionEntry state
		if (collection != null) {
			session.getPersistenceContextInternal()
					.getCollectionEntry(collection)
					.afterAction(collection);
		}
		DecompositionSupport.syncOwnerCollectionLoadedState( persister, affectedOwner, session );

		// Evict from cache
		if ( persister.hasCache() ) {
			assert cacheKey != null;
			final CollectionDataAccess cache = persister.getCacheAccessStrategy();
			cache.remove(session, cacheKey);
		}

		// Fire POST_COLLECTION_UPDATE event
		DecompositionSupport.firePostUpdate( persister, collection, affectedOwner, affectedOwnerId, session );

		// Update statistics
		final var statistics = session.getFactory().getStatistics();
		if (statistics.isStatisticsEnabled()) {
			statistics.updateCollection(persister.getRole());
		}
	}
}
