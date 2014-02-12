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

import org.hibernate.metamodel.reflite.spi.FieldDescriptor;
import org.hibernate.metamodel.reflite.spi.MethodDescriptor;
import org.hibernate.metamodel.reflite.spi.Name;
import org.hibernate.metamodel.reflite.spi.TypeDescriptor;

/**
 * Implementation of a type descriptor
 *
 * @author Steve Ebersole
 */
public class TypeDescriptorImpl implements TypeDescriptor {
	private final Name name;

	private final boolean isInterface;
	private final boolean hasDefaultConstructor;

	private TypeDescriptor superType;
	private TypeDescriptor[] interfaces;

	private FieldDescriptor[] fieldDescriptors;
	private MethodDescriptor[] methodDescriptors;

	public TypeDescriptorImpl(Name name, boolean isInterface, boolean hasDefaultConstructor) {
		this.name = name;
		this.isInterface = isInterface;
		this.hasDefaultConstructor = hasDefaultConstructor;
	}

	@Override
	public Name getName() {
		return name;
	}

	@Override
	public boolean isInterface() {
		return isInterface;
	}

	@Override
	public boolean hasDefaultConstructor() {
		return hasDefaultConstructor;
	}

	@Override
	public TypeDescriptor getSuperType() {
		return superType;
	}

	@Override
	public TypeDescriptor[] getInterfaceTypes() {
		return interfaces;
	}

	@Override
	public FieldDescriptor[] getDeclaredFields() {
		return fieldDescriptors;
	}

	@Override
	public MethodDescriptor[] getDeclaredMethods() {
		return methodDescriptors;
	}

	void setSuperType(TypeDescriptor superType) {
		this.superType = superType;
	}

	void setInterfaces(TypeDescriptor[] interfaces) {
		this.interfaces = interfaces;
	}

	void setFields(FieldDescriptor[] fieldDescriptors) {
		this.fieldDescriptors = fieldDescriptors;
	}

	void setMethods(MethodDescriptor[] methodDescriptors) {
		this.methodDescriptors = methodDescriptors;
	}
}
