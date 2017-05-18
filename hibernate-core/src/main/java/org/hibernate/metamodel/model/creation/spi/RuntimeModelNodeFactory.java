/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.creation.spi;

import org.hibernate.HibernateException;
import org.hibernate.boot.model.domain.EmbeddedValueMapping;
import org.hibernate.boot.model.domain.EntityMapping;
import org.hibernate.boot.model.domain.MappedSuperclassMapping;
import org.hibernate.cache.spi.access.CollectionRegionAccessStrategy;
import org.hibernate.cache.spi.access.EntityRegionAccessStrategy;
import org.hibernate.cache.spi.access.NaturalIdRegionAccessStrategy;
import org.hibernate.mapping.Collection;
import org.hibernate.metamodel.model.domain.spi.PersistentCollectionMetadata;
import org.hibernate.metamodel.model.domain.internal.MappedSuperclassImpl;
import org.hibernate.metamodel.model.domain.spi.ManagedTypeImplementor;
import org.hibernate.metamodel.model.domain.spi.MappedSuperclassImplementor;
import org.hibernate.metamodel.model.domain.spi.EmbeddedContainer;
import org.hibernate.metamodel.model.domain.spi.EmbeddedTypeImplementor;
import org.hibernate.metamodel.model.domain.spi.EntityTypeImplementor;
import org.hibernate.service.Service;

/**
 * Contract for creating persister instances including {@link EntityTypeImplementor},
 * {@link PersistentCollectionMetadata} and {@link EmbeddedTypeImplementor}
 *
 * @author Steve Ebersole
 */
public interface RuntimeModelNodeFactory extends Service {
	/**
	 * Create an entity persister instance.
	 * <p/>
	 * A persister will not be completely usable after return from this method.  The returned
	 * reference is good for linking references together, etc.  The persister will be fully
	 * initialized later via its {@link EntityTypeImplementor#afterInitialize} method during {@link #finishUp}
	 *
	 * @param bootMapping The mapping information describing the entity
	 * @param entityCacheAccessStrategy The cache access strategy for the entity region
	 * @param naturalIdCacheAccessStrategy The cache access strategy for the entity's natural-id cross-ref region
	 * @param creationContext Access to additional information needed to create an EntityPersister
	 *
	 * @return An appropriate entity persister instance.
	 *
	 * @throws HibernateException Indicates a problem building the persister.
	 */
	<J> EntityTypeImplementor<J> createEntityPersister(
			EntityMapping bootMapping,
			EntityRegionAccessStrategy entityCacheAccessStrategy,
			NaturalIdRegionAccessStrategy naturalIdCacheAccessStrategy,
			RuntimeModelCreationContext creationContext) throws HibernateException;

	default <J> MappedSuperclassImplementor<J> createMappedSuperclass(
			MappedSuperclassMapping bootMapping,
			EntityRegionAccessStrategy entityCacheAccessStrategy,
			NaturalIdRegionAccessStrategy naturalIdCacheAccessStrategy,
			RuntimeModelCreationContext creationContext) throws HibernateException {
		return new MappedSuperclassImpl<>();
	}

	/**
	 * Create a collection persister instance.
	 * <p/>
	 * A persister will not be completely usable after return from this method.  The returned
	 * reference is good for linking references together, etc.  The persister will be fully
	 * initialized later via its {@link EntityTypeImplementor#afterInitialize} method during {@link #finishUp}
	 *
	 * @param collectionBinding The mapping information describing the collection
	 * @param cacheAccessStrategy The cache access strategy for the collection region
	 * @param creationContext Access to additional information needed to create an EntityPersister
	 *
	 * @return An appropriate collection persister instance.
	 *
	 * @throws HibernateException Indicates a problem building the persister.
	 */
	PersistentCollectionMetadata createCollectionPersister(
			Collection collectionBinding,
			ManagedTypeImplementor source,
			String localName,
			CollectionRegionAccessStrategy cacheAccessStrategy,
			RuntimeModelCreationContext creationContext) throws HibernateException;

	/**
	 * Create an embeddable persister instance.
	 *
	 * @param embeddedValueMapping The mapping information describing the composition
	 * @param creationContext Access to additional information needed to create a persister
	 *
	 * @return An appropriate collection persister instance.
	 *
	 * @throws HibernateException Indicates a problem building the persister.
	 */
	<J> EmbeddedTypeImplementor<J> createEmbeddablePersister(
			EmbeddedValueMapping embeddedValueMapping,
			EmbeddedContainer source,
			String localName,
			RuntimeModelCreationContext creationContext);

	/**
	 * Called after all entity mapping descriptors have been processed.
	 *
	 * @param creationContext Access to additional information
	 */
	void finishUp(RuntimeModelCreationContext creationContext);
}
