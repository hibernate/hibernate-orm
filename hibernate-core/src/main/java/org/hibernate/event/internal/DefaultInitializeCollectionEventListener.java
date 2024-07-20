/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.event.internal;

import org.hibernate.HibernateException;
import org.hibernate.cache.spi.access.CollectionDataAccess;
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
import org.hibernate.metamodel.model.domain.NavigableRole;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.sql.results.internal.ResultsHelper;
import org.hibernate.stat.spi.StatisticsImplementor;

import static org.hibernate.pretty.MessageHelper.collectionInfoString;

/**
 * @author Gavin King
 */
public class DefaultInitializeCollectionEventListener implements InitializeCollectionEventListener {
	private static final CoreMessageLogger LOG = CoreLogging.messageLogger( DefaultInitializeCollectionEventListener.class );

	/**
	 * called by a collection that wants to initialize itself
	 */
	@Override
	public void onInitializeCollection(InitializeCollectionEvent event) throws HibernateException {
		final PersistentCollection<?> collection = event.getCollection();
		final SessionImplementor source = event.getSession();

		final PersistenceContext persistenceContext = source.getPersistenceContextInternal();
		final CollectionEntry ce = persistenceContext.getCollectionEntry( collection );
		if ( ce == null ) {
			throw new HibernateException( "collection was evicted" );
		}
		if ( !collection.wasInitialized() ) {
			final CollectionPersister loadedPersister = ce.getLoadedPersister();
			final Object loadedKey = ce.getLoadedKey();
			if ( LOG.isTraceEnabled() ) {
				LOG.tracev(
						"Initializing collection {0}",
						collectionInfoString( loadedPersister, collection, loadedKey, source )
				);
				LOG.trace( "Checking second-level cache" );
			}

			final boolean foundInCache = initializeCollectionFromCache( loadedKey, loadedPersister, collection, source );
			if ( foundInCache ) {
				if ( LOG.isTraceEnabled() ) {
					LOG.trace( "Collection initialized from cache" );
				}
			}
			else {
				if ( LOG.isTraceEnabled() ) {
					LOG.trace( "Collection not cached" );
				}
				loadedPersister.initialize( loadedKey, source );
				handlePotentiallyEmptyCollection( collection, persistenceContext, loadedKey, loadedPersister );
				if ( LOG.isTraceEnabled() ) {
					LOG.trace( "Collection initialized" );
				}

				final StatisticsImplementor statistics = source.getFactory().getStatistics();
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
	private boolean initializeCollectionFromCache(
			Object id,
			CollectionPersister persister,
			PersistentCollection<?> collection,
			SessionImplementor source) {

		if ( source.getLoadQueryInfluencers().hasEnabledFilters()
				&& persister.isAffectedByEnabledFilters( source ) ) {
			LOG.trace( "Disregarding cached version (if any) of collection due to enabled filters" );
			return false;
		}

		if ( persister.hasCache() && source.getCacheMode().isGetEnabled() ) {
			final SessionFactoryImplementor factory = source.getFactory();
			final CollectionDataAccess cacheAccessStrategy = persister.getCacheAccessStrategy();
			final Object ck = cacheAccessStrategy.generateCacheKey( id, persister, factory, source.getTenantIdentifier() );
			final Object ce = CacheHelper.fromSharedCache( source, ck, persister, cacheAccessStrategy );

			final StatisticsImplementor statistics = factory.getStatistics();
			if ( statistics.isStatisticsEnabled() ) {
				final NavigableRole navigableRole = persister.getNavigableRole();
				final String regionName = cacheAccessStrategy.getRegion().getName();
				if ( ce == null ) {
					statistics.collectionCacheMiss( navigableRole, regionName );
				}
				else {
					statistics.collectionCacheHit( navigableRole, regionName );
				}
			}

			if ( ce == null ) {
				return false;
			}
			else {
				final CollectionCacheEntry cacheEntry = (CollectionCacheEntry)
						persister.getCacheEntryStructure().destructure( ce, factory );
				final PersistenceContext persistenceContext = source.getPersistenceContextInternal();
				cacheEntry.assemble( collection, persister, persistenceContext.getCollectionOwner( id, persister ) );
				persistenceContext.getCollectionEntry( collection ).postInitialize( collection, source );
				// addInitializedCollection(collection, persister, id);
				return true;
			}
		}
		else {
			return false;
		}

	}
}
