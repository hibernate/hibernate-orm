/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.results.internal;


import org.hibernate.CacheMode;
import org.hibernate.SharedSessionContract;
import org.hibernate.cache.spi.entry.CollectionCacheEntry;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.CollectionEntry;
import org.hibernate.engine.spi.PersistenceContext;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.event.monitor.spi.EventMonitor;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.sql.results.jdbc.spi.JdbcValues;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesMapping;
import org.hibernate.sql.results.spi.RowReader;
import org.hibernate.sql.results.spi.RowTransformer;

import static org.hibernate.pretty.MessageHelper.collectionInfoString;

/**
 * @author Steve Ebersole
 */
public class ResultsHelper {

	private static final CoreMessageLogger log = CoreLogging.messageLogger( ResultsHelper.class );

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
		return new StandardRowReader<>(
				jdbcValuesMapping.resolveAssemblers( sessionFactory ),
				rowTransformer,
				transformedResultJavaType
		);
	}

	public static void finalizeCollectionLoading(
			PersistenceContext persistenceContext,
			CollectionPersister collectionDescriptor,
			PersistentCollection<?> collection,
			Object key,
			boolean hasNoQueuedAdds) {
		final var session = persistenceContext.getSession();
		final var collectionEntry =
				initializedEntry( persistenceContext, collectionDescriptor, collection, key, session );
		if ( collectionDescriptor.getCollectionType().hasHolder() ) {
			addCollectionHolder( persistenceContext, collectionDescriptor, collection );
		}
		persistenceContext.getBatchFetchQueue().removeBatchLoadableCollection( collectionEntry );
		if ( addToCache( session, collectionEntry, collectionDescriptor, hasNoQueuedAdds ) ) {
			addCollectionToCache( persistenceContext, collectionDescriptor, collection, key );
		}
		final var statistics = session.getFactory().getStatistics();
		if ( statistics.isStatisticsEnabled() ) {
			statistics.loadCollection( collectionDescriptor.getRole() );
		}

		if ( log.isTraceEnabled() ) {
			log.trace( "Collection fully initialized: "
						+ collectionInfoString( collectionDescriptor, collection, key, session ) );
		}

		// todo (6.0) : there is other logic still needing to be implemented here.  caching, etc
		// 		see org.hibernate.engine.loading.internal.CollectionLoadContext#endLoadingCollection in 5.x
	}

	private static boolean addToCache(
			SharedSessionContract session,
			CollectionEntry collectionEntry,
			CollectionPersister collectionDescriptor,
			boolean hasNoQueuedAdds) {
		return hasNoQueuedAdds  // there were no queued additions
			&& collectionDescriptor.hasCache()  // the collection role has a cache
			&& session.getCacheMode().isPutEnabled()  // the session cache mode allows puts
			&& !collectionEntry.isDoremove();  // this is not a forced initialization during flush
	}

	private static void addCollectionHolder(
			PersistenceContext persistenceContext,
			CollectionPersister collectionDescriptor,
			PersistentCollection<?> collectionInstance) {
		// in case of PersistentArrayHolder we have to realign
		// the EntityEntry loaded state with the entity values
		final Object owner = collectionInstance.getOwner();
		final var loadedState = persistenceContext.getEntry( owner ).getLoadedState();
		if ( loadedState != null ) {
			final var mapping = collectionDescriptor.getAttributeMapping();
			final int propertyIndex = mapping.getStateArrayPosition();
			loadedState[propertyIndex] = mapping.getValue( owner );
		}
		// else it must be an immutable entity or loaded in read-only mode,
		// but, unfortunately, we have no way to reliably determine that here
		persistenceContext.addCollectionHolder( collectionInstance );
	}

	private static CollectionEntry initializedEntry(
			PersistenceContext context,
			CollectionPersister collectionDescriptor,
			PersistentCollection<?> collectionInstance,
			Object key,
			SharedSessionContractImplementor session) {
		final var collectionEntry = context.getCollectionEntry( collectionInstance );
		if ( collectionEntry == null ) {
			return context.addInitializedCollection( collectionDescriptor, collectionInstance, key );
		}
		else {
			collectionEntry.postInitialize( collectionInstance, session );
			return collectionEntry;
		}
	}

	/**
	 * Add the collection to the second-level cache
	 */
	private static void addCollectionToCache(
			PersistenceContext persistenceContext,
			CollectionPersister collectionDescriptor,
			PersistentCollection<?> collection,
			Object key) {
		final var session = persistenceContext.getSession();

		if ( log.isTraceEnabled() ) {
			log.trace( "Caching collection: "
						+ collectionInfoString( collectionDescriptor, collection, key, session ) );
		}

		if ( session.getLoadQueryInfluencers().hasEnabledFilters()
				&& collectionDescriptor.isAffectedByEnabledFilters( session ) ) {
			// some filters affecting the collection are enabled on the session, so do not do the put into the cache.
			log.debug( "Refusing to add to cache due to enabled filters" );
			// todo : add the notion of enabled filters to the cache key to differentiate filtered collections from non-filtered;
			//      DefaultInitializeCollectionEventHandler.initializeCollectionFromCache() (which makes sure to not read from
			//      cache with enabled filters).
			// EARLY EXIT!!!!!
			return;
		}

		final Object version;
		if ( collectionDescriptor.isVersioned() ) {
			final Object collectionOwner =
					getCollectionOwner( persistenceContext, collectionDescriptor, collection, key, session );
			if ( collectionOwner == null ) {
				log.debug( "Unable to resolve owner of loading collection for second level caching. Refusing to add to cache.");
				return;
			}
			version = persistenceContext.getEntry( collectionOwner ).getVersion();
		}
		else {
			version = null;
		}

		addCollectionToCache( persistenceContext, collectionDescriptor, collection, key, version );
	}

	private static void addCollectionToCache(
			PersistenceContext context,
			CollectionPersister collectionDescriptor,
			PersistentCollection<?> collection,
			Object key,
			Object version) {
		final var session = context.getSession();
		final var factory = session.getFactory();
		final var entry = new CollectionCacheEntry( collection, collectionDescriptor );
		final var cacheAccess = collectionDescriptor.getCacheAccessStrategy();
		final Object cacheKey = cacheAccess.generateCacheKey(
				key,
				collectionDescriptor,
				session.getFactory(),
				session.getTenantIdentifier()
		);

		// CollectionRegionAccessStrategy has no update, so avoid putting uncommitted data via putFromLoad
		if ( isPutFromLoad( context, collectionDescriptor, entry ) ) {
			final var eventListenerManager = session.getEventListenerManager();
			final var eventMonitor = session.getEventMonitor();
			final var cachePutEvent = eventMonitor.beginCachePutEvent();
			boolean put = false;
			try {
				eventListenerManager.cachePutStart();
				put = cacheAccess.putFromLoad(
						session,
						cacheKey,
						collectionDescriptor.getCacheEntryStructure().structure( entry ),
						version,
						factory.getSessionFactoryOptions().isMinimalPutsEnabled()
						&& session.getCacheMode() != CacheMode.REFRESH
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

				final var statistics = factory.getStatistics();
				if ( put && statistics.isStatisticsEnabled() ) {
					statistics.collectionCachePut(
							collectionDescriptor.getNavigableRole(),
							cacheAccess.getRegion().getName()
					);
				}
			}
		}
	}

	private static boolean isPutFromLoad(
			PersistenceContext context,
			CollectionPersister collectionDescriptor,
			CollectionCacheEntry entry) {
		if ( collectionDescriptor.getElementType().isEntityType() ) {
			final var entityPersister = collectionDescriptor.getElementPersister();
			for ( Object id : entry.getState() ) {
				if ( context.wasInsertedDuringTransaction( entityPersister, id ) ) {
					return false;
				}
			}
		}
		return true;
	}

	private static Object getCollectionOwner(
			PersistenceContext context,
			CollectionPersister collectionDescriptor,
			PersistentCollection<?> collection,
			Object key,
			SharedSessionContractImplementor session) {
		final Object collectionOwner = context.getCollectionOwner( key, collectionDescriptor );
		if ( collectionOwner == null ) {
			// This happens when the collection key is defined by a property-ref. In this case, the collection key
			// and the owner key would not match up. Use the key of the owner instance associated with the collection
			// itself, if there is one. If the collection does already know about its owner, that owner should be the
			// same instance as associated with the PC, but we do the resolution against the PC anyway just to be safe,
			// since the lookup should not be costly.
			if ( collection != null ) {
				final Object linkedOwner = collection.getOwner();
				if ( linkedOwner != null ) {
					final Object ownerKey =
							collectionDescriptor.getOwnerEntityPersister()
									.getIdentifier( linkedOwner, session );
					return context.getCollectionOwner( ownerKey, collectionDescriptor );
				}
			}
			return null;
		}
		else {
			return collectionOwner;
		}
	}
}
