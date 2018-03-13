/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate;

import java.io.Serializable;

/**
 * Provides an API for querying/managing the second level cache regions.
 * <p/>
 * CAUTION: None of these methods respect any isolation or transactional
 * semantics associated with the underlying caches.  Specifically, evictions
 * perform an immediate "hard" removal outside any transactions and/or locking
 * scheme(s).
 *
 * @author Steve Ebersole
 */
@SuppressWarnings( {"UnusedDeclaration"})
public interface Cache extends javax.persistence.Cache {
	/**
	 * Access to the SessionFactory this Cache is bound to.
	 *
	 * @return The SessionFactory
	 */
	SessionFactory getSessionFactory();



	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Entity data

	/**
	 * Determine whether the cache contains data for the given entity "instance".
	 * <p/>
	 * The semantic here is whether the cache contains data visible for the
	 * current call context.
	 *
	 * @param entityClass The entity class.
	 * @param identifier The entity identifier
	 *
	 * @return True if the underlying cache contains corresponding data; false
	 * otherwise.
	 */
	boolean containsEntity(Class entityClass, Serializable identifier);

	/**
	 * Determine whether the cache contains data for the given entity "instance".
	 * <p/>
	 * The semantic here is whether the cache contains data visible for the
	 * current call context.
	 *
	 * @param entityName The entity name.
	 * @param identifier The entity identifier
	 *
	 * @return True if the underlying cache contains corresponding data; false otherwise.
	 */
	boolean containsEntity(String entityName, Serializable identifier);

	/**
	 * Evicts the entity data for a particular entity "instance".
	 *
	 * @param entityClass The entity class.
	 * @param identifier The entity identifier
	 *
	 * @since 5.3
	 */
	void evictEntityData(Class entityClass, Serializable identifier);

	/**
	 * Evicts the entity data for a particular entity "instance".
	 *
	 * @param entityName The entity name.
	 * @param identifier The entity identifier
	 *
	 * @since 5.3
	 */
	void evictEntityData(String entityName, Serializable identifier);

	/**
	 * Evicts all entity data from the given region (i.e. for all entities of
	 * type).
	 *
	 * @param entityClass The entity class.
	 *
	 * @since 5.3
	 */
	void evictEntityData(Class entityClass);

	/**
	 * Evicts all entity data from the given region (i.e. for all entities of
	 * type).
	 *
	 * @param entityName The entity name.
	 *
	 * @since 5.3
	 */
	void evictEntityData(String entityName);

	/**
	 * Evict data from all entity regions.
	 *
	 * @since 5.3
	 */
	void evictEntityData();


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Natural-id data


	/**
	 * Evict cached data for the given entity's natural-id
	 *
	 * @param entityClass The entity class.
	 *
	 * @since 5.3
	 */
	void evictNaturalIdData(Class entityClass);

	/**
	 * Evict cached data for the given entity's natural-id
	 *
	 * @param entityName The entity name.
	 *
	 * @since 5.3
	 */
	void evictNaturalIdData(String entityName);

	/**
	 * Evict cached data for all natural-ids (for all entities)
	 *
	 * @since 5.3
	 */
	void evictNaturalIdData();




	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Collection data

	/**
	 * Determine whether the cache contains data for the given collection.
	 * <p/>
	 * The semantic here is whether the cache contains data visible for the
	 * current call context.
	 *
	 * @param role The name of the collection role (in form
	 * [owner-entity-name].[collection-property-name]) whose regions should be
	 * evicted.
	 * @param ownerIdentifier The identifier of the owning entity
	 *
	 * @return True if the underlying cache contains corresponding data; false otherwise.
	 */
	@SuppressWarnings( {"UnusedDeclaration"})
	boolean containsCollection(String role, Serializable ownerIdentifier);


	/**
	 * Evicts the cache data for the given identified collection "instance"
	 *
	 * @param role The "collection role" (in form [owner-entity-name].[collection-property-name]).
	 * @param ownerIdentifier The identifier of the owning entity
	 *
	 * @since 5.3
	 */
	void evictCollectionData(String role, Serializable ownerIdentifier);

	/**
	 * Evicts cached data for the given collection role
	 *
	 * @param role The "collection role" (in form [owner-entity-name].[collection-property-name]).
	 *
	 * @since 5.3
	 */
	void evictCollectionData(String role);

	/**
	 * Evict cache data for all collections
	 *
	 * @since 5.3
	 */
	void evictCollectionData();




	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Query result data

	/**
	 * Determine whether the cache contains data for the given query.
	 * <p/>
	 * The semantic here is whether the cache contains any data for the given
	 * region name since query result caches are not transactionally isolated.
	 *
	 * @param regionName The cache name given to the query.
	 *
	 * @return True if the underlying cache contains corresponding data; false otherwise.
	 */
	boolean containsQuery(String regionName);

	/**
	 * Evicts all cached query results from the default region.
	 */
	void evictDefaultQueryRegion();

	/**
	 * Evicts all cached query results under the given name.
	 *
	 * @param regionName The cache name associated to the queries being cached.
	 */
	void evictQueryRegion(String regionName);

	/**
	 * Evict data from all query regions.
	 */
	void evictQueryRegions();



	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Misc

	/**
	 * Evict all data from the named cache region
	 *
	 * @since 5.3
	 */
	void evictRegion(String regionName);

	/**
	 * {@inheritDoc}
	 *
	 * @apiNote Hibernate impl - we only evict entity data here in keeping
	 * with the JPA intent (JPA only defines caching for entity data).  For
	 * evicting all cache regions (collections, natural-ids and query results),
	 * use {@link #evictAllRegions} instead.
	 */
	@Override
	default void evictAll() {
		// Evict only the "JPA cache", which is purely defined as the entity regions.
		evictEntityData();
	}

	/**
	 * Evict data from all cache regions.
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
	 * @deprecated Use {@link Cache#evictEntityData(Class, Serializable)} instead
	 */
	@Deprecated
	default void evictEntity(Class entityClass, Serializable identifier) {
		evictEntityData( entityClass, identifier );
	}

	/**
	 * Evicts the entity data for a particular entity "instance".
	 *
	 * @param entityName The entity name.
	 * @param identifier The entity identifier
	 *
	 * @deprecated Use {@link Cache#evictEntityData(String, Serializable)} instead
	 */
	@Deprecated
	default void evictEntity(String entityName, Serializable identifier) {
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
	default void evictEntityRegion(Class entityClass) {
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
	default void evictNaturalIdRegion(Class entityClass) {
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
	 * @param role The "collection role" (in form [owner-entity-name].[collection-property-name]).
	 * @param ownerIdentifier The identifier of the owning entity
	 *
	 * @deprecated Use {@link Cache#evictCollectionData(String, Serializable)} instead
	 */
	@Deprecated
	default void evictCollection(String role, Serializable ownerIdentifier) {
		evictCollectionData( role, ownerIdentifier );
	}

	/**
	 * Evicts all entity data from the given region (i.e. evicts cached data
	 * for all of the specified collection role).
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
