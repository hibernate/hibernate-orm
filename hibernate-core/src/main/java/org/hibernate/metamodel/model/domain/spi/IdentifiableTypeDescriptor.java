/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.spi;

import javax.persistence.metamodel.IdentifiableType;

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
}
