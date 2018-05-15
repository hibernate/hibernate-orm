/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.loader.entity;

import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.WrongClassException;
import org.hibernate.cache.spi.access.EntityDataAccess;
import org.hibernate.cache.spi.entry.CacheEntry;
import org.hibernate.cache.spi.entry.ReferenceCacheEntryImpl;
import org.hibernate.engine.internal.CacheHelper;
import org.hibernate.engine.internal.StatefulPersistenceContext;
import org.hibernate.engine.internal.TwoPhaseLoad;
import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.engine.spi.EntityKey;
import org.hibernate.engine.spi.ManagedEntity;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.engine.spi.Status;
import org.hibernate.event.internal.AbstractLockUpgradeEventListener;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.EventSource;
import org.hibernate.event.spi.EventType;
import org.hibernate.event.spi.LoadEvent;
import org.hibernate.event.spi.LoadEventListener;
import org.hibernate.event.spi.PostLoadEventListener;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.metamodel.model.domain.spi.EntityTypeDescriptor;

/**
 * @author Vlad Mihalcea
 */
public class CacheEntityLoaderHelper extends AbstractLockUpgradeEventListener {

	public static final CacheEntityLoaderHelper INSTANCE = new CacheEntityLoaderHelper();

	private static final CoreMessageLogger LOG = CoreLogging.messageLogger( CacheEntityLoaderHelper.class );

	private static final boolean traceEnabled = LOG.isTraceEnabled();

	public enum EntityStatus {
		MANAGED,
		REMOVED_ENTITY_MARKER,
		INCONSISTENT_RTN_CLASS_MARKER
	}

	private CacheEntityLoaderHelper() {
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
	 *
	 * @return The entity from the session-level cache, or null.
	 *
	 * @throws HibernateException Generally indicates problems applying a lock-mode.
	 */
	public PersistenceContextEntry loadFromSessionCache(
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
					LOG.debug(
							"Load request found matching entity in context, but it is scheduled for removal; returning null" );
					return new PersistenceContextEntry( old, EntityStatus.REMOVED_ENTITY_MARKER );
				}
			}
			if ( options.isAllowNulls() ) {
				final EntityTypeDescriptor descriptor = event.getSession()
						.getFactory()
						.getMetamodel()
						.getEntityDescriptor( keyToLoad.getEntityName() );
				if ( !descriptor.isInstance( old ) ) {
					LOG.debug(
							"Load request found matching entity in context, but the matched entity was of an inconsistent return type; returning null"
					);
					return new PersistenceContextEntry( old, EntityStatus.INCONSISTENT_RTN_CLASS_MARKER );
				}
			}
			upgradeLock( old, oldEntry, event.getLockOptions(), event.getSession() );
		}

		return new PersistenceContextEntry( old, EntityStatus.MANAGED );
	}

	/**
	 * Attempts to load the entity from the second-level cache.
	 *
	 * @param event The load event
	 * @param entityDescriptor The persister for the entity being requested for load
	 *
	 * @return The entity from the second-level cache, or null.
	 */
	public Object loadFromSecondLevelCache(
			final LoadEvent event,
			final EntityTypeDescriptor entityDescriptor,
			final EntityKey entityKey) {

		final SessionImplementor source = event.getSession();
		final boolean useCache = entityDescriptor.canReadFromCache()
				&& source.getCacheMode().isGetEnabled()
				&& event.getLockMode().lessThan( LockMode.READ );

		if ( !useCache ) {
			// we can't use cache here
			return null;
		}

		final Object ce = getFromSharedCache( event, entityDescriptor, source );

		if ( ce == null ) {
			// nothing was found in cache
			return null;
		}

		return processCachedEntry( event, entityDescriptor, ce, source, entityKey );
	}


	private Object processCachedEntry(
			final LoadEvent event,
			final EntityTypeDescriptor entityDescriptor,
			final Object ce,
			final SessionImplementor source,
			final EntityKey entityKey) {

		CacheEntry entry = (CacheEntry) entityDescriptor.getCacheEntryStructure().destructure( ce, source.getFactory() );
		if ( entry.isReferenceEntry() ) {
			if ( event.getInstanceToLoad() != null ) {
				throw new HibernateException(
						"Attempt to load entity [%s] from cache using provided object instance, but cache " +
								"is storing references: " + event.getEntityId() );
			}
			else {
				return convertCacheReferenceEntryToEntity(
						(ReferenceCacheEntryImpl) entry,
						event.getSession(),
						entityKey
				);
			}
		}
		else {
			Object entity = convertCacheEntryToEntity( entry, event.getEntityId(), entityDescriptor, event, entityKey );

			if ( !entityDescriptor.isInstance( entity ) ) {
				throw new WrongClassException(
						"loaded object was of wrong class " + entity.getClass(),
						event.getEntityId(),
						entityDescriptor.getEntityName()
				);
			}

			return entity;
		}
	}

	private Object getFromSharedCache(
			final LoadEvent event,
			final EntityTypeDescriptor entityDescriptor,
			SessionImplementor source) {
		final EntityDataAccess cacheAccess = entityDescriptor.getHierarchy().getEntityCacheAccess();
		final Object ck = cacheAccess.generateCacheKey(
				event.getEntityId(),
				entityDescriptor.getHierarchy(),
				source.getFactory(),
				source.getTenantIdentifier()
		);

		final Object ce = CacheHelper.fromSharedCache( source, ck, cacheAccess );
		if ( source.getFactory().getStatistics().isStatisticsEnabled() ) {
			if ( ce == null ) {
				source.getFactory().getStatistics().entityCacheMiss(
						entityDescriptor.getNavigableRole(),
						cacheAccess.getRegion().getName()
				);
			}
			else {
				source.getFactory().getStatistics().entityCacheHit(
						entityDescriptor.getNavigableRole(),
						cacheAccess.getRegion().getName()
				);
			}
		}
		return ce;
	}

	private Object convertCacheReferenceEntryToEntity(
			ReferenceCacheEntryImpl referenceCacheEntry,
			EventSource session,
			EntityKey entityKey) {
		final Object entity = referenceCacheEntry.getReference();

		if ( entity == null ) {
			throw new IllegalStateException(
					"Reference cache entry contained null : " + referenceCacheEntry.toString() );
		}
		else {
			makeEntityCircularReferenceSafe( referenceCacheEntry, session, entity, entityKey );
			return entity;
		}
	}

	private void makeEntityCircularReferenceSafe(
			ReferenceCacheEntryImpl referenceCacheEntry,
			EventSource session,
			Object entity,
			EntityKey entityKey) {

		// make it circular-reference safe
		final StatefulPersistenceContext statefulPersistenceContext = (StatefulPersistenceContext) session.getPersistenceContext();

		if ( ( entity instanceof ManagedEntity ) ) {
			statefulPersistenceContext.addReferenceEntry(
					entity,
					Status.READ_ONLY
			);
		}
		else {
			TwoPhaseLoad.addUninitializedCachedEntity(
					entityKey,
					entity,
					referenceCacheEntry.getSubclassDescriptor(),
					LockMode.NONE,
					referenceCacheEntry.getVersion(),
					session
			);
		}
		statefulPersistenceContext.initializeNonLazyCollections();
	}

	private Object convertCacheEntryToEntity(
			CacheEntry entry,
			Object entityId,
			EntityTypeDescriptor entityDescriptor,
			LoadEvent event,
			EntityKey entityKey) {

		throw new NotYetImplementedFor6Exception(  );
//
//		final EventSource session = event.getSession();
//		final SessionFactoryImplementor factory = session.getFactory();
//		final EntityTypeDescriptor subclassPersister;
//
//		if ( traceEnabled ) {
//			LOG.tracef(
//					"Converting second-level cache entry [%s] into entity : %s",
//					entry,
//					MessageHelper.infoString( descriptor, entityId, factory )
//			);
//		}
//
//		final Object entity;
//
//		subclassPersister = factory.getEntityPersister( entry.getSubclass() );
//		final Object optionalObject = event.getInstanceToLoad();
//		entity = optionalObject == null
//				? session.instantiate( subclassPersister, entityId )
//				: optionalObject;
//
//		// make it circular-reference safe
//		TwoPhaseLoad.addUninitializedCachedEntity(
//				entityKey,
//				entity,
//				subclassPersister,
//				LockMode.NONE,
//				entry.getVersion(),
//				session
//		);
//
//		final PersistenceContext persistenceContext = session.getPersistenceContext();
//		final Object[] values;
//		final Object version;
//		final boolean isReadOnly;
//
//		final Type[] types = subclassPersister.getPropertyTypes();
//		// initializes the entity by (desired) side-effect
//		values = ( (StandardCacheEntryImpl) entry ).assemble(
//				entity, entityId, subclassPersister, session.getInterceptor(), session
//		);
//		if ( ( (StandardCacheEntryImpl) entry ).isDeepCopyNeeded() ) {
//			TypeHelper.deepCopy(
//					values,
//					types,
//					subclassPersister.getPropertyUpdateability(),
//					values,
//					session
//			);
//		}
//		version = Versioning.getVersion( values, subclassPersister );
//		LOG.tracef( "Cached Version : %s", version );
//
//		final Object proxy = persistenceContext.getProxy( entityKey );
//		if ( proxy != null ) {
//			// there is already a proxy for this impl
//			// only set the status to read-only if the proxy is read-only
//			isReadOnly = ( (HibernateProxy) proxy ).getHibernateLazyInitializer().isReadOnly();
//		}
//		else {
//			isReadOnly = session.isDefaultReadOnly();
//		}
//
//		persistenceContext.addEntry(
//				entity,
//				( isReadOnly ? Status.READ_ONLY : Status.MANAGED ),
//				values,
//				null,
//				entityId,
//				version,
//				LockMode.NONE,
//				true,
//				subclassPersister,
//				false
//		);
//		subclassPersister.afterInitialize( entity, session );
//		persistenceContext.initializeNonLazyCollections();
//
//		//PostLoad is needed for EJB3
//		PostLoadEvent postLoadEvent = event.getPostLoadEvent()
//				.setEntity( entity )
//				.setId( entityId )
//				.setPersister( descriptor );
//
//		for ( PostLoadEventListener listener : postLoadEventListeners( session ) ) {
//			listener.onPostLoad( postLoadEvent );
//		}
//
//		return entity;
	}

	private Iterable<PostLoadEventListener> postLoadEventListeners(EventSource session) {
		return session
				.getFactory()
				.getServiceRegistry()
				.getService( EventListenerRegistry.class )
				.getEventListenerGroup( EventType.POST_LOAD )
				.listeners();
	}

	public static class PersistenceContextEntry {
		private final Object entity;
		private EntityStatus status;

		public PersistenceContextEntry(Object entity, EntityStatus status) {
			this.entity = entity;
			this.status = status;
		}

		public Object getEntity() {
			return entity;
		}

		public EntityStatus getStatus() {
			return status;
		}

		public boolean isManaged() {
			return EntityStatus.MANAGED == status;
		}
	}
}
