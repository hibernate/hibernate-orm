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
import org.hibernate.boot.model.domain.spi.EmbeddedValueMappingImplementor;
import org.hibernate.cache.spi.access.CollectionDataAccess;
import org.hibernate.mapping.Collection;
import org.hibernate.metamodel.model.domain.spi.IdentifiableTypeDescriptor;
import org.hibernate.metamodel.model.domain.spi.PersistentCollectionDescriptor;
import org.hibernate.metamodel.model.domain.spi.ManagedTypeDescriptor;
import org.hibernate.metamodel.model.domain.spi.MappedSuperclassDescriptor;
import org.hibernate.metamodel.model.domain.spi.EmbeddedContainer;
import org.hibernate.metamodel.model.domain.spi.EmbeddedTypeDescriptor;
import org.hibernate.metamodel.model.domain.spi.EntityDescriptor;
import org.hibernate.metamodel.model.domain.spi.SingularPersistentAttribute;
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
	 * @param superTypeDescriptor
	 * @param creationContext Access to additional information needed to create an EntityPersister
	 *
	 * @return An appropriate entity persister instance.
	 *
	 * @throws HibernateException Indicates a problem building the persister.
	 */
	<J> EntityDescriptor<J> createEntityDescriptor(
			EntityMapping bootMapping,
			IdentifiableTypeDescriptor superTypeDescriptor,
			RuntimeModelCreationContext creationContext) throws HibernateException;

	<J> MappedSuperclassDescriptor<J> createMappedSuperclassDescriptor(
			MappedSuperclassMapping bootMapping,
			IdentifiableTypeDescriptor superTypeDescriptor,
			RuntimeModelCreationContext creationContext) throws HibernateException;

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
			RuntimeModelCreationContext creationContext) throws HibernateException;

	/**
	 * Create an embeddable persister instance.
	 *
	 * @param bootModelEmbeddedDescriptor The boot-model representation of the composition
	 * @param runtimeModelContainer The runtime-model container for the embedded to tbe created
	 * @param localName The composite name
	 * @param singularAttributeDisposition The disposition to use for any singular attributes
	 * 		that are part of this composition
	 * @param creationContext Access to additional information needed to create a persister
	 *
	 * @return An appropriate collection persister instance.
	 *
	 * @throws HibernateException Indicates a problem building the persister.
	 */
	<J> EmbeddedTypeDescriptor<J> createEmbeddedTypeDescriptor(
			EmbeddedValueMappingImplementor bootValueMapping,
			EmbeddedContainer runtimeModelContainer,
			EmbeddedTypeDescriptor<? super J> superTypeDescriptor,
			String localName,
			SingularPersistentAttribute.Disposition singularAttributeDisposition,
			RuntimeModelCreationContext creationContext);

	/**
	 * Called after all entity mapping descriptors have been processed.
	 *
	 * @apiNote Intended for custom implementors needing to perform some
	 * logic after all runtime descriptors are built
	 *
	 * @param creationContext Access to additional information
	 */
	default void finishUp(RuntimeModelCreationContext creationContext) {
		// by default, impls would have nothing to do.
	}
}
