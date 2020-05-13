/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.loader.entity;

import java.io.Serializable;

import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.WrongClassException;
import org.hibernate.cache.spi.access.EntityDataAccess;
import org.hibernate.cache.spi.entry.CacheEntry;
import org.hibernate.cache.spi.entry.ReferenceCacheEntryImpl;
import org.hibernate.cache.spi.entry.StandardCacheEntryImpl;
import org.hibernate.engine.internal.CacheHelper;
import org.hibernate.engine.internal.StatefulPersistenceContext;
import org.hibernate.engine.internal.TwoPhaseLoad;
import org.hibernate.engine.internal.Versioning;
import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.engine.spi.EntityKey;
import org.hibernate.engine.spi.ManagedEntity;
import org.hibernate.engine.spi.PersistenceContext;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.engine.spi.Status;
import org.hibernate.event.internal.AbstractLockUpgradeEventListener;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.EventSource;
import org.hibernate.event.spi.EventType;
import org.hibernate.event.spi.LoadEvent;
import org.hibernate.event.spi.LoadEventListener;
import org.hibernate.event.spi.PostLoadEvent;
import org.hibernate.event.spi.PostLoadEventListener;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.FastSessionServices;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.pretty.MessageHelper;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.stat.internal.StatsHelper;
import org.hibernate.stat.spi.StatisticsImplementor;
import org.hibernate.type.Type;
import org.hibernate.type.TypeHelper;

/**
 * @author Vlad Mihalcea
 */
public class CacheEntityLoaderHelper extends AbstractLockUpgradeEventListener {

	public static final CacheEntityLoaderHelper INSTANCE = new CacheEntityLoaderHelper();

	private static final CoreMessageLogger LOG = CoreLogging.messageLogger( CacheEntityLoaderHelper.class );

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
				final EntityPersister persister = event.getSession()
						.getFactory()
						.getEntityPersister( keyToLoad.getEntityName() );
				if ( !persister.isInstance( old ) ) {
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
	 * @param persister The persister for the entity being requested for load
	 *
	 * @return The entity from the second-level cache, or null.
	 */
	public Object loadFromSecondLevelCache(
			final LoadEvent event,
			final EntityPersister persister,
			final EntityKey entityKey) {

		final SessionImplementor source = event.getSession();
		final boolean useCache = persister.canReadFromCache()
				&& source.getCacheMode().isGetEnabled()
				&& event.getLockMode().lessThan( LockMode.READ );

		if ( !useCache ) {
			// we can't use cache here
			return null;
		}

		final Object ce = getFromSharedCache( event, persister, source );

		if ( ce == null ) {
			// nothing was found in cache
			return null;
		}

		return processCachedEntry( event, persister, ce, source, entityKey );
	}


	private Object processCachedEntry(
			final LoadEvent event,
			final EntityPersister persister,
			final Object ce,
			final SessionImplementor source,
			final EntityKey entityKey) {

		CacheEntry entry = (CacheEntry) persister.getCacheEntryStructure().destructure( ce, source.getFactory() );
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
			Object entity = convertCacheEntryToEntity( entry, event.getEntityId(), persister, event, entityKey );

			if ( !persister.isInstance( entity ) ) {
				throw new WrongClassException(
						"loaded object was of wrong class " + entity.getClass(),
						event.getEntityId(),
						persister.getEntityName()
				);
			}

			return entity;
		}
	}

	private Object getFromSharedCache(
			final LoadEvent event,
			final EntityPersister persister,
			SessionImplementor source) {
		final EntityDataAccess cache = persister.getCacheAccessStrategy();
		final SessionFactoryImplementor factory = source.getFactory();
		final Object ck = cache.generateCacheKey(
				event.getEntityId(),
				persister,
				factory,
				source.getTenantIdentifier()
		);

		final Object ce = CacheHelper.fromSharedCache( source, ck, persister.getCacheAccessStrategy() );
		final StatisticsImplementor statistics = factory.getStatistics();
		if ( statistics.isStatisticsEnabled() ) {
			if ( ce == null ) {
				statistics.entityCacheMiss(
						StatsHelper.INSTANCE.getRootEntityRole( persister ),
						cache.getRegion().getName()
				);
			}
			else {
				statistics.entityCacheHit(
						StatsHelper.INSTANCE.getRootEntityRole( persister ),
						cache.getRegion().getName()
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
					referenceCacheEntry.getSubclassPersister(),
					LockMode.NONE,
					referenceCacheEntry.getVersion(),
					session
			);
		}
		statefulPersistenceContext.initializeNonLazyCollections();
	}

	private Object convertCacheEntryToEntity(
			CacheEntry entry,
			Serializable entityId,
			EntityPersister persister,
			LoadEvent event,
			EntityKey entityKey) {

		final EventSource session = event.getSession();
		final SessionFactoryImplementor factory = session.getFactory();
		final EntityPersister subclassPersister;

		if ( LOG.isTraceEnabled() ) {
			LOG.tracef(
					"Converting second-level cache entry [%s] into entity : %s",
					entry,
					MessageHelper.infoString( persister, entityId, factory )
			);
		}

		final Object entity;

		subclassPersister = factory.getEntityPersister( entry.getSubclass() );
		final Object optionalObject = event.getInstanceToLoad();
		entity = optionalObject == null
				? session.instantiate( subclassPersister, entityId )
				: optionalObject;

		// make it circular-reference safe
		TwoPhaseLoad.addUninitializedCachedEntity(
				entityKey,
				entity,
				subclassPersister,
				LockMode.NONE,
				entry.getVersion(),
				session
		);

		final PersistenceContext persistenceContext = session.getPersistenceContext();
		final Object[] values;
		final Object version;
		final boolean isReadOnly;

		final Type[] types = subclassPersister.getPropertyTypes();
		// initializes the entity by (desired) side-effect
		values = ( (StandardCacheEntryImpl) entry ).assemble(
				entity, entityId, subclassPersister, session.getInterceptor(), session
		);
		if ( ( (StandardCacheEntryImpl) entry ).isDeepCopyNeeded() ) {
			TypeHelper.deepCopy(
					values,
					types,
					subclassPersister.getPropertyUpdateability(),
					values,
					session
			);
		}
		version = Versioning.getVersion( values, subclassPersister );
		LOG.tracef( "Cached Version : %s", version );

		final Object proxy = persistenceContext.getProxy( entityKey );
		if ( proxy != null ) {
			// there is already a proxy for this impl
			// only set the status to read-only if the proxy is read-only
			isReadOnly = ( (HibernateProxy) proxy ).getHibernateLazyInitializer().isReadOnly();
		}
		else {
			isReadOnly = session.isDefaultReadOnly();
		}

		persistenceContext.addEntry(
				entity,
				( isReadOnly ? Status.READ_ONLY : Status.MANAGED ),
				values,
				null,
				entityId,
				version,
				LockMode.NONE,
				true,
				subclassPersister,
				false
		);
		subclassPersister.afterInitialize( entity, session );
		persistenceContext.initializeNonLazyCollections();

		//PostLoad is needed for EJB3
		PostLoadEvent postLoadEvent = event.getPostLoadEvent()
				.setEntity( entity )
				.setId( entityId )
				.setPersister( persister );

		session.getSessionFactory()
				.getFastSessionServices()
				.firePostLoadEvent( postLoadEvent );

		return entity;
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
