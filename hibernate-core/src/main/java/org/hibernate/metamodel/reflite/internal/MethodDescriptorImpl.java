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

import org.hibernate.metamodel.reflite.spi.JavaTypeDescriptor;
import org.hibernate.metamodel.reflite.spi.MethodDescriptor;

/**
 * @author Steve Ebersole
 */
public class MethodDescriptorImpl implements MethodDescriptor {
	private final String name;
	private final JavaTypeDescriptor declaringType;
	private final int modifiers;
	private final JavaTypeDescriptor returnType;
	private final Collection<JavaTypeDescriptor> parameterTypes;

	public MethodDescriptorImpl(
			String name,
			JavaTypeDescriptor declaringType,
			int modifiers,
			JavaTypeDescriptor returnType,
			Collection<JavaTypeDescriptor> parameterTypes) {
		this.name = name;
		this.declaringType = declaringType;
		this.modifiers = modifiers;
		this.returnType = returnType;
		this.parameterTypes = parameterTypes;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public JavaTypeDescriptor getDeclaringType() {
		return declaringType;
	}

	@Override
	public int getModifiers() {
		return modifiers;
	}

	@Override
	public JavaTypeDescriptor getReturnType() {
		return returnType;
	}

	@Override
	public Collection<JavaTypeDescriptor> getParameterTypes() {
		return parameterTypes;
	}

	@Override
	public String toString() {
		return "MethodDescriptorImpl{" + declaringType.getName().toString() + '#' + name + '}';
	}
}
