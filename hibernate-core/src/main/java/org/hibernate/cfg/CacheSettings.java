/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.cfg;

import org.hibernate.Incubating;
import org.hibernate.annotations.CacheLayout;
import org.hibernate.cache.spi.RegionFactory;
import org.hibernate.cache.spi.TimestampsCacheFactory;
import org.hibernate.jpa.SpecHints;

/**
 * Settings for Hibernate's second-level caching
 *
 * @author Steve Ebersole
 */
public interface CacheSettings {
	/**
	 * Specifies the {@link jakarta.persistence.SharedCacheMode}.
	 * <p>
	 * Hibernate is designed to be used with
	 * {@link jakarta.persistence.SharedCacheMode#ENABLE_SELECTIVE
	 * ENABLE_SELECTIVE}, and we strongly discourage the use of
	 * {@link jakarta.persistence.SharedCacheMode#ALL ALL} or
	 * {@link jakarta.persistence.SharedCacheMode#DISABLE_SELECTIVE
	 * DISABLE_SELECTIVE}, since in any multiuser system a cache is
	 * <em>always</em> a potential source of bugs which are difficult
	 * to isolate and reproduce. Caching should never be turned on
	 * "by accident".
	 * <p>
	 * Setting the shared cache mode to
	 * {@link jakarta.persistence.SharedCacheMode#NONE} has very
	 * nearly the same effect as {@linkplain #USE_SECOND_LEVEL_CACHE
	 * disabling the second-level cache}, globally suppressing every
	 * occurrence of the {@link jakarta.persistence.Cacheable} and
	 * {@link org.hibernate.annotations.Cache} annotations.
	 *
	 * @see jakarta.persistence.SharedCacheMode
	 *
	 * @settingDefault {@code ENABLE_SELECTIVE}
	 */
	String JAKARTA_SHARED_CACHE_MODE = "jakarta.persistence.sharedCache.mode";

	/**
	 * Set a default value for {@link SpecHints#HINT_SPEC_CACHE_RETRIEVE_MODE},
	 * used when the hint is not explicitly specified.
	 * <p>
	 * It does not usually make sense to change the default from
	 * {@link jakarta.persistence.CacheRetrieveMode#USE}.
	 *
	 * @see SpecHints#HINT_SPEC_CACHE_RETRIEVE_MODE
	 *
	 * @settingDefault {@code USE}
	 */
	String JAKARTA_SHARED_CACHE_RETRIEVE_MODE = SpecHints.HINT_SPEC_CACHE_RETRIEVE_MODE;

	/**
	 * Set a default value for {@link SpecHints#HINT_SPEC_CACHE_STORE_MODE},
	 * used when the hint is not explicitly specified.
	 * <p>
	 * It does not usually make sense to change the default from
	 * {@link jakarta.persistence.CacheStoreMode#USE}.
	 *
	 * @see SpecHints#HINT_SPEC_CACHE_RETRIEVE_MODE
	 *
	 * @settingDefault {@code USE}
	 */
	String JAKARTA_SHARED_CACHE_STORE_MODE = SpecHints.HINT_SPEC_CACHE_STORE_MODE;


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Hibernate settings
	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/**
	 * When enabled, specifies that the second-level cache may be used.
	 * <p>
	 * By default, if the configured {@link RegionFactory}
	 * is not the {@link org.hibernate.cache.internal.NoCachingRegionFactory}, then
	 * the second-level cache is enabled. Otherwise, the second-level cache is disabled.
	 *
	 * @settingDefault {@code true} when a {@linkplain #CACHE_REGION_FACTORY provider} is
	 * specified; {@code false} otherwise.
	 *
	 * @see #CACHE_REGION_FACTORY
	 * @see org.hibernate.boot.SessionFactoryBuilder#applySecondLevelCacheSupport(boolean)
	 */
	String USE_SECOND_LEVEL_CACHE = "hibernate.cache.use_second_level_cache";

	/**
	 * Enable the query results cache
	 *
	 * @settingDefault {@code false}
	 *
	 * @see org.hibernate.boot.SessionFactoryBuilder#applyQueryCacheSupport(boolean)
	 */
	String USE_QUERY_CACHE = "hibernate.cache.use_query_cache";

	/**
	 * Specifies the default {@link org.hibernate.annotations.CacheLayout} to use for the query cache.
	 *
	 * @see org.hibernate.boot.SessionFactoryBuilder#applyQueryCacheLayout(CacheLayout)
	 * @see org.hibernate.annotations.QueryCacheLayout
	 * @since 6.5
	 */
	@Incubating
	String QUERY_CACHE_LAYOUT = "hibernate.cache.query_cache_layout";

	/**
	 * The {@link RegionFactory} implementation, either:
	 * <ul>
	 *     <li>a short strategy name, for example, {@code jcache} or
	 *         {@code infinispan},
	 *     <li>an instance of {@code RegionFactory},
	 *     <li>a {@link Class} object representing a class that implements
	 *         {@code RegionFactory}, or
	 *     <li>the name of a class implementing {@code RegionFactory}.
	 * </ul>
	 *
	 * @settingDefault {@link org.hibernate.cache.internal.NoCachingRegionFactory},
	 *                 so that caching is disabled.
	 *
	 * @see #USE_SECOND_LEVEL_CACHE
	 *
	 * @apiNote The term {@code "class"} appears in the setting name due to legacy reasons;
	 *          however it can accept instances.
	 */
	String CACHE_REGION_FACTORY = "hibernate.cache.region.factory_class";

	/**
	 * Specifies the {@link org.hibernate.cache.spi.TimestampsCacheFactory} to use.
	 *
	 * @see org.hibernate.boot.SessionFactoryBuilder#applyTimestampsCacheFactory(TimestampsCacheFactory)
	 */
	String QUERY_CACHE_FACTORY = "hibernate.cache.query_cache_factory";

	/**
	 * The {@code CacheProvider} region name prefix
	 *
	 * @see org.hibernate.boot.SessionFactoryBuilder#applyCacheRegionPrefix(String)
	 */
	String CACHE_REGION_PREFIX = "hibernate.cache.region_prefix";

	/**
	 * Specifies the {@link org.hibernate.annotations.CacheConcurrencyStrategy} to use by
	 * default when an entity is marked {@link jakarta.persistence.Cacheable @Cacheable},
	 * but no concurrency strategy is explicitly specified via the
	 * {@link org.hibernate.annotations.Cache} annotation.
	 * <p>
	 * An explicit strategy may be specified using
	 * {@link org.hibernate.annotations.Cache#usage @Cache(usage=...)}.
	 *
	 * @settingDefault The cache provider's default strategy
	 *
	 * @see org.hibernate.boot.MetadataBuilder#applyAccessType(org.hibernate.cache.spi.access.AccessType)
	 * @see RegionFactory#getDefaultAccessType
	 */
	String DEFAULT_CACHE_CONCURRENCY_STRATEGY = "hibernate.cache.default_cache_concurrency_strategy";

	/**
	 * Optimize interaction with the second-level cache to minimize writes, at the cost
	 * of an additional read before each write. This setting is useful if writes to the
	 * cache are much more expensive than reads from the cache, for example, if the cache
	 * is a distributed cache.
	 * <p>
	 * It's not usually necessary to set this explicitly because, by default, it's set
	 * to a {@linkplain org.hibernate.boot.SessionFactoryBuilder#applyMinimalPutsForCaching(boolean)
	 * sensible value} by the second-level cache implementation.
	 *
	 * @settingDefault The cache provider's default
	 *
	 * @see org.hibernate.boot.SessionFactoryBuilder#applyMinimalPutsForCaching(boolean)
	 * @see RegionFactory#isMinimalPutsEnabledByDefault()
	 */
	String USE_MINIMAL_PUTS = "hibernate.cache.use_minimal_puts";

	/**
	 * Enables the use of structured second-level cache entries. This makes the cache
	 * entries human-readable, but carries a performance cost.
	 *
	 * @settingDefault {@code false}
	 *
	 * @see org.hibernate.boot.SessionFactoryBuilder#applyStructuredCacheEntries(boolean)
	 */
	String USE_STRUCTURED_CACHE = "hibernate.cache.use_structured_entries";

	/**
	 * Enables the automatic eviction of a bidirectional association's collection
	 * cache when an element in the {@link jakarta.persistence.ManyToOne} collection
	 * is added, updated, or removed without properly managing the change on the
	 * {@link jakarta.persistence.OneToMany} side.
	 *
	 * @settingDefault {@code false}
	 *
	 * @see org.hibernate.boot.SessionFactoryBuilder#applyAutomaticEvictionOfCollectionCaches(boolean)
	 */
	String AUTO_EVICT_COLLECTION_CACHE = "hibernate.cache.auto_evict_collection_cache";

	/**
	 * Enable direct storage of entity references into the second level cache when
	 * applicable. This is appropriate only for immutable entities.
	 * <p>
	 * By default, entities are always stored in a "disassembled" form, that is, as
	 * a tuple of attribute values.
	 *
	 * @settingDefault {@code false}
	 *
	 * @see org.hibernate.boot.SessionFactoryBuilder#applyDirectReferenceCaching(boolean)
	 */
	String USE_DIRECT_REFERENCE_CACHE_ENTRIES = "hibernate.cache.use_reference_entries";

	/**
	 * Specifies the {@link org.hibernate.cache.spi.CacheKeysFactory} to use, either:
	 * <ul>
	 *     <li>an instance of {@link org.hibernate.cache.spi.CacheKeysFactory},
	 *     <li>a {@link Class} implementing {@link org.hibernate.cache.spi.CacheKeysFactory},
	 *     <li>the name of a class implementing {@link org.hibernate.cache.spi.CacheKeysFactory},
	 *     <li>{@code "default"} as a short name for {@link org.hibernate.cache.internal.DefaultCacheKeysFactory}, or
	 *     <li>{@code "simple"} as a short name for {@link org.hibernate.cache.internal.SimpleCacheKeysFactory}.
	 * </ul>
	 *
	 * @since 5.2
	 *
	 * @deprecated this is only honored for {@code hibernate-infinispan}
	 */
	@Deprecated
	@SuppressWarnings("DeprecatedIsStillUsed")
	String CACHE_KEYS_FACTORY = "hibernate.cache.keys_factory";

	/**
	 * Entity cache configuration properties follow the pattern
	 * {@code hibernate.classcache.packagename.ClassName usage[, region]}
	 * where {@code usage} is the cache strategy used and {@code region} the cache region name
	 */
	String CLASS_CACHE_PREFIX = "hibernate.classcache";

	/**
	 * Collection cache configuration properties follow the pattern
	 * {@code hibernate.collectioncache.packagename.ClassName.role usage[, region]}
	 * where {@code usage} is the cache strategy used and {@code region} the cache region name
	 */
	String COLLECTION_CACHE_PREFIX = "hibernate.collectioncache";


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Legacy JPA settings
	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/**
	 * @see jakarta.persistence.SharedCacheMode
	 *
	 * @deprecated Use {@link #JAKARTA_SHARED_CACHE_MODE} instead
	 */
	@Deprecated
	@SuppressWarnings("DeprecatedIsStillUsed")
	String JPA_SHARED_CACHE_MODE = "javax.persistence.sharedCache.mode";

	/**
	 * @deprecated Use {@link #JAKARTA_SHARED_CACHE_RETRIEVE_MODE} instead
	 */
	@Deprecated
	@SuppressWarnings("DeprecatedIsStillUsed")
	String JPA_SHARED_CACHE_RETRIEVE_MODE = "javax.persistence.cache.retrieveMode";

	/**
	 * @deprecated Use {@link #JAKARTA_SHARED_CACHE_STORE_MODE} instead
	 */
	@Deprecated
	@SuppressWarnings("DeprecatedIsStillUsed")
	String JPA_SHARED_CACHE_STORE_MODE = "javax.persistence.cache.storeMode";

}
