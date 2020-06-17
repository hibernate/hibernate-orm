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
import org.hibernate.bytecode.enhance.spi.LazyPropertyInitializer;
import org.hibernate.cache.spi.access.EntityDataAccess;
import org.hibernate.cache.spi.entry.CacheEntry;
import org.hibernate.engine.profile.Fetch;
import org.hibernate.engine.profile.FetchProfile;
import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.engine.spi.EntityKey;
import org.hibernate.engine.spi.LoadQueryInfluencers;
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
import org.hibernate.graph.spi.AttributeNodeImplementor;
import org.hibernate.graph.spi.GraphImplementor;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.pretty.MessageHelper;
import org.hibernate.property.access.internal.PropertyAccessStrategyBackRefImpl;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.stat.internal.StatsHelper;
import org.hibernate.stat.spi.StatisticsImplementor;
import org.hibernate.type.EntityType;
import org.hibernate.type.Type;
import org.hibernate.type.TypeHelper;

import org.jboss.logging.Logger;

/**
 * Functionality relating to the Hibernate two-phase loading process, that may be reused by persisters
 * that do not use the Loader framework
 *
 * @author Gavin King
 */
public final class TwoPhaseLoad {
	private static final CoreMessageLogger LOG = Logger.getMessageLogger(
			CoreMessageLogger.class,
			TwoPhaseLoad.class.getName()
	);

	private TwoPhaseLoad() {
	}

	/**
	 * Register the "hydrated" state of an entity instance, after the first step of 2-phase loading.
	 *
	 * Add the "hydrated state" (an array) of an uninitialized entity to the session. We don't try
	 * to resolve any associations yet, because there might be other entities waiting to be
	 * read from the JDBC result set we are currently processing
	 *
	 * @param persister The persister for the hydrated entity
	 * @param id The entity identifier
	 * @param values The entity values
	 * @param rowId The rowId for the entity
	 * @param object An optional instance for the entity being loaded
	 * @param lockMode The lock mode
	 * @param session The Session
	 */
	public static void postHydrate(
			final EntityPersister persister,
			final Serializable id,
			final Object[] values,
			final Object rowId,
			final Object object,
			final LockMode lockMode,
			final SharedSessionContractImplementor session) {
		final Object version = Versioning.getVersion( values, persister );
		session.getPersistenceContextInternal().addEntry(
				object,
				Status.LOADING,
				values,
				rowId,
				id,
				version,
				lockMode,
				true,
				persister,
				false
			);

		if ( version != null && LOG.isTraceEnabled() ) {
			final String versionStr = persister.isVersioned()
					? persister.getVersionType().toLoggableString( version, session.getFactory() )
					: "null";
			LOG.tracef( "Version: %s", versionStr );
		}
	}

	/**
	 * @deprecated This method will be removed. Use {@link #initializeEntity(Object, boolean, SharedSessionContractImplementor, PreLoadEvent, Iterable)} instead.
	 *
	 * @param entity The entity being loaded
	 * @param readOnly Is the entity being loaded as read-only
	 * @param session The Session
	 * @param preLoadEvent The (re-used) pre-load event
	 */
	@Deprecated
	public static void initializeEntity(
			final Object entity,
			final boolean readOnly,
			final SharedSessionContractImplementor session,
			final PreLoadEvent preLoadEvent) {
		final EventListenerGroup<PreLoadEventListener> listenerGroup = session
			.getFactory()
			.getServiceRegistry()
			.getService( EventListenerRegistry.class )
			.getEventListenerGroup( EventType.PRE_LOAD );
		final Iterable<PreLoadEventListener> listeners = listenerGroup.listeners();
		initializeEntity( entity, readOnly, session, preLoadEvent, listeners, EntityResolver.DEFAULT );
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
	 * @param preLoadEventListeners the pre-load event listeners
	 */
	public static void initializeEntity(
		final Object entity,
		final boolean readOnly,
		final SharedSessionContractImplementor session,
		final PreLoadEvent preLoadEvent,
		final Iterable<PreLoadEventListener> preLoadEventListeners) {
		initializeEntity( entity, readOnly, session, preLoadEvent, preLoadEventListeners, EntityResolver.DEFAULT );
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
	 * @param preLoadEventListeners the pre-load event listeners
	 * @param entityResolver the resolver used for to-one entity associations
	 *                       (not used when an entity is a bytecode-enhanced lazy entity)
	 */
	public static void initializeEntity(
			final Object entity,
			final boolean readOnly,
			final SharedSessionContractImplementor session,
			final PreLoadEvent preLoadEvent,
			final Iterable<PreLoadEventListener> preLoadEventListeners,
			final EntityResolver entityResolver) {
		final PersistenceContext persistenceContext = session.getPersistenceContextInternal();
		final EntityEntry entityEntry = persistenceContext.getEntry( entity );
		if ( entityEntry == null ) {
			throw new AssertionFailure( "possible non-threadsafe access to the session" );
		}
		initializeEntityEntryLoadedState( entity, entityEntry, session, entityResolver );
		initializeEntityFromEntityEntryLoadedState( entity, entityEntry, readOnly, session, preLoadEvent, preLoadEventListeners );
	}

	public static void initializeEntityEntryLoadedState(
			final Object entity,
			final EntityEntry entityEntry,
			final SharedSessionContractImplementor session,
			final EntityResolver entityResolver) throws HibernateException {
		final EntityPersister persister = entityEntry.getPersister();
		final Serializable id = entityEntry.getId();
		final Object[] hydratedState = entityEntry.getLoadedState();

		final boolean debugEnabled = LOG.isDebugEnabled();

		if ( debugEnabled ) {
			LOG.debugf(
					"Resolving attributes for %s",
					MessageHelper.infoString( persister, id, session.getFactory() )
			);
		}

		String entityName = persister.getEntityName();
		String[] propertyNames = persister.getPropertyNames();
		final Type[] types = persister.getPropertyTypes();
		
		final GraphImplementor<?> fetchGraphContext = session.getFetchGraphLoadContext();
		
		for ( int i = 0; i < hydratedState.length; i++ ) {
			final Object value = hydratedState[i];
			if ( debugEnabled ) {
				LOG.debugf(
					"Processing attribute `%s` : value = %s",
					propertyNames[i],
					value == LazyPropertyInitializer.UNFETCHED_PROPERTY ? "<un-fetched>" : value == PropertyAccessStrategyBackRefImpl.UNKNOWN ? "<unknown>" : value
				);
			}

			if ( value == LazyPropertyInitializer.UNFETCHED_PROPERTY ) {
				if ( debugEnabled ) {
					LOG.debugf( "Resolving <un-fetched> attribute : `%s`", propertyNames[i] );
				}

				// IMPLEMENTATION NOTE: This is a lazy property on a bytecode-enhanced entity.
				// hydratedState[i] needs to remain LazyPropertyInitializer.UNFETCHED_PROPERTY so that
				// setPropertyValues() below (ultimately AbstractEntityTuplizer#setPropertyValues) works properly
				// No resolution is necessary, unless the lazy property is a collection.
				if ( types[i].isCollectionType() ) {
					// IMPLEMENTATION NOTE: this is a lazy collection property on a bytecode-enhanced entity.
					// HHH-10989: We need to resolve the collection so that a CollectionReference is added to StatefulPersistentContext.
					// As mentioned above, hydratedState[i] needs to remain LazyPropertyInitializer.UNFETCHED_PROPERTY
					// so do not assign the resolved, uninitialized PersistentCollection back to hydratedState[i].
					Boolean overridingEager = getOverridingEager( session, entityName, propertyNames[i], types[i], debugEnabled );
					types[i].resolve( value, session, entity, overridingEager );
				}
			}
			else if ( value != PropertyAccessStrategyBackRefImpl.UNKNOWN ) {
				if ( debugEnabled ) {
					final boolean isLazyEnhanced = persister.getBytecodeEnhancementMetadata()
						.getLazyAttributesMetadata()
						.getLazyAttributeNames()
						.contains( propertyNames[i] );
					LOG.debugf( "Attribute (`%s`)  - enhanced for lazy-loading? - %s", propertyNames[i], isLazyEnhanced );
				}

				// we know value != LazyPropertyInitializer.UNFETCHED_PROPERTY
				Boolean overridingEager = getOverridingEager( session, entityName, propertyNames[i], types[i], debugEnabled );
				hydratedState[i] = types[i].isEntityType()
						? entityResolver.resolve( (EntityType) types[i], value, session, entity, overridingEager )
						: types[i].resolve( value, session, entity, overridingEager );
			}
			else {
				if ( debugEnabled ) {
					LOG.debugf( "Skipping <unknown> attribute : `%s`", propertyNames[i] );
				}
			}

			if ( session.getFetchGraphLoadContext() != fetchGraphContext ) {
				session.setFetchGraphLoadContext( fetchGraphContext );
			}
		}
	}

	public static void initializeEntityFromEntityEntryLoadedState(
			final Object entity,
			final EntityEntry entityEntry,
			final boolean readOnly,
			final SharedSessionContractImplementor session,
			final PreLoadEvent preLoadEvent,
			final Iterable<PreLoadEventListener> preLoadEventListeners) throws HibernateException {

		final PersistenceContext persistenceContext = session.getPersistenceContextInternal();
		final EntityPersister persister = entityEntry.getPersister();
		final Serializable id = entityEntry.getId();
		final Object[] hydratedState = entityEntry.getLoadedState();

		final boolean debugEnabled = LOG.isDebugEnabled();

		//Must occur after resolving identifiers!
		if ( session.isEventSource() ) {
			preLoadEvent.setEntity( entity ).setState( hydratedState ).setId( id ).setPersister( persister );
			for ( PreLoadEventListener listener : preLoadEventListeners ) {
				listener.onPreLoad( preLoadEvent );
			}
		}

		persister.setPropertyValues( entity, hydratedState );

		final SessionFactoryImplementor factory = session.getFactory();
		final StatisticsImplementor statistics = factory.getStatistics();
		if ( persister.canWriteToCache() && session.getCacheMode().isPutEnabled() ) {

			if ( debugEnabled ) {
				LOG.debugf(
						"Adding entity to second-level cache: %s",
						MessageHelper.infoString( persister, id, session.getFactory() )
				);
			}

			final Object version = Versioning.getVersion( hydratedState, persister );
			final CacheEntry entry = persister.buildCacheEntry( entity, hydratedState, version, session );
			final EntityDataAccess cache = persister.getCacheAccessStrategy();
			final Object cacheKey = cache.generateCacheKey( id, persister, factory, session.getTenantIdentifier() );

			// explicit handling of caching for rows just inserted and then somehow forced to be read
			// from the database *within the same transaction*.  usually this is done by
			// 		1) Session#refresh, or
			// 		2) Session#clear + some form of load
			//
			// we need to be careful not to clobber the lock here in the cache so that it can be rolled back if need be
			if ( session.getPersistenceContextInternal().wasInsertedDuringTransaction( persister, id ) ) {
				cache.update(
						session,
						cacheKey,
						persister.getCacheEntryStructure().structure( entry ),
						version,
						version
				);
			}
			else {
				final SessionEventListenerManager eventListenerManager = session.getEventListenerManager();
				try {
					eventListenerManager.cachePutStart();
					final boolean put = cache.putFromLoad(
							session,
							cacheKey,
							persister.getCacheEntryStructure().structure( entry ),
							version,
							useMinimalPuts( session, entityEntry )
					);

					if ( put && statistics.isStatisticsEnabled() ) {
						statistics.entityCachePut(
								StatsHelper.INSTANCE.getRootEntityRole( persister ),
								cache.getRegion().getName()
						);
					}
				}
				finally {
					eventListenerManager.cachePutEnd();
				}
			}
		}

		if ( persister.hasNaturalIdentifier() ) {
			persistenceContext.getNaturalIdHelper().cacheNaturalIdCrossReferenceFromLoad(
					persister,
					id,
					persistenceContext.getNaturalIdHelper().extractNaturalIdValues( hydratedState, persister )
			);
		}

		boolean isReallyReadOnly = readOnly;
		if ( !persister.isMutable() ) {
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
					hydratedState,
					persister.getPropertyTypes(),
					persister.getPropertyUpdateability(),
					//after setting values to object
					hydratedState,
					session
			);
			persistenceContext.setEntryStatus( entityEntry, Status.MANAGED );
		}

		if ( debugEnabled ) {
			LOG.debugf(
					"Done materializing entity %s",
					MessageHelper.infoString( persister, id, session.getFactory() )
			);
		}

		if ( statistics.isStatisticsEnabled() ) {
			statistics.loadEntity( persister.getEntityName() );
		}
	}

	/**
	 * Perform the afterInitialize() step. This needs to be done after the collections have been properly initialized
	 * thus a separate step.
	 *
	 * @param entity The entity being loaded
	 * @param session The Session
	 */
	public static void afterInitialize(
			final Object entity,
			final SharedSessionContractImplementor session) {
		final PersistenceContext persistenceContext = session.getPersistenceContext();
		final EntityEntry entityEntry = persistenceContext.getEntry( entity );

		entityEntry.getPersister().afterInitialize( entity, session );
	}

	/**
	 * Check if eager of the association is overridden (i.e. skipping metamodel strategy), including (order sensitive):
	 * <ol>
	 *     <li>fetch graph</li>
	 *     <li>fetch profile</li>
	 * </ol>
	 *
	 * @param session session
	 * @param entityName entity name
	 * @param associationName association name
	 * @param associationType association type
	 * @param isDebugEnabled if debug log level enabled
	 * @return null if there is no overriding, true if it is overridden to eager and false if it is overridden to lazy
	 */
	private static Boolean getOverridingEager(
			final SharedSessionContractImplementor session,
			final String entityName,
			final String associationName,
			final Type associationType,
			final boolean isDebugEnabled) {
		// Performance: check type.isCollectionType() first, as type.isAssociationType() is megamorphic
		if ( associationType.isCollectionType() || associationType.isAssociationType()  ) {

			// check 'fetch graph' first; skip 'fetch profile' if 'fetch graph' takes effect
			Boolean overridingEager = isEagerFetchGraph( session, associationName, associationType );

			if ( overridingEager != null ) {
				//This method is very hot, and private so let's piggy back on the fact that the caller already knows the debugging state.
				if ( isDebugEnabled ) {
					LOG.debugf(
							"Overriding eager fetching using fetch graph. EntityName: %s, associationName: %s, eager fetching: %s",
							entityName,
							associationName,
							overridingEager
					);
				}

				return overridingEager;
			}
			
			// check 'fetch profile' next; skip 'metamodel' if 'fetch profile' takes effect
			overridingEager = isEagerFetchProfile( session, entityName, associationName );

			if ( overridingEager != null ) {
				//This method is very hot, and private so let's piggy back on the fact that the caller already knows the debugging state.
				if ( isDebugEnabled ) {
					LOG.debugf(
							"Overriding eager fetching using active fetch profile. EntityName: %s, associationName: %s, eager fetching: %s",
							entityName,
							associationName,
							overridingEager
					);
				}
				return overridingEager;
			}
		}
		// let 'metamodel' decide eagerness
		return null;
	}

	private static Boolean isEagerFetchProfile(SharedSessionContractImplementor session, String entityName, String associationName) {
		LoadQueryInfluencers loadQueryInfluencers = session.getLoadQueryInfluencers();

		// Performance: avoid concatenating entityName + "." + associationName when there is no need,
		// as otherwise this section becomes an hot allocation point.
		if ( loadQueryInfluencers.hasEnabledFetchProfiles() ) {
			final String role =  entityName + '.' + associationName;
			final SessionFactoryImplementor factory = session.getFactory();
			for ( String fetchProfileName : loadQueryInfluencers.getEnabledFetchProfileNames() ) {
				FetchProfile fp = factory.getFetchProfile( fetchProfileName );
				Fetch fetch = fp.getFetchByRole( role );
				if ( fetch != null && Fetch.Style.JOIN == fetch.getStyle() ) {
					return true;
				}
			}
		}

		return null;
	}
	
	private static Boolean isEagerFetchGraph(SharedSessionContractImplementor session, String associationName, Type associationType) {
		final GraphImplementor<?> context = session.getFetchGraphLoadContext();
		
		if ( context != null ) {
			// 'fetch graph' is in effect, so null should not be returned
			final AttributeNodeImplementor<Object> attributeNode = context.findAttributeNode( associationName );
			if ( attributeNode != null ) {
				if ( associationType.isCollectionType() ) {
					// to do: deal with Map's key and value
					session.setFetchGraphLoadContext( null );
				}
				else {
					// set 'fetchGraphContext' to sub-graph so graph is explored further (internal loading)
					final GraphImplementor<?> subContext = attributeNode.getSubGraphMap().get( associationType.getReturnedClass() );
					if ( subContext != null ) {
						session.setFetchGraphLoadContext( subContext );
					}
					else {
						session.setFetchGraphLoadContext( null );
					}
				}
				// explicit 'fetch graph' applies, so fetch eagerly
				return true;
			}
			else {
				// implicit 'fetch graph' applies, so fetch lazily
				return false;
			}
		}

		return null;
	}

	/**
	 * This method will be removed.
	 * @deprecated Use {@link #postLoad(Object, SharedSessionContractImplementor, PostLoadEvent)}
	 * instead.
	 */
	@Deprecated
	public static void postLoad(
			final Object entity,
			final SharedSessionContractImplementor session,
			final PostLoadEvent postLoadEvent,
			final Iterable<PostLoadEventListener> postLoadEventListeners) {
		postLoad( entity, session, postLoadEvent );
	}

	/**
	 * PostLoad cannot occur during initializeEntity, as that call occurs *before*
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
			final EntityEntry entityEntry = session.getPersistenceContextInternal().getEntry( entity );

			postLoadEvent.setEntity( entity ).setId( entityEntry.getId() ).setPersister( entityEntry.getPersister() );

			session.getFactory().getFastSessionServices().firePostLoadEvent( postLoadEvent );
		}
	}

	private static boolean useMinimalPuts(SharedSessionContractImplementor session, EntityEntry entityEntry) {
		if ( session.getFactory().getSessionFactoryOptions().isMinimalPutsEnabled() ) {
			return session.getCacheMode() != CacheMode.REFRESH;
		}
		else {
			final EntityPersister persister = entityEntry.getPersister();
			return persister.hasLazyProperties()
					&& persister.isLazyPropertiesCacheable();
		}
	}

	/**
	 * Add an uninitialized instance of an entity class, as a placeholder to ensure object
	 * identity. Must be called before <tt>postHydrate()</tt>.
	 *
	 * Create a "temporary" entry for a newly instantiated entity. The entity is uninitialized,
	 * but we need the mapping from id to instance in order to guarantee uniqueness.
	 *
	 * @param key The entity key
	 * @param object The entity instance
	 * @param persister The entity persister
	 * @param lockMode The lock mode
	 * @param session The Session
	 */
	public static void addUninitializedEntity(
			final EntityKey key,
			final Object object,
			final EntityPersister persister,
			final LockMode lockMode,
			final SharedSessionContractImplementor session) {
		session.getPersistenceContextInternal().addEntity(
				object,
				Status.LOADING,
				null,
				key,
				null,
				lockMode,
				true,
				persister,
				false
		);
	}

	/**
	 * Same as {@link #addUninitializedEntity}, but here for an entity from the second level cache
	 *
	 * @param key The entity key
	 * @param object The entity instance
	 * @param persister The entity persister
	 * @param lockMode The lock mode
	 * @param version The version
	 * @param session The Session
	 */
	public static void addUninitializedCachedEntity(
			final EntityKey key,
			final Object object,
			final EntityPersister persister,
			final LockMode lockMode,
			final Object version,
			final SharedSessionContractImplementor session) {
		session.getPersistenceContextInternal().addEntity(
				object,
				Status.LOADING,
				null,
				key,
				version,
				lockMode,
				true,
				persister,
				false
		);
	}

	/**
	 * Implementations determine how a to-one associations is resolved.
	 *
	 * @see #initializeEntity(Object, boolean, SharedSessionContractImplementor, PreLoadEvent, Iterable, EntityResolver)
	 */
	public interface EntityResolver {

		Object resolve(
				EntityType entityType,
				Object value,
				SharedSessionContractImplementor session,
				Object owner,
				Boolean overridingEager
		);

		EntityResolver DEFAULT = (entityType, value, session, owner, overridingEager) ->
				entityType.resolve( value, session, owner, overridingEager );
	}
}
