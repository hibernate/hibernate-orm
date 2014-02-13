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
import org.hibernate.metamodel.reflite.spi.TypeDescriptor;

/**
 * @author Steve Ebersole
 */
public class FieldDescriptorImpl implements FieldDescriptor {
	private final String name;
	private final TypeDescriptor fieldType;

	private final int modifiers;

	private final TypeDescriptor declaringType;

	public FieldDescriptorImpl(String name, TypeDescriptor fieldType, int modifiers, TypeDescriptor declaringType) {
		this.name = name;
		this.fieldType = fieldType;
		this.modifiers = modifiers;
		this.declaringType = declaringType;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public TypeDescriptor getType() {
		return fieldType;
	}

	@Override
	public int getModifiers() {
		return modifiers;
	}

	@Override
	public TypeDescriptor getDeclaringType() {
		return declaringType;
	}

	@Override
	public String toString() {
		return "FieldDescriptorImpl{" + declaringType.getName().toString() + '#' + name + '}';
	}
}
