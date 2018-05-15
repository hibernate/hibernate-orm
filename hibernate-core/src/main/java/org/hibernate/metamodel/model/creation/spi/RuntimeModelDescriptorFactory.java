/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.creation.spi;

import org.hibernate.HibernateException;
import org.hibernate.boot.model.domain.EntityMapping;
import org.hibernate.boot.model.domain.MappedSuperclassMapping;
import org.hibernate.boot.model.domain.spi.EmbeddedValueMappingImplementor;
import org.hibernate.mapping.Property;
import org.hibernate.metamodel.model.domain.spi.EmbeddedContainer;
import org.hibernate.metamodel.model.domain.spi.EmbeddedTypeDescriptor;
import org.hibernate.metamodel.model.domain.spi.EntityTypeDescriptor;
import org.hibernate.metamodel.model.domain.spi.IdentifiableTypeDescriptor;
import org.hibernate.metamodel.model.domain.spi.ManagedTypeDescriptor;
import org.hibernate.metamodel.model.domain.spi.MappedSuperclassTypeDescriptor;
import org.hibernate.metamodel.model.domain.spi.PersistentCollectionDescriptor;
import org.hibernate.metamodel.model.domain.spi.SingularPersistentAttribute;
import org.hibernate.service.Service;

/**
 * Contract for creating persister instances including {@link EntityTypeDescriptor},
 * {@link PersistentCollectionDescriptor} and {@link EmbeddedTypeDescriptor}
 *
 * @author Steve Ebersole
 */
public interface RuntimeModelDescriptorFactory extends Service {
	/**
	 * Create an entity persister instance.
	 * <p/>
	 * A descriptor will not be completely usable after return from this method.  The returned
	 * reference is good for linking references together, etc.  The persister will be fully
	 * initialized later via {@link EntityTypeDescriptor#finishInitialization}
	 *
	 * @param bootMapping The mapping information describing the entity
	 * @param superTypeDescriptor
	 * @param creationContext Access to additional information needed to create an EntityPersister
	 *
	 * @return An appropriate entity persister instance.
	 *
	 * @throws HibernateException Indicates a problem building the persister.
	 */
	<J> EntityTypeDescriptor<J> createEntityDescriptor(
			EntityMapping bootMapping,
			IdentifiableTypeDescriptor superTypeDescriptor,
			RuntimeModelCreationContext creationContext) throws HibernateException;

	<J> MappedSuperclassTypeDescriptor<J> createMappedSuperclassDescriptor(
			MappedSuperclassMapping bootMapping,
			IdentifiableTypeDescriptor superTypeDescriptor,
			RuntimeModelCreationContext creationContext) throws HibernateException;

	/**
	 * Create an embeddable persister instance.
	 */
	<J> EmbeddedTypeDescriptor<J> createEmbeddedTypeDescriptor(
			EmbeddedValueMappingImplementor bootValueMapping,
			EmbeddedContainer runtimeModelContainer,
			EmbeddedTypeDescriptor<? super J> superTypeDescriptor,
			String localName,
			SingularPersistentAttribute.Disposition disposition,
			RuntimeModelCreationContext creationContext);

	/**
	 * Create a collection persister instance.
	 * <p/>
	 * A descriptor will not be completely usable after return from this method.  The returned
	 * reference is good for linking references together, etc.  The persister will be fully
	 * initialized later via {@link PersistentCollectionDescriptor#finishInitialization}
	 *
	 * @throws HibernateException Indicates a problem building the persister.
	 */
	<O,C,E> PersistentCollectionDescriptor<O,C,E> createPersistentCollectionDescriptor(
			Property pluralProperty,
			ManagedTypeDescriptor<O> runtimeManagedType,
			RuntimeModelCreationContext creationContext) throws HibernateException;

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
