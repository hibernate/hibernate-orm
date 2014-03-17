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
import java.util.List;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;

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
	public DotName getName();

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

	/**
	 * Get the resolved type descriptors for any parameterized type arguments defined on the type.
	 * <p/>
	 * For example, given a JavaTypeDescriptor describing a parameterized Java type resolved to
	 * be {@code List<Person>}, this method would return JavaTypeDescriptor for {@code Person}
	 *
	 * @return The resolved parameter type descriptors
	 */
	public List<JavaTypeDescriptor> getResolvedParameterTypes();

	/**
	 * Get the Jandex ClassInfo object for this type.
	 *
	 * @return The annotations.
	 */
	public ClassInfo getJandexClassInfo();

	/**
	 * Get the annotation of the named type, if one, defined on this type
	 * or any of its super-types
	 *
	 * @param annotationType DotName of the type of annotation to find.
	 *
	 * @return The annotation.
	 */
	public AnnotationInstance findTypeAnnotation(DotName annotationType);

	/**
	 * Get the annotation of the named type, if one, defined on this type.
	 *
	 * @param annotationType DotName of the type of annotation to find.
	 *
	 * @return The annotation.
	 */
	public AnnotationInstance findLocalTypeAnnotation(DotName annotationType);

	/**
	 * Locate all annotations of the given type within the bounds of this type.
	 * "Within bounds" means on the type itself, on its fields and on its
	 * methods.
	 * <p/>
	 * This form (as opposed to {@link #findLocalAnnotations}) also checks super
	 * types.
	 *
	 * @param annotationType DotName of the type of annotation to find.
	 *
	 * @return The annotations found.
	 */
	public Collection<AnnotationInstance> findAnnotations(DotName annotationType);

	/**
	 * Locate all annotations of the given type within the bounds of this type.
	 * "Within bounds" means on the type itself, on its fields and on its
	 * methods.
	 *
	 * @param annotationType DotName of the type of annotation to find.
	 *
	 * @return The annotations found.
	 */
	public Collection<AnnotationInstance> findLocalAnnotations(DotName annotationType);

	/**
	 * Same function as the {@link Class#isAssignableFrom(Class)} method.
	 *
	 * @param check The type to be checked
	 *
	 * @return the {@code boolean} value indicating whether objects of the
	 * type {@code cls} can be assigned to references of this type
	 */
	public boolean isAssignableFrom(JavaTypeDescriptor check);
}
