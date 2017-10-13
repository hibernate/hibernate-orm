/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.internal;

import java.io.Serializable;

import org.hibernate.AssertionFailure;
import org.hibernate.CacheMode;
import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.cache.spi.access.EntityDataAccess;
import org.hibernate.cache.spi.entry.CacheEntry;
import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.engine.spi.EntityKey;
import org.hibernate.engine.spi.PersistenceContext;
import org.hibernate.engine.spi.SessionEventListenerManager;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.engine.spi.Status;
import org.hibernate.event.service.spi.EventListenerGroup;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.EventType;
import org.hibernate.event.spi.PostLoadEvent;
import org.hibernate.event.spi.PostLoadEventListener;
import org.hibernate.event.spi.PreLoadEvent;
import org.hibernate.event.spi.PreLoadEventListener;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.metamodel.model.domain.spi.EntityDescriptor;
import org.hibernate.metamodel.model.domain.spi.StateArrayContributor;
import org.hibernate.pretty.MessageHelper;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.type.internal.TypeHelper;

import org.jboss.logging.Logger;

/**
 * Functionality relating to the Hibernate two-phase loading process, that may be reused by descriptors
 * that do not use the Loader framework
 *
 * @author Gavin King
 *
 * @deprecated to be removed in 6.0 (this was always considered an internal class); two-phase loading is
 * handled via {@link org.hibernate.sql.results.spi.Initializer} and friends
 */
@Deprecated
public final class TwoPhaseLoad {
	private static final CoreMessageLogger LOG = Logger.getMessageLogger(
			CoreMessageLogger.class,
			TwoPhaseLoad.class.getName()
	);

	private TwoPhaseLoad() {
	}

	/**
	 * Register the "hydrated" state of an entity instance, afterQuery the first step of 2-phase loading.
	 *
	 * Add the "hydrated state" (an array) of an uninitialized entity to the session. We don't try
	 * to resolve any associations yet, because there might be other entities waiting to be
	 * read from the JDBC result set we are currently processing
	 *
	 * @param descriptor The descriptor for the hydrated entity
	 * @param id The entity identifier
	 * @param values The entity values
	 * @param rowId The rowId for the entity
	 * @param object An optional instance for the entity being loaded
	 * @param lockMode The lock mode
	 * @param session The Session
	 */
	public static void postHydrate(
			final EntityDescriptor descriptor,
			final Serializable id,
			final Object[] values,
			final Object rowId,
			final Object object,
			final LockMode lockMode,
			final SharedSessionContractImplementor session) {
		final Object version = Versioning.getVersion( values, descriptor );
		session.getPersistenceContext().addEntry(
				object,
				Status.LOADING,
				values,
				rowId,
				id,
				version,
				lockMode,
				true,
				descriptor,
				false
			);

		if ( version != null && LOG.isTraceEnabled() ) {
			final String versionStr = descriptor.getHierarchy().getVersionDescriptor() != null
					? descriptor.getHierarchy().getVersionDescriptor().getJavaTypeDescriptor().extractLoggableRepresentation( version )
					: "null";
			LOG.tracef( "Version: %s", versionStr );
		}
	}

	/**
	 * Perform the second step of 2-phase load. Fully initialize the entity
	 * instance.
	 * <p/>
	 * After processing a JDBC result set, we "resolve" all the associations
	 * between the entities which were instantiated and had their state
	 * "hydrated" into an array
	 *
	 * @param entity The entity being loaded
	 * @param readOnly Is the entity being loaded as read-only
	 * @param session The Session
	 * @param preLoadEvent The (re-used) pre-load event
	 */
	public static void initializeEntity(
			final Object entity,
			final boolean readOnly,
			final SharedSessionContractImplementor session,
			final PreLoadEvent preLoadEvent) {
		final PersistenceContext persistenceContext = session.getPersistenceContext();
		final EntityEntry entityEntry = persistenceContext.getEntry( entity );
		if ( entityEntry == null ) {
			throw new AssertionFailure( "possible non-threadsafe access to the session" );
		}
		doInitializeEntity( entity, entityEntry, readOnly, session, preLoadEvent );
	}

	private static void doInitializeEntity(
			final Object entity,
			final EntityEntry entityEntry,
			final boolean readOnly,
			final SharedSessionContractImplementor session,
			final PreLoadEvent preLoadEvent) throws HibernateException {
		final PersistenceContext persistenceContext = session.getPersistenceContext();
		final EntityDescriptor<?> entityDescriptor = entityEntry.getDescriptor();
		final Serializable id = entityEntry.getId();
		final Object[] hydratedState = entityEntry.getLoadedState();

		final boolean debugEnabled = LOG.isDebugEnabled();
		if ( debugEnabled ) {
			LOG.debugf(
					"Resolving associations for %s",
					MessageHelper.infoString( entityDescriptor, id, session.getFactory() )
			);
		}

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

		//Must occur afterQuery resolving identifiers!
		if ( session.isEventSource() ) {
			preLoadEvent.setEntity( entity ).setState( hydratedState ).setId( id ).setDescriptor( entityDescriptor );

			final EventListenerGroup<PreLoadEventListener> listenerGroup = session
					.getFactory()
					.getServiceRegistry()
					.getService( EventListenerRegistry.class )
					.getEventListenerGroup( EventType.PRE_LOAD );
			for ( PreLoadEventListener listener : listenerGroup.listeners() ) {
				listener.onPreLoad( preLoadEvent );
			}
		}

		entityDescriptor.setPropertyValues( entity, hydratedState );

		final SessionFactoryImplementor factory = session.getFactory();
		final EntityDataAccess cacheAccess = entityDescriptor.getHierarchy().getEntityCacheAccess();
		if ( cacheAccess != null && session.getCacheMode().isPutEnabled() ) {

			if ( debugEnabled ) {
				LOG.debugf(
						"Adding entity to second-level cache: %s",
						MessageHelper.infoString( entityDescriptor, id, session.getFactory() )
				);
			}

			final Object version = Versioning.getVersion( hydratedState, entityDescriptor );
			final CacheEntry entry = entityDescriptor.buildCacheEntry( entity, hydratedState, version, session );
			final Object cacheKey = cacheAccess.generateCacheKey(
					id,
					entityDescriptor.getHierarchy(),
					factory,
					session.getTenantIdentifier()
			);

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
							useMinimalPuts( session, entityEntry )
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
		if ( !entityDescriptor.getJavaTypeDescriptor().getMutabilityPlan().isMutable() ) {
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

		entityDescriptor.afterInitialize( entity, session );

		if ( debugEnabled ) {
			LOG.debugf(
					"Done materializing entity %s",
					MessageHelper.infoString( entityDescriptor, id, session.getFactory() )
			);
		}

		if ( factory.getStatistics().isStatisticsEnabled() ) {
			factory.getStatistics().loadEntity( entityDescriptor.getEntityName() );
		}
	}
	
	/**
	 * PostLoad cannot occur during initializeEntity, as that call occurs *beforeQuery*
	 * the Set collections are added to the persistence context by Loader.
	 * Without the split, LazyInitializationExceptions can occur in the Entity's
	 * postLoad if it acts upon the collection.
	 *
	 * HHH-6043
	 * 
	 * @param entity The entity
	 * @param session The Session
	 * @param postLoadEvent The (re-used) post-load event
	 */
	public static void postLoad(
			final Object entity,
			final SharedSessionContractImplementor session,
			final PostLoadEvent postLoadEvent) {
		
		if ( session.isEventSource() ) {
			final PersistenceContext persistenceContext
					= session.getPersistenceContext();
			final EntityEntry entityEntry = persistenceContext.getEntry( entity );

			postLoadEvent.setEntity( entity ).setId( entityEntry.getId() ).setDescriptor( entityEntry.getDescriptor() );

			final EventListenerGroup<PostLoadEventListener> listenerGroup = session.getFactory()
							.getServiceRegistry()
							.getService( EventListenerRegistry.class )
							.getEventListenerGroup( EventType.POST_LOAD );
			for ( PostLoadEventListener listener : listenerGroup.listeners() ) {
				listener.onPostLoad( postLoadEvent );
			}
		}
	}

	private static boolean useMinimalPuts(SharedSessionContractImplementor session, EntityEntry entityEntry) {
		if ( session.getFactory().getSessionFactoryOptions().isMinimalPutsEnabled() ) {
			return session.getCacheMode() != CacheMode.REFRESH;
		}
		else {
			return entityEntry.getDescriptor().hasLazyProperties()
					&& entityEntry.getDescriptor().isLazyPropertiesCacheable();
		}
	}

	/**
	 * Add an uninitialized instance of an entity class, as a placeholder to ensure object
	 * identity. Must be called beforeQuery <tt>postHydrate()</tt>.
	 *
	 * Create a "temporary" entry for a newly instantiated entity. The entity is uninitialized,
	 * but we need the mapping from id to instance in order to guarantee uniqueness.
	 *
	 * @param key The entity key
	 * @param object The entity instance
	 * @param descriptor The entity descriptor
	 * @param lockMode The lock mode
	 * @param session The Session
	 */
	public static void addUninitializedEntity(
			final EntityKey key,
			final Object object,
			final EntityDescriptor descriptor,
			final LockMode lockMode,
			final SharedSessionContractImplementor session) {
		session.getPersistenceContext().addEntity(
				object,
				Status.LOADING,
				null,
				key,
				null,
				lockMode,
				true,
				descriptor,
				false
		);
	}

	/**
	 * Same as {@link #addUninitializedEntity}, but here for an entity from the second level cache
	 *
	 * @param key The entity key
	 * @param object The entity instance
	 * @param descriptor The entity descriptor
	 * @param lockMode The lock mode
	 * @param version The version
	 * @param session The Session
	 */
	public static void addUninitializedCachedEntity(
			final EntityKey key,
			final Object object,
			final EntityDescriptor descriptor,
			final LockMode lockMode,
			final Object version,
			final SharedSessionContractImplementor session) {
		session.getPersistenceContext().addEntity(
				object,
				Status.LOADING,
				null,
				key,
				version,
				lockMode,
				true,
				descriptor,
				false
		);
	}
}
