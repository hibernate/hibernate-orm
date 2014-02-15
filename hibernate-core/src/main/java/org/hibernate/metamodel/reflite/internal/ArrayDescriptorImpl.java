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

import org.hibernate.metamodel.reflite.spi.ArrayDescriptor;
import org.hibernate.metamodel.reflite.spi.FieldDescriptor;
import org.hibernate.metamodel.reflite.spi.JavaTypeDescriptor;
import org.hibernate.metamodel.reflite.spi.MethodDescriptor;
import org.hibernate.metamodel.reflite.spi.Name;

/**
 * @author Steve Ebersole
 */
public class ArrayDescriptorImpl implements ArrayDescriptor {
	private final Name name;
	private final int modifiers;
	private JavaTypeDescriptor componentType;

	public ArrayDescriptorImpl(Name name, int modifiers, JavaTypeDescriptor componentType) {
		this.name = name;
		this.modifiers = modifiers;
		this.componentType = componentType;
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
	public Collection<FieldDescriptor> getDeclaredFields() {
		return Collections.emptyList();
	}

	@Override
	public Collection<MethodDescriptor> getDeclaredMethods() {
		return Collections.emptyList();
	}

	@Override
	public JavaTypeDescriptor getComponentType() {
		return componentType;
	}
}
