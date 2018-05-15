/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.event.internal;

import java.util.Set;

import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.NonUniqueObjectException;
import org.hibernate.NotYetImplementedFor6Exception;
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
import org.hibernate.metamodel.model.domain.spi.EntityIdentifierComposite;
import org.hibernate.metamodel.model.domain.spi.EntityIdentifierCompositeNonAggregated;
import org.hibernate.metamodel.model.domain.spi.EntityTypeDescriptor;
import org.hibernate.metamodel.model.domain.spi.EntityValuedNavigable;
import org.hibernate.metamodel.model.domain.spi.PersistentAttributeDescriptor;
import org.hibernate.pretty.MessageHelper;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.proxy.LazyInitializer;

/**
 * Defines the default load event listeners used by hibernate for loading entities
 * in response to generated load events.
 *
 * @author Steve Ebersole
 */
public class DefaultLoadEventListener implements LoadEventListener {

	private static final CoreMessageLogger LOG = CoreLogging.messageLogger( DefaultLoadEventListener.class );

	private static final boolean traceEnabled = LOG.isTraceEnabled();

	/**
	 * Handle the given load event.
	 *
	 * @param event The load event to be handled.
	 */
	public void onLoad(
			final LoadEvent event,
			final LoadEventListener.LoadType loadType) {

		final EntityTypeDescriptor entityDescriptor = getDescriptor( event );

		if ( entityDescriptor == null ) {
			throw new HibernateException( "Unable to locate entityDescriptor: " + event.getEntityClassName() );
		}

		final Class idClass = entityDescriptor.getHierarchy().getIdentifierDescriptor().getJavaType();
		if ( idClass != null &&
				!idClass.isInstance( event.getEntityId() ) &&
				!DelayedPostInsertIdentifier.class.isInstance( event.getEntityId() ) ) {
			checkIdClass( entityDescriptor, event, loadType, idClass );
		}

		doOnLoad( entityDescriptor, event, loadType );
	}

	protected EntityTypeDescriptor getDescriptor(final LoadEvent event ) {
		if ( event.getInstanceToLoad() != null ) {
			//the load() which takes an entity does not pass an entityName
			event.setEntityClassName( event.getInstanceToLoad().getClass().getName() );
			return event.getSession().getEntityDescriptor(
					null,
					event.getInstanceToLoad()
			);
		}
		else {
			return event.getSession().getFactory().getMetamodel().getEntityDescriptor( event.getEntityClassName() );
		}
	}

	private void doOnLoad(
			final EntityTypeDescriptor entityDescriptor,
			final LoadEvent event,
			final LoadEventListener.LoadType loadType) {

		try {
			final EntityKey keyToLoad = event.getSession().generateEntityKey( event.getEntityId(), entityDescriptor );
			if ( loadType.isNakedEntityReturned() ) {
				//do not return a proxy!
				//(this option indicates we are initializing a proxy)
				event.setResult( load( event, entityDescriptor, keyToLoad, loadType ) );
			}
			else {
				//return a proxy if appropriate
				if ( event.getLockMode() == LockMode.NONE ) {
					event.setResult( proxyOrLoad( event, entityDescriptor, keyToLoad, loadType ) );
				}
				else {
					event.setResult( lockAndLoad( event, entityDescriptor, keyToLoad, loadType, event.getSession() ) );
				}
			}
		}
		catch (HibernateException e) {
			LOG.unableToLoadCommand( e );
			throw e;
		}
	}

	private void checkIdClass(
			final EntityTypeDescriptor entityDescriptor,
			final LoadEvent event,
			final LoadEventListener.LoadType loadType,
			final Class idClass) {
		// we may have the kooky jpa requirement of allowing find-by-id where
		// "id" is the "simple pk value" of a dependent objects parent.  This
		// is part of its generally goofy "derived identity" "feature"
		if ( entityDescriptor.getHierarchy().getIdentifierDescriptor() instanceof EntityIdentifierCompositeNonAggregated ) {
			final EntityIdentifierCompositeNonAggregated dependantIdDescriptor = (EntityIdentifierCompositeNonAggregated) entityDescriptor.getHierarchy().getIdentifierDescriptor();
			final Set attributes = dependantIdDescriptor.getEmbeddedDescriptor().getAttributes();
			if ( attributes.size() == 1 ) {
				final PersistentAttributeDescriptor attribute = (PersistentAttributeDescriptor) attributes.iterator().next();
				if ( attribute instanceof EntityValuedNavigable ) {
					if ( attribute.getJavaTypeDescriptor().getJavaType().isInstance( event.getEntityId() ) ) {
						// yep that's what we have...
						loadByDerivedIdentitySimplePkValue(
								event,
								loadType,
								entityDescriptor,
								dependantIdDescriptor,
								( (EntityValuedNavigable) attribute ).getEntityDescriptor()
						);
						return;
					}
				}
			}
		}
		throw new TypeMismatchException(
				"Provided id of the wrong type for class " + entityDescriptor.getEntityName() + ". Expected: " + idClass
						+ ", got " + event.getEntityId().getClass()
		);
	}

	private void loadByDerivedIdentitySimplePkValue(
			LoadEvent event,
			LoadEventListener.LoadType options,
			EntityTypeDescriptor dependentDescriptor,
			EntityIdentifierComposite dependentIdType,
			EntityTypeDescriptor parentDescriptor) {
				throw new NotYetImplementedFor6Exception(  );
//		final EntityKey parentEntityKey = event.getSession().generateEntityKey( event.getEntityId(), parentDescriptor );
//		final Object parent = doLoad( event, parentDescriptor, parentEntityKey, options );
//
//		final Serializable dependent = (Serializable) dependentIdType.instantiate( parent, event.getSession() );
//		dependentIdType.setPropertyValues( dependent, new Object[] {parent}, dependentDescriptor.getHierarchy().getRepresentation() );
//		final EntityKey dependentEntityKey = event.getSession().generateEntityKey( dependent, dependentDescriptor );
//		event.setEntityId( dependent );
//
//		event.setResult( doLoad( event, dependentDescriptor, dependentEntityKey, options ) );
	}

	/**
	 * Performs the load of an entity.
	 *
	 * @param event The initiating load request event
	 * @param entityDescriptor The entityDescriptor corresponding to the entity to be loaded
	 * @param keyToLoad The key of the entity to be loaded
	 * @param options The defined load options
	 *
	 * @return The loaded entity.
	 *
	 * @throws HibernateException
	 */
	private Object load(
			final LoadEvent event,
			final EntityTypeDescriptor entityDescriptor,
			final EntityKey keyToLoad,
			final LoadEventListener.LoadType options) {

		if ( event.getInstanceToLoad() != null ) {
			if ( event.getSession().getPersistenceContext().getEntry( event.getInstanceToLoad() ) != null ) {
				throw new PersistentObjectException(
						"attempted to load into an instance that was already associated with the session: " +
								MessageHelper.infoString(
										entityDescriptor,
										event.getEntityId(),
										event.getSession().getFactory()
								)
				);
			}

			entityDescriptor.getHierarchy().getIdentifierDescriptor().injectIdentifier(
					event.getInstanceToLoad(),
					event.getEntityId(),
					event.getSession()
			);
		}

		final Object entity = doLoad( event, entityDescriptor, keyToLoad, options );

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
	 * @param entityDescriptor The entityDescriptor corresponding to the entity to be loaded
	 * @param keyToLoad The key of the entity to be loaded
	 * @param options The defined load options
	 *
	 * @return The result of the proxy/load operation.
	 */
	private Object proxyOrLoad(
			final LoadEvent event,
			final EntityTypeDescriptor entityDescriptor,
			final EntityKey keyToLoad,
			final LoadEventListener.LoadType options) {

		if ( traceEnabled ) {
			LOG.tracev(
					"Loading entity: {0}",
					MessageHelper.infoString( entityDescriptor, event.getEntityId(), event.getSession().getFactory() )
			);
		}

		// this class has no proxies (so do a shortcut)
		if ( !entityDescriptor.hasProxy() ) {
			return load( event, entityDescriptor, keyToLoad, options );
		}

		final PersistenceContext persistenceContext = event.getSession().getPersistenceContext();

		// look for a proxy
		Object proxy = persistenceContext.getProxy( keyToLoad );
		if ( proxy != null ) {
			return returnNarrowedProxy( event, entityDescriptor, keyToLoad, options, persistenceContext, proxy );
		}

		if ( options.isAllowProxyCreation() ) {
			return createProxyIfNecessary( event, entityDescriptor, keyToLoad, options, persistenceContext );
		}

		// return a newly loaded object
		return load( event, entityDescriptor, keyToLoad, options );
	}

	/**
	 * Given a proxy, initialize it and/or narrow it provided either
	 * is necessary.
	 *
	 * @param event The initiating load request event
	 * @param entityDescriptor The entityDescriptor corresponding to the entity to be loaded
	 * @param keyToLoad The key of the entity to be loaded
	 * @param options The defined load options
	 * @param persistenceContext The originating session
	 * @param proxy The proxy to narrow
	 *
	 * @return The created/existing proxy
	 */
	private Object returnNarrowedProxy(
			final LoadEvent event,
			final EntityTypeDescriptor entityDescriptor,
			final EntityKey keyToLoad,
			final LoadEventListener.LoadType options,
			final PersistenceContext persistenceContext,
			final Object proxy) {
		if ( traceEnabled ) {
			LOG.trace( "Entity proxy found in session cache" );
		}
		LazyInitializer li = ( (HibernateProxy) proxy ).getHibernateLazyInitializer();
		if ( li.isUnwrap() ) {
			return li.getImplementation();
		}
		Object impl = null;
		if ( !options.isAllowProxyCreation() ) {
			impl = load( event, entityDescriptor, keyToLoad, options );
			if ( impl == null ) {
				event.getSession()
						.getFactory()
						.getEntityNotFoundDelegate()
						.handleEntityNotFound( entityDescriptor.getEntityName(), keyToLoad.getIdentifier() );
			}
		}
		return persistenceContext.narrowProxy( proxy, entityDescriptor, keyToLoad, impl );
	}

	/**
	 * If there is already a corresponding proxy associated with the
	 * persistence context, return it; otherwise create a proxy, associate it
	 * with the persistence context, and return the just-created proxy.
	 *
	 * @param event The initiating load request event
	 * @param entityDescriptor The entityDescriptor corresponding to the entity to be loaded
	 * @param keyToLoad The key of the entity to be loaded
	 * @param options The defined load options
	 * @param persistenceContext The originating session
	 *
	 * @return The created/existing proxy
	 */
	private Object createProxyIfNecessary(
			final LoadEvent event,
			final EntityTypeDescriptor entityDescriptor,
			final EntityKey keyToLoad,
			final LoadEventListener.LoadType options,
			final PersistenceContext persistenceContext) {
		Object existing = persistenceContext.getEntity( keyToLoad );
		if ( existing != null ) {
			// return existing object or initialized proxy (unless deleted)
			if ( traceEnabled ) {
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
		if ( traceEnabled ) {
			LOG.trace( "Creating new proxy for entity" );
		}
		// return new uninitialized proxy
		Object proxy = entityDescriptor.createProxy( event.getEntityId(), event.getSession() );
		persistenceContext.getBatchFetchQueue().addBatchLoadableEntityKey( keyToLoad );
		persistenceContext.addProxy( keyToLoad, proxy );
		return proxy;
	}

	/**
	 * If the class to be loaded has been configured with a cache, then lock
	 * given id in that cache and then perform the load.
	 *
	 * @param event The initiating load request event
	 * @param entityDescriptor The entityDescriptor corresponding to the entity to be loaded
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
			final EntityTypeDescriptor entityDescriptor,
			final EntityKey keyToLoad,
			final LoadEventListener.LoadType options,
			final SessionImplementor source) {
		SoftLock lock = null;
		final Object ck;
		final EntityDataAccess cacheAccess = entityDescriptor.getHierarchy().getEntityCacheAccess();
		if ( entityDescriptor.canWriteToCache() ) {
			ck = cacheAccess.generateCacheKey(
					event.getEntityId(),
					entityDescriptor.getHierarchy(),
					source.getFactory(),
					source.getTenantIdentifier()
			);
			lock = cacheAccess.lockItem( source, ck, null );
		}
		else {
			ck = null;
		}

		Object entity;
		try {
			entity = load( event, entityDescriptor, keyToLoad, options );
		}
		finally {
			if ( entityDescriptor.canWriteToCache() ) {
				cacheAccess.unlockItem( source, ck, lock );
			}
		}

		return event.getSession().getPersistenceContext().proxyFor( entityDescriptor, keyToLoad, entity );
	}


	/**
	 * Coordinates the efforts to load a given entity.  First, an attempt is
	 * made to load the entity from the session-level cache.  If not found there,
	 * an attempt is made to locate it in second-level cache.  Lastly, an
	 * attempt is made to load it directly from the datasource.
	 *
	 * @param event The load event
	 * @param entityDescriptor The entityDescriptor for the entity being requested for load
	 * @param keyToLoad The EntityKey representing the entity to be loaded.
	 * @param options The load options.
	 *
	 * @return The loaded entity, or null.
	 */
	private Object doLoad(
			final LoadEvent event,
			final EntityTypeDescriptor entityDescriptor,
			final EntityKey keyToLoad,
			final LoadEventListener.LoadType options) {

		if ( traceEnabled ) {
			LOG.tracev(
					"Attempting to resolve: {0}",
					MessageHelper.infoString( entityDescriptor, event.getEntityId(), event.getSession().getFactory() )
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

		entity = CacheEntityLoaderHelper.INSTANCE.loadFromSecondLevelCache( event, entityDescriptor, keyToLoad );
		if ( entity != null ) {
			if ( traceEnabled ) {
				LOG.tracev(
						"Resolved object in second-level cache: {0}",
						MessageHelper.infoString( entityDescriptor, event.getEntityId(), event.getSession().getFactory() )
				);
			}
		}
		else {
			if ( traceEnabled ) {
				LOG.tracev(
						"Object not resolved in any cache: {0}",
						MessageHelper.infoString( entityDescriptor, event.getEntityId(), event.getSession().getFactory() )
				);
			}
			entity = loadFromDatasource( event, entityDescriptor );
		}

		if ( entity != null && entityDescriptor.getHierarchy().getNaturalIdDescriptor() != null ) {
			event.getSession().getPersistenceContext().getNaturalIdHelper().cacheNaturalIdCrossReferenceFromLoad(
					entityDescriptor,
					event.getEntityId(),
					event.getSession().getPersistenceContext().getNaturalIdHelper().extractNaturalIdValues(
							entity,
							entityDescriptor
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
	 * @param entityDescriptor The entityDescriptor for the entity being requested for load
	 *
	 * @return The object loaded from the datasource, or null if not found.
	 */
	protected Object loadFromDatasource(
			final LoadEvent event,
			final EntityTypeDescriptor entityDescriptor) {
		Object entity = entityDescriptor.getSingleIdLoader()
				.load( event.getEntityId(), event.getLockOptions(), event.getSession() );

		if ( event.isAssociationFetch() && event.getSession().getFactory().getStatistics().isStatisticsEnabled() ) {
			event.getSession().getFactory().getStatistics().fetchEntity( event.getEntityClassName() );
		}

		return entity;
	}

}
