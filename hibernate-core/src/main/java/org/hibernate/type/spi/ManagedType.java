/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.type.spi;

import org.hibernate.type.descriptor.java.spi.ManagedJavaDescriptor;

/**
 * @author Steve Ebersole
 */
public interface ManagedType<J> extends Type<J> {
	ManagedType<? super J> getSuperType();

	@Override
	ManagedJavaDescriptor<J> getJavaTypeDescriptor();
}
