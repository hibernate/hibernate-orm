/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008-2011, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.event.internal;

import java.io.Serializable;

import org.hibernate.HibernateException;
import org.hibernate.cache.spi.CacheKey;
import org.hibernate.cache.spi.entry.CollectionCacheEntry;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.internal.CacheHelper;
import org.hibernate.engine.spi.CollectionEntry;
import org.hibernate.engine.spi.PersistenceContext;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.event.spi.InitializeCollectionEvent;
import org.hibernate.event.spi.InitializeCollectionEventListener;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.pretty.MessageHelper;

/**
 * @author Gavin King
 */
public class DefaultInitializeCollectionEventListener implements InitializeCollectionEventListener {
	private static final CoreMessageLogger LOG = CoreLogging.messageLogger( DefaultInitializeCollectionEventListener.class );

	/**
	 * called by a collection that wants to initialize itself
	 */
	public void onInitializeCollection(InitializeCollectionEvent event) throws HibernateException {
		PersistentCollection collection = event.getCollection();
		SessionImplementor source = event.getSession();

		CollectionEntry ce = source.getPersistenceContext().getCollectionEntry( collection );
		if ( ce == null ) {
			throw new HibernateException( "collection was evicted" );
		}
		if ( !collection.wasInitialized() ) {
			final boolean traceEnabled = LOG.isTraceEnabled();
			if ( traceEnabled ) {
				LOG.tracev(
						"Initializing collection {0}",
						MessageHelper.collectionInfoString(
								ce.getLoadedPersister(),
								collection,
								ce.getLoadedKey(),
								source
						)
				);
				LOG.trace( "Checking second-level cache" );
			}

			final boolean foundInCache = initializeCollectionFromCache(
					ce.getLoadedKey(),
					ce.getLoadedPersister(),
					collection,
					source
			);

			if ( foundInCache ) {
				if ( traceEnabled ) {
					LOG.trace( "Collection initialized from cache" );
				}
			}
			else {
				if ( traceEnabled ) {
					LOG.trace( "Collection not cached" );
				}
				ce.getLoadedPersister().initialize( ce.getLoadedKey(), source );
				if ( traceEnabled ) {
					LOG.trace( "Collection initialized" );
				}

				if ( source.getFactory().getStatistics().isStatisticsEnabled() ) {
					source.getFactory().getStatisticsImplementor().fetchCollection(
							ce.getLoadedPersister().getRole()
					);
				}
			}
		}
	}

	/**
	 * Try to initialize a collection from the cache
	 *
	 * @param id The id of the collection of initialize
	 * @param persister The collection persister
	 * @param collection The collection to initialize
	 * @param source The originating session
	 *
	 * @return true if we were able to initialize the collection from the cache;
	 *         false otherwise.
	 */
	private boolean initializeCollectionFromCache(
			Serializable id,
			CollectionPersister persister,
			PersistentCollection collection,
			SessionImplementor source) {

		if ( !source.getLoadQueryInfluencers().getEnabledFilters().isEmpty()
				&& persister.isAffectedByEnabledFilters( source ) ) {
			LOG.trace( "Disregarding cached version (if any) of collection due to enabled filters" );
			return false;
		}

		final boolean useCache = persister.hasCache() && source.getCacheMode().isGetEnabled();

		if ( !useCache ) {
			return false;
		}

		final SessionFactoryImplementor factory = source.getFactory();
		final CacheKey ck = source.generateCacheKey( id, persister.getKeyType(), persister.getRole() );
		final Object ce = CacheHelper.fromSharedCache( source, ck, persister.getCacheAccessStrategy() );

		if ( factory.getStatistics().isStatisticsEnabled() ) {
			if ( ce == null ) {
				factory.getStatisticsImplementor()
						.secondLevelCacheMiss( persister.getCacheAccessStrategy().getRegion().getName() );
			}
			else {
				factory.getStatisticsImplementor()
						.secondLevelCacheHit( persister.getCacheAccessStrategy().getRegion().getName() );
			}
		}

		if ( ce == null ) {
			return false;
		}

		CollectionCacheEntry cacheEntry = (CollectionCacheEntry) persister.getCacheEntryStructure().destructure(
				ce,
				factory
		);

		final PersistenceContext persistenceContext = source.getPersistenceContext();
		cacheEntry.assemble( collection, persister, persistenceContext.getCollectionOwner( id, persister ) );
		persistenceContext.getCollectionEntry( collection ).postInitialize( collection );
		// addInitializedCollection(collection, persister, id);
		return true;
	}
}
