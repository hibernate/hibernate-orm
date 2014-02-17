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
import java.util.Map;

import org.hibernate.metamodel.reflite.spi.FieldDescriptor;
import org.hibernate.metamodel.reflite.spi.InterfaceDescriptor;
import org.hibernate.metamodel.reflite.spi.MethodDescriptor;
import org.hibernate.metamodel.reflite.spi.Name;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.DotName;

/**
 * @author Steve Ebersole
 */
public class InterfaceDescriptorImpl implements InterfaceDescriptor {
	private final Name name;
	private final int modifiers;
	private final Map<DotName, AnnotationInstance> annotationMap;

	private Collection<InterfaceDescriptor> extendedInterfaceTypes;

	private Collection<FieldDescriptor> fields;
	private Collection<MethodDescriptor> methods;

	public InterfaceDescriptorImpl(
			Name name,
			int modifiers,
			Map<DotName, AnnotationInstance> annotationMap) {
		this.name = name;
		this.modifiers = modifiers;
		this.annotationMap = annotationMap != null
				? annotationMap
				: Collections.<DotName, AnnotationInstance>emptyMap();
	}

	@Override
	public Name getName() {
		return name;
	}

	@Override
	public int getModifiers() {
		return modifiers;
	}

	@Override
	public Collection<InterfaceDescriptor> getExtendedInterfaceTypes() {
		return extendedInterfaceTypes;
	}

	@Override
	public Map<DotName, AnnotationInstance> getAnnotations() {
		return annotationMap;
	}

	@Override
	public Collection<FieldDescriptor> getDeclaredFields() {
		return fields;
	}

	@Override
	public Collection<MethodDescriptor> getDeclaredMethods() {
		return methods;
	}

	@Override
	public String toString() {
		return "InterfaceDescriptorImpl{" + name.toString() + '}';
	}

	void setExtendedInterfaceTypes(Collection<InterfaceDescriptor> extendedInterfaceTypes) {
		this.extendedInterfaceTypes = extendedInterfaceTypes;
	}

	void setFields(Collection<FieldDescriptor> fields) {
		this.fields = fields;
	}

	void setMethods(Collection<MethodDescriptor> methods) {
		this.methods = methods;
	}
}
