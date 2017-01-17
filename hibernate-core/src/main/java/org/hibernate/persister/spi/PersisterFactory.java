/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.persister.spi;

import org.hibernate.HibernateException;
import org.hibernate.cache.spi.access.CollectionRegionAccessStrategy;
import org.hibernate.cache.spi.access.EntityRegionAccessStrategy;
import org.hibernate.cache.spi.access.NaturalIdRegionAccessStrategy;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.persister.collection.spi.CollectionPersister;
import org.hibernate.persister.common.spi.ManagedTypeImplementor;
import org.hibernate.persister.embeddable.spi.EmbeddableContainer;
import org.hibernate.persister.embeddable.spi.EmbeddablePersister;
import org.hibernate.persister.entity.spi.EntityPersister;
import org.hibernate.service.Service;

/**
 * Contract for creating persister instances including {@link EntityPersister},
 * {@link CollectionPersister} and {@link org.hibernate.persister.embeddable.spi.EmbeddablePersister}
 *
 * @author Steve Ebersole
 */
public interface PersisterFactory extends Service {
	/**
	 * Create an entity persister instance.
	 * <p/>
	 * A persister will not be completely usable after return from this method.  The returned
	 * reference is good for linking references together, etc.  The persister will be fully
	 * initialized later via its {@link EntityPersister#afterInitialize} method during {@link #finishUp}
	 *
	 * @param entityBinding The mapping information describing the entity
	 * @param entityCacheAccessStrategy The cache access strategy for the entity region
	 * @param naturalIdCacheAccessStrategy The cache access strategy for the entity's natural-id cross-ref region
	 * @param creationContext Access to additional information needed to create an EntityPersister
	 *
	 * @return An appropriate entity persister instance.
	 *
	 * @throws HibernateException Indicates a problem building the persister.
	 */
	EntityPersister createEntityPersister(
			PersistentClass entityBinding,
			EntityRegionAccessStrategy entityCacheAccessStrategy,
			NaturalIdRegionAccessStrategy naturalIdCacheAccessStrategy,
			PersisterCreationContext creationContext) throws HibernateException;

	/**
	 * Create a collection persister instance.
	 * <p/>
	 * A persister will not be completely usable after return from this method.  The returned
	 * reference is good for linking references together, etc.  The persister will be fully
	 * initialized later via its {@link EntityPersister#afterInitialize} method during {@link #finishUp}
	 *
	 * @param collectionBinding The mapping information describing the collection
	 * @param cacheAccessStrategy The cache access strategy for the collection region
	 * @param creationContext Access to additional information needed to create an EntityPersister
	 *
	 * @return An appropriate collection persister instance.
	 *
	 * @throws HibernateException Indicates a problem building the persister.
	 */
	CollectionPersister createCollectionPersister(
			Collection collectionBinding,
			ManagedTypeImplementor source,
			String localName,
			CollectionRegionAccessStrategy cacheAccessStrategy,
			PersisterCreationContext creationContext) throws HibernateException;

	/**
	 * Create an embeddable persister instance.
	 *
	 * @param componentBinding The mapping information describing the composition
	 * @param creationContext Access to additional information needed to create a persister
	 *
	 * @return An appropriate collection persister instance.
	 *
	 * @throws HibernateException Indicates a problem building the persister.
	 */
	EmbeddablePersister createEmbeddablePersister(
			Component componentBinding,
			EmbeddableContainer source,
			String localName,
			PersisterCreationContext creationContext);

	/**
	 * Called after all entity mapping descriptors have been processed.
	 *
	 * @param creationContext Access to additional information
	 */
	void finishUp(PersisterCreationContext creationContext);
}
