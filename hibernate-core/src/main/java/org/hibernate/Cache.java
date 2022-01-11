/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate;

import java.io.Serializable;

/**
 * An API for directly querying and managing the second level cache.
 * <p>
 * Note that only entities and collection roles explicitly annotated
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
 * evicts a whole cache region, or {@link #evictEntityData(Class, Object)}, to evict
 * a single item.
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
 *
 * @author Steve Ebersole
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
	 * {@inheritDoc}
	 *
	 * @apiNote This operation only affects cached data for entities, in keeping
	 * with the intent of the JPA specification, which only defines caching for
	 * entity data. To evict all data from every cache region, including cached
	 * collections, natural-id mappings, and cached query results, use
	 * {@link #evictAllRegions()} instead.
	 */
	@Override
	default void evictAll() {
		// Evict only the "JPA cache", which is purely defined as the entity regions.
		evictEntityData();
	}

	/**
	 * Evict all cached data from every cache region.
	 */
	default void evictAllRegions() {
		evictEntityData();
		evictNaturalIdData();
		evictCollectionData();
		evictDefaultQueryRegion();
		evictQueryRegions();
	}



	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Deprecations (5.3)

	/**
	 * Evicts the entity data for a particular entity "instance".
	 *
	 * @param entityClass The entity class.
	 * @param identifier The entity identifier
	 *
	 * @deprecated Use {@link Cache#evictEntityData(Class, Object)} instead
	 */
	@Deprecated
	default void evictEntity(Class<?> entityClass, Object identifier) {
		evictEntityData( entityClass, identifier );
	}

	/**
	 * Evicts the entity data for a particular entity "instance".
	 *
	 * @param entityName The entity name.
	 * @param identifier The entity identifier
	 *
	 * @deprecated Use {@link Cache#evictEntityData(String, Object)} instead
	 */
	@Deprecated
	default void evictEntity(String entityName, Object identifier) {
		evictEntityData( entityName, identifier );
	}

	/**
	 * Evicts all entity data from the given region (i.e. for all entities of
	 * type).
	 *
	 * @param entityClass The entity class.
	 *
	 * @deprecated Use {@link Cache#evictEntityData(Class)} instead
	 */
	@Deprecated
	default void evictEntityRegion(Class<?> entityClass) {
		evictEntityData( entityClass );
	}

	/**
	 * Evicts all entity data from the given region (i.e. for all entities of
	 * type).
	 *
	 * @param entityName The entity name.
	 *
	 * @deprecated Use {@link Cache#evictEntityData(String)} instead
	 */
	@Deprecated
	default void evictEntityRegion(String entityName) {
		evictEntityData( entityName );
	}

	/**
	 * Evict data from all entity regions.
	 *
	 * @deprecated Use {@link Cache#evictEntityData()} instead
	 */
	@Deprecated
	default void evictEntityRegions() {
		evictEntityData();
	}

	/**
	 * Evicts all naturalId data from the given region (i.e. for all entities of
	 * type).
	 *
	 * @param entityClass The entity class.
	 *
	 * @deprecated Use {@link Cache#evictNaturalIdData(Class)} instead
	 */
	@Deprecated
	default void evictNaturalIdRegion(Class<?> entityClass) {
		evictNaturalIdData( entityClass );
	}

	/**
	 * Evicts all naturalId data from the given region (i.e. for all entities of
	 * type).
	 *
	 * @param entityName The entity name.
	 *
	 * @deprecated Use {@link Cache#evictNaturalIdData(String)} instead
	 */
	@Deprecated
	default void evictNaturalIdRegion(String entityName) {
		evictNaturalIdData( entityName );
	}

	/**
	 * Evict data from all naturalId regions.
	 *
	 * @deprecated Use {@link Cache#evictNaturalIdData()} instead
	 */
	@Deprecated
	default void evictNaturalIdRegions() {
		evictNaturalIdData();
	}

	/**
	 * Evicts the cache data for the given identified collection instance.
	 *
	 * @param role The "collection role" (of form {@code package.OwnerEntityName.collectionPropertyName}).
	 * @param ownerIdentifier The identifier of the owning entity
	 *
	 * @deprecated Use {@link Cache#evictCollectionData(String, Object)} instead
	 */
	@Deprecated
	default void evictCollection(String role, Serializable ownerIdentifier) {
		evictCollectionData( role, ownerIdentifier );
	}

	/**
	 * Evicts all entity data from the given region (i.e. evicts all cached data
	 * for the specified collection role).
	 *
	 * @param role The "collection role" (in form [owner-entity-name].[collection-property-name]).
	 *
	 * @deprecated Use {@link Cache#evictCollectionData(String)} instead
	 */
	@Deprecated
	default void evictCollectionRegion(String role) {
		evictCollectionData( role );
	}

	/**
	 * Evict data from all collection regions.
	 *
	 * @deprecated Use {@link Cache#evictCollectionData()} instead
	 */
	@Deprecated
	default void evictCollectionRegions() {
		evictCollectionData();
	}

}
