/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.spi;

import javax.persistence.metamodel.MappedSuperclassType;

import org.hibernate.boot.model.domain.MappedSuperclassMapping;
import org.hibernate.metamodel.model.creation.spi.RuntimeModelCreationContext;
import org.hibernate.metamodel.model.creation.spi.RuntimeModelDescriptorFactory;
import org.hibernate.metamodel.model.domain.MappedSuperclassDomainType;

/**
 * Hibernate extension to the JPA {@link MappedSuperclassType} descriptor
 *
 * @author Steve Ebersole
 */
public interface MappedSuperclassTypeDescriptor<T>
		extends IdentifiableTypeDescriptor<T>, MappedSuperclassDomainType<T> {
	// logically this should change to extend ManagedTypeImplementor instead of IdentifiableTypeImplementor
	// this would allow us to continue to support the case of MappedSuperclass as super-type of Embeddable.
	// Hibernate allows this.  JPA does not - MappedSuperclass is only supported as part of an entity hierarchy,
	// which is seen in the fact that JPA's MappedSuperclassType extends its IdentifiableType.  So even if
	// we make the change here, the MappedSuperclassImplementor is still a JPA IdentifiableType through
	// MappedSuperclassType.
	//
	// todo (6.0) - make a decision ^^


	/**
	 * Unless a custom {@link RuntimeModelDescriptorFactory} is used, it is
	 * expected that implementations of MappedSuperclassDescriptor define a
	 * constructor accepting the following arguments:
	 *
	 * 		* {@link MappedSuperclassMapping} is the boot-model description of
	 * 			the mapped-superclass
	 * 		* {@link IdentifiableTypeDescriptor} is the runtime-model descriptor
	 * 			of mapped-superclass's super type
	 * 		* {@link RuntimeModelCreationContext} - access to additional
	 *         information useful while constructing the descriptor.
	 */
	Class[] STANDARD_CONSTRUCTOR_SIG = new Class[] {
			MappedSuperclassMapping.class,
			IdentifiableTypeDescriptor.class,
			RuntimeModelCreationContext.class
	};
}
