/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.event.internal;

import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.NonUniqueObjectException;
import org.hibernate.PersistentObjectException;
import org.hibernate.TypeMismatchException;
import org.hibernate.action.internal.DelayedPostInsertIdentifier;
import org.hibernate.bytecode.enhance.spi.interceptor.EnhancementAsProxyLazinessInterceptor;
import org.hibernate.cache.spi.access.EntityDataAccess;
import org.hibernate.cache.spi.access.SoftLock;
import org.hibernate.engine.spi.EntityKey;
import org.hibernate.engine.spi.PersistenceContext;
import org.hibernate.engine.spi.PersistentAttributeInterceptable;
import org.hibernate.engine.spi.PersistentAttributeInterceptor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.event.spi.EventSource;
import org.hibernate.event.spi.LoadEvent;
import org.hibernate.event.spi.LoadEventListener;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.loader.ast.internal.CacheEntityLoaderHelper;
import org.hibernate.loader.ast.internal.CacheEntityLoaderHelper.PersistenceContextEntry;
import org.hibernate.metamodel.mapping.AttributeMapping;
import org.hibernate.metamodel.mapping.CompositeIdentifierMapping;
import org.hibernate.metamodel.mapping.EmbeddableMappingType;
import org.hibernate.metamodel.mapping.EntityIdentifierMapping;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.MappingType;
import org.hibernate.metamodel.mapping.NonAggregatedIdentifierMapping;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.proxy.LazyInitializer;
import org.hibernate.stat.spi.StatisticsImplementor;

import static org.hibernate.engine.internal.ManagedTypeHelper.asPersistentAttributeInterceptable;
import static org.hibernate.engine.internal.ManagedTypeHelper.isPersistentAttributeInterceptable;
import static org.hibernate.pretty.MessageHelper.infoString;

/**
 * Defines the default load event listeners used by hibernate for loading entities
 * in response to generated load events.
 *
 * @author Steve Ebersole
 */
public class DefaultLoadEventListener implements LoadEventListener {

	private static final CoreMessageLogger LOG = CoreLogging.messageLogger( DefaultLoadEventListener.class );

	/**
	 * Handle the given load event.
	 *
	 * @param event The load event to be handled.
	 */
	public void onLoad(LoadEvent event, LoadType loadType) throws HibernateException {
		final EntityPersister persister = getPersister( event );
		if ( persister == null ) {
			throw new HibernateException( "Unable to locate persister: " + event.getEntityClassName() );
		}
		checkId( event, loadType, persister );
		doOnLoad( persister, event, loadType );
	}

	private void checkId(LoadEvent event, LoadType loadType, EntityPersister persister) {
		final Object id = event.getEntityId();
		if ( !persister.getIdentifierMapping().getJavaType().isInstance( id )
				&& !( id instanceof DelayedPostInsertIdentifier ) ) {
			final Class<?> idClass = persister.getIdentifierType().getReturnedClass();
			if ( handleIdType( persister, event, loadType, idClass ) ) {
				throw new TypeMismatchException(
						"Supplied id had wrong type: entity '" + persister.getEntityName()
								+ "' has id type '" + idClass
								+ "' but supplied id was of type '" + event.getEntityId().getClass() + "'"
				);
			}
		}
	}

	protected EntityPersister getPersister(final LoadEvent event) {
		final Object instanceToLoad = event.getInstanceToLoad();
		final EventSource source = event.getSession();
		if ( instanceToLoad != null ) {
			//the load() which takes an entity does not pass an entityName
			event.setEntityClassName( instanceToLoad.getClass().getName() );
			return source.getEntityPersister( null, instanceToLoad );
		}
		else {
			return source.getFactory().getMappingMetamodel().getEntityDescriptor( event.getEntityClassName() );
		}
	}

	private void doOnLoad(EntityPersister persister, LoadEvent event, LoadType loadType) {
		try {
			final EventSource session = event.getSession();
			final EntityKey keyToLoad = session.generateEntityKey( event.getEntityId(), persister );
			if ( loadType.isNakedEntityReturned() ) {
				//do not return a proxy!
				//(this option indicates we are initializing a proxy)
				event.setResult( load( event, persister, keyToLoad, loadType ) );
			}
			else {
				//return a proxy if appropriate
				Object result = event.getLockMode() == LockMode.NONE
						? proxyOrLoad( event, persister, keyToLoad, loadType )
						: lockAndLoad( event, persister, keyToLoad, loadType );
				event.setResult( result );
			}
		}
		catch (HibernateException e) {
			LOG.unableToLoadCommand( e );
			throw e;
		}
	}

	//TODO: this method is completely unreadable, clean it up:
	private boolean handleIdType(EntityPersister persister, LoadEvent event, LoadType loadType, Class<?> idClass) {
		// we may have the jpa requirement of allowing find-by-id where id is the "simple pk value" of a
		// dependent objects parent. This is part of its generally goofy derived identity "feature"
		final EntityIdentifierMapping idMapping = persister.getIdentifierMapping();
		if ( idMapping instanceof CompositeIdentifierMapping ) {
			final CompositeIdentifierMapping compositeIdMapping = (CompositeIdentifierMapping) idMapping;
			final EmbeddableMappingType partMappingType = compositeIdMapping.getPartMappingType();
			if ( partMappingType.getNumberOfAttributeMappings() == 1 ) {
				final AttributeMapping singleIdAttribute = partMappingType.getAttributeMapping( 0 );
				if ( singleIdAttribute.getMappedType() instanceof EntityMappingType ) {
					final EntityMappingType parentIdTargetMapping = (EntityMappingType) singleIdAttribute.getMappedType();
					final EntityIdentifierMapping parentIdTargetIdMapping = parentIdTargetMapping.getIdentifierMapping();
					final MappingType parentIdType = parentIdTargetIdMapping instanceof CompositeIdentifierMapping
							? ((CompositeIdentifierMapping) parentIdTargetIdMapping).getMappedIdEmbeddableTypeDescriptor()
							: parentIdTargetIdMapping.getMappedType();
					if ( parentIdType.getMappedJavaType().getJavaTypeClass().isInstance( event.getEntityId() ) ) {
						// yep that's what we have...
						loadByDerivedIdentitySimplePkValue(
								event,
								loadType,
								persister,
								compositeIdMapping,
								(EntityPersister) parentIdTargetMapping
						);
						return false;
					}
					else {
						return !idClass.isInstance( event.getEntityId() );
					}
				}
				else {
					return !idClass.isInstance( event.getEntityId() );
				}
			}
			else if ( idMapping instanceof NonAggregatedIdentifierMapping ) {
				return !idClass.isInstance( event.getEntityId() );
			}
			else {
				return true;
			}
		}
		else {
			return true;
		}
	}

	private void loadByDerivedIdentitySimplePkValue(
			LoadEvent event,
			LoadType options,
			EntityPersister dependentPersister,
			CompositeIdentifierMapping dependentIdType,
			EntityPersister parentPersister) {
		final EventSource session = event.getSession();
		final EntityKey parentEntityKey = session.generateEntityKey( event.getEntityId(), parentPersister );
		final Object parent = doLoad( event, parentPersister, parentEntityKey, options );
		final Object dependent = dependentIdType.instantiate();
		dependentIdType.getPartMappingType().setValues( dependent, new Object[] { parent } );
		final EntityKey dependentEntityKey = session.generateEntityKey( dependent, dependentPersister );
		event.setEntityId( dependent );
		event.setResult( doLoad( event, dependentPersister, dependentEntityKey, options ) );
	}

	/**
	 * Performs the load of an entity.
	 *
	 * @param event The initiating load request event
	 * @param persister The persister corresponding to the entity to be loaded
	 * @param keyToLoad The key of the entity to be loaded
	 * @param options The defined load options
	 *
	 * @return The loaded entity.
	 */
	private Object load(LoadEvent event, EntityPersister persister, EntityKey keyToLoad, LoadType options) {
		final EventSource session = event.getSession();
		if ( event.getInstanceToLoad() != null ) {
			if ( session.getPersistenceContextInternal().getEntry( event.getInstanceToLoad() ) != null ) {
				throw new PersistentObjectException(
						"attempted to load into an instance that was already associated with the session: "
								+ infoString( persister, event.getEntityId(), session.getFactory() )
				);
			}
			persister.setIdentifier( event.getInstanceToLoad(), event.getEntityId(), session);
		}

		final Object entity = doLoad( event, persister, keyToLoad, options );
		boolean isOptionalInstance = event.getInstanceToLoad() != null;
		if ( entity == null
				&& ( !options.isAllowNulls() || isOptionalInstance ) ) {
			session.getFactory().getEntityNotFoundDelegate()
					.handleEntityNotFound( event.getEntityClassName(), event.getEntityId() );
		}
		else if ( isOptionalInstance && entity != event.getInstanceToLoad() ) {
			throw new NonUniqueObjectException( event.getEntityId(), event.getEntityClassName() );
		}
		return entity;
	}

	/**
	 * Based on configured options, will either return a pre-existing proxy,
	 * generate a new proxy, or perform an actual load.
	 *
	 * @param event The initiating load request event
	 * @param persister The persister corresponding to the entity to be loaded
	 * @param keyToLoad The key of the entity to be loaded
	 * @param options The defined load options
	 *
	 * @return The result of the proxy/load operation.
	 */
	private Object proxyOrLoad(LoadEvent event, EntityPersister persister, EntityKey keyToLoad, LoadType options) {
		if ( LOG.isTraceEnabled() ) {
			LOG.tracev(
					"Loading entity: {0}",
					infoString( persister, event.getEntityId(), persister.getFactory() )
			);
		}
		if ( hasBytecodeProxy( persister, options ) ) {
			return loadWithBytecodeProxy( event, persister, keyToLoad, options );
		}
		else if ( persister.hasProxy() ) {
			return loadWithRegularProxy( event, persister, keyToLoad, options );
		}
		else {
			// no proxies, just return a newly loaded object
			return load( event, persister, keyToLoad, options );
		}
	}

	private Object loadWithBytecodeProxy(LoadEvent event, EntityPersister persister, EntityKey keyToLoad, LoadType options) {
		// This is the case where we can use the entity itself as a proxy:
		// if there is already a managed entity instance associated with the PC, return it
		final EventSource session = event.getSession();
		final PersistenceContext persistenceContext = session.getPersistenceContextInternal();
		final Object managed = persistenceContext.getEntity( keyToLoad );
		if ( managed != null ) {
			return options.isCheckDeleted() && wasDeleted( persistenceContext, managed ) ? null : managed;
		}
		else if ( persister.getRepresentationStrategy().getProxyFactory() != null ) {
			// we have a HibernateProxy factory, this case is more complicated
			return loadWithProxyFactory( event, persister, keyToLoad );
		}
		else if ( persister.hasSubclasses() ) {
			// the entity class has subclasses and there is no HibernateProxy factory
			return load( event, persister, keyToLoad, options );
		}
		else {
			// no HibernateProxy factory, and no subclasses
			return createBatchLoadableEnhancedProxy( persister, keyToLoad, session );
		}
	}

	private Object loadWithRegularProxy(LoadEvent event, EntityPersister persister, EntityKey keyToLoad, LoadType options) {
		// This is the case where the proxy is a separate object:
		// look for a proxy
		final Object proxy = event.getSession().getPersistenceContextInternal().getProxy( keyToLoad );
		if ( proxy != null ) {
			// narrow the existing proxy to the type we're looking for
			return narrowedProxy( event, persister, keyToLoad, options, proxy );
		}
		else if ( options.isAllowProxyCreation() ) {
			// return a new proxy
			return createProxyIfNecessary( event, persister, keyToLoad, options );
		}
		else {
			// return a newly loaded object
			return load( event, persister, keyToLoad, options );
		}
	}

	private static boolean hasBytecodeProxy(EntityPersister persister, LoadType options) {
		return options.isAllowProxyCreation()
			&& persister.getEntityPersister().getBytecodeEnhancementMetadata().isEnhancedForLazyLoading();
	}

	private static Object loadWithProxyFactory(LoadEvent event, EntityPersister persister, EntityKey keyToLoad) {
		final EventSource session = event.getSession();
		final PersistenceContext persistenceContext = session.getPersistenceContextInternal();
//		if ( persistenceContext.containsDeletedUnloadedEntityKey( keyToLoad ) ) {
//			// an unloaded proxy with this key was deleted
//			return null;
//		}
//		else {
			// if the entity defines a HibernateProxy factory, see if there is an
			// existing proxy associated with the PC - and if so, use it
			final Object proxy = persistenceContext.getProxy( keyToLoad );
			if ( proxy != null ) {
				LOG.trace( "Entity proxy found in session cache" );
				if ( LOG.isDebugEnabled() && HibernateProxy.extractLazyInitializer( proxy ).isUnwrap() ) {
					LOG.debug( "Ignoring NO_PROXY to honor laziness" );
				}
				return persistenceContext.narrowProxy( proxy, persister, keyToLoad, null );
			}
			else if ( persister.hasSubclasses() ) {
				// specialized handling for entities with subclasses with a HibernateProxy factory
				return proxyOrCached( event, persister, keyToLoad );
			}
			else {
				// no existing proxy, and no subclasses
				return createBatchLoadableEnhancedProxy( persister, keyToLoad, session );
			}
//		}
	}

	private static PersistentAttributeInterceptable createBatchLoadableEnhancedProxy(
			EntityPersister persister,
			EntityKey keyToLoad,
			EventSource session) {
		if ( keyToLoad.isBatchLoadable( session.getLoadQueryInfluencers() ) ) {
			// Add a batch-fetch entry into the queue for this entity
			session.getPersistenceContextInternal().getBatchFetchQueue().addBatchLoadableEntityKey( keyToLoad );
		}
		// This is the crux of HHH-11147
		// create the (uninitialized) entity instance - has only id set
		return persister.getBytecodeEnhancementMetadata().createEnhancedProxy( keyToLoad, true, session );
	}

	private static Object proxyOrCached(LoadEvent event, EntityPersister persister, EntityKey keyToLoad) {
		final Object cachedEntity = CacheEntityLoaderHelper.INSTANCE.loadFromSecondLevelCache(
				event.getSession(),
				null,
				LockMode.NONE,
				persister,
				keyToLoad
		);
		if ( cachedEntity != null ) {
			return cachedEntity;
		}
		// entities with subclasses that define a ProxyFactory can create a HibernateProxy
		return createProxy( event, persister, keyToLoad );
	}

	/**
	 * Given a proxy, initialize it and/or narrow it provided either
	 * is necessary.
	 *
	 * @param event The initiating load request event
	 * @param persister The persister corresponding to the entity to be loaded
	 * @param keyToLoad The key of the entity to be loaded
	 * @param options The defined load options
	 * @param proxy The proxy to narrow
	 *
	 * @return The created/existing proxy
	 */
	private Object narrowedProxy(LoadEvent event, EntityPersister persister, EntityKey keyToLoad, LoadType options, Object proxy) {
		if ( LOG.isTraceEnabled() ) {
			LOG.trace( "Entity proxy found in session cache" );
		}
		LazyInitializer li = HibernateProxy.extractLazyInitializer( proxy );
		if ( li.isUnwrap() ) {
			return li.getImplementation();
		}
		else {
			final PersistenceContext persistenceContext = event.getSession().getPersistenceContextInternal();
			if ( options.isAllowProxyCreation() ) {
				return persistenceContext.narrowProxy( proxy, persister, keyToLoad, null );
			}
			else {
				final Object impl = proxyImplementation( event, persister, keyToLoad, options );
				return impl == null ? null : persistenceContext.narrowProxy( proxy, persister, keyToLoad, impl );
			}
		}
	}

	private Object proxyImplementation(LoadEvent event, EntityPersister persister, EntityKey keyToLoad, LoadType options) {
		Object entity = load( event, persister, keyToLoad, options );
		if ( entity != null ) {
			return entity;
		}
		else {
			if ( options != LoadEventListener.INTERNAL_LOAD_NULLABLE ) {
				// throw an appropriate exception
				event.getSession().getFactory().getEntityNotFoundDelegate()
						.handleEntityNotFound( persister.getEntityName(), keyToLoad.getIdentifier() );
			}
			// Otherwise, if it's INTERNAL_LOAD_NULLABLE, the proxy is
			// for a non-existing association mapped as @NotFound.
			// Don't throw an exception; just return null.
			return null;
		}
	}

	/**
	 * If there is already a corresponding proxy associated with the
	 * persistence context, return it; otherwise create a proxy, associate it
	 * with the persistence context, and return the just-created proxy.
	 *
	 * @param event The initiating load request event
	 * @param persister The persister corresponding to the entity to be loaded
	 * @param keyToLoad The key of the entity to be loaded
	 * @param options The defined load options
	 *
	 * @return The created/existing proxy
	 */
	private static Object createProxyIfNecessary(LoadEvent event, EntityPersister persister, EntityKey keyToLoad, LoadType options) {
		final PersistenceContext persistenceContext = event.getSession().getPersistenceContextInternal();
		final Object existing = persistenceContext.getEntity( keyToLoad );
		if ( existing != null ) {
			// return existing object or initialized proxy (unless deleted)
			if ( LOG.isTraceEnabled() ) {
				LOG.trace( "Entity found in session cache" );
			}
			return options.isCheckDeleted() && wasDeleted( persistenceContext, existing ) ? null : existing;
		}
		else {
			if ( LOG.isTraceEnabled() ) {
				LOG.trace( "Creating new proxy for entity" );
			}
			return createProxy( event, persister, keyToLoad );
		}
	}

	private static boolean wasDeleted(PersistenceContext persistenceContext, Object existing) {
		return persistenceContext.getEntry( existing ).getStatus().isDeletedOrGone();
	}

	private static Object createProxy(LoadEvent event, EntityPersister persister, EntityKey keyToLoad) {
		// return new uninitialized proxy
		final Object proxy = persister.createProxy( event.getEntityId(), event.getSession() );
		PersistenceContext persistenceContext = event.getSession().getPersistenceContextInternal();
		persistenceContext.getBatchFetchQueue().addBatchLoadableEntityKey( keyToLoad );
		persistenceContext.addProxy( keyToLoad, proxy );
		return proxy;
	}

	/**
	 * If the class to be loaded has been configured with a cache, then lock
	 * given id in that cache and then perform the load.
	 *
	 * @param event The initiating load request event
	 * @param persister The persister corresponding to the entity to be loaded
	 * @param keyToLoad The key of the entity to be loaded
	 * @param options The defined load options
	 *
	 * @return The loaded entity
	 */
	private Object lockAndLoad(LoadEvent event, EntityPersister persister, EntityKey keyToLoad, LoadType options) {
		final SessionImplementor source = event.getSession();;
		final EntityDataAccess cache = persister.getCacheAccessStrategy();

		final SoftLock lock;
		final Object ck;
		final boolean canWriteToCache = persister.canWriteToCache();
		if ( canWriteToCache ) {
			ck = cache.generateCacheKey(
					event.getEntityId(),
					persister,
					source.getFactory(),
					source.getTenantIdentifier()
			);
			lock = cache.lockItem( source, ck, null );
		}
		else {
			lock = null;
			ck = null;
		}

		final Object entity;
		try {
			entity = load( event, persister, keyToLoad, options );
		}
		finally {
			if ( canWriteToCache ) {
				cache.unlockItem( source, ck, lock );
			}
		}

		return source.getPersistenceContextInternal().proxyFor( persister, keyToLoad, entity );
	}


	/**
	 * Coordinates the efforts to load a given entity.  First, an attempt is
	 * made to load the entity from the session-level cache.  If not found there,
	 * an attempt is made to locate it in second-level cache.  Lastly, an
	 * attempt is made to load it directly from the datasource.
	 *
	 * @param event The load event
	 * @param persister The persister for the entity being requested for load
	 * @param keyToLoad The EntityKey representing the entity to be loaded.
	 * @param options The load options.
	 *
	 * @return The loaded entity, or null.
	 */
	private Object doLoad(LoadEvent event, EntityPersister persister, EntityKey keyToLoad, LoadType options) {
		final EventSource session = event.getSession();

		if ( LOG.isTraceEnabled() ) {
			LOG.tracev(
					"Attempting to resolve: {0}",
					infoString( persister, event.getEntityId(), session.getFactory() )
			);
		}

		if ( session.getPersistenceContextInternal().containsDeletedUnloadedEntityKey( keyToLoad ) ) {
			return null;
		}
		else {
			final PersistenceContextEntry persistenceContextEntry
					= CacheEntityLoaderHelper.INSTANCE.loadFromSessionCache( event, keyToLoad, options );
			final Object entity = persistenceContextEntry.getEntity();
			if ( entity != null ) {
				if ( persistenceContextEntry.isManaged() ) {
					initializeIfNecessary( entity );
					return entity;
				}
				else {
					return null;
				}
			}
			else {
				return load( event, persister, keyToLoad );
			}
		}
	}

	private static void initializeIfNecessary(Object entity) {
		if ( isPersistentAttributeInterceptable( entity ) ) {
			final PersistentAttributeInterceptable interceptable = asPersistentAttributeInterceptable( entity );
			final PersistentAttributeInterceptor interceptor = interceptable.$$_hibernate_getInterceptor();
			if ( interceptor instanceof EnhancementAsProxyLazinessInterceptor ) {
				( (EnhancementAsProxyLazinessInterceptor) interceptor ).forceInitialize( entity, null );
			}
		}
	}

	private Object load(LoadEvent event, EntityPersister persister, EntityKey keyToLoad) {
		final Object entity = loadFromCacheOrDatasource( event, persister, keyToLoad );
		if ( entity != null && persister.hasNaturalIdentifier() ) {
			event.getSession().getPersistenceContextInternal().getNaturalIdResolutions()
					.cacheResolutionFromLoad(
							event.getEntityId(),
							persister.getNaturalIdMapping().extractNaturalIdFromEntity( entity ),
							persister
					);
		}
		return entity;
	}

	private Object loadFromCacheOrDatasource(LoadEvent event, EntityPersister persister, EntityKey keyToLoad) {
		final Object entity = CacheEntityLoaderHelper.INSTANCE.loadFromSecondLevelCache( event, persister, keyToLoad );
		if ( entity != null ) {
			if ( LOG.isTraceEnabled() ) {
				LOG.tracev(
						"Resolved object in second-level cache: {0}",
						infoString( persister, event.getEntityId(), event.getSession().getFactory() )
				);
			}
			return entity;
		}
		else {
			if ( LOG.isTraceEnabled() ) {
				LOG.tracev(
						"Object not resolved in any cache: {0}",
						infoString( persister, event.getEntityId(), event.getSession().getFactory() )
				);
			}
			return loadFromDatasource( event, persister );
		}
	}

	/**
	 * Performs the process of loading an entity from the configured
	 * underlying datasource.
	 *
	 * @param event The load event
	 * @param persister The persister for the entity being requested for load
	 *
	 * @return The object loaded from the datasource, or null if not found.
	 */
	protected Object loadFromDatasource(final LoadEvent event, final EntityPersister persister) {
		Object entity = persister.load(
				event.getEntityId(),
				event.getInstanceToLoad(),
				event.getLockOptions(),
				event.getSession(),
				event.getReadOnly()
		);

		// todo (6.0) : this is a change from previous versions
		//		specifically the load call previously always returned a non-proxy
		//		so we emulate that here.  Longer term we should make the
		//		persister/loader/initializer sensitive to this fact - possibly
		//		passing LoadType along

		final LazyInitializer lazyInitializer = HibernateProxy.extractLazyInitializer( entity );
		if ( lazyInitializer != null ) {
			entity = lazyInitializer.getImplementation();
		}

		final StatisticsImplementor statistics = event.getSession().getFactory().getStatistics();
		if ( event.isAssociationFetch() && statistics.isStatisticsEnabled() ) {
			statistics.fetchEntity( event.getEntityClassName() );
		}

		return entity;
	}

}
