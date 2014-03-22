/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2014, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.metamodel.reflite.spi;

import org.jboss.jandex.DotName;

/**
 * The repository of reflite JavaTypeDescriptor info.
 *
 * @author Steve Ebersole
 */
public interface JavaTypeDescriptorRepository {
	/**
	 * Builds a type-safe wrapper around a type/package name
	 *
	 * @param name The type/package name to wrap
	 *
	 * @return The type-safe wrapper.
	 */
	public DotName buildName(String name);

	/**
	 * Obtain the reflite JavaTypeDescriptor for the given name
	 *
	 * @param typeName The name of the type for which to obtain the descriptor
	 *
	 * @return The descriptor
	 */
	public JavaTypeDescriptor getType(DotName typeName);

	/**
	 * Makes a dynamic (map model) type descriptor
	 *
	 * @param typeName The type name
	 * @param superType The type's super-type; can be {@code null}
	 *
	 * @return The generated dynamic type (or the existing one)
	 */
	public DynamicTypeDescriptor makeDynamicType(DotName typeName, DynamicTypeDescriptor superType);

	/**
	 * Create a reflite JavaTypeDescriptor representing an array of the
	 * given type
	 *
	 * @param componentType The array component (element) type
	 *
	 * @return The array descriptor
	 */
	public ArrayDescriptor arrayType(JavaTypeDescriptor componentType);

	/**
	 * Convenient access to the descriptor for the JDK {@link java.util.Collection} type
	 *
	 * @return The descriptor for the JDK {@link java.util.Collection} type
	 */
	public InterfaceDescriptor jdkCollectionDescriptor();

	/**
	 * Convenient access to the descriptor for the JDK {@link java.util.List} type
	 *
	 * @return The descriptor for the JDK {@link java.util.List} type
	 */
	public InterfaceDescriptor jdkListDescriptor();

	/**
	 * Convenient access to the descriptor for the JDK {@link java.util.Set} type
	 *
	 * @return The descriptor for the JDK {@link java.util.Set} type
	 */
	public InterfaceDescriptor jdkSetDescriptor();

	/**
	 * Convenient access to the descriptor for the JDK {@link java.util.Map} type
	 *
	 * @return The descriptor for the JDK {@link java.util.Map} type
	 */
	public InterfaceDescriptor jdkMapDescriptor();
}
