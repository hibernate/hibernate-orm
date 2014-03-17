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

import java.util.List;

import org.hibernate.metamodel.reflite.spi.JavaTypeDescriptor;
import org.hibernate.metamodel.reflite.spi.ParameterizedType;

/**
 * Implementation of the ParameterizedType contract
 *
 * @author Steve Ebersole
 */
public class ParameterizedTypeImpl implements ParameterizedType {
	private final JavaTypeDescriptor erasedType;
	private final List<JavaTypeDescriptor> resolvedParameterTypes;

	public ParameterizedTypeImpl(
			JavaTypeDescriptor erasedType,
			List<JavaTypeDescriptor> resolvedParameterTypes) {
		this.erasedType = erasedType;
		this.resolvedParameterTypes = resolvedParameterTypes;
	}

	@Override
	public JavaTypeDescriptor getErasedType() {
		return erasedType;
	}

	@Override
	public List<JavaTypeDescriptor> getResolvedParameterTypes() {
		return resolvedParameterTypes;
	}
}
