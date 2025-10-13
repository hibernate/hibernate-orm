/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate;

/**
 * An API for directly querying and managing the second-level cache.
 * <p>
 * Hibernate has two levels of caching:
 * <ul>
 * <li>The <em>first-level cache</em> is better known as the persistence context.
 *     It's the collection of managed entity instances associated with an open
 *     {@link Session}.
 * <li>The <em>second-level cache</em> is shared between all sessions belonging to
 *     a given {@link SessionFactory}. It stores the state of an entity instance
 *     in a destructured format, as a tuple of persistent attribute values. The
 *     second-level cache is also used to store cached query result sets.
 * </ul>
 * <p>
 * By nature, a second-level cache tends to undermine the ACID properties of
 * transaction processing in a relational database. A second-level cache is often
 * by far the easiest way to improve the performance of a system, but only at the
 * cost of making it much more difficult to reason about concurrency. And so the
 * cache is a potential source of bugs which are difficult to isolate and reproduce.
 * <p>
 * Therefore, only entities and collection roles explicitly annotated
 * {@link jakarta.persistence.Cacheable} or {@link org.hibernate.annotations.Cache}
 * are eligible for storage in the second-level cache, and so by default the state
 * of an entity is always retrieved from the database when requested.
 * <p>
 * Hibernate segments the second-level cache into named <em>regions</em>, one for
 * each mapped entity hierarchy or collection role, each with its own policies for
 * expiry, persistence, and replication, which must be configured externally to
 * Hibernate. An entity hierarchy or collection role may be explicitly assigned a
 * region using the {@link org.hibernate.annotations.Cache} annotation, but, by
 * default, the region name is just the name of the entity class or collection role.
 * <p>
 * The appropriate policies depend on the kind of data an entity represents. For
 * example, a program might have different caching policies for "reference" data,
 * for transactional data, and for data used for analytics. Ordinarily, the
 * implementation of those policies is the responsibility of the
 * {@linkplain org.hibernate.cache.spi.RegionFactory cache provider} and is
 * transparent to code which makes use of a Hibernate {@link Session}. At worst,
 * interaction with the cache may be controlled by specification of an explicit
 * {@link CacheMode}.
 * <p>
 * Very occasionally, it's necessary or advantageous to control the cache explicitly
 * via programmatic eviction, using, for example, {@link #evictEntityData(Class)} to
 * evict a whole cache region, or {@link #evictEntityData(Class, Object)}, to evict
 * a single item.
 * <p>
 * The second-level cache is never aware of changes made externally to Hibernate,
 * for example, directly via JDBC, or by another program. Therefore, programmatic
 * eviction might be required in order to ensure that such updates do not result
 * in stale data being read from the cache.
 * <p>
 * If multiple entities or roles are mapped to the same cache region, they share
 * policies and even the same FIFO-type expiry queue (if any). This sounds useful,
 * but comes with the downside that {@link #evictEntityData(Class)} for any one of
 * the entities evicts <em>all</em> entities mapped to the same region. It's
 * therefore much more common to have a distinct region for each entity and role.
 * <p>
 * None of the operations of this interface respect any isolation or transactional
 * semantics associated with the underlying caches. In particular, eviction via
 * the methods of this interface causes an immediate "hard" removal outside any
 * current transaction and/or locking scheme.
 * <p>
 * The {@link org.hibernate.annotations.Cache} annotation also specifies a
 * {@link org.hibernate.annotations.CacheConcurrencyStrategy}, a policy governing
 * access to the second-level cache by concurrent transactions. Either:
 * <ul>
 * <li>{@linkplain org.hibernate.annotations.CacheConcurrencyStrategy#READ_ONLY
 *     read-only access} for immutable data,
 * <li>{@linkplain org.hibernate.annotations.CacheConcurrencyStrategy#NONSTRICT_READ_WRITE
 *     read/write access with no locking}, when concurrent updates are
 *     extremely improbable,
 * <li>{@linkplain org.hibernate.annotations.CacheConcurrencyStrategy#READ_WRITE
 *     read/write access using soft locks} when concurrent updates are possible
 *     but not common, or
 * <li>{@linkplain org.hibernate.annotations.CacheConcurrencyStrategy#TRANSACTIONAL
 *     transactional access} when concurrent updates are frequent.
 * </ul>
 * <p>
 * It's important to always explicitly specify an appropriate policy, taking into
 * account the expected patterns of data access, most importantly, the frequency
 * of updates.
 * <p>
 * Query result sets may also be stored in the second-level cache. A query is made
 * eligible for caching by calling
 * {@link org.hibernate.query.SelectionQuery#setCacheable(boolean)}, and may be
 * assigned to a region of the second-level cache by calling
 * {@link org.hibernate.query.SelectionQuery#setCacheRegion(String)}. It's very
 * important to understand that any entity instance in a query result set is cached
 * by its id. If the entity itself is not {@linkplain org.hibernate.annotations.Cache
 * cacheable}, or if the instance is not available in the second-level cache at the
 * time a result set is retrieved from the cache, then the state of the entity must
 * be read from the database. <em>This negates the benefits of caching the result
 * set.</em> It's therefore very important to carefully "match" the caching policies
 * of a query and the entities it returns.
 * <p>
 * Hibernate does not itself contain a high-quality implementation of a second-level
 * cache backend with expiry, persistence, and replication, and depends on a plug-in
 * implementation of {@link org.hibernate.cache.spi.RegionFactory} to integrate a
 * backend storage mechanism. Therefore, the second-level cache is completely disabled
 * by default, unless {@value org.hibernate.cfg.AvailableSettings#CACHE_REGION_FACTORY}
 * is explicitly specified. For convenience, the second-level cache may also be enabled
 * or disabled using {@value org.hibernate.cfg.AvailableSettings#USE_SECOND_LEVEL_CACHE}.
 *
 * @author Steve Ebersole
 *
 * @see org.hibernate.annotations.Cache
 * @see org.hibernate.annotations.CacheConcurrencyStrategy
 * @see org.hibernate.cfg.AvailableSettings#CACHE_REGION_FACTORY
 * @see org.hibernate.cfg.AvailableSettings#USE_SECOND_LEVEL_CACHE
 */
public interface Cache extends jakarta.persistence.Cache {
	/**
	 * The {@link SessionFactory} to which this {@code Cache} belongs.
	 *
	 * @return The SessionFactory
	 */
	SessionFactory getSessionFactory();




	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Entity data

	/**
	 * Determine whether the cache contains an item for the entity of the given
	 * type, and with the given identifier.
	 *
	 * @param entityClass The entity type
	 * @param identifier The entity identifier
	 *
	 * @return True if the underlying cache contains corresponding data; false
	 * otherwise.
	 */
	boolean containsEntity(Class<?> entityClass, Object identifier);

	/**
	 * Determine whether the cache contains an item for the entity of the type
	 * with the given name, and with the given identifier.
	 *
	 * @param entityName The entity name
	 * @param identifier The entity identifier
	 *
	 * @return True if the underlying cache contains corresponding data; false otherwise.
	 */
	boolean containsEntity(String entityName, Object identifier);

	/**
	 * Evicts the cached item for the entity of the given type, and with the
	 * given identifier, if there is any such item in the cache.
	 *
	 * @param entityClass The entity type
	 * @param identifier The entity identifier
	 *
	 * @since 5.3
	 */
	void evictEntityData(Class<?> entityClass, Object identifier);

	/**
	 * Evict the cached item for the entity of the type with the given name,
	 * and with the given identifier, if there is any such item in the cache.
	 *
	 * @param entityName The entity name
	 * @param identifier The entity identifier
	 *
	 * @since 5.3
	 */
	void evictEntityData(String entityName, Object identifier);

	/**
	 * Evict all cached data from the cache region to which the given entity
	 * type is assigned. Thus, every cached item for the given entity type will
	 * be evicted, along with any cached items for any other entity type
	 * assigned to the same cache region.
	 *
	 * @param entityClass The entity type
	 *
	 * @since 5.3
	 */
	void evictEntityData(Class<?> entityClass);

	/**
	 * Evict all cached data from the cache region to which the given named
	 * entity type is assigned. Thus, every cached item for the given entity
	 * type will be evicted, along with any cached items for any other entity
	 * type assigned to the same cache region.
	 *
	 * @param entityName The entity name
	 *
	 * @since 5.3
	 */
	void evictEntityData(String entityName);

	/**
	 * Evict all cached data from every cache region to which any entity type
	 * is assigned.
	 *
	 * @since 5.3
	 */
	void evictEntityData();


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Natural-id data


	/**
	 * Evict all cached natural id mappings for the given entity type.
	 *
	 * @param entityClass The entity type
	 *
	 * @since 5.3
	 */
	void evictNaturalIdData(Class<?> entityClass);

	/**
	 * Evict all cached natural id mappings for the entity type with the
	 * given name.
	 *
	 * @param entityName The entity name
	 *
	 * @since 5.3
	 */
	void evictNaturalIdData(String entityName);

	/**
	 * Evict all cached natural id mappings for every entity type.
	 *
	 * @since 5.3
	 */
	void evictNaturalIdData();




	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Collection data

	/**
	 * Determine whether the cache contains an item for the collection with the
	 * given role and given identifier.
	 *
	 * @param role The name of the collection role in the form
	 *             {@code package.OwnerEntityName.collectionPropertyName}
	 * @param ownerIdentifier The identifier of the owning entity
	 *
	 * @return True if the underlying cache contains corresponding data; false otherwise.
	 */
	boolean containsCollection(String role, Object ownerIdentifier);

	/**
	 * Evict the cached item for the collection with the given role and given
	 * identifier, if there is any such item in the cache.
	 *
	 * @param role The name of the collection role in the form
	 *             {@code package.OwnerEntityName.collectionPropertyName}
	 * @param ownerIdentifier The identifier of the owning entity
	 *
	 * @since 5.3
	 */
	void evictCollectionData(String role, Object ownerIdentifier);

	/**
	 * Evict all cached data from the cache region to which the given collection
	 * role is assigned.
	 *
	 * @param role The name of the collection role in the form
	 *             {@code package.OwnerEntityName.collectionPropertyName}
	 *
	 * @since 5.3
	 */
	void evictCollectionData(String role);

	/**
	 * Evict all cache data from every cache region to which some collection
	 * role is assigned.
	 *
	 * @since 5.3
	 */
	void evictCollectionData();




	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Query result data

	/**
	 * Determine whether the given region name contains cached query results.
	 *
	 * @param regionName The name of a cache region to which some query is assigned
	 *
	 * @return True if the underlying cache contains corresponding data; false otherwise.
	 */
	boolean containsQuery(String regionName);

	/**
	 * Evict all cached query results from the default region.
	 */
	void evictDefaultQueryRegion();

	/**
	 * Evict all cached query results from the region with the given name.
	 *
	 * @param regionName The cache name associated to the queries being cached.
	 */
	void evictQueryRegion(String regionName);

	/**
	 * Evict all cached query results from every region.
	 */
	void evictQueryRegions();



	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Misc

	/**
	 * Evict all cached data from the named cache region.
	 *
	 * @since 5.3
	 */
	void evictRegion(String regionName);

	/**
	 * Evict all cached entity data.
	 *
	 * @apiNote This operation only affects cached data for entities, in keeping
	 * with the intent of the JPA specification, which only defines caching for
	 * entity data. To evict all data from every cache region, including cached
	 * collections, natural id mappings, and cached query results, use
	 * {@link #evictAllRegions()} instead.
	 */
	@Override
	void evictAll();

	/**
	 * Evict all cached data from every cache region, including cached
	 * collections, natural id mappings, and cached query results.
	 */
	void evictAllRegions();
}
