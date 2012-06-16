/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.event.internal;

import java.io.Serializable;

import org.jboss.logging.Logger;

import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.NonUniqueObjectException;
import org.hibernate.PersistentObjectException;
import org.hibernate.TypeMismatchException;
import org.hibernate.cache.spi.CacheKey;
import org.hibernate.cache.spi.access.SoftLock;
import org.hibernate.cache.spi.entry.CacheEntry;
import org.hibernate.engine.internal.TwoPhaseLoad;
import org.hibernate.engine.internal.Versioning;
import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.engine.spi.EntityKey;
import org.hibernate.engine.spi.PersistenceContext;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.engine.spi.Status;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.EventSource;
import org.hibernate.event.spi.EventType;
import org.hibernate.event.spi.LoadEvent;
import org.hibernate.event.spi.LoadEventListener;
import org.hibernate.event.spi.PostLoadEvent;
import org.hibernate.event.spi.PostLoadEventListener;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.pretty.MessageHelper;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.proxy.LazyInitializer;
import org.hibernate.type.EmbeddedComponentType;
import org.hibernate.type.EntityType;
import org.hibernate.type.Type;
import org.hibernate.type.TypeHelper;

/**
 * Defines the default load event listeners used by hibernate for loading entities
 * in response to generated load events.
 *
 * @author Steve Ebersole
 */
public class DefaultLoadEventListener extends AbstractLockUpgradeEventListener implements LoadEventListener {

	public static final Object REMOVED_ENTITY_MARKER = new Object();
	public static final Object INCONSISTENT_RTN_CLASS_MARKER = new Object();
	public static final LockMode DEFAULT_LOCK_MODE = LockMode.NONE;

    private static final CoreMessageLogger LOG = Logger.getMessageLogger(CoreMessageLogger.class,
                                                                       DefaultLoadEventListener.class.getName());


	/**
	 * Handle the given load event.
	 *
	 * @param event The load event to be handled.
	 * @throws HibernateException
	 */
	public void onLoad(LoadEvent event, LoadEventListener.LoadType loadType) throws HibernateException {

		final SessionImplementor source = event.getSession();

		EntityPersister persister;
		if ( event.getInstanceToLoad() != null ) {
			persister = source.getEntityPersister( null, event.getInstanceToLoad() ); //the load() which takes an entity does not pass an entityName
			event.setEntityClassName( event.getInstanceToLoad().getClass().getName() );
		}
		else {
			persister = source.getFactory().getEntityPersister( event.getEntityClassName() );
		}

		if ( persister == null ) {
			throw new HibernateException(
					"Unable to locate persister: " +
					event.getEntityClassName()
				);
		}

		final Class idClass = persister.getIdentifierType().getReturnedClass();
		if ( idClass != null && ! idClass.isInstance( event.getEntityId() ) ) {
			// we may have the kooky jpa requirement of allowing find-by-id where
			// "id" is the "simple pk value" of a dependent objects parent.  This
			// is part of its generally goofy "derived identity" "feature"
			if ( persister.getEntityMetamodel().getIdentifierProperty().isEmbedded() ) {
				final EmbeddedComponentType dependentIdType =
						(EmbeddedComponentType) persister.getEntityMetamodel().getIdentifierProperty().getType();
				if ( dependentIdType.getSubtypes().length == 1 ) {
					final Type singleSubType = dependentIdType.getSubtypes()[0];
					if ( singleSubType.isEntityType() ) {
						final EntityType dependentParentType = (EntityType) singleSubType;
						final Type dependentParentIdType = dependentParentType.getIdentifierOrUniqueKeyType( source.getFactory() );
						if ( dependentParentIdType.getReturnedClass().isInstance( event.getEntityId() ) ) {
							// yep that's what we have...
							loadByDerivedIdentitySimplePkValue(
									event,
									loadType,
									persister,
									dependentIdType,
									source.getFactory().getEntityPersister( dependentParentType.getAssociatedEntityName() )
							);
							return;
						}
					}
				}
			}
			throw new TypeMismatchException(
					"Provided id of the wrong type for class " + persister.getEntityName() + ". Expected: " + idClass + ", got " + event.getEntityId().getClass()
			);
		}

		final  EntityKey keyToLoad = source.generateEntityKey( event.getEntityId(), persister );

		try {
			if ( loadType.isNakedEntityReturned() ) {
				//do not return a proxy!
				//(this option indicates we are initializing a proxy)
				event.setResult( load(event, persister, keyToLoad, loadType) );
			}
			else {
				//return a proxy if appropriate
				if ( event.getLockMode() == LockMode.NONE ) {
					event.setResult( proxyOrLoad(event, persister, keyToLoad, loadType) );
				}
				else {
					event.setResult( lockAndLoad(event, persister, keyToLoad, loadType, source) );
				}
			}
		}
		catch(HibernateException e) {
			LOG.unableToLoadCommand( e );
			throw e;
		}
	}

	private void loadByDerivedIdentitySimplePkValue(
			LoadEvent event,
			LoadEventListener.LoadType options,
			EntityPersister dependentPersister,
			EmbeddedComponentType dependentIdType,
			EntityPersister parentPersister) {
		final EntityKey parentEntityKey = event.getSession().generateEntityKey( event.getEntityId(), parentPersister );
		final Object parent = doLoad( event, parentPersister, parentEntityKey, options );

		final Serializable dependent = (Serializable) dependentIdType.instantiate( parent, event.getSession() );
		dependentIdType.setPropertyValues( dependent, new Object[] {parent}, dependentPersister.getEntityMode() );
		final EntityKey dependentEntityKey = event.getSession().generateEntityKey( dependent, dependentPersister );
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
	 * @return The loaded entity.
	 * @throws HibernateException
	 */
	protected Object load(
		final LoadEvent event,
		final EntityPersister persister,
		final EntityKey keyToLoad,
		final LoadEventListener.LoadType options) {

		if ( event.getInstanceToLoad() != null ) {
			if ( event.getSession().getPersistenceContext().getEntry( event.getInstanceToLoad() ) != null ) {
				throw new PersistentObjectException(
						"attempted to load into an instance that was already associated with the session: " +
						MessageHelper.infoString( persister, event.getEntityId(), event.getSession().getFactory() )
					);
			}
			persister.setIdentifier( event.getInstanceToLoad(), event.getEntityId(), event.getSession() );
		}

		Object entity = doLoad(event, persister, keyToLoad, options);

		boolean isOptionalInstance = event.getInstanceToLoad() != null;

		if ( !options.isAllowNulls() || isOptionalInstance ) {
			if ( entity == null ) {
				event.getSession().getFactory().getEntityNotFoundDelegate().handleEntityNotFound( event.getEntityClassName(), event.getEntityId() );
			}
		}

		if ( isOptionalInstance && entity != event.getInstanceToLoad() ) {
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
	 * @return The result of the proxy/load operation.
	 */
	protected Object proxyOrLoad(
		final LoadEvent event,
		final EntityPersister persister,
		final EntityKey keyToLoad,
		final LoadEventListener.LoadType options) {

		if ( LOG.isTraceEnabled() ) {
			LOG.tracev( "Loading entity: {0}",
					MessageHelper.infoString( persister, event.getEntityId(), event.getSession().getFactory() ) );
		}

        // this class has no proxies (so do a shortcut)
        if (!persister.hasProxy()) {
			return load(event, persister, keyToLoad, options);
		}

        final PersistenceContext persistenceContext = event.getSession().getPersistenceContext();

		// look for a proxy
        Object proxy = persistenceContext.getProxy(keyToLoad);
        if (proxy != null) {
			return returnNarrowedProxy(event, persister, keyToLoad, options, persistenceContext, proxy);
		}

        if (options.isAllowProxyCreation()) {
			return createProxyIfNecessary(event, persister, keyToLoad, options, persistenceContext);
		}

        // return a newly loaded object
        return load(event, persister, keyToLoad, options);
	}

	/**
	 * Given a proxy, initialize it and/or narrow it provided either
	 * is necessary.
	 *
	 * @param event The initiating load request event
	 * @param persister The persister corresponding to the entity to be loaded
	 * @param keyToLoad The key of the entity to be loaded
	 * @param options The defined load options
	 * @param persistenceContext The originating session
	 * @param proxy The proxy to narrow
	 * @return The created/existing proxy
	 */
	private Object returnNarrowedProxy(
			final LoadEvent event,
			final EntityPersister persister,
			final EntityKey keyToLoad,
			final LoadEventListener.LoadType options,
			final PersistenceContext persistenceContext,
			final Object proxy) {
		LOG.trace( "Entity proxy found in session cache" );
		LazyInitializer li = ( (HibernateProxy) proxy ).getHibernateLazyInitializer();
		if ( li.isUnwrap() ) {
			return li.getImplementation();
		}
		Object impl = null;
		if ( !options.isAllowProxyCreation() ) {
			impl = load( event, persister, keyToLoad, options );
			if ( impl == null ) {
				event.getSession().getFactory().getEntityNotFoundDelegate().handleEntityNotFound( persister.getEntityName(), keyToLoad.getIdentifier());
			}
		}
		return persistenceContext.narrowProxy( proxy, persister, keyToLoad, impl );
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
	 * @param persistenceContext The originating session
	 * @return The created/existing proxy
	 */
	private Object createProxyIfNecessary(
			final LoadEvent event,
			final EntityPersister persister,
			final EntityKey keyToLoad,
			final LoadEventListener.LoadType options,
			final PersistenceContext persistenceContext) {
		Object existing = persistenceContext.getEntity( keyToLoad );
		if ( existing != null ) {
			// return existing object or initialized proxy (unless deleted)
			LOG.trace( "Entity found in session cache" );
			if ( options.isCheckDeleted() ) {
				EntityEntry entry = persistenceContext.getEntry( existing );
				Status status = entry.getStatus();
				if ( status == Status.DELETED || status == Status.GONE ) {
					return null;
				}
			}
			return existing;
		}
		LOG.trace( "Creating new proxy for entity" );
		// return new uninitialized proxy
		Object proxy = persister.createProxy( event.getEntityId(), event.getSession() );
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
	 * @param source The originating session
	 * @return The loaded entity
	 * @throws HibernateException
	 */
	protected Object lockAndLoad(
			final LoadEvent event,
			final EntityPersister persister,
			final EntityKey keyToLoad,
			final LoadEventListener.LoadType options,
			final SessionImplementor source) {
		SoftLock lock = null;
		final CacheKey ck;
		if ( persister.hasCache() ) {
			ck = source.generateCacheKey(
					event.getEntityId(),
					persister.getIdentifierType(),
					persister.getRootEntityName()
			);
			lock = persister.getCacheAccessStrategy().lockItem( ck, null );
		}
		else {
			ck = null;
		}

		Object entity;
		try {
			entity = load(event, persister, keyToLoad, options);
		}
		finally {
			if ( persister.hasCache() ) {
				persister.getCacheAccessStrategy().unlockItem( ck, lock );
			}
		}

		return event.getSession().getPersistenceContext().proxyFor( persister, keyToLoad, entity );
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
	 * @return The loaded entity, or null.
	 */
	protected Object doLoad(
			final LoadEvent event,
			final EntityPersister persister,
			final EntityKey keyToLoad,
			final LoadEventListener.LoadType options) {

		final boolean traceEnabled = LOG.isTraceEnabled();
		if ( traceEnabled ) LOG.tracev( "Attempting to resolve: {0}",
					MessageHelper.infoString( persister, event.getEntityId(), event.getSession().getFactory() ) );

		Object entity = loadFromSessionCache( event, keyToLoad, options );
		if ( entity == REMOVED_ENTITY_MARKER ) {
			LOG.debug( "Load request found matching entity in context, but it is scheduled for removal; returning null" );
			return null;
		}
		if ( entity == INCONSISTENT_RTN_CLASS_MARKER ) {
			LOG.debug( "Load request found matching entity in context, but the matched entity was of an inconsistent return type; returning null" );
			return null;
		}
		if ( entity != null ) {
			if (traceEnabled) LOG.tracev("Resolved object in session cache: {0}",
						MessageHelper.infoString( persister, event.getEntityId(), event.getSession().getFactory() ) );
			return entity;
		}

		entity = loadFromSecondLevelCache(event, persister, options);
		if ( entity != null ) {
			if ( traceEnabled ) LOG.tracev( "Resolved object in second-level cache: {0}",
					MessageHelper.infoString( persister, event.getEntityId(), event.getSession().getFactory() ) );
		}
		else {
			if ( traceEnabled ) LOG.tracev( "Object not resolved in any cache: {0}",
					MessageHelper.infoString( persister, event.getEntityId(), event.getSession().getFactory() ) );
			entity = loadFromDatasource(event, persister, keyToLoad, options);
		}
		
		if (entity != null && persister.hasNaturalIdentifier()) {
			event.getSession().getPersistenceContext().getNaturalIdHelper().cacheNaturalIdCrossReferenceFromLoad(
					persister,
					event.getEntityId(),
					event.getSession().getPersistenceContext().getNaturalIdHelper().extractNaturalIdValues( entity, persister )
			);
		}


		return entity;
	}

	/**
	 * Performs the process of loading an entity from the configured
	 * underlying datasource.
	 *
	 * @param event The load event
	 * @param persister The persister for the entity being requested for load
	 * @param keyToLoad The EntityKey representing the entity to be loaded.
	 * @param options The load options.
	 * @return The object loaded from the datasource, or null if not found.
	 */
	protected Object loadFromDatasource(
			final LoadEvent event,
			final EntityPersister persister,
			final EntityKey keyToLoad,
			final LoadEventListener.LoadType options) {
		final SessionImplementor source = event.getSession();
		Object entity = persister.load(
				event.getEntityId(),
				event.getInstanceToLoad(),
				event.getLockOptions(),
				source
		);
		
		if ( event.isAssociationFetch() && source.getFactory().getStatistics().isStatisticsEnabled() ) {
			source.getFactory().getStatisticsImplementor().fetchEntity( event.getEntityClassName() );
		}

		return entity;
	}

	/**
	 * Attempts to locate the entity in the session-level cache.
	 * <p/>
	 * If allowed to return nulls, then if the entity happens to be found in
	 * the session cache, we check the entity type for proper handling
	 * of entity hierarchies.
	 * <p/>
	 * If checkDeleted was set to true, then if the entity is found in the
	 * session-level cache, it's current status within the session cache
	 * is checked to see if it has previously been scheduled for deletion.
	 *
	 * @param event The load event
	 * @param keyToLoad The EntityKey representing the entity to be loaded.
	 * @param options The load options.
	 * @return The entity from the session-level cache, or null.
	 * @throws HibernateException Generally indicates problems applying a lock-mode.
	 */
	protected Object loadFromSessionCache(
			final LoadEvent event,
			final EntityKey keyToLoad,
			final LoadEventListener.LoadType options) throws HibernateException {

		SessionImplementor session = event.getSession();
		Object old = session.getEntityUsingInterceptor( keyToLoad );

		if ( old != null ) {
			// this object was already loaded
			EntityEntry oldEntry = session.getPersistenceContext().getEntry( old );
			if ( options.isCheckDeleted() ) {
				Status status = oldEntry.getStatus();
				if ( status == Status.DELETED || status == Status.GONE ) {
					return REMOVED_ENTITY_MARKER;
				}
			}
			if ( options.isAllowNulls() ) {
				final EntityPersister persister = event.getSession().getFactory().getEntityPersister( keyToLoad.getEntityName() );
				if ( ! persister.isInstance( old ) ) {
					return INCONSISTENT_RTN_CLASS_MARKER;
				}
			}
			upgradeLock( old, oldEntry, event.getLockOptions(), event.getSession() );
		}

		return old;
	}

	/**
	 * Attempts to load the entity from the second-level cache.
	 *
	 * @param event The load event
	 * @param persister The persister for the entity being requested for load
	 * @param options The load options.
	 * @return The entity from the second-level cache, or null.
	 */
	protected Object loadFromSecondLevelCache(
			final LoadEvent event,
			final EntityPersister persister,
			final LoadEventListener.LoadType options) {

		final SessionImplementor source = event.getSession();

		final boolean useCache = persister.hasCache()
				&& source.getCacheMode().isGetEnabled()
				&& event.getLockMode().lessThan(LockMode.READ);

		if ( useCache ) {

			final SessionFactoryImplementor factory = source.getFactory();

			final CacheKey ck = source.generateCacheKey(
					event.getEntityId(),
					persister.getIdentifierType(),
					persister.getRootEntityName()
			);
			Object ce = persister.getCacheAccessStrategy().get( ck, source.getTimestamp() );
			if ( factory.getStatistics().isStatisticsEnabled() ) {
				if ( ce == null ) {
					factory.getStatisticsImplementor().secondLevelCacheMiss(
							persister.getCacheAccessStrategy().getRegion().getName()
					);
				}
				else {
					factory.getStatisticsImplementor().secondLevelCacheHit(
							persister.getCacheAccessStrategy().getRegion().getName()
					);
				}
			}

			if ( ce != null ) {
				CacheEntry entry = (CacheEntry) persister.getCacheEntryStructure().destructure( ce, factory );

				// Entity was found in second-level cache...
				return assembleCacheEntry(
						entry,
						event.getEntityId(),
						persister,
						event
				);
			}
		}

		return null;
	}

	private Object assembleCacheEntry(
			final CacheEntry entry,
			final Serializable id,
			final EntityPersister persister,
			final LoadEvent event) throws HibernateException {

		final Object optionalObject = event.getInstanceToLoad();
		final EventSource session = event.getSession();
		final SessionFactoryImplementor factory = session.getFactory();

		if ( LOG.isTraceEnabled() ) {
			LOG.tracev( "Assembling entity from second-level cache: {0}",
					MessageHelper.infoString( persister, id, factory ) );
		}

		EntityPersister subclassPersister = factory.getEntityPersister( entry.getSubclass() );
		Object result = optionalObject == null ?
				session.instantiate( subclassPersister, id ) : optionalObject;

		// make it circular-reference safe
		final EntityKey entityKey = session.generateEntityKey( id, subclassPersister );
		TwoPhaseLoad.addUninitializedCachedEntity(
				entityKey,
				result,
				subclassPersister,
				LockMode.NONE,
				entry.areLazyPropertiesUnfetched(),
				entry.getVersion(),
				session
			);

		Type[] types = subclassPersister.getPropertyTypes();
		Object[] values = entry.assemble( result, id, subclassPersister, session.getInterceptor(), session ); // intializes result by side-effect
		TypeHelper.deepCopy(
				values,
				types,
				subclassPersister.getPropertyUpdateability(),
				values,
				session
		);

		Object version = Versioning.getVersion( values, subclassPersister );
		LOG.tracev( "Cached Version: {0}", version );

		final PersistenceContext persistenceContext = session.getPersistenceContext();
		boolean isReadOnly = session.isDefaultReadOnly();
		if ( persister.isMutable() ) {
			Object proxy = persistenceContext.getProxy( entityKey );
			if ( proxy != null ) {
				// there is already a proxy for this impl
				// only set the status to read-only if the proxy is read-only
				isReadOnly = ( ( HibernateProxy ) proxy ).getHibernateLazyInitializer().isReadOnly();
			}
		}
		else {
			isReadOnly = true;
		}
		persistenceContext.addEntry(
				result,
				( isReadOnly ? Status.READ_ONLY : Status.MANAGED ),
				values,
				null,
				id,
				version,
				LockMode.NONE,
				true,
				subclassPersister,
				false,
				entry.areLazyPropertiesUnfetched()
			);
		subclassPersister.afterInitialize( result, entry.areLazyPropertiesUnfetched(), session );
		persistenceContext.initializeNonLazyCollections();
		// upgrade the lock if necessary:
		//lock(result, lockMode);

		//PostLoad is needed for EJB3
		//TODO: reuse the PostLoadEvent...
		PostLoadEvent postLoadEvent = new PostLoadEvent( session )
				.setEntity( result )
				.setId( id )
				.setPersister( persister );

		for ( PostLoadEventListener listener : postLoadEventListeners( session ) ) {
			listener.onPostLoad( postLoadEvent );
		}

		return result;
	}

	private Iterable<PostLoadEventListener> postLoadEventListeners(EventSource session) {
		return session
				.getFactory()
				.getServiceRegistry()
				.getService( EventListenerRegistry.class )
				.getEventListenerGroup( EventType.POST_LOAD )
				.listeners();
	}
}
