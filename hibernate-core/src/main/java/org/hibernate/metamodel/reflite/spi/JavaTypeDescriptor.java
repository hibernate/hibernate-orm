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

import java.util.Collection;

/**
 * Describes information about a Java type.  Might (more concretely) be any of:<ul>
 *     <li>{@link ClassDescriptor}</li>
 *     <li>{@link InterfaceDescriptor}</li>
 *     <li>{@link PrimitiveTypeDescriptor}</li>
 *     <li>{@link PrimitiveWrapperTypeDescriptor}</li>
 *     <li>{@link ArrayDescriptor}</li>
 *     <li>{@link VoidDescriptor}</li>
 * </ul>
 *
 * @author Steve Ebersole
 */
public interface JavaTypeDescriptor {
	/**
	 * Retrieve the type name
	 *
	 * @return The type name
	 */
	public Name getName();

	/**
	 * Access the type's modifiers.  See {@link java.lang.reflect.Modifier}
	 *
	 * @return The modifiers
	 */
	public int getModifiers();

	/**
	 * Get all the fields declared by this type.
	 *
	 * @return All fields declared by this type
	 */
	public Collection<FieldDescriptor> getDeclaredFields();

	/**
	 * Get all the methods declared by this type.
	 *
	 * @return All fields declared by this type
	 */
	public Collection<MethodDescriptor> getDeclaredMethods();
}
