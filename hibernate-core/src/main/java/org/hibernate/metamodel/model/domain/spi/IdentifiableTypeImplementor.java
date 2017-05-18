/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.spi;

import javax.persistence.metamodel.IdentifiableType;

import org.hibernate.EntityMode;
import org.hibernate.boot.model.domain.IdentifiableTypeMapping;
import org.hibernate.boot.model.domain.spi.IdentifiableTypeMappingImplementor;
import org.hibernate.metamodel.model.creation.spi.RuntimeModelCreationContext;
import org.hibernate.tuple.entity.EntityTuplizer;

/**
 * Hibernate extension SPI for working with {@link IdentifiableType} implementations, which includes
 * both mapped-superclasses {@link MappedSuperclassImplementor}
 * and {@link EntityTypeImplementor}
 *
 * @author Steve Ebersole
 */
public interface IdentifiableTypeImplementor<T>
		extends InheritanceCapable<T>, IdentifiableType<T> {
	@Override
	default IdentifiableTypeImplementor<? super T> getSupertype() {
		return getSuperclassType();
	}

	@Override
	IdentifiableTypeImplementor<? super T> getSuperclassType();

	EntityHierarchy getHierarchy();

	@Override
	default EntityMode getEntityMode() {
		return getHierarchy().getRepresentationMode();
	}

	@Override
	EntityTuplizer getTuplizer();

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
			IdentifiableTypeImplementor<? super T> superType,
			IdentifiableTypeMapping bootMapping,
			RuntimeModelCreationContext creationContext);

	void completeInitialization(
			EntityHierarchy entityHierarchy,
			IdentifiableTypeImplementor<? super T> superType,
			IdentifiableTypeMappingImplementor bootMapping,
			RuntimeModelCreationContext creationContext);

}
