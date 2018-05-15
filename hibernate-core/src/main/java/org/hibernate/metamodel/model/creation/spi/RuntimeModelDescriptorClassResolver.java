/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.creation.spi;

import org.hibernate.boot.model.domain.EmbeddedValueMapping;
import org.hibernate.boot.model.domain.EntityMapping;
import org.hibernate.boot.model.domain.MappedSuperclassMapping;
import org.hibernate.mapping.Collection;
import org.hibernate.metamodel.model.domain.internal.composite.EmbeddedTypeDescriptorImpl;
import org.hibernate.metamodel.model.domain.internal.MappedSuperclassTypeImpl;
import org.hibernate.metamodel.model.domain.spi.EmbeddedTypeDescriptor;
import org.hibernate.metamodel.model.domain.spi.EntityTypeDescriptor;
import org.hibernate.metamodel.model.domain.spi.MappedSuperclassTypeDescriptor;
import org.hibernate.metamodel.model.domain.spi.PersistentCollectionDescriptor;
import org.hibernate.service.Service;

/**
 * Given an entity or collection mapping, resolve the appropriate persister class to use.
 * <p/>
 * The persister class is chosen according to the following rules:<ol>
 *     <li>the persister class defined explicitly via annotation or XML</li>
 *     <li>the persister class returned by the installed {@link RuntimeModelDescriptorClassResolver}</li>
 *     <li>the default provider as chosen by Hibernate Core (best choice most of the time)</li>
 * </ol>
 *
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
 * @author Steve Ebersole
 */
public interface RuntimeModelDescriptorClassResolver extends Service {
	/**
	 * Returns the entity persister class for a given entityName or null
	 * if the entity persister class should be the default.
	 *
	 * @param bootMapping The boot-time entity mapping
	 *
	 * @return The entity persister class to use
	 */
	Class<? extends EntityTypeDescriptor> getEntityDescriptorClass(EntityMapping bootMapping);

	/**
	 * Returns the collection persister class for a given collection role or null
	 * if the collection persister class should be the default.
	 *
	 * @param bootMapping The embedded mapping metadata
	 *
	 * @return The persister class to use
	 *
	 * @since 6.0
	 */
	default Class<? extends MappedSuperclassTypeDescriptor> getMappedSuperclassDescriptorClass(MappedSuperclassMapping bootMapping) {
		return MappedSuperclassTypeImpl.class;
	}

	/**
	 * Returns the collection persister class for a given collection role or null
	 * if the collection persister class should be the default.
	 *
	 * @param bootMapping The collection metadata
	 *
	 * @return The collection persister class to use
	 */
	Class<? extends PersistentCollectionDescriptor> getCollectionDescriptorClass(Collection bootMapping);

	/**
	 * Returns the collection persister class for a given collection role or null
	 * if the collection persister class should be the default.
	 *
	 * @param bootValueMapping The embedded mapping metadata
	 *
	 * @return The persister class to use
	 *
	 * @since 6.0
	 */
	default Class<? extends EmbeddedTypeDescriptor> getEmbeddedTypeDescriptorClass(EmbeddedValueMapping bootValueMapping) {
		return EmbeddedTypeDescriptorImpl.class;
	}
}
