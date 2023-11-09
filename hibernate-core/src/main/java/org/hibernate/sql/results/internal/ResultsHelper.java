/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.internal;

import java.util.List;
import java.util.function.Supplier;

import org.hibernate.CacheMode;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
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
import org.hibernate.event.spi.EventManager;
import org.hibernate.event.spi.HibernateEvent;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.collection.QueryableCollection;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.pretty.MessageHelper;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ast.spi.SqlAstCreationContext;
import org.hibernate.sql.exec.spi.ExecutionContext;
import org.hibernate.sql.results.ResultsLogger;
import org.hibernate.sql.results.graph.AssemblerCreationState;
import org.hibernate.sql.results.graph.DomainResultAssembler;
import org.hibernate.sql.results.graph.Initializer;
import org.hibernate.sql.results.graph.instantiation.DynamicInstantiationResult;
import org.hibernate.sql.results.jdbc.spi.JdbcValues;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesMapping;
import org.hibernate.sql.results.spi.RowReader;
import org.hibernate.sql.results.spi.RowTransformer;
import org.hibernate.stat.spi.StatisticsImplementor;

/**
 * @author Steve Ebersole
 */
public class ResultsHelper {

	private static final CoreMessageLogger LOG = CoreLogging.messageLogger( ResultsHelper.class );

	public static <R> RowReader<R> createRowReader(
			ExecutionContext executionContext,
			LockOptions lockOptions,
			RowTransformer<R> rowTransformer,
			Class<R> transformedResultJavaType,
			JdbcValues jdbcValues) {
		return createRowReader( executionContext, lockOptions, rowTransformer, transformedResultJavaType, jdbcValues.getValuesMapping());
	}

	public static <R> RowReader<R> createRowReader(
			ExecutionContext executionContext,
			LockOptions lockOptions,
			RowTransformer<R> rowTransformer,
			Class<R> transformedResultJavaType,
			JdbcValuesMapping jdbcValuesMapping) {
		final SessionFactoryImplementor sessionFactory = executionContext.getSession().getFactory();

		//custom Map<NavigablePath, Initializer>
		final NavigablePathMapToInitializer initializerMap = new NavigablePathMapToInitializer();
		final InitializersList.Builder initializersBuilder = new InitializersList.Builder();

		final List<DomainResultAssembler<?>> assemblers = jdbcValuesMapping.resolveAssemblers(
				new AssemblerCreationState() {
					Boolean dynamicInstantiation;

					@Override
					public boolean isScrollResult() {
						return executionContext.isScrollResult();
					}

					@Override
					public boolean isDynamicInstantiation() {
						if ( dynamicInstantiation == null ) {
							dynamicInstantiation = jdbcValuesMapping.getDomainResults()
									.stream()
									.anyMatch( domainResult -> domainResult instanceof DynamicInstantiationResult );
						}
						return dynamicInstantiation;
					}

					@Override
					public LockMode determineEffectiveLockMode(String identificationVariable) {
						return lockOptions.getEffectiveLockMode( identificationVariable );
					}

					@Override
					public Initializer resolveInitializer(
							NavigablePath navigablePath,
							ModelPart fetchedModelPart,
							Supplier<Initializer> producer) {
						final Initializer existing = initializerMap.get( navigablePath );
						if ( existing != null ) {
							if ( fetchedModelPart.getNavigableRole().equals(
									existing.getInitializedPart().getNavigableRole() ) ) {
								ResultsLogger.RESULTS_MESSAGE_LOGGER.tracef(
										"Returning previously-registered initializer : %s",
										existing
								);
								return existing;
							}
						}

						final Initializer initializer = producer.get();
						ResultsLogger.RESULTS_MESSAGE_LOGGER.tracef(
								"Registering initializer : %s",
								initializer
						);

						initializerMap.put( navigablePath, initializer );
						initializersBuilder.addInitializer( initializer );

						return initializer;
					}

					@Override
					public SqlAstCreationContext getSqlAstCreationContext() {
						return sessionFactory;
					}

					@Override
					public ExecutionContext getExecutionContext() {
						return executionContext;
					}
				}
		);

		initializerMap.logInitializers();

		final InitializersList initializersList = initializersBuilder.build( initializerMap );

		return new StandardRowReader<>( assemblers, initializersList, rowTransformer, transformedResultJavaType );
	}

	public static void finalizeCollectionLoading(
			PersistenceContext persistenceContext,
			CollectionPersister collectionDescriptor,
			PersistentCollection<?> collectionInstance,
			Object key,
			boolean hasNoQueuedAdds) {
		CollectionEntry collectionEntry = persistenceContext.getCollectionEntry( collectionInstance );
		if ( collectionEntry == null ) {
			collectionEntry = persistenceContext.addInitializedCollection(
					collectionDescriptor,
					collectionInstance,
					key
			);
		}
		else {
			collectionEntry.postInitialize( collectionInstance );
		}

		if ( collectionDescriptor.getCollectionType().hasHolder() ) {
			// in case of PersistentArrayHolder we have to realign the EntityEntry loaded state with
			// the entity values
			final Object owner = collectionInstance.getOwner();
			final EntityEntry entry = persistenceContext.getEntry( owner );
			final PluralAttributeMapping mapping = collectionDescriptor.getAttributeMapping();
			final int propertyIndex = mapping.getStateArrayPosition();
			final Object[] loadedState = entry.getLoadedState();
			loadedState[propertyIndex] = mapping.getValue( owner );
			persistenceContext.addCollectionHolder( collectionInstance );
		}

		final BatchFetchQueue batchFetchQueue = persistenceContext.getBatchFetchQueue();
		batchFetchQueue.removeBatchLoadableCollection( collectionEntry );

		final SharedSessionContractImplementor session = persistenceContext.getSession();
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

		if ( LOG.isDebugEnabled() ) {
			LOG.debugf(
					"Collection fully initialized: %s",
					MessageHelper.collectionInfoString(
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

		if ( LOG.isDebugEnabled() ) {
			LOG.debugf( "Caching collection: %s", MessageHelper.collectionInfoString( collectionDescriptor, collectionInstance, key, session ) );
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
		if ( collectionDescriptor.getElementType().isAssociationType() ) {
			final EntityPersister entityPersister = ( (QueryableCollection) collectionDescriptor ).getElementPersister();
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
			final EventManager eventManager = session.getEventManager();
			final HibernateEvent cachePutEvent = eventManager.beginCachePutEvent();
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
				eventManager.completeCachePutEvent(
						cachePutEvent,
						session,
						cacheAccess,
						collectionDescriptor,
						put,
						EventManager.CacheActionDescription.COLLECTION_INSERT
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
