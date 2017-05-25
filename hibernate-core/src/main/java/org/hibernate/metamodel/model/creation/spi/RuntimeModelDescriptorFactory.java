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
import org.hibernate.mapping.Collection;
import org.hibernate.metamodel.model.domain.spi.PersistentCollectionDescriptor;
import org.hibernate.metamodel.model.domain.internal.MappedSuperclassImpl;
import org.hibernate.metamodel.model.domain.spi.ManagedTypeDescriptor;
import org.hibernate.metamodel.model.domain.spi.MappedSuperclassDescriptor;
import org.hibernate.metamodel.model.domain.spi.EmbeddedContainer;
import org.hibernate.metamodel.model.domain.spi.EmbeddedTypeDescriptor;
import org.hibernate.metamodel.model.domain.spi.EntityDescriptor;
import org.hibernate.service.Service;

/**
 * Contract for creating persister instances including {@link EntityDescriptor},
 * {@link PersistentCollectionDescriptor} and {@link EmbeddedTypeDescriptor}
 *
 * @author Steve Ebersole
 */
public interface RuntimeModelDescriptorFactory extends Service {
	/**
	 * Create an entity persister instance.
	 * <p/>
	 * A persister will not be completely usable after return from this method.  The returned
	 * reference is good for linking references together, etc.  The persister will be fully
	 * initialized later via its {@link EntityDescriptor#afterInitialize} method during {@link #finishUp}
	 *
	 * @param bootMapping The mapping information describing the entity
	 * @param creationContext Access to additional information needed to create an EntityPersister
	 *
	 * @return An appropriate entity persister instance.
	 *
	 * @throws HibernateException Indicates a problem building the persister.
	 */
	<J> EntityDescriptor<J> createEntityDescriptor(
			EntityMapping bootMapping,
			RuntimeModelCreationContext creationContext) throws HibernateException;

	default <J> MappedSuperclassDescriptor<J> createMappedSuperclassDescriptor(
			MappedSuperclassMapping bootMapping,
			RuntimeModelCreationContext creationContext) throws HibernateException {
		return new MappedSuperclassImpl<>();
	}

	/**
	 * Create a collection persister instance.
	 * <p/>
	 * A persister will not be completely usable after return from this method.  The returned
	 * reference is good for linking references together, etc.  The persister will be fully
	 * initialized later via its {@link EntityDescriptor#afterInitialize} method during {@link #finishUp}
	 *
	 * @param collectionBinding The mapping information describing the collection
	 * @param cacheAccessStrategy The cache access strategy for the collection region
	 * @param creationContext Access to additional information needed to create an EntityPersister
	 *
	 * @return An appropriate collection persister instance.
	 *
	 * @throws HibernateException Indicates a problem building the persister.
	 */
	<O,C,E> PersistentCollectionDescriptor<O,C,E> createPersistentCollectionDescriptor(
			Collection collectionBinding,
			ManagedTypeDescriptor<O> source,
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
	<J> EmbeddedTypeDescriptor<J> createEmbeddedTypeDescriptor(
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
