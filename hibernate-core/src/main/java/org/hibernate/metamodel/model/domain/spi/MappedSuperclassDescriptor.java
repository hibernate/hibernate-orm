/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.spi;

import javax.persistence.metamodel.MappedSuperclassType;

/**
 * @author Steve Ebersole
 */
public interface MappedSuperclassDescriptor<T>
		extends IdentifiableTypeDescriptor<T>, MappedSuperclassType<T> {
	// logically this should change to extend ManagedTypeImplementor instead of IdentifiableTypeImplementor
	// this would allow us to continue to support the case of MappedSuperclass as super-type of Embeddable.
	// Hibernate allows this.  JPA does not - MappedSuperclass is only supported as part of an entity hierarchy,
	// which is seen in the fact that JPA's MappedSuperclassType extends its IdentifiableType.  So even if
	// we make the change here, the MappedSuperclassImplementor is still a JPA IdentifiableType through
	// MappedSuperclassType.
	//
	// todo (6.0) - make a decision ^^
}
