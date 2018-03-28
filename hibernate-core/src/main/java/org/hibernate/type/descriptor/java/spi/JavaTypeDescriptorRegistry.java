/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.descriptor.java.spi;

import java.io.Serializable;

import org.hibernate.type.descriptor.java.JavaTypeDescriptor;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * Basically a map from {@link Class} -> {@link JavaTypeDescriptor}
 *
 * @author Steve Ebersole
 * @author Andrea Boriero
 *
 * @since 5.3
 */
public class JavaTypeDescriptorRegistry
		extends org.hibernate.type.descriptor.java.JavaTypeDescriptorRegistry
		implements Serializable {

	private final TypeConfiguration typeConfiguration;
	private final org.hibernate.type.descriptor.java.JavaTypeDescriptorRegistry javaTypeDescriptorRegistry;

	public JavaTypeDescriptorRegistry(TypeConfiguration typeConfiguration) {
		this.typeConfiguration = typeConfiguration;
		javaTypeDescriptorRegistry = org.hibernate.type.descriptor.java.JavaTypeDescriptorRegistry.INSTANCE;
	}

	@Override
	public <T> JavaTypeDescriptor<T> getDescriptor(Class<T> javaType) {
		return javaTypeDescriptorRegistry.getDescriptor( javaType );
	}

	@Override
	public void addDescriptor(JavaTypeDescriptor descriptor) {
		javaTypeDescriptorRegistry.addDescriptor( descriptor );
	}
}
