/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.loading.internal;

import java.io.Serializable;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.hibernate.CacheMode;
import org.hibernate.EntityMode;
import org.hibernate.HibernateException;
import org.hibernate.bytecode.spi.BytecodeEnhancementMetadata;
import org.hibernate.cache.spi.access.CollectionDataAccess;
import org.hibernate.cache.spi.entry.CollectionCacheEntry;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.CollectionEntry;
import org.hibernate.engine.spi.CollectionKey;
import org.hibernate.engine.spi.PersistenceContext;
import org.hibernate.engine.spi.SessionEventListenerManager;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.engine.spi.Status;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.collection.QueryableCollection;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.pretty.MessageHelper;
import org.hibernate.stat.spi.StatisticsImplementor;

/**
 * Represents state associated with the processing of a given {@link ResultSet}
 * in regards to loading collections.
 * <p/>
 * Another implementation option to consider is to not expose {@link ResultSet}s
 * directly (in the JDBC redesign) but to always "wrap" them and apply a
 * [series of] context[s] to that wrapper.
 *
 * @author Steve Ebersole
 */
public class CollectionLoadContext {
	private static final CoreMessageLogger LOG = CoreLogging.messageLogger( CollectionLoadContext.class );

	private final LoadContexts loadContexts;
	private final ResultSet resultSet;
	private Set<CollectionKey> localLoadingCollectionKeys = new HashSet<>();

	/**
	 * Creates a collection load context for the given result set.
	 *
	 * @param loadContexts Callback to other collection load contexts.
	 * @param resultSet The result set this is "wrapping".
	 */
	public CollectionLoadContext(LoadContexts loadContexts, ResultSet resultSet) {
		this.loadContexts = loadContexts;
		this.resultSet = resultSet;
	}

	public ResultSet getResultSet() {
		return resultSet;
	}

	public LoadContexts getLoadContext() {
		return loadContexts;
	}

	/**
	 * Retrieve the collection that is being loaded as part of processing this
	 * result set.
	 * <p/>
	 * Basically, there are two valid return values from this method:<ul>
	 * <li>an instance of {@link org.hibernate.collection.spi.PersistentCollection} which indicates to
	 * continue loading the result set row data into that returned collection
	 * instance; this may be either an instance already associated and in the
	 * midst of being loaded, or a newly instantiated instance as a matching
	 * associated collection was not found.</li>
	 * <li><i>null</i> indicates to ignore the corresponding result set row
	 * data relating to the requested collection; this indicates that either
	 * the collection was found to already be associated with the persistence
	 * context in a fully loaded state, or it was found in a loading state
	 * associated with another result set processing context.</li>
	 * </ul>
	 *
	 * @param persister The persister for the collection being requested.
	 * @param key The key of the collection being requested.
	 *
	 * @return The loading collection (see discussion above).
	 */
	public PersistentCollection getLoadingCollection(final CollectionPersister persister, final Serializable key) {
		final CollectionKey collectionKey = new CollectionKey( persister, key );
		if ( LOG.isTraceEnabled() ) {
			LOG.tracev( "Starting attempt to find loading collection [{0}]",
					MessageHelper.collectionInfoString( persister.getRole(), key ) );
		}
		final LoadingCollectionEntry loadingCollectionEntry = loadContexts.locateLoadingCollectionEntry( collectionKey );
		if ( loadingCollectionEntry == null ) {
			// look for existing collection as part of the persistence context
			PersistentCollection collection = loadContexts.getPersistenceContext().getCollection( collectionKey );
			if ( collection != null ) {
				if ( collection.wasInitialized() ) {
					LOG.trace( "Collection already initialized; ignoring" );
					// ignore this row of results! Note the early exit
					return null;
				}
				LOG.trace( "Collection not yet initialized; initializing" );
			}
			else {
				final Object owner = loadContexts.getPersistenceContext().getCollectionOwner( key, persister );
				final boolean newlySavedEntity = owner != null
						&& loadContexts.getPersistenceContext().getEntry( owner ).getStatus() != Status.LOADING;
				if ( newlySavedEntity ) {
					// important, to account for newly saved entities in query
					// todo : some kind of check for new status...
					LOG.trace( "Owning entity already loaded; ignoring" );
					return null;
				}
				// create one
				LOG.tracev( "Instantiating new collection [key={0}, rs={1}]", key, resultSet );
				collection = persister.getCollectionType().instantiate(
						loadContexts.getPersistenceContext().getSession(), persister, key );
			}
			collection.beforeInitialize( persister, -1 );
			collection.beginRead();
			localLoadingCollectionKeys.add( collectionKey );
			loadContexts.registerLoadingCollectionXRef( collectionKey, new LoadingCollectionEntry( resultSet, persister, key, collection ) );
			return collection;
		}
		if ( loadingCollectionEntry.getResultSet() == resultSet ) {
			LOG.trace( "Found loading collection bound to current result set processing; reading row" );
			return loadingCollectionEntry.getCollection();
		}
		// ignore this row, the collection is in process of
		// being loaded somewhere further "up" the stack
		LOG.trace( "Collection is already being initialized; ignoring row" );
		return null;
	}

	/**
	 * Finish the process of collection-loading for this bound result set.  Mainly this
	 * involves cleaning up resources and notifying the collections that loading is
	 * complete.
	 *
	 * @param persister The persister for which to complete loading.
	 */
	public void endLoadingCollections(CollectionPersister persister) {
		final SharedSessionContractImplementor session = getLoadContext().getPersistenceContext().getSession();
		if ( !loadContexts.hasLoadingCollectionEntries()
				&& localLoadingCollectionKeys.isEmpty() ) {
			return;
		}

		// in an effort to avoid concurrent-modification-exceptions (from
		// potential recursive calls back through here as a result of the
		// eventual call to PersistentCollection#endRead), we scan the
		// internal loadingCollections map for matches and store those matches
		// in a temp collection.  the temp collection is then used to "drive"
		// the #endRead processing.
		List<LoadingCollectionEntry> matches = null;
		final Iterator itr = localLoadingCollectionKeys.iterator();
		final PersistenceContext persistenceContext = session.getPersistenceContextInternal();
		while ( itr.hasNext() ) {
			final CollectionKey collectionKey = (CollectionKey) itr.next();
			final LoadingCollectionEntry lce = loadContexts.locateLoadingCollectionEntry( collectionKey );
			if ( lce == null ) {
				LOG.loadingCollectionKeyNotFound( collectionKey );
			}
			else if ( lce.getResultSet() == resultSet && lce.getPersister() == persister ) {
				if ( matches == null ) {
					matches = new ArrayList<>();
				}
				matches.add( lce );
				if ( lce.getCollection().getOwner() == null ) {
					persistenceContext.addUnownedCollection(
							new CollectionKey(
									persister,
									lce.getKey()
							),
							lce.getCollection()
					);
				}
				LOG.tracev( "Removing collection load entry [{0}]", lce );

				// todo : i'd much rather have this done from #endLoadingCollection(CollectionPersister,LoadingCollectionEntry)...
				loadContexts.unregisterLoadingCollectionXRef( collectionKey );
				itr.remove();
			}
		}

		endLoadingCollections( persister, matches );
		if ( localLoadingCollectionKeys.isEmpty() ) {
			// todo : hack!!!
			// NOTE : here we cleanup the load context when we have no more local
			// LCE entries.  This "works" for the time being because really
			// only the collection load contexts are implemented.  Long term,
			// this cleanup should become part of the "close result set"
			// processing from the (sandbox/jdbc) jdbc-container code.
			loadContexts.cleanup( resultSet );
		}
	}

	private void endLoadingCollections(CollectionPersister persister, List<LoadingCollectionEntry> matchedCollectionEntries) {
		if ( matchedCollectionEntries == null ) {
			if ( LOG.isDebugEnabled() ) {
				LOG.debugf( "No collections were found in result set for role: %s", persister.getRole() );
			}
			return;
		}

		final int count = matchedCollectionEntries.size();
		if ( LOG.isDebugEnabled() ) {
			LOG.debugf( "%s collections were found in result set for role: %s", count, persister.getRole() );
		}

		for ( LoadingCollectionEntry matchedCollectionEntry : matchedCollectionEntries ) {
			endLoadingCollection( matchedCollectionEntry, persister );
		}

		if ( LOG.isDebugEnabled() ) {
			LOG.debugf( "%s collections initialized for role: %s", count, persister.getRole() );
		}
	}

	private void endLoadingCollection(LoadingCollectionEntry lce, CollectionPersister persister) {
		LOG.tracev( "Ending loading collection [{0}]", lce );
		final PersistenceContext persistenceContext = getLoadContext().getPersistenceContext();
		final SharedSessionContractImplementor session = persistenceContext.getSession();

		// warning: can cause a recursive calls! (proxy initialization)
		final PersistentCollection loadingCollection = lce.getCollection();
		final boolean hasNoQueuedAdds = loadingCollection.endRead();

		if ( persister.getCollectionType().hasHolder() ) {
			persistenceContext.addCollectionHolder( loadingCollection );
		}

		CollectionEntry ce = persistenceContext.getCollectionEntry( loadingCollection );
		if ( ce == null ) {
			ce = persistenceContext.addInitializedCollection( persister, loadingCollection, lce.getKey() );
		}
		else {
			ce.postInitialize( loadingCollection );
//			if (ce.getLoadedPersister().getBatchSize() > 1) { // not the best place for doing this, moved into ce.postInitialize
//				getLoadContext().getPersistenceContext().getBatchFetchQueue().removeBatchLoadableCollection(ce); 
//			}
		}

		// The collection has been completely initialized and added to the PersistenceContext.

		if ( loadingCollection.getOwner() != null ) {
			// If the owner is bytecode-enhanced and the owner's collection value is uninitialized,
			// then go ahead and set it to the newly initialized collection.
			final EntityPersister ownerEntityPersister = persister.getOwnerEntityPersister();
			final BytecodeEnhancementMetadata bytecodeEnhancementMetadata =
					ownerEntityPersister.getBytecodeEnhancementMetadata();
			if ( bytecodeEnhancementMetadata.isEnhancedForLazyLoading() ) {
				// Lazy properties in embeddables/composites are not currently supported for embeddables (HHH-10480),
				// so check to make sure the collection is not in an embeddable before checking to see if
				// the collection is lazy.
				// TODO: More will probably need to be done here when HHH-10480 is fixed..
				if ( StringHelper.qualifier( persister.getRole() ).length() ==
						ownerEntityPersister.getEntityName().length() ) {
					// Assume the collection is not in an embeddable.
					// Strip off <entityName><dot> to get the collection property name.
					final String propertyName = persister.getRole().substring(
							ownerEntityPersister.getEntityName().length() + 1
					);
					if ( !bytecodeEnhancementMetadata.isAttributeLoaded( loadingCollection.getOwner(), propertyName ) ) {
						int propertyIndex = ownerEntityPersister.getEntityMetamodel().getPropertyIndex(
								propertyName
						);
						ownerEntityPersister.setPropertyValue(
								loadingCollection.getOwner(),
								propertyIndex,
								loadingCollection
						);
					}
				}
			}
		}

		// add to cache if:
		boolean addToCache =
				// there were no queued additions
				hasNoQueuedAdds
				// and the role has a cache
				&& persister.hasCache()
				// and this is not a forced initialization during flush
				&& session.getCacheMode().isPutEnabled() && !ce.isDoremove();
		if ( addToCache ) {
			addCollectionToCache( lce, persister );
		}

		if ( LOG.isDebugEnabled() ) {
			LOG.debugf(
					"Collection fully initialized: %s",
					MessageHelper.collectionInfoString( persister, loadingCollection, lce.getKey(), session )
			);
		}
		final StatisticsImplementor statistics = session.getFactory().getStatistics();
		if ( statistics.isStatisticsEnabled() ) {
			statistics.loadCollection( persister.getRole() );
		}
	}

	/**
	 * Add the collection to the second-level cache
	 *
	 * @param lce The entry representing the collection to add
	 * @param persister The persister
	 */
	private void addCollectionToCache(LoadingCollectionEntry lce, CollectionPersister persister) {
		final PersistenceContext persistenceContext = getLoadContext().getPersistenceContext();
		final SharedSessionContractImplementor session = persistenceContext.getSession();
		final SessionFactoryImplementor factory = session.getFactory();

		if ( LOG.isDebugEnabled() ) {
			LOG.debugf( "Caching collection: %s", MessageHelper.collectionInfoString( persister, lce.getCollection(), lce.getKey(), session ) );
		}

		if ( session.getLoadQueryInfluencers().hasEnabledFilters() && persister.isAffectedByEnabledFilters( session ) ) {
			// some filters affecting the collection are enabled on the session, so do not do the put into the cache.
			if ( LOG.isDebugEnabled() ) {
				LOG.debug( "Refusing to add to cache due to enabled filters" );
			}
			// todo : add the notion of enabled filters to the cache key to differentiate filtered collections from non-filtered;
			//      DefaultInitializeCollectionEventHandler.initializeCollectionFromCache() (which makes sure to not read from
			//      cache with enabled filters).
			// EARLY EXIT!!!!!
			return;
		}

		final Object version;
		if ( persister.isVersioned() ) {
			Object collectionOwner = persistenceContext.getCollectionOwner( lce.getKey(), persister );
			if ( collectionOwner == null ) {
				// generally speaking this would be caused by the collection key being defined by a property-ref, thus
				// the collection key and the owner key would not match up.  In this case, try to use the key of the
				// owner instance associated with the collection itself, if one.  If the collection does already know
				// about its owner, that owner should be the same instance as associated with the PC, but we do the
				// resolution against the PC anyway just to be safe since the lookup should not be costly.
				if ( lce.getCollection() != null ) {
					final Object linkedOwner = lce.getCollection().getOwner();
					if ( linkedOwner != null ) {
						final Serializable ownerKey = persister.getOwnerEntityPersister().getIdentifier( linkedOwner, session );
						collectionOwner = persistenceContext.getCollectionOwner( ownerKey, persister );
					}
				}
				if ( collectionOwner == null ) {
					throw new HibernateException(
							"Unable to resolve owner of loading collection [" +
									MessageHelper.collectionInfoString( persister, lce.getCollection(), lce.getKey(), session ) +
									"] for second level caching"
					);
				}
			}
			version = persistenceContext.getEntry( collectionOwner ).getVersion();
		}
		else {
			version = null;
		}

		final CollectionCacheEntry entry = new CollectionCacheEntry( lce.getCollection(), persister );
		final CollectionDataAccess cacheAccess = persister.getCacheAccessStrategy();
		final Object cacheKey = cacheAccess.generateCacheKey(
				lce.getKey(),
				persister,
				session.getFactory(),
				session.getTenantIdentifier()
		);

		boolean isPutFromLoad = true;
		if ( persister.getElementType().isAssociationType() ) {
			for ( Serializable id : entry.getState() ) {
				EntityPersister entityPersister = ( (QueryableCollection) persister ).getElementPersister();
				if ( persistenceContext.wasInsertedDuringTransaction( entityPersister, id ) ) {
					isPutFromLoad = false;
					break;
				}
			}
		}

		// CollectionRegionAccessStrategy has no update, so avoid putting uncommitted data via putFromLoad
		if (isPutFromLoad) {
			final SessionEventListenerManager eventListenerManager = session.getEventListenerManager();
			try {
				eventListenerManager.cachePutStart();
				final boolean put = cacheAccess.putFromLoad(
						session,
						cacheKey,
						persister.getCacheEntryStructure().structure( entry ),
						version,
						factory.getSessionFactoryOptions().isMinimalPutsEnabled() && session.getCacheMode()!= CacheMode.REFRESH
				);

				final StatisticsImplementor statistics = factory.getStatistics();
				if ( put && statistics.isStatisticsEnabled() ) {
					statistics.collectionCachePut(
							persister.getNavigableRole(),
							persister.getCacheAccessStrategy().getRegion().getName()
					);
				}
			}
			finally {
				eventListenerManager.cachePutEnd();
			}
		}
	}

	void cleanup() {
		if ( !localLoadingCollectionKeys.isEmpty() ) {
			LOG.localLoadingCollectionKeysCount( localLoadingCollectionKeys.size() );
		}
		loadContexts.cleanupCollectionXRefs( localLoadingCollectionKeys );
		localLoadingCollectionKeys.clear();
	}


	@Override
	public String toString() {
		return super.toString() + "<rs=" + resultSet + ">";
	}
}
