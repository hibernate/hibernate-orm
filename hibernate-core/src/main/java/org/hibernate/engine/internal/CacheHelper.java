package org.hibernate.engine.internal;

import jakarta.annotation.Nonnull;
import org.hibernate.cache.MutableCacheKeyBuilder;
import org.hibernate.cache.spi.access.CachedDomainDataAccess;
import org.hibernate.cache.spi.access.CollectionDataAccess;
import org.hibernate.cache.spi.access.EntityDataAccess;
import org.hibernate.cache.spi.access.SoftLock;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.internal.EntityCollectionPart;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.type.descriptor.converter.spi.BasicValueConverter;
import org.hibernate.type.descriptor.java.JavaType;

import java.util.function.Consumer;
import java.util.function.Function;

/**
 * @author Steve Ebersole
 * @author Sanne Grinovero
 */
public final class CacheHelper {

	private CacheHelper() {
	}

	public record CacheLock(@Nonnull EntityDataAccess cache, Object cacheKey, SoftLock lock) {
	}

	/**
	 * Whether the given session reads a historical snapshot of the given
	 * entity, which the second-level cache, holding the current state of
	 * the entity, cannot supply.
	 */
	public static boolean readsHistory(
			@Nonnull SharedSessionContractImplementor session,
			@Nonnull EntityPersister persister) {
		final var auxiliaryMapping = persister.getAuxiliaryMapping();
		return auxiliaryMapping != null
			&& auxiliaryMapping.isAffectedByInfluencers( session.getLoadQueryInfluencers() );
	}

	/**
	 * Whether the changeset affects the collection table or its associated
	 * entities, its elements or the keys of a map, and therefore the
	 * collection's cached membership.
	 */
	public static boolean readsHistory(
			@Nonnull SharedSessionContractImplementor session,
			@Nonnull CollectionPersister persister) {
		final var influencers = session.getLoadQueryInfluencers();
		if ( influencers.getTemporalIdentifier() == null ) {
			return false;
		}
		final var auxiliaryMapping = persister.getAttributeMapping().getAuxiliaryMapping();
		return auxiliaryMapping != null && auxiliaryMapping.isAffectedByInfluencers( influencers )
			|| ( persister.isOneToMany() || persister.isManyToMany() )
					&& readsHistory( session, persister.getElementPersister() )
			|| persister.getAttributeMapping().getIndexDescriptor() instanceof EntityCollectionPart index
					&& readsHistory( session, index.getAssociatedEntityMappingType().getEntityPersister() );
	}

	/**
	 * A cached collection holds identifiers. A change to a map key may hide or reveal
	 * its entry without modifying the collection or invalidating its cache entry.
	 */
	public static boolean canUseCollectionCache(
			@Nonnull SharedSessionContractImplementor session, @Nonnull CollectionPersister persister) {
		if ( !persister.hasCache() || readsHistory( session, persister ) ) {
			return false;
		}
		if ( persister.getAttributeMapping().getIndexDescriptor() instanceof EntityCollectionPart key ) {
			final var keyType = key.getAssociatedEntityMappingType();
			return !keyType.hasWhereRestrictions() && keyType.getSoftDeleteMapping() == null;
		}
		return true;
	}

	public static Object fromSharedCache(
			SharedSessionContractImplementor session,
			Object cacheKey,
			EntityPersister persister,
			CachedDomainDataAccess cacheAccess) {
		return fromSharedCache( session, cacheKey, persister, false, cacheAccess );
	}

	public static Object fromSharedCache(
			SharedSessionContractImplementor session,
			Object cacheKey,
			EntityPersister persister,
			boolean isNaturalKey,
			CachedDomainDataAccess cacheAccess) {
		if ( session.isRootTenant() ) {
			return null;
		}
		final var eventListenerManager = session.getEventListenerManager();
		Object cachedValue = null;
		eventListenerManager.cacheGetStart();
		final var eventMonitor = session.getEventMonitor();
		final var cacheGetEvent = eventMonitor.beginCacheGetEvent();
		try {
			cachedValue = cacheAccess.get( session, cacheKey );
		}
		finally {
			eventMonitor.completeCacheGetEvent(
					cacheGetEvent,
					session,
					cacheAccess.getRegion(),
					persister,
					isNaturalKey,
					cachedValue != null
			);
			eventListenerManager.cacheGetEnd( cachedValue != null );
		}
		return cachedValue;
	}

	public static Object fromSharedCache(
			SharedSessionContractImplementor session,
			Object cacheKey,
			CollectionPersister persister,
			CachedDomainDataAccess cacheAccess) {
		if ( session.isRootTenant() ) {
			return null;
		}
		final var eventListenerManager = session.getEventListenerManager();
		Object cachedValue = null;
		eventListenerManager.cacheGetStart();
		final var eventMonitor = session.getEventMonitor();
		final var cacheGetEvent = eventMonitor.beginCacheGetEvent();
		try {
			cachedValue = cacheAccess.get( session, cacheKey );
		}
		finally {
			eventMonitor.completeCacheGetEvent(
					cacheGetEvent,
					session,
					cacheAccess.getRegion(),
					persister,
					cachedValue != null
			);
			eventListenerManager.cacheGetEnd( cachedValue != null );
		}
		return cachedValue;
	}

	public static void writingToCache(
			@Nonnull EntityPersister persister,
			@Nonnull Consumer<EntityDataAccess> action) {
		if ( persister.canWriteToCache() ) {
			final var cache = persister.getCacheAccessStrategy();
			assert cache != null;
			action.accept( cache );
		}
	}

	public static void readingFromCache(
			@Nonnull EntityPersister persister,
			@Nonnull Consumer<EntityDataAccess> action) {
		if ( persister.canReadFromCache() ) {
			final var cache = persister.getCacheAccessStrategy();
			assert cache != null;
			action.accept( cache );
		}
	}

	public static <T> T readingFromCache(
			@Nonnull EntityPersister persister,
			@Nonnull Function<EntityDataAccess, T> action,
			T defaultValue) {
		if ( persister.canReadFromCache() ) {
			final var cache = persister.getCacheAccessStrategy();
			assert cache != null;
			return action.apply( cache );
		}
		else {
			return defaultValue;
		}
	}

	public static <T> T writingToCache(
			@Nonnull EntityPersister persister,
			@Nonnull Function<EntityDataAccess, T> action,
			T defaultValue) {
		if ( persister.canWriteToCache() ) {
			final var cache = persister.getCacheAccessStrategy();
			assert cache != null;
			return action.apply( cache );
		}
		else {
			return defaultValue;
		}
	}

	public static void usingCache(
			@Nonnull CollectionPersister persister,
			@Nonnull Consumer<CollectionDataAccess> action) {
		if ( persister.hasCache() ) {
			final var cache = persister.getCacheAccessStrategy();
			assert cache != null;
			action.accept( cache );
		}
	}

	public static <T> T usingCache(
			@Nonnull CollectionPersister persister,
			@Nonnull Function<CollectionDataAccess, T> action,
			T defaultValue) {
		if ( persister.hasCache() ) {
			final var cache = persister.getCacheAccessStrategy();
			assert cache != null;
			return action.apply( cache );
		}
		else {
			return defaultValue;
		}
	}

	public static void addBasicValueToCacheKey(
			MutableCacheKeyBuilder cacheKey,
			Object value,
			JdbcMapping jdbcMapping,
			SharedSessionContractImplementor session) {
		final BasicValueConverter converter = jdbcMapping.getValueConverter();
		final Object convertedValue;
		final JavaType javaType;
		if ( converter == null ) {
			javaType = jdbcMapping.getJavaTypeDescriptor();
			convertedValue = value;
		}
		else {
			javaType = converter.getRelationalJavaType();
			convertedValue = converter.toRelationalValue( value );
		}
		addBasicValueToCacheKey( cacheKey, convertedValue, javaType, session );
	}

	public static <T> void addBasicValueToCacheKey(
			MutableCacheKeyBuilder cacheKey,
			T value,
			JavaType<T> javaType,
			SharedSessionContractImplementor session) {
		if ( value == null ) {
			cacheKey.addValue( null );
			cacheKey.addHashCode( 0 );
		}
		else {
			cacheKey.addValue( javaType.getMutabilityPlan().disassemble( value, session ) );
			cacheKey.addHashCode( javaType.extractHashCode( value ) );
		}
	}
}
