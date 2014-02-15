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

import org.hibernate.metamodel.reflite.spi.FieldDescriptor;
import org.hibernate.metamodel.reflite.spi.InterfaceDescriptor;
import org.hibernate.metamodel.reflite.spi.MethodDescriptor;
import org.hibernate.metamodel.reflite.spi.Name;

/**
 * @author Steve Ebersole
 */
public class InterfaceDescriptorImpl implements InterfaceDescriptor {
	private final Name name;
	private final int modifiers;

	private Collection<InterfaceDescriptor> extendedInterfaceTypes;

	private Collection<FieldDescriptor> declaredFields;
	private Collection<MethodDescriptor> declaredMethods;

	public InterfaceDescriptorImpl(Name name, int modifiers) {
		this.name = name;
		this.modifiers = modifiers;
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
	public Collection<FieldDescriptor> getDeclaredFields() {
		return declaredFields;
	}

	@Override
	public Collection<MethodDescriptor> getDeclaredMethods() {
		return declaredMethods;
	}

	@Override
	public String toString() {
		return "InterfaceDescriptorImpl{" + name.toString() + '}';
	}

	void setExtendedInterfaceTypes(Collection<InterfaceDescriptor> extendedInterfaceTypes) {
		this.extendedInterfaceTypes = extendedInterfaceTypes;
	}

	void setDeclaredFields(Collection<FieldDescriptor> declaredFields) {
		this.declaredFields = declaredFields;
	}

	void setDeclaredMethods(Collection<MethodDescriptor> declaredMethods) {
		this.declaredMethods = declaredMethods;
	}
}
