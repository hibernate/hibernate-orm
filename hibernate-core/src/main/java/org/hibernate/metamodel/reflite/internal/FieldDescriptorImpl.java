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

import java.util.Collections;
import java.util.Map;

import org.hibernate.metamodel.reflite.spi.FieldDescriptor;
import org.hibernate.metamodel.reflite.spi.JavaTypeDescriptor;
import org.hibernate.metamodel.reflite.spi.ParameterizedType;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.DotName;

/**
 * @author Steve Ebersole
 */
public class FieldDescriptorImpl implements FieldDescriptor {
	private final String name;
	private final ParameterizedType fieldType;

	private final int modifiers;

	private final JavaTypeDescriptor declaringType;
	private final Map<DotName, AnnotationInstance> annotationMap;

	public FieldDescriptorImpl(
			String name,
			ParameterizedType fieldType,
			int modifiers,
			JavaTypeDescriptor declaringType,
			Map<DotName, AnnotationInstance> annotationMap) {
		this.name = name;
		this.fieldType = fieldType;
		this.modifiers = modifiers;
		this.declaringType = declaringType;
		this.annotationMap = annotationMap != null
				? annotationMap
				: Collections.<DotName, AnnotationInstance>emptyMap();
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public ParameterizedType getType() {
		return fieldType;
	}

	@Override
	public int getModifiers() {
		return modifiers;
	}

	@Override
	public JavaTypeDescriptor getDeclaringType() {
		return declaringType;
	}

	@Override
	public Map<DotName, AnnotationInstance> getAnnotations() {
		return annotationMap;
	}

	@Override
	public String toLoggableForm() {
		return declaringType.getName().toString() + '#' + name;
	}

	@Override
	public String toSignatureForm() {
		return fieldType.getErasedType().getName().toString() + ' ' + name;
	}

	@Override
	public String toString() {
		return "FieldDescriptorImpl{" + declaringType.getName().toString() + '#' + name + '}';
	}
}
