/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.event.internal;

import org.hibernate.HibernateException;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.PersistenceContext;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.event.spi.InitializeCollectionEvent;
import org.hibernate.event.spi.InitializeCollectionEventListener;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.sql.results.internal.ResultsHelper;

import static org.hibernate.collection.spi.AbstractPersistentCollection.checkPersister;
import static org.hibernate.event.internal.EventListenerLogging.EVENT_LISTENER_LOGGER;
import static org.hibernate.loader.internal.CacheLoadHelper.initializeCollectionFromCache;
import static org.hibernate.pretty.MessageHelper.collectionInfoString;

/**
 * @author Gavin King
 */
public class DefaultInitializeCollectionEventListener implements InitializeCollectionEventListener {

	/**
	 * called by a collection that wants to initialize itself
	 */
	@Override
	public void onInitializeCollection(InitializeCollectionEvent event) throws HibernateException {
		final var collection = event.getCollection();
		final var source = event.getSession();
		final var persistenceContext = source.getPersistenceContextInternal();
		final var collectionEntry = persistenceContext.getCollectionEntry( collection );
		if ( collectionEntry == null ) {
			throw new HibernateException( "Collection was evicted" );
		}
		if ( !collection.wasInitialized() ) {
			final var loadedPersister = collectionEntry.getLoadedPersister();
			checkPersister(collection, loadedPersister);
			final Object loadedKey = collectionEntry.getLoadedKey();
			if ( EVENT_LISTENER_LOGGER.isTraceEnabled() ) {
				EVENT_LISTENER_LOGGER.initializingCollection(
						collectionInfoString( loadedPersister, collection, loadedKey, source ) );
			}

			final boolean foundInCache = initializeFromCache( loadedKey, loadedPersister, collection, source );
			if ( foundInCache ) {
				EVENT_LISTENER_LOGGER.collectionInitializedFromCache();
			}
			else {
				EVENT_LISTENER_LOGGER.collectionNotCached();
				loadedPersister.initialize( loadedKey, source );
				handlePotentiallyEmptyCollection( collection, persistenceContext, loadedKey, loadedPersister );
				EVENT_LISTENER_LOGGER.collectionInitialized();

				final var statistics = source.getFactory().getStatistics();
				if ( statistics.isStatisticsEnabled() ) {
					statistics.fetchCollection( loadedPersister.getRole() );
				}
			}
		}
	}

	public static void handlePotentiallyEmptyCollection(
			PersistentCollection<?> collection,
			PersistenceContext persistenceContext,
			Object loadedKey,
			CollectionPersister loadedPersister) {
		if ( !collection.wasInitialized() ) {
			collection.initializeEmptyCollection( loadedPersister );
			ResultsHelper.finalizeCollectionLoading(
					persistenceContext,
					loadedPersister,
					collection,
					loadedKey,
					true
			);
		}
	}

	/**
	 * Try to initialize a collection from the cache
	 *
	 * @param id The id of the collection to initialize
	 * @param persister The collection persister
	 * @param collection The collection to initialize
	 * @param source The originating session
	 *
	 * @return true if we were able to initialize the collection from the cache;
	 *         false otherwise.
	 */
	private boolean initializeFromCache(
			Object id,
			CollectionPersister persister,
			PersistentCollection<?> collection,
			SessionImplementor source) {
		if ( source.getLoadQueryInfluencers().hasEnabledFilters()
				&& persister.isAffectedByEnabledFilters( source ) ) {
			EVENT_LISTENER_LOGGER.disregardingCachedVersionDueToEnabledFilters();
			return false;
		}
		else {
			return initializeCollectionFromCache( id, persister, collection, source );
		}
	}
}
