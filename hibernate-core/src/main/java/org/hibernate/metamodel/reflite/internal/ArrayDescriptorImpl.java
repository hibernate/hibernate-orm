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
package org.hibernate.metamodel.reflite.internal;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.hibernate.metamodel.reflite.spi.ArrayDescriptor;
import org.hibernate.metamodel.reflite.spi.FieldDescriptor;
import org.hibernate.metamodel.reflite.spi.JavaTypeDescriptor;
import org.hibernate.metamodel.reflite.spi.MethodDescriptor;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;

/**
 * @author Steve Ebersole
 */
public class ArrayDescriptorImpl implements ArrayDescriptor {
	private final DotName name;
	private final int modifiers;
	private JavaTypeDescriptor componentType;

	public ArrayDescriptorImpl(DotName name, int modifiers, JavaTypeDescriptor componentType) {
		this.name = name;
		this.modifiers = modifiers;
		this.componentType = componentType;
	}

	@Override
	public DotName getName() {
		return name;
	}

	@Override
	public int getModifiers() {
		return modifiers;
	}

	@Override
	public Collection<FieldDescriptor> getDeclaredFields() {
		return Collections.emptyList();
	}

	@Override
	public Collection<MethodDescriptor> getDeclaredMethods() {
		return Collections.emptyList();
	}

	@Override
	public AnnotationInstance findTypeAnnotation(DotName annotationType) {
		return null;
	}

	@Override
	public AnnotationInstance findLocalTypeAnnotation(DotName annotationType) {
		return null;
	}

	@Override
	public Collection<AnnotationInstance> findAnnotations(DotName annotationType) {
		return Collections.emptyList();
	}

	@Override
	public Collection<AnnotationInstance> findLocalAnnotations(DotName annotationType) {
		return Collections.emptyList();
	}

	@Override
	public boolean isAssignableFrom(JavaTypeDescriptor check) {
		if ( check == null ) {
			throw new IllegalArgumentException( "Descriptor to check cannot be null" );
		}

		if ( equals( check ) ) {
			return true;
		}

		if ( ArrayDescriptor.class.isInstance( check ) ) {
			final ArrayDescriptor other = (ArrayDescriptor) check;
			return getComponentType().isAssignableFrom( other.getComponentType() );
		}

		return false;
	}

	@Override
	public List<JavaTypeDescriptor> getResolvedParameterTypes() {
		return Collections.emptyList();
	}

	@Override
	public ClassInfo getJandexClassInfo() {
		return null;
	}

	@Override
	public JavaTypeDescriptor getComponentType() {
		return componentType;
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}

		final ArrayDescriptorImpl that = (ArrayDescriptorImpl) o;
		return name.equals( that.name );
	}

	@Override
	public int hashCode() {
		return name.hashCode();
	}
}
