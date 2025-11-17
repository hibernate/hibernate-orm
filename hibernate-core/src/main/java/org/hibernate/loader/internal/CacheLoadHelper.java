/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.loader.internal;

import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.bytecode.enhance.spi.interceptor.EnhancementAsProxyLazinessInterceptor;
import org.hibernate.cache.spi.entry.CacheEntry;
import org.hibernate.cache.spi.entry.CollectionCacheEntry;
import org.hibernate.cache.spi.entry.ReferenceCacheEntryImpl;
import org.hibernate.cache.spi.entry.StandardCacheEntryImpl;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.engine.spi.EntityKey;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.engine.spi.Status;
import org.hibernate.event.spi.LoadEventListener;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.type.Type;
import org.hibernate.type.TypeHelper;

import static org.hibernate.engine.internal.CacheHelper.fromSharedCache;
import static org.hibernate.engine.internal.ManagedTypeHelper.asPersistentAttributeInterceptable;
import static org.hibernate.engine.internal.ManagedTypeHelper.isManagedEntity;
import static org.hibernate.engine.internal.ManagedTypeHelper.isPersistentAttributeInterceptable;
import static org.hibernate.engine.internal.Versioning.getVersion;
import static org.hibernate.loader.internal.CacheLoadHelper.PersistenceContextEntry.EntityStatus.INCONSISTENT_RTN_CLASS_MARKER;
import static org.hibernate.loader.internal.CacheLoadHelper.PersistenceContextEntry.EntityStatus.MANAGED;
import static org.hibernate.loader.internal.CacheLoadHelper.PersistenceContextEntry.EntityStatus.REMOVED_ENTITY_MARKER;
import static org.hibernate.loader.ast.internal.LoaderHelper.upgradeLock;
import static org.hibernate.proxy.HibernateProxy.extractLazyInitializer;
import static org.hibernate.sql.results.LoadingLogger.LOADING_LOGGER;
import static org.hibernate.stat.internal.StatsHelper.getRootEntityRole;

/**
 * Some logic which originally resided in
 * {@link org.hibernate.event.internal.DefaultLoadEventListener} and in
 * {@link org.hibernate.event.internal.DefaultInitializeCollectionEventListener}
 * but was moved here to make it reusable.
 */
public class CacheLoadHelper {

	private CacheLoadHelper() {
	}

	/**
	 * Attempts to locate the entity in the session-level cache.
	 * <p>
	 * If allowed to return nulls, then if the entity happens to be found
	 * in the session cache, we check the entity type for proper handling
	 * of entity hierarchies.
	 * <p>
	 * If {@code checkDeleted} was true, and if the entity is found in the
	 * session-level cache, its current status within the session cache
	 * is checked to see if it has previously been scheduled for deletion.
	 *
	 * @param keyToLoad The EntityKey representing the entity to be loaded
	 * @param lockOptions The lock options
	 * @param options The load options
	 * @param session The originating session
	 *
	 * @return The entity from the session-level cache, or null.
	 *
	 * @throws HibernateException Generally indicates problems applying a lock mode.
	 */
	public static PersistenceContextEntry loadFromSessionCache(
			EntityKey keyToLoad,
			LockOptions lockOptions,
			LoadEventListener.LoadType options,
			SharedSessionContractImplementor session) {
		final Object old = session.getEntityUsingInterceptor( keyToLoad );
		final PersistenceContextEntry.EntityStatus entityStatus;
		if ( old != null ) {
			// this object was already loaded
			final var oldEntry = session.getPersistenceContext().getEntry( old );
			entityStatus = entityStatus( keyToLoad, options, session, oldEntry, old );
			if ( entityStatus == MANAGED ) {
				upgradeLock( old, oldEntry, lockOptions, session );
			}
		}
		else {
			entityStatus = MANAGED;
		}
		return new PersistenceContextEntry( old, entityStatus );
	}

	// Used by Hibernate Reactive
	public static PersistenceContextEntry.EntityStatus entityStatus(
			EntityKey keyToLoad,
			LoadEventListener.LoadType options,
			SharedSessionContractImplementor session,
			EntityEntry oldEntry,
			Object old) {
		if ( options.isCheckDeleted() && oldEntry.getStatus().isDeletedOrGone() ) {
			LOADING_LOGGER.foundEntityScheduledForRemoval();
			return REMOVED_ENTITY_MARKER;
		}
		else if ( options.isAllowNulls() && !session.getFactory().getMappingMetamodel()
				.getEntityDescriptor( keyToLoad.getEntityName() ).isInstance( old ) ) {
			LOADING_LOGGER.foundEntityWrongType();
			return INCONSISTENT_RTN_CLASS_MARKER;
		}
		else {
			return MANAGED;
		}
	}

	/**
	 * Attempts to load the entity from the second-level cache.
	 *
	 * @param source The source
	 * @param entity The entity
	 * @param lockMode The lock mode
	 * @param persister The persister for the entity being requested for load
	 * @param entityKey The entity key
	 *
	 * @return The entity from the second-level cache, or null.
	 */
	public static Object loadFromSecondLevelCache(
			final SharedSessionContractImplementor source,
			final Object entity,
			final LockMode lockMode,
			final EntityPersister persister,
			final EntityKey entityKey) {
		final boolean useCache =
				persister.canReadFromCache()
						&& source.getCacheMode().isGetEnabled()
						&& lockMode.lessThan( LockMode.READ );
		if ( useCache ) {
			final Object cacheEntry = getFromSharedCache( entityKey.getIdentifier(), persister, source );
			return cacheEntry == null ? null : processCachedEntry( entity, persister, cacheEntry, source, entityKey );
		}
		else {
			// we can't use cache here
			return null;
		}
	}

	private static Object getFromSharedCache(
			final Object entityId,
			final EntityPersister persister,
			final SharedSessionContractImplementor source) {
		final var cache = persister.getCacheAccessStrategy();
		final var factory = source.getFactory();
		final Object cacheKey = cache.generateCacheKey( entityId, persister, factory, source.getTenantIdentifier() );
		final Object cacheEntry = fromSharedCache( source, cacheKey, persister, persister.getCacheAccessStrategy() );
		final var statistics = factory.getStatistics();
		if ( statistics.isStatisticsEnabled() ) {
			final var rootEntityRole = getRootEntityRole( persister );
			final String regionName = cache.getRegion().getName();
			if ( cacheEntry == null ) {
				statistics.entityCacheMiss( rootEntityRole, regionName );
			}
			else {
				statistics.entityCacheHit( rootEntityRole, regionName );
			}
		}
		return cacheEntry;
	}

	private static Object processCachedEntry(
			final Object instanceToLoad,
			final EntityPersister persister,
			final Object cacheEntry,
			final SharedSessionContractImplementor source,
			final EntityKey entityKey) {
		final var entry = (CacheEntry)
				persister.getCacheEntryStructure().destructure( cacheEntry, source.getFactory() );
		if ( entry.isReferenceEntry() ) {
			if ( instanceToLoad != null ) {
				throw new HibernateException( "Attempt to load entity from cache using provided object instance, "
						+ "but cache is storing references: " + entityKey.getIdentifier() );
			}
			else {
				return convertCacheReferenceEntryToEntity( (ReferenceCacheEntryImpl) entry, source, entityKey );
			}
		}
		else {
			final Object entity =
					convertCacheEntryToEntity(
							entry,
							entityKey.getIdentifier(),
							source,
							persister,
							instanceToLoad,
							entityKey
					);
			if ( !persister.isInstance( entity ) ) {
				// Clean up the inconsistent return class entity from the persistence context
				final var persistenceContext = source.getPersistenceContext();
				persistenceContext.removeEntry( entity );
				persistenceContext.removeEntity( entityKey );
				return null;
			}
			return entity;
		}
	}

	private static Object convertCacheReferenceEntryToEntity(
			ReferenceCacheEntryImpl referenceCacheEntry,
			SharedSessionContractImplementor session,
			EntityKey entityKey) {
		final Object entity = referenceCacheEntry.getReference();
		if ( entity == null ) {
			throw new IllegalStateException( "Reference cache entry contained null: " + referenceCacheEntry );
		}
		else {
			makeEntityCircularReferenceSafe( referenceCacheEntry, session, entity, entityKey );
			return entity;
		}
	}

	private static void makeEntityCircularReferenceSafe(
			ReferenceCacheEntryImpl referenceCacheEntry,
			SharedSessionContractImplementor session,
			Object entity,
			EntityKey entityKey) {
		// make it circular-reference safe
		final var persistenceContext = session.getPersistenceContext();
		if ( isManagedEntity( entity ) ) {
			final var entityHolder = persistenceContext.addEntityHolder( entityKey, entity );
			final var entityEntry = persistenceContext.addReferenceEntry( entity, Status.READ_ONLY );
			entityHolder.setEntityEntry( entityEntry );
		}
		else {
			addUninitializedCachedEntity(
					entityKey,
					entity,
					referenceCacheEntry.getSubclassPersister(),
					LockMode.NONE,
					referenceCacheEntry.getVersion(),
					session
			);
		}
		persistenceContext.initializeNonLazyCollections();
	}

	private static Object convertCacheEntryToEntity(
			CacheEntry entry,
			Object entityId,
			SharedSessionContractImplementor source,
			EntityPersister persister,
			Object instanceToLoad,
			EntityKey entityKey) {

		final var subclassPersister =
				source.getFactory().getMappingMetamodel()
						.getEntityDescriptor( entry.getSubclass() );
		final var persistenceContext = source.getPersistenceContextInternal();
		final var oldHolder = persistenceContext.getEntityHolder( entityKey );

		final Object entity;
		if ( instanceToLoad != null ) {
			entity = instanceToLoad;
		}
		else {
			entity = oldHolder != null && oldHolder.getEntity() != null
					? oldHolder.getEntity()
					: source.instantiate( subclassPersister, entityId );
		}

		if ( isPersistentAttributeInterceptable( entity ) ) {
			final var persistentAttributeInterceptor =
					asPersistentAttributeInterceptable( entity ).$$_hibernate_getInterceptor();
			// if we do this after the entity has been initialized, the
			// BytecodeLazyAttributeInterceptor#isAttributeLoaded(String fieldName)
			// would return false
			if ( persistentAttributeInterceptor == null
					|| persistentAttributeInterceptor instanceof EnhancementAsProxyLazinessInterceptor ) {
				persister.getBytecodeEnhancementMetadata()
						.injectInterceptor( entity, entityId, source );
			}
		}

		// make it circular-reference safe
		final var holder = persistenceContext.addEntityHolder( entityKey, entity );
		final Object proxy = holder.getProxy();
		final boolean isReadOnly;
		if ( proxy != null ) {
			// there is already a proxy for this impl
			// only set the status to read-only if the proxy is read-only
			final var lazyInitializer = extractLazyInitializer( proxy );
			assert lazyInitializer != null;
			lazyInitializer.setImplementation( entity );
			isReadOnly = lazyInitializer.isReadOnly();
		}
		else {
			isReadOnly = source.isDefaultReadOnly();
		}
		holder.setEntityEntry(
				persistenceContext.addEntry(
						entity,
						Status.LOADING,
						null,
						null,
						entityKey.getIdentifier(),
						entry.getVersion(),
						LockMode.NONE,
						true,
						persister,
						false
				)
		);

		final Type[] types = subclassPersister.getPropertyTypes();
		// initializes the entity by (desired) side effect
		final var standardCacheEntry = (StandardCacheEntryImpl) entry;
		final Object[] values = standardCacheEntry.assemble(
				entity,
				entityId,
				subclassPersister,
				source.getInterceptor(),
				source
		);
		if ( standardCacheEntry.isDeepCopyNeeded() ) {
			TypeHelper.deepCopy(
					values,
					types,
					subclassPersister.getPropertyUpdateability(),
					values,
					source
			);
		}
		final Object version = getVersion( values, subclassPersister );
		final var entityEntry = persistenceContext.addEntry(
				entity,
				isReadOnly ? Status.READ_ONLY : Status.MANAGED,
				values,
				null,
				entityId,
				version,
				LockMode.NONE,
				true,
				subclassPersister,
				false
		);
		holder.setEntityEntry( entityEntry );
		subclassPersister.afterInitialize( entity, source );
		entityEntry.postLoad( entity );
		persistenceContext.initializeNonLazyCollections();

		return entity;
	}

	/**
	 * Try to initialize a collection from the cache
	 *
	 * @param key The key of the collection to initialize
	 * @param persister The collection persister
	 * @param collection The collection to initialize
	 * @param source The originating session
	 *
	 * @return true if we were able to initialize the collection from the cache;
	 *         false otherwise.
	 */
	public static boolean initializeCollectionFromCache(
			Object key,
			CollectionPersister persister,
			PersistentCollection<?> collection,
			SharedSessionContractImplementor source) {
		if ( persister.hasCache() && source.getCacheMode().isGetEnabled() ) {
			final Object cachedEntry = getFromSharedCache( key, persister, source );
			if ( cachedEntry == null ) {
				return false;
			}
			else {
				processCachedEntry( key, persister, cachedEntry, collection, source );
				return true;
			}
		}
		else {
			// we can't use cache here
			return false;
		}
	}

	private static void processCachedEntry(
			Object key,
			CollectionPersister persister,
			Object cachedEntry,
			PersistentCollection<?> collection,
			SharedSessionContractImplementor source) {
		final var cacheEntry = (CollectionCacheEntry)
				persister.getCacheEntryStructure().destructure( cachedEntry, source.getFactory() );
		final var persistenceContext = source.getPersistenceContextInternal();
		cacheEntry.assemble( collection, persister, persistenceContext.getCollectionOwner( key, persister ) );
		persistenceContext.getCollectionEntry( collection ).postInitialize( collection, source );
		// addInitializedCollection(collection, persister, key);
	}

	private static Object getFromSharedCache(
			Object key,
			CollectionPersister persister,
			SharedSessionContractImplementor source) {
		final var factory = source.getFactory();
		final var cache = persister.getCacheAccessStrategy();
		final Object cacheKey = cache.generateCacheKey( key, persister, factory, source.getTenantIdentifier() );
		final Object cachedEntry = fromSharedCache( source, cacheKey, persister, cache );
		final var statistics = factory.getStatistics();
		if ( statistics.isStatisticsEnabled() ) {
			final var navigableRole = persister.getNavigableRole();
			final String regionName = cache.getRegion().getName();
			if ( cachedEntry == null ) {
				statistics.collectionCacheMiss( navigableRole, regionName );
			}
			else {
				statistics.collectionCacheHit( navigableRole, regionName );
			}
		}
		return cachedEntry;
	}

	/**
	 *
	 * @param key The entity key
	 * @param object The entity instance
	 * @param persister The entity persister
	 * @param lockMode The lock mode
	 * @param version The version
	 * @param session The Session
	 */
	static void addUninitializedCachedEntity(
			final EntityKey key,
			final Object object,
			final EntityPersister persister,
			final LockMode lockMode,
			final Object version,
			final SharedSessionContractImplementor session) {
		final var persistenceContext = session.getPersistenceContextInternal();
		final var entityHolder = persistenceContext.addEntityHolder( key, object );
		final var entityEntry = persistenceContext.addEntry(
				object,
				Status.LOADING,
				null,
				null,
				key.getIdentifier(),
				version,
				lockMode,
				true,
				persister,
				false
		);
		entityHolder.setEntityEntry( entityEntry );
		final Object proxy = entityHolder.getProxy();
		if ( proxy != null ) {
			// there is already a proxy for this impl
			final var lazyInitializer = extractLazyInitializer( proxy );
			assert lazyInitializer != null;
			lazyInitializer.setImplementation( object );
		}
	}

	public record PersistenceContextEntry(Object entity, EntityStatus status) {
		public enum EntityStatus {
			MANAGED,
			REMOVED_ENTITY_MARKER,
			INCONSISTENT_RTN_CLASS_MARKER
		}

		public boolean isManaged() {
			return EntityStatus.MANAGED == status;
		}
	}
}
