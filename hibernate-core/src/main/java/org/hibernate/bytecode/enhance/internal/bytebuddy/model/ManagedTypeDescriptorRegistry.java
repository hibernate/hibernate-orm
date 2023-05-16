/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.bytecode.enhance.internal.bytebuddy.model;

import org.hibernate.HibernateException;

/**
 * @author Steve Ebersole
 */
public interface ManagedTypeDescriptorRegistry {
	ManagedTypeDescriptor resolveDescriptor(String name);

	ManagedTypeDescriptor findDescriptor(String name);

	default ManagedTypeDescriptor getDescriptor(String name) {
		final ManagedTypeDescriptor descriptor = findDescriptor( name );
		if ( descriptor == null ) {
			throw new HibernateException( "Could not locate ManagedTypeDescriptor for " + name );
		}
		return descriptor;
	}

	default ManagedTypeDescriptor getDescriptor(ClassDetails classDetails) {
		return getDescriptor( classDetails.getClassName() );
	}
}
