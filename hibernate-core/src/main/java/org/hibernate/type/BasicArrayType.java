/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type;

import java.util.Objects;

import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.JdbcTypeIndicators;

/**
 * A type that maps between {@link java.sql.Types#ARRAY ARRAY} and {@code T[]}
 *
 * @author Jordan Gigov
 * @author Christian Beikov
 */
public class BasicArrayType<T,E>
		extends AbstractSingleColumnStandardBasicType<T>
		implements AdjustableBasicType<T>, BasicPluralType<T, E> {

	private final BasicType<E> baseDescriptor;
	private final String name;

	public BasicArrayType(BasicType<E> baseDescriptor, JdbcType arrayJdbcType, JavaType<T> arrayTypeDescriptor) {
		super( arrayJdbcType, arrayTypeDescriptor );
		this.baseDescriptor = baseDescriptor;
		this.name = baseDescriptor.getName() + "[]";
	}

	@Override
	public BasicType<E> getElementType() {
		return baseDescriptor;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	protected boolean registerUnderJavaType() {
		return true;
	}

	@Override
	public <X> BasicType<X> resolveIndicatedType(JdbcTypeIndicators indicators, JavaType<X> domainJtd) {
		// TODO: maybe fallback to some encoding by default if the DB doesn't support arrays natively?
		//  also, maybe move that logic into the ArrayJdbcType
		//noinspection unchecked
		return (BasicType<X>) this;
	}

	@Override
	public boolean equals(Object o) {
		return o == this || o.getClass() == BasicArrayType.class
				&& Objects.equals( baseDescriptor, ( (BasicArrayType<?, ?>) o ).baseDescriptor );
	}

	@Override
	public int hashCode() {
		return baseDescriptor.hashCode();
	}
}
