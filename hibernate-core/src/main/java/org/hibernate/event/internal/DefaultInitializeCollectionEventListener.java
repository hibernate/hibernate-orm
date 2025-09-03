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
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.sql.results.internal.ResultsHelper;

import static org.hibernate.collection.spi.AbstractPersistentCollection.checkPersister;
import static org.hibernate.loader.internal.CacheLoadHelper.initializeCollectionFromCache;
import static org.hibernate.pretty.MessageHelper.collectionInfoString;

/**
 * @author Gavin King
 */
public class DefaultInitializeCollectionEventListener implements InitializeCollectionEventListener {

	private static final CoreMessageLogger log = CoreLogging.messageLogger( DefaultInitializeCollectionEventListener.class );

	/**
	 * called by a collection that wants to initialize itself
	 */
	@Override
	public void onInitializeCollection(InitializeCollectionEvent event) throws HibernateException {
		final var collection = event.getCollection();
		final var source = event.getSession();

		final var persistenceContext = source.getPersistenceContextInternal();
		final var ce = persistenceContext.getCollectionEntry( collection );
		if ( ce == null ) {
			throw new HibernateException( "Collection was evicted" );
		}
		if ( !collection.wasInitialized() ) {
			final var loadedPersister = ce.getLoadedPersister();
			checkPersister(collection, loadedPersister);
			final Object loadedKey = ce.getLoadedKey();
			if ( log.isTraceEnabled() ) {
				log.trace( "Initializing collection "
						   + collectionInfoString( loadedPersister, collection, loadedKey, source ) );
			}

			final boolean foundInCache = initializeFromCache( loadedKey, loadedPersister, collection, source );
			if ( foundInCache ) {
				log.trace( "Collection initialized from cache" );
			}
			else {
				log.trace( "Collection not cached" );
				loadedPersister.initialize( loadedKey, source );
				handlePotentiallyEmptyCollection( collection, persistenceContext, loadedKey, loadedPersister );
				log.trace( "Collection initialized" );

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
			log.trace( "Disregarding cached version (if any) of collection due to enabled filters" );
			return false;
		}
		else {
			return initializeCollectionFromCache( id, persister, collection, source );
		}
	}
}
