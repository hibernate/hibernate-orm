/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.event.internal;

import java.io.Serializable;

import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.NonUniqueObjectException;
import org.hibernate.PersistentObjectException;
import org.hibernate.TypeMismatchException;
import org.hibernate.action.internal.DelayedPostInsertIdentifier;
import org.hibernate.cache.spi.access.EntityDataAccess;
import org.hibernate.cache.spi.access.SoftLock;
import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.engine.spi.EntityKey;
import org.hibernate.engine.spi.PersistenceContext;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.engine.spi.Status;
import org.hibernate.event.spi.LoadEvent;
import org.hibernate.event.spi.LoadEventListener;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.loader.entity.CacheEntityLoaderHelper;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.pretty.MessageHelper;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.proxy.LazyInitializer;
import org.hibernate.type.EmbeddedComponentType;
import org.hibernate.type.EntityType;
import org.hibernate.type.Type;

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
	 *
	 * @throws HibernateException
	 */
	public void onLoad(
			final LoadEvent event,
			final LoadEventListener.LoadType loadType) throws HibernateException {

		final EntityPersister persister = getPersister( event );

		if ( persister == null ) {
			throw new HibernateException( "Unable to locate persister: " + event.getEntityClassName() );
		}

		final Class idClass = persister.getIdentifierType().getReturnedClass();
		if ( idClass != null &&
				!idClass.isInstance( event.getEntityId() ) &&
				!DelayedPostInsertIdentifier.class.isInstance( event.getEntityId() ) ) {
			checkIdClass( persister, event, loadType, idClass );
		}

		doOnLoad( persister, event, loadType );
	}

	protected EntityPersister getPersister( final LoadEvent event ) {
		if ( event.getInstanceToLoad() != null ) {
			//the load() which takes an entity does not pass an entityName
			event.setEntityClassName( event.getInstanceToLoad().getClass().getName() );
			return event.getSession().getEntityPersister(
					null,
					event.getInstanceToLoad()
			);
		}
		else {
			return event.getSession().getFactory().getEntityPersister( event.getEntityClassName() );
		}
	}

	private void doOnLoad(
			final EntityPersister persister,
			final LoadEvent event,
			final LoadEventListener.LoadType loadType) {

		try {
			final EntityKey keyToLoad = event.getSession().generateEntityKey( event.getEntityId(), persister );
			if ( loadType.isNakedEntityReturned() ) {
				//do not return a proxy!
				//(this option indicates we are initializing a proxy)
				event.setResult( load( event, persister, keyToLoad, loadType ) );
			}
			else {
				//return a proxy if appropriate
				if ( event.getLockMode() == LockMode.NONE ) {
					event.setResult( proxyOrLoad( event, persister, keyToLoad, loadType ) );
				}
				else {
					event.setResult( lockAndLoad( event, persister, keyToLoad, loadType, event.getSession() ) );
				}
			}
		}
		catch (HibernateException e) {
			LOG.unableToLoadCommand( e );
			throw e;
		}
	}

	private void checkIdClass(
			final EntityPersister persister,
			final LoadEvent event,
			final LoadEventListener.LoadType loadType,
			final Class idClass) {
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
						final Type dependentParentIdType = dependentParentType.getIdentifierOrUniqueKeyType( event.getSession().getFactory() );
						if ( dependentParentIdType.getReturnedClass().isInstance( event.getEntityId() ) ) {
							// yep that's what we have...
							loadByDerivedIdentitySimplePkValue(
									event,
									loadType,
									persister,
									dependentIdType,
									event.getSession().getFactory().getEntityPersister( dependentParentType.getAssociatedEntityName() )
							);
							return;
						}
					}
				}
			}
			throw new TypeMismatchException(
					"Provided id of the wrong type for class " + persister.getEntityName() + ". Expected: " + idClass
							+ ", got " + event.getEntityId().getClass()
			);
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
	 *
	 * @return The loaded entity.
	 *
	 * @throws HibernateException
	 */
	private Object load(
			final LoadEvent event,
			final EntityPersister persister,
			final EntityKey keyToLoad,
			final LoadEventListener.LoadType options) {

		if ( event.getInstanceToLoad() != null ) {
			if ( event.getSession().getPersistenceContext().getEntry( event.getInstanceToLoad() ) != null ) {
				throw new PersistentObjectException(
						"attempted to load into an instance that was already associated with the session: " +
								MessageHelper.infoString(
										persister,
										event.getEntityId(),
										event.getSession().getFactory()
								)
				);
			}
			persister.setIdentifier( event.getInstanceToLoad(), event.getEntityId(), event.getSession() );
		}

		final Object entity = doLoad( event, persister, keyToLoad, options );

		boolean isOptionalInstance = event.getInstanceToLoad() != null;

		if ( entity == null && ( !options.isAllowNulls() || isOptionalInstance ) ) {
			event.getSession()
					.getFactory()
					.getEntityNotFoundDelegate()
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
	private Object proxyOrLoad(
			final LoadEvent event,
			final EntityPersister persister,
			final EntityKey keyToLoad,
			final LoadEventListener.LoadType options) {

		if ( LOG.isTraceEnabled() ) {
			LOG.tracev(
					"Loading entity: {0}",
					MessageHelper.infoString( persister, event.getEntityId(), event.getSession().getFactory() )
			);
		}

		// this class has no proxies (so do a shortcut)
		if ( !persister.hasProxy() ) {
			return load( event, persister, keyToLoad, options );
		}

		final PersistenceContext persistenceContext = event.getSession().getPersistenceContext();

		// look for a proxy
		Object proxy = persistenceContext.getProxy( keyToLoad );
		if ( proxy != null ) {
			return returnNarrowedProxy( event, persister, keyToLoad, options, persistenceContext, proxy );
		}

		if ( options.isAllowProxyCreation() ) {
			return createProxyIfNecessary( event, persister, keyToLoad, options, persistenceContext );
		}

		// return a newly loaded object
		return load( event, persister, keyToLoad, options );
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
	 *
	 * @return The created/existing proxy
	 */
	private Object returnNarrowedProxy(
			final LoadEvent event,
			final EntityPersister persister,
			final EntityKey keyToLoad,
			final LoadEventListener.LoadType options,
			final PersistenceContext persistenceContext,
			final Object proxy) {
		if ( LOG.isTraceEnabled() ) {
			LOG.trace( "Entity proxy found in session cache" );
		}
		LazyInitializer li = ( (HibernateProxy) proxy ).getHibernateLazyInitializer();
		if ( li.isUnwrap() ) {
			return li.getImplementation();
		}
		Object impl = null;
		if ( !options.isAllowProxyCreation() ) {
			impl = load( event, persister, keyToLoad, options );
			if ( impl == null ) {
				event.getSession()
						.getFactory()
						.getEntityNotFoundDelegate()
						.handleEntityNotFound( persister.getEntityName(), keyToLoad.getIdentifier() );
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
	 *
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
			if ( LOG.isTraceEnabled() ) {
				LOG.trace( "Entity found in session cache" );
			}
			if ( options.isCheckDeleted() ) {
				EntityEntry entry = persistenceContext.getEntry( existing );
				Status status = entry.getStatus();
				if ( status == Status.DELETED || status == Status.GONE ) {
					return null;
				}
			}
			return existing;
		}
		if ( LOG.isTraceEnabled() ) {
			LOG.trace( "Creating new proxy for entity" );
		}
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
	 *
	 * @return The loaded entity
	 *
	 * @throws HibernateException
	 */
	private Object lockAndLoad(
			final LoadEvent event,
			final EntityPersister persister,
			final EntityKey keyToLoad,
			final LoadEventListener.LoadType options,
			final SessionImplementor source) {
		SoftLock lock = null;
		final Object ck;
		final EntityDataAccess cache = persister.getCacheAccessStrategy();
		if ( persister.canWriteToCache() ) {
			ck = cache.generateCacheKey(
					event.getEntityId(),
					persister,
					source.getFactory(),
					source.getTenantIdentifier()
			);
			lock = persister.getCacheAccessStrategy().lockItem( source, ck, null );
		}
		else {
			ck = null;
		}

		Object entity;
		try {
			entity = load( event, persister, keyToLoad, options );
		}
		finally {
			if ( persister.canWriteToCache() ) {
				cache.unlockItem( source, ck, lock );
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
	 *
	 * @return The loaded entity, or null.
	 */
	private Object doLoad(
			final LoadEvent event,
			final EntityPersister persister,
			final EntityKey keyToLoad,
			final LoadEventListener.LoadType options) {

		if ( LOG.isTraceEnabled() ) {
			LOG.tracev(
					"Attempting to resolve: {0}",
					MessageHelper.infoString( persister, event.getEntityId(), event.getSession().getFactory() )
			);
		}

		CacheEntityLoaderHelper.PersistenceContextEntry persistenceContextEntry = CacheEntityLoaderHelper.INSTANCE.loadFromSessionCache(
				event,
				keyToLoad,
				options
		);
		Object entity = persistenceContextEntry.getEntity();

		if ( entity != null ) {
			return persistenceContextEntry.isManaged() ? entity : null;
		}

		entity = CacheEntityLoaderHelper.INSTANCE.loadFromSecondLevelCache( event, persister, keyToLoad );
		if ( entity != null ) {
			if ( LOG.isTraceEnabled() ) {
				LOG.tracev(
						"Resolved object in second-level cache: {0}",
						MessageHelper.infoString( persister, event.getEntityId(), event.getSession().getFactory() )
				);
			}
		}
		else {
			if ( LOG.isTraceEnabled() ) {
				LOG.tracev(
						"Object not resolved in any cache: {0}",
						MessageHelper.infoString( persister, event.getEntityId(), event.getSession().getFactory() )
				);
			}
			entity = loadFromDatasource( event, persister );
		}

		if ( entity != null && persister.hasNaturalIdentifier() ) {
			event.getSession().getPersistenceContext().getNaturalIdHelper().cacheNaturalIdCrossReferenceFromLoad(
					persister,
					event.getEntityId(),
					event.getSession().getPersistenceContext().getNaturalIdHelper().extractNaturalIdValues(
							entity,
							persister
					)
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
	 *
	 * @return The object loaded from the datasource, or null if not found.
	 */
	protected Object loadFromDatasource(
			final LoadEvent event,
			final EntityPersister persister) {
		Object entity = persister.load(
				event.getEntityId(),
				event.getInstanceToLoad(),
				event.getLockOptions(),
				event.getSession()
		);

		if ( event.isAssociationFetch() && event.getSession().getFactory().getStatistics().isStatisticsEnabled() ) {
			event.getSession().getFactory().getStatistics().fetchEntity( event.getEntityClassName() );
		}

		return entity;
	}

}
