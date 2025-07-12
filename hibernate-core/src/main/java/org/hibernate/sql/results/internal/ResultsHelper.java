/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.results.internal;


import org.hibernate.CacheMode;
import org.hibernate.cache.spi.access.CollectionDataAccess;
import org.hibernate.cache.spi.entry.CollectionCacheEntry;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.BatchFetchQueue;
import org.hibernate.engine.spi.CollectionEntry;
import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.engine.spi.PersistenceContext;
import org.hibernate.engine.spi.SessionEventListenerManager;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.event.monitor.spi.EventMonitor;
import org.hibernate.event.monitor.spi.DiagnosticEvent;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.sql.results.jdbc.spi.JdbcValues;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesMapping;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesMappingResolution;
import org.hibernate.sql.results.spi.RowReader;
import org.hibernate.sql.results.spi.RowTransformer;
import org.hibernate.stat.spi.StatisticsImplementor;
import org.hibernate.type.EntityType;

import static org.hibernate.pretty.MessageHelper.collectionInfoString;

/**
 * @author Steve Ebersole
 */
public class ResultsHelper {

	private static final CoreMessageLogger LOG = CoreLogging.messageLogger( ResultsHelper.class );

	public static <R> RowReader<R> createRowReader(
			SessionFactoryImplementor sessionFactory,
			RowTransformer<R> rowTransformer,
			Class<R> transformedResultJavaType,
			JdbcValues jdbcValues) {
		return createRowReader( sessionFactory, rowTransformer, transformedResultJavaType, jdbcValues.getValuesMapping() );
	}

	public static <R> RowReader<R> createRowReader(
			SessionFactoryImplementor sessionFactory,
			RowTransformer<R> rowTransformer,
			Class<R> transformedResultJavaType,
			JdbcValuesMapping jdbcValuesMapping) {
		final JdbcValuesMappingResolution jdbcValuesMappingResolution = jdbcValuesMapping.resolveAssemblers( sessionFactory );
		return new StandardRowReader<>(
				jdbcValuesMappingResolution,
				rowTransformer,
				transformedResultJavaType
		);
	}

	public static void finalizeCollectionLoading(
			PersistenceContext persistenceContext,
			CollectionPersister collectionDescriptor,
			PersistentCollection<?> collectionInstance,
			Object key,
			boolean hasNoQueuedAdds) {
		final SharedSessionContractImplementor session = persistenceContext.getSession();

		CollectionEntry collectionEntry = persistenceContext.getCollectionEntry( collectionInstance );
		if ( collectionEntry == null ) {
			collectionEntry = persistenceContext.addInitializedCollection( collectionDescriptor, collectionInstance, key );
		}
		else {
			collectionEntry.postInitialize( collectionInstance, session );
		}

		if ( collectionDescriptor.getCollectionType().hasHolder() ) {
			// in case of PersistentArrayHolder we have to realign
			// the EntityEntry loaded state with the entity values
			final Object owner = collectionInstance.getOwner();
			final EntityEntry entry = persistenceContext.getEntry( owner );
			final Object[] loadedState = entry.getLoadedState();
			if ( loadedState != null ) {
				final PluralAttributeMapping mapping = collectionDescriptor.getAttributeMapping();
				final int propertyIndex = mapping.getStateArrayPosition();
				loadedState[propertyIndex] = mapping.getValue( owner );
			}
			// else it must be an immutable entity or loaded in read-only mode,
			// but unfortunately we have no way to reliably determine that here
			persistenceContext.addCollectionHolder( collectionInstance );
		}

		final BatchFetchQueue batchFetchQueue = persistenceContext.getBatchFetchQueue();
		batchFetchQueue.removeBatchLoadableCollection( collectionEntry );

		// add to cache if:
		final boolean addToCache =
				// there were no queued additions
				hasNoQueuedAdds
						// and the role has a cache
						&& collectionDescriptor.hasCache()
						// and this is not a forced initialization during flush
						&& session.getCacheMode().isPutEnabled() && !collectionEntry.isDoremove();
		if ( addToCache ) {
			addCollectionToCache( persistenceContext, collectionDescriptor, collectionInstance, key );
		}

		if ( LOG.isTraceEnabled() ) {
			LOG.tracef(
					"Collection fully initialized: %s",
					collectionInfoString(
							collectionDescriptor,
							collectionInstance,
							key,
							session
					)
			);
		}

		final StatisticsImplementor statistics = session.getFactory().getStatistics();
		if ( statistics.isStatisticsEnabled() ) {
			statistics.loadCollection( collectionDescriptor.getRole() );
		}

		// todo (6.0) : there is other logic still needing to be implemented here.  caching, etc
		// 		see org.hibernate.engine.loading.internal.CollectionLoadContext#endLoadingCollection in 5.x
	}

	/**
	 * Add the collection to the second-level cache
	 */
	private static void addCollectionToCache(
			PersistenceContext persistenceContext,
			CollectionPersister collectionDescriptor,
			PersistentCollection<?> collectionInstance,
			Object key) {
		final SharedSessionContractImplementor session = persistenceContext.getSession();
		final SessionFactoryImplementor factory = session.getFactory();

		if ( LOG.isTraceEnabled() ) {
			LOG.trace( "Caching collection: "
					+ collectionInfoString( collectionDescriptor, collectionInstance, key, session ) );
		}

		if ( session.getLoadQueryInfluencers().hasEnabledFilters() && collectionDescriptor.isAffectedByEnabledFilters( session ) ) {
			// some filters affecting the collection are enabled on the session, so do not do the put into the cache.
			LOG.debug( "Refusing to add to cache due to enabled filters" );
			// todo : add the notion of enabled filters to the cache key to differentiate filtered collections from non-filtered;
			//      DefaultInitializeCollectionEventHandler.initializeCollectionFromCache() (which makes sure to not read from
			//      cache with enabled filters).
			// EARLY EXIT!!!!!
			return;
		}

		final Object version;
		if ( collectionDescriptor.isVersioned() ) {
			Object collectionOwner = persistenceContext.getCollectionOwner( key, collectionDescriptor );
			if ( collectionOwner == null ) {
				// generally speaking this would be caused by the collection key being defined by a property-ref, thus
				// the collection key and the owner key would not match up.  In this case, try to use the key of the
				// owner instance associated with the collection itself, if one.  If the collection does already know
				// about its owner, that owner should be the same instance as associated with the PC, but we do the
				// resolution against the PC anyway just to be safe since the lookup should not be costly.
				if ( collectionInstance != null ) {
					final Object linkedOwner = collectionInstance.getOwner();
					if ( linkedOwner != null ) {
						final Object ownerKey = collectionDescriptor.getOwnerEntityPersister().getIdentifier( linkedOwner, session );
						collectionOwner = persistenceContext.getCollectionOwner( ownerKey, collectionDescriptor );
					}
				}
				if ( collectionOwner == null ) {
					LOG.debugf( "Unable to resolve owner of loading collection for second level caching. Refusing to add to cache.");
					return;
				}
			}
			version = persistenceContext.getEntry( collectionOwner ).getVersion();
		}
		else {
			version = null;
		}

		final CollectionCacheEntry entry = new CollectionCacheEntry( collectionInstance, collectionDescriptor );
		final CollectionDataAccess cacheAccess = collectionDescriptor.getCacheAccessStrategy();
		final Object cacheKey = cacheAccess.generateCacheKey(
				key,
				collectionDescriptor,
				session.getFactory(),
				session.getTenantIdentifier()
		);

		boolean isPutFromLoad = true;
		if ( collectionDescriptor.getElementType() instanceof EntityType ) {
			final EntityPersister entityPersister = collectionDescriptor.getElementPersister();
			for ( Object id : entry.getState() ) {
				if ( persistenceContext.wasInsertedDuringTransaction( entityPersister, id ) ) {
					isPutFromLoad = false;
					break;
				}
			}
		}

		// CollectionRegionAccessStrategy has no update, so avoid putting uncommitted data via putFromLoad
		if ( isPutFromLoad ) {
			final SessionEventListenerManager eventListenerManager = session.getEventListenerManager();
			final EventMonitor eventMonitor = session.getEventMonitor();
			final DiagnosticEvent cachePutEvent = eventMonitor.beginCachePutEvent();
			boolean put = false;
			try {
				eventListenerManager.cachePutStart();
				put = cacheAccess.putFromLoad(
						session,
						cacheKey,
						collectionDescriptor.getCacheEntryStructure().structure( entry ),
						version,
						factory.getSessionFactoryOptions().isMinimalPutsEnabled()
								&& session.getCacheMode()!= CacheMode.REFRESH
				);
			}
			finally {
				eventMonitor.completeCachePutEvent(
						cachePutEvent,
						session,
						cacheAccess,
						collectionDescriptor,
						put,
						EventMonitor.CacheActionDescription.COLLECTION_INSERT
				);
				eventListenerManager.cachePutEnd();

				final StatisticsImplementor statistics = factory.getStatistics();
				if ( put && statistics.isStatisticsEnabled() ) {
					statistics.collectionCachePut(
							collectionDescriptor.getNavigableRole(),
							collectionDescriptor.getCacheAccessStrategy().getRegion().getName()
					);
				}

			}
		}
	}
}
