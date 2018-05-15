/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.spi;

import javax.persistence.metamodel.IdentifiableType;

import org.hibernate.metamodel.model.domain.IdentifiableDomainType;

/**
 * Hibernate extension SPI for working with {@link IdentifiableType} implementations, which includes
 * both mapped-superclasses {@link MappedSuperclassTypeDescriptor}
 * and {@link EntityTypeDescriptor}
 *
 * @author Steve Ebersole
 */
public interface IdentifiableTypeDescriptor<T> extends InheritanceCapable<T>, IdentifiableDomainType<T> {
	@Override
	default IdentifiableTypeDescriptor<? super T> getSupertype() {
		return getSuperclassType();
	}

	@Override
	IdentifiableTypeDescriptor<? super T> getSuperclassType();

	@Override
	SimpleTypeDescriptor<?> getIdType();

	EntityHierarchy getHierarchy();

	interface InFlightAccess<X> extends ManagedTypeDescriptor.InFlightAccess<X> {
	}

	@Override
	InFlightAccess<T> getInFlightAccess();
}
