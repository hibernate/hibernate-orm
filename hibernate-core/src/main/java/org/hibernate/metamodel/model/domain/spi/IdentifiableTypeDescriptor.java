/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.spi;

import javax.persistence.metamodel.IdentifiableType;

import org.hibernate.boot.model.domain.IdentifiableTypeMapping;
import org.hibernate.boot.model.domain.spi.IdentifiableTypeMappingImplementor;
import org.hibernate.metamodel.model.creation.spi.RuntimeModelCreationContext;

/**
 * Hibernate extension SPI for working with {@link IdentifiableType} implementations, which includes
 * both mapped-superclasses {@link MappedSuperclassDescriptor}
 * and {@link EntityDescriptor}
 *
 * @author Steve Ebersole
 */
public interface IdentifiableTypeDescriptor<T> extends InheritanceCapable<T>, IdentifiableType<T> {
	@Override
	default IdentifiableTypeDescriptor<? super T> getSupertype() {
		return getSuperclassType();
	}

	@Override
	IdentifiableTypeDescriptor<? super T> getSuperclassType();

	EntityHierarchy getHierarchy();

	/**
	 * Called after all EntityPersister instance have been created and (at least partially) initialized.
	 *
	 * @param entityHierarchy The entity hierarchy descriptor for the entity tree this
	 * 		identifiable belongs to
	 * @param superType The entity's super's EntityPersister
	 * @param bootMapping The mapping object from Hibernate's boot-time model.  Should be
	 * 		the same mapping instance originally passed to the ctor, but we want to not
	 * 		have to store that around as instance state - so we pass it in here again
	 * @param creationContext Access to the database model
	 */
	void finishInstantiation(
			EntityHierarchy entityHierarchy,
			IdentifiableTypeDescriptor<? super T> superType,
			IdentifiableTypeMapping bootMapping,
			RuntimeModelCreationContext creationContext);

	void completeInitialization(
			EntityHierarchy entityHierarchy,
			IdentifiableTypeDescriptor<? super T> superType,
			IdentifiableTypeMappingImplementor bootMapping,
			RuntimeModelCreationContext creationContext);

}
