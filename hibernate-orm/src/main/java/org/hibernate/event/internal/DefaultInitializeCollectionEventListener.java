/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.event.internal;

import java.io.Serializable;

import org.hibernate.HibernateException;
import org.hibernate.cache.spi.access.CollectionRegionAccessStrategy;
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
		final CollectionRegionAccessStrategy cacheAccessStrategy = persister.getCacheAccessStrategy();
		final Object ck = cacheAccessStrategy.generateCacheKey( id, persister, factory, source.getTenantIdentifier() );
		final Object ce = CacheHelper.fromSharedCache( source, ck, persister.getCacheAccessStrategy() );

		if ( factory.getStatistics().isStatisticsEnabled() ) {
			if ( ce == null ) {
				factory.getStatisticsImplementor()
						.secondLevelCacheMiss( cacheAccessStrategy.getRegion().getName() );
			}
			else {
				factory.getStatisticsImplementor()
						.secondLevelCacheHit( cacheAccessStrategy.getRegion().getName() );
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
