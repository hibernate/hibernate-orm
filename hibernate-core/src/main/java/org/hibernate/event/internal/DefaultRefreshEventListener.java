/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.event.internal;

import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.NonUniqueObjectException;
import org.hibernate.TransientObjectException;
import org.hibernate.UnresolvableObjectException;
import org.hibernate.bytecode.enhance.spi.interceptor.LazyAttributeLoadingInterceptor;
import org.hibernate.bytecode.spi.BytecodeEnhancementMetadata;
import org.hibernate.cache.spi.access.CollectionDataAccess;
import org.hibernate.cache.spi.access.EntityDataAccess;
import org.hibernate.cache.spi.access.SoftLock;
import org.hibernate.engine.internal.Cascade;
import org.hibernate.engine.internal.CascadePoint;
import org.hibernate.engine.spi.ActionQueue;
import org.hibernate.engine.spi.CascadingActions;
import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.engine.spi.EntityKey;
import org.hibernate.engine.spi.PersistenceContext;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.event.spi.EventSource;
import org.hibernate.event.spi.RefreshContext;
import org.hibernate.event.spi.RefreshEvent;
import org.hibernate.event.spi.RefreshEventListener;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.loader.ast.spi.CascadingFetchProfile;
import org.hibernate.metamodel.spi.MappingMetamodelImplementor;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.proxy.LazyInitializer;
import org.hibernate.type.CollectionType;
import org.hibernate.type.ComponentType;
import org.hibernate.type.CompositeType;
import org.hibernate.type.Type;

import static org.hibernate.pretty.MessageHelper.infoString;

/**
 * Defines the default refresh event listener used by hibernate for refreshing entities
 * in response to generated refresh events.
 *
 * @author Steve Ebersole
 */
public class DefaultRefreshEventListener implements RefreshEventListener {
	private static final CoreMessageLogger LOG = CoreLogging.messageLogger( DefaultRefreshEventListener.class );

	@Override
	public void onRefresh(RefreshEvent event) throws HibernateException {
		onRefresh( event, RefreshContext.create() );
	}

	/**
	 * Handle the given refresh event.
	 *
	 * @param event The refresh event to be handled.
	 */
	@Override
	public void onRefresh(RefreshEvent event, RefreshContext refreshedAlready) {
		final EventSource source = event.getSession();
		final PersistenceContext persistenceContext = source.getPersistenceContextInternal();
		final Object object = event.getObject();
		if ( persistenceContext.reassociateIfUninitializedProxy( object ) ) {
			final boolean isTransient = isTransient( event, source, object );
			// If refreshAlready is not empty then the refresh is the result of a
			// cascade refresh and the refresh of the parent will take care of initializing the lazy
			// entity and setting the correct lock on it, this is needed only when the refresh is called directly on a lazy entity
			if ( refreshedAlready.isEmpty() ) {
				final LazyInitializer lazyInitializer = HibernateProxy.extractLazyInitializer( object );
				final EntityPersister persister;
				if ( lazyInitializer != null ) {
					persister = source.getEntityPersister( lazyInitializer.getEntityName(), object );
				}
				else if ( !isTransient ) {
					final EntityEntry entry = persistenceContext.getEntry( object );
					persister = entry.getPersister();
				}
				else {
					persister = source.getEntityPersister( source.guessEntityName( object ), object );
				}

				refresh(
						event,
						null,
						source,
						persister,
						lazyInitializer,
						null,
						persister.getIdentifier( object, event.getSession() ),
						persistenceContext
				);
				if ( lazyInitializer != null ) {
					refreshedAlready.add( lazyInitializer.getImplementation() );
				}
			}

			if ( isTransient ) {
				source.setReadOnly( object, source.isDefaultReadOnly() );
			}
		}
		else {
			final Object entity = persistenceContext.unproxyAndReassociate( object );
			if ( refreshedAlready.add( entity) ) {
				refresh( event, refreshedAlready, entity );
			}
			else {
				LOG.trace( "Already refreshed" );
			}
		}
	}

	private static boolean isTransient(RefreshEvent event, EventSource source, Object object) {
		final String entityName = event.getEntityName();
		return entityName != null ? !source.contains( entityName, object) : !source.contains(object);
	}

	private static void refresh(RefreshEvent event, RefreshContext refreshedAlready, Object object) {
		final EventSource source = event.getSession();
		final PersistenceContext persistenceContext = source.getPersistenceContextInternal();
		final EntityEntry entry = persistenceContext.getEntry( object );

		final EntityPersister persister;
		final Object id;
		if ( entry == null ) {
			//refresh() does not pass an entityName
			persister = source.getEntityPersister( event.getEntityName(), object );
			id = persister.getIdentifier( object, event.getSession() );
			if ( id == null ) {
				throw new TransientObjectException( "transient instance passed to refresh");
			}
			if ( LOG.isTraceEnabled() ) {
				LOG.tracev(
						"Refreshing transient {0}",
						infoString( persister, id, event.getFactory() )
				);
			}
			if ( persistenceContext.getEntry( source.generateEntityKey( id, persister ) ) != null ) {
				throw new NonUniqueObjectException( id, persister.getEntityName() );
			}
		}
		else {
			if ( LOG.isTraceEnabled() ) {
				LOG.tracev(
						"Refreshing ",
						infoString( entry.getPersister(), entry.getId(), event.getFactory() )
				);
			}
			if ( !entry.isExistsInDatabase() ) {
				throw new UnresolvableObjectException(
						entry.getId(),
						"this instance does not yet exist as a row in the database"
				);
			}
			persister = entry.getPersister();
			id = entry.getId();
		}

		// cascade the refresh prior to refreshing this entity
		Cascade.cascade(
				CascadingActions.REFRESH,
				CascadePoint.BEFORE_REFRESH,
				source,
				persister,
				object,
				refreshedAlready
		);

		if ( entry != null ) {
			persistenceContext.removeEntityHolder( entry.getEntityKey() );
			if ( persister.hasCollections() ) {
				new EvictVisitor( source, object ).process( object, persister );
			}
			persistenceContext.removeEntry( object );
		}

		evictEntity( object, persister, id, source );
		evictCachedCollections( persister, id, source );

		refresh( event, object, source, persister, null, entry, id, persistenceContext );
	}

	private static void refresh(
			RefreshEvent event,
			Object object,
			EventSource source,
			EntityPersister persister,
			LazyInitializer lazyInitializer,
			EntityEntry entry,
			Object id,
			PersistenceContext persistenceContext) {
		final BytecodeEnhancementMetadata instrumentationMetadata = persister.getInstrumentationMetadata();
		if ( object != null && instrumentationMetadata.isEnhancedForLazyLoading() ) {
			final LazyAttributeLoadingInterceptor interceptor = instrumentationMetadata.extractInterceptor( object );
			if ( interceptor != null ) {
				// The list of initialized lazy fields have to be cleared in order to refresh them from the database.
				interceptor.clearInitializedLazyFields();
			}
		}

		final Object result = source.getLoadQueryInfluencers().fromInternalFetchProfile(
				CascadingFetchProfile.REFRESH,
				() -> doRefresh( event, source, object, entry, persister, lazyInitializer, id, persistenceContext )
		);
		UnresolvableObjectException.throwIfNull( result, id, persister.getEntityName() );
	}

	private static void evictEntity(Object object, EntityPersister persister, Object id, EventSource source) {
		if ( persister.canWriteToCache() ) {
			Object previousVersion = null;
			if ( persister.isVersionPropertyGenerated() ) {
				// we need to grab the version value from the entity, otherwise
				// we have issues with generated-version entities that may have
				// multiple actions queued during the same flush
				previousVersion = persister.getVersion( object );
			}
			final EntityDataAccess cache = persister.getCacheAccessStrategy();
			final Object ck = cache.generateCacheKey(
					id,
					persister,
					source.getFactory(),
					source.getTenantIdentifier()
			);
			final SoftLock lock = cache.lockItem( source, ck, previousVersion );
			cache.remove( source, ck );
			source.getActionQueue().registerProcess( (success, session) -> cache.unlockItem( session, ck, lock ) );
		}
	}

	private static Object doRefresh(
			RefreshEvent event,
			EventSource source,
			Object object,
			EntityEntry entry,
			EntityPersister persister,
			LazyInitializer lazyInitializer,
			Object id,
			PersistenceContext persistenceContext) {
		// Handle the requested lock-mode (if one) in relation to the entry's (if one) current lock-mode
		LockOptions lockOptionsToUse = event.getLockOptions();
		final LockMode requestedLockMode = lockOptionsToUse.getLockMode();
		final LockMode postRefreshLockMode;
		if ( entry != null ) {
			final LockMode currentLockMode = entry.getLockMode();
			if ( currentLockMode.greaterThan( requestedLockMode ) ) {
				// the requested lock-mode is less restrictive than the current one
				//		- pass along the current lock-mode (after accounting for WRITE)
				lockOptionsToUse = event.getLockOptions().makeCopy();
				if ( currentLockMode == LockMode.WRITE
						|| currentLockMode == LockMode.PESSIMISTIC_WRITE
						|| currentLockMode == LockMode.PESSIMISTIC_READ ) {
					// our transaction should already hold the exclusive lock on
					// the underlying row - so READ should be sufficient.
					//
					// in fact, this really holds true for any current lock-mode that indicates we
					// hold an exclusive lock on the underlying row - but we *need* to handle
					// WRITE specially because the Loader/Locker mechanism does not allow for WRITE
					// locks
					lockOptionsToUse.setLockMode( LockMode.READ );
					// and prepare to reset the entry lock-mode to the previous lock mode after
					// the refresh completes
					postRefreshLockMode = currentLockMode;
				}
				else {
					lockOptionsToUse.setLockMode( currentLockMode );
					postRefreshLockMode = null;
				}
			}
			else {
				postRefreshLockMode = null;
			}
		}
		else {
			postRefreshLockMode = null;
		}

		final Object result = persister.load( id, object, lockOptionsToUse, source );
		if ( result != null ) {
			// apply `postRefreshLockMode`, if needed
			if ( postRefreshLockMode != null ) {
				// if we get here, there was a previous entry, and we need to re-set its lock-mode
				//		- however, the refresh operation actually creates a new entry, so get it
				persistenceContext.getEntry( result ).setLockMode( postRefreshLockMode );
			}

			source.setReadOnly( result, isReadOnly( entry, persister, lazyInitializer, source ) );
		}
		return result;
	}

	private static boolean isReadOnly(
			EntityEntry entry,
			EntityPersister persister,
			LazyInitializer lazyInitializer,
			EventSource source) {
		// Keep the same read-only/modifiable setting for the entity that it had before refreshing;
		// If it was transient, then set it to the default for the source.
		if ( !persister.isMutable() ) {
			return true;
		}
		else if ( entry != null ) {
			return entry.isReadOnly();
		}
		else if ( lazyInitializer != null ) {
			return lazyInitializer.isReadOnly();
		}
		else {
			return source.isDefaultReadOnly();
		}
	}

	private static void evictCachedCollections(EntityPersister persister, Object id, EventSource source) {
		evictCachedCollections( persister.getPropertyTypes(), id, source );
	}

	private static void evictCachedCollections(Type[] types, Object id, EventSource source)
			throws HibernateException {
		final ActionQueue actionQueue = source.getActionQueue();
		final SessionFactoryImplementor factory = source.getFactory();
		final MappingMetamodelImplementor metamodel = factory.getRuntimeMetamodels().getMappingMetamodel();
		for ( Type type : types ) {
			if ( type instanceof CollectionType ) {
				final String role = ((CollectionType) type).getRole();
				final CollectionPersister collectionPersister = metamodel.getCollectionDescriptor( role );
				if ( collectionPersister.hasCache() ) {
					final CollectionDataAccess cache = collectionPersister.getCacheAccessStrategy();
					final Object ck = cache.generateCacheKey(
						id,
						collectionPersister,
						factory,
						source.getTenantIdentifier()
					);
					final SoftLock lock = cache.lockItem( source, ck, null );
					cache.remove( source, ck );
					actionQueue.registerProcess( (success, session) -> cache.unlockItem( session, ck, lock ) );
				}
			}
			else if ( type instanceof ComponentType ) {
				// Only components can contain collections
				ComponentType compositeType = (ComponentType) type;
				evictCachedCollections( compositeType.getSubtypes(), id, source );
			}
		}
	}
}
