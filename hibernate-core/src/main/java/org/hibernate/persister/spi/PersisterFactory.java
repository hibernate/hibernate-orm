/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.persister.spi;

import org.hibernate.HibernateException;
import org.hibernate.cache.spi.access.CollectionDataAccess;
import org.hibernate.cache.spi.access.EntityDataAccess;
import org.hibernate.cache.spi.access.NaturalIdDataAccess;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.metamodel.spi.RuntimeModelCreationContext;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.service.Service;

/**
 * Contract for creating persister instances (both {@link EntityPersister} and {@link CollectionPersister} varieties).
 *
 * @author Steve Ebersole
 */
public interface PersisterFactory extends Service {
	/**
	 * Create an entity persister instance.
	 *
	 * @param entityBinding The mapping information describing the entity
	 * @param entityCacheAccessStrategy The cache access strategy for the entity region
	 * @param naturalIdCacheAccessStrategy The cache access strategy for the entity's natural-id cross-ref region
	 * @param creationContext Access to additional information needed to create the EntityPersister
	 */
	default EntityPersister createEntityPersister(
			PersistentClass entityBinding,
			EntityDataAccess entityCacheAccessStrategy,
			NaturalIdDataAccess naturalIdCacheAccessStrategy,
			RuntimeModelCreationContext creationContext) {
		// for now, to minimize impact on existing custom impls, default this form to delegate to the original
		return createEntityPersister(
				entityBinding,
				entityCacheAccessStrategy,
				naturalIdCacheAccessStrategy,
				(PersisterCreationContext) creationContext
		);
	}

	/**
	 * Create a collection persister instance.
	 *
	 * @param collectionBinding The mapping information describing the collection
	 * @param cacheAccessStrategy The cache access strategy for the collection region
	 * @param creationContext Access to additional information needed to create an EntityPersister
	 */
	default CollectionPersister createCollectionPersister(
			Collection collectionBinding,
			CollectionDataAccess cacheAccessStrategy,
			RuntimeModelCreationContext creationContext) {
		// for now, to minimize impact on existing custom impls, default this form to delegate to the original
		return createCollectionPersister( collectionBinding, cacheAccessStrategy, (PersisterCreationContext) creationContext );
	}

	/**
	 * Create an entity persister instance.
	 *
	 * @param entityBinding The mapping information describing the entity
	 * @param entityCacheAccessStrategy The cache access strategy for the entity region
	 * @param naturalIdCacheAccessStrategy The cache access strategy for the entity's natural-id cross-ref region
	 * @param creationContext Access to additional information needed to create an EntityPersister
	 *
	 * @return An appropriate entity persister instance.
	 *
	 * @deprecated (since 6.0) use {@link #createEntityPersister(PersistentClass, EntityDataAccess, NaturalIdDataAccess, RuntimeModelCreationContext)}
	 * instead
	 */
	@Deprecated
	EntityPersister createEntityPersister(
			PersistentClass entityBinding,
			EntityDataAccess entityCacheAccessStrategy,
			NaturalIdDataAccess naturalIdCacheAccessStrategy,
			PersisterCreationContext creationContext) throws HibernateException;

	/**
	 * Create a collection persister instance.
	 *
	 * @param collectionBinding The mapping information describing the collection
	 * @param cacheAccessStrategy The cache access strategy for the collection region
	 * @param creationContext Access to additional information needed to create an EntityPersister
	 *
	 * @deprecated (since 6.0) use {@link #createCollectionPersister(Collection, CollectionDataAccess, RuntimeModelCreationContext)}
	 * instead
	 */
	@Deprecated
	CollectionPersister createCollectionPersister(
			Collection collectionBinding,
			CollectionDataAccess cacheAccessStrategy,
			PersisterCreationContext creationContext);
}
