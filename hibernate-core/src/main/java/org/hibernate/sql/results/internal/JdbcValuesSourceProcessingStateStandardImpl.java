/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.internal;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import org.hibernate.AssertionFailure;
import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.cache.spi.access.EntityDataAccess;
import org.hibernate.cache.spi.entry.CacheEntry;
import org.hibernate.engine.internal.Versioning;
import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.engine.spi.EntityKey;
import org.hibernate.engine.spi.PersistenceContext;
import org.hibernate.engine.spi.SessionEventListenerManager;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.engine.spi.Status;
import org.hibernate.event.service.spi.EventListenerGroup;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.EventSource;
import org.hibernate.event.spi.EventType;
import org.hibernate.event.spi.PostLoadEvent;
import org.hibernate.event.spi.PostLoadEventListener;
import org.hibernate.event.spi.PreLoadEvent;
import org.hibernate.event.spi.PreLoadEventListener;
import org.hibernate.metamodel.model.domain.spi.EntityDescriptor;
import org.hibernate.metamodel.model.domain.spi.StateArrayContributor;
import org.hibernate.pretty.MessageHelper;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.sql.exec.spi.ExecutionContext;
import org.hibernate.sql.results.spi.JdbcValuesSourceProcessingOptions;
import org.hibernate.sql.results.spi.JdbcValuesSourceProcessingState;
import org.hibernate.sql.results.spi.LoadingEntityEntry;
import org.hibernate.type.internal.TypeHelper;

import org.jboss.logging.Logger;

/**
 * @author Steve Ebersole
 */
public class JdbcValuesSourceProcessingStateStandardImpl implements JdbcValuesSourceProcessingState {
	private static final Logger log = Logger.getLogger( JdbcValuesSourceProcessingStateStandardImpl.class );

	private final ExecutionContext executionContext;
	private final JdbcValuesSourceProcessingOptions processingOptions;

	private Map<EntityKey,LoadingEntityEntry> loadingEntityMap;
	private Map<Object,EntityKey> hydratedEntityKeys;

	// todo (6.0) : "loading collections" as well?

	public JdbcValuesSourceProcessingStateStandardImpl(
			ExecutionContext executionContext,
			JdbcValuesSourceProcessingOptions processingOptions) {
		this.executionContext = executionContext;
		this.processingOptions = processingOptions;
	}

	@Override
	public QueryOptions getQueryOptions() {
		return executionContext.getQueryOptions();
	}

	@Override
	public JdbcValuesSourceProcessingOptions getProcessingOptions() {
		return processingOptions;
	}

	@Override
	public void registerLoadingEntity(
			EntityKey entityKey,
			Function<EntityKey,LoadingEntityEntry> entryProducer) {
		if ( loadingEntityMap == null ) {
			loadingEntityMap = new HashMap<>();
		}

		final LoadingEntityEntry loadingEntity = loadingEntityMap.compute(
				entityKey,
				(key, existingValue) -> {
					if ( existingValue == null ) {
						log.debugf(
								"Generating LoadingEntity registration : %s[id=%s]",
								entityKey.getEntityName(),
								entityKey.getIdentifier()
						);
						return entryProducer.apply( key );
					}
					else {
						log.debugf(
								"Attempt to add duplicate LoadingEntity registration for same EntityKey [%s]",
								entityKey
						);
						return existingValue;
					}
				}
		);

		if ( hydratedEntityKeys == null ) {
			hydratedEntityKeys = new HashMap<>();
		}

		hydratedEntityKeys.put( loadingEntity.getEntityInstance(), entityKey );
	}

	@Override
	public SharedSessionContractImplementor getPersistenceContext() {
		return executionContext.getSession();
	}

	@Override
	public void finishUp() {
		// for arrays, we should end the collection load beforeQuery resolving the entities, since the
		// actual array instances are not instantiated during loading
		finishLoadingArrays();

		// now finish loading the entities (2-phase load)
		performTwoPhaseLoad();

		// now we can finalize loading collections
		finishLoadingCollections();

		// finally, perform post-load operations
		postLoad();
	}

	private void finishLoadingArrays() {
//		for ( CollectionReferenceInitializer arrayReferenceInitializer : arrayReferenceInitializers ) {
//			arrayReferenceInitializer.endLoading( context );
//		}
	}

	private void performTwoPhaseLoad() {
		if ( loadingEntityMap == null ) {
			return;
		}

		// IMPORTANT: reuse the same event instances for performance!
		final PreLoadEvent preLoadEvent;
		if ( executionContext.getSession().isEventSource() ) {
			preLoadEvent = new PreLoadEvent( (EventSource) executionContext.getSession() );
		}
		else {
			preLoadEvent = null;
		}

		log.tracev( "Total objects hydrated: {0}", loadingEntityMap.size() );

		// todo : consider the feasibility of building a dependency graph for association dependencies
		// 		e.g. Employee#1 should be resolved before Employee#2 when Employee#1 is Employee#2's manager
		//		this could happen inside #registerHydratedEntity

		for ( LoadingEntityEntry loadingEntity : loadingEntityMap.values() ) {
			//TwoPhaseLoad.initializeEntity(
			initializeEntity(
					loadingEntity,
					isReadOnly(),
					executionContext.getSession(),
					preLoadEvent
			);
		}
	}

	@SuppressWarnings("SimplifiableIfStatement")
	private boolean isReadOnly() {
		if ( getQueryOptions().isReadOnly() != null ) {
			return getQueryOptions().isReadOnly();
		}

		if ( executionContext.getSession() instanceof EventSource ) {
			return executionContext.getSession().isDefaultReadOnly();
		}

		return false;
	}

	/**
	 * Copy of TwoPhaseLoad#initializeEntity until that can be adapted to this SQL-AST approach
	 */
	public void initializeEntity(
			final LoadingEntityEntry loadingEntity,
			final boolean readOnly,
			final SharedSessionContractImplementor session,
			final PreLoadEvent preLoadEvent) {
		final PersistenceContext persistenceContext = session.getPersistenceContext();
		EntityEntry entityEntry = persistenceContext.getEntry( loadingEntity.getEntityInstance() );
		if ( entityEntry == null ) {
			final EntityKey entityKey = hydratedEntityKeys.get( loadingEntity.getEntityInstance() );
			if ( entityKey == null ) {
				throw new AssertionFailure( "possible non-threadsafe access to the session - could not locate EntityEntry for entity instance : " + loadingEntity
						.getEntityInstance() );
			}

			// todo (6.0) : shouldn't we wait until hydratedState is "resolved" before adding the entry?
			//		that depends on having access to loading entities from "other ResultSets"
			//		as we have when force loading non-lazy associations

			entityEntry = persistenceContext.addEntity(
					entityKey,
					Status.LOADING,
					loadingEntity.getHydratedEntityState(),
					loadingEntity.getEntityKey(),
					// todo : we need to handle version
					null,
					// todo : handle LockMode
					LockMode.NONE,
					true,
					loadingEntity.getDescriptor(),
					// disableVersionIncrement?
					false
			);
		}
		doInitializeEntity( loadingEntity, entityEntry, readOnly, session, preLoadEvent );
	}

	@SuppressWarnings("unchecked")
	private void doInitializeEntity(
			final LoadingEntityEntry loadingEntity,
			final EntityEntry entityEntry,
			final boolean readOnly,
			final SharedSessionContractImplementor session,
			final PreLoadEvent preLoadEvent) throws HibernateException {
		final PersistenceContext persistenceContext = session.getPersistenceContext();
		final EntityDescriptor<?> entityDescriptor = entityEntry.getDescriptor();
		final Serializable id = entityEntry.getId();
		final Object[] hydratedState = entityEntry.getLoadedState();

		final boolean debugEnabled = log.isDebugEnabled();
		if ( debugEnabled ) {
			log.debugf(
					"Resolving associations for %s",
					MessageHelper.infoString( entityDescriptor, id, session.getFactory() )
			);
		}

		final Object entityInstance = loadingEntity.getEntityInstance();

		for ( StateArrayContributor<?> contributor : entityDescriptor.getStateArrayContributors() ) {
			final int position = contributor.getStateArrayPosition();
			final Object value = hydratedState[position];

			hydratedState[ contributor.getStateArrayPosition() ] = contributor.resolveHydratedState(
					value,
					session,
					// the container ("owner")... for now just pass null.
					// ultimately we need to account for fetch parent if the
					// current sub-contributor is a fetch
					null
			);
		}

		// Must occur after resolving identifiers!
		if ( session.isEventSource() ) {
			preLoadEvent.setEntity( entityInstance ).setState( hydratedState ).setId( id ).setPersister( entityDescriptor );

			final EventListenerGroup<PreLoadEventListener> listenerGroup = session
					.getFactory()
					.getServiceRegistry()
					.getService( EventListenerRegistry.class )
					.getEventListenerGroup( EventType.PRE_LOAD );
			for ( PreLoadEventListener listener : listenerGroup.listeners() ) {
				listener.onPreLoad( preLoadEvent );
			}
		}

		entityDescriptor.setPropertyValues( entityInstance, hydratedState );

		final SessionFactoryImplementor factory = session.getFactory();
		final EntityDataAccess cacheAccess = factory.getCache().getEntityRegionAccess( entityDescriptor.getHierarchy() );
		if ( cacheAccess != null && session.getCacheMode().isPutEnabled() ) {

			if ( debugEnabled ) {
				log.debugf(
						"Adding entityInstance to second-level cache: %s",
						MessageHelper.infoString( entityDescriptor, id, session.getFactory() )
				);
			}

			final Object version = Versioning.getVersion( hydratedState, entityDescriptor );
			final CacheEntry entry = entityDescriptor.buildCacheEntry( entityInstance, hydratedState, version, session );
			final Object cacheKey = cacheAccess.generateCacheKey( id, entityDescriptor.getHierarchy(), factory, session.getTenantIdentifier() );

			// explicit handling of caching for rows just inserted and then somehow forced to be read
			// from the database *within the same transaction*.  usually this is done by
			// 		1) Session#refresh, or
			// 		2) Session#clear + some form of load
			//
			// we need to be careful not to clobber the lock here in the cache so that it can be rolled back if need be
			if ( session.getPersistenceContext().wasInsertedDuringTransaction( entityDescriptor, id ) ) {
				cacheAccess.update(
						session,
						cacheKey,
						entityDescriptor.getCacheEntryStructure().structure( entry ),
						version,
						version
				);
			}
			else {
				final SessionEventListenerManager eventListenerManager = session.getEventListenerManager();
				try {
					eventListenerManager.cachePutStart();
					final boolean put = cacheAccess.putFromLoad(
							session,
							cacheKey,
							entityDescriptor.getCacheEntryStructure().structure( entry ),
							version,
							//useMinimalPuts( session, entityEntry )
							false
					);

					if ( put && factory.getStatistics().isStatisticsEnabled() ) {
						factory.getStatistics().secondLevelCachePut( cacheAccess.getRegion().getName() );
					}
				}
				finally {
					eventListenerManager.cachePutEnd();
				}
			}
		}

		if ( entityDescriptor.getHierarchy().getNaturalIdDescriptor() != null ) {
			persistenceContext.getNaturalIdHelper().cacheNaturalIdCrossReferenceFromLoad(
					entityDescriptor,
					id,
					persistenceContext.getNaturalIdHelper().extractNaturalIdValues( hydratedState, entityDescriptor )
			);
		}

		boolean isReallyReadOnly = readOnly;
		if ( !entityDescriptor.getHierarchy().isMutable() ) {
			isReallyReadOnly = true;
		}
		else {
			final Object proxy = persistenceContext.getProxy( entityEntry.getEntityKey() );
			if ( proxy != null ) {
				// there is already a proxy for this impl
				// only set the status to read-only if the proxy is read-only
				isReallyReadOnly = ( (HibernateProxy) proxy ).getHibernateLazyInitializer().isReadOnly();
			}
		}
		if ( isReallyReadOnly ) {
			//no need to take a snapshot - this is a
			//performance optimization, but not really
			//important, except for entities with huge
			//mutable property values
			persistenceContext.setEntryStatus( entityEntry, Status.READ_ONLY );
		}
		else {
			//take a snapshot
			TypeHelper.deepCopy(
					entityDescriptor,
					hydratedState,
					hydratedState,
					StateArrayContributor::isUpdatable
			);
			persistenceContext.setEntryStatus( entityEntry, Status.MANAGED );
		}

		entityDescriptor.afterInitialize( entityInstance, session );

		if ( debugEnabled ) {
			log.debugf(
					"Done materializing entityInstance %s",
					MessageHelper.infoString( entityDescriptor, id, session.getFactory() )
			);
		}

		if ( factory.getStatistics().isStatisticsEnabled() ) {
			factory.getStatistics().loadEntity( entityDescriptor.getEntityName() );
		}
	}

	private void finishLoadingCollections() {
//		for ( InitializerCollection initializer : initializers ) {
//			initializer.endLoading( context );
//		}

		// todo (6.0) : need something like org.hibernate.engine.loading.internal.LoadingCollectionEntry

		throw new NotYetImplementedFor6Exception(  );
	}

	private void postLoad() {
		if ( loadingEntityMap == null ) {
			return;
		}

		// IMPORTANT: reuse the same event instances for performance!
		final PostLoadEvent postLoadEvent;
		if ( executionContext.getSession().isEventSource() ) {
			postLoadEvent = new PostLoadEvent( (EventSource) executionContext.getSession() );
		}
		else {
			postLoadEvent = null;
		}

		if ( !getPersistenceContext().isEventSource() ) {
			return;
		}

		for ( LoadingEntityEntry loadingEntity : loadingEntityMap.values() ) {
			final PersistenceContext persistenceContext = getPersistenceContext().getPersistenceContext();
			final EntityEntry entityEntry = persistenceContext.getEntry( loadingEntity.getEntityInstance() );

			postLoadEvent.setEntity( loadingEntity.getEntityInstance() )
					.setId( entityEntry.getId() )
					.setDescriptor( entityEntry.getDescriptor() );

			final EventListenerGroup<PostLoadEventListener> listenerGroup = getPersistenceContext().getFactory()
					.getServiceRegistry()
					.getService( EventListenerRegistry.class )
					.getEventListenerGroup( EventType.POST_LOAD );
			for ( PostLoadEventListener listener : listenerGroup.listeners() ) {
				listener.onPostLoad( postLoadEvent );
			}
		}
	}

}
