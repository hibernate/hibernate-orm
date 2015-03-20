/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2015, Red Hat Inc. or third-party contributors as
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
package org.hibernate.type.descriptor.java;

import org.hibernate.type.descriptor.WrapperOptions;

/**
 * Describes a Java Enum type.
 *
 * @author Steve Ebersole
 */
public class EnumJavaTypeDescriptor<T extends Enum> extends AbstractTypeDescriptor<T> {
	@SuppressWarnings("unchecked")
	protected EnumJavaTypeDescriptor(Class<T> type) {
		super( type, ImmutableMutabilityPlan.INSTANCE );
	}

	@Override
	public String toString(T value) {
		return value == null ? "<null>" : value.name();
	}

	@Override
	public T fromString(String string) {
		return string == null ? null : (T) Enum.valueOf( getJavaTypeClass(), string );
	}

	@Override
	@SuppressWarnings("unchecked")
	public <X> X unwrap(T value, Class<X> type, WrapperOptions options) {
		return (X) value;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <X> T wrap(X value, WrapperOptions options) {
		return (T) value;
	}
}
