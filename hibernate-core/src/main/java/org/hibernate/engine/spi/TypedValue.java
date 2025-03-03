/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.spi;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serial;
import java.io.Serializable;

import org.hibernate.internal.util.ValueHolder;
import org.hibernate.type.Type;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * An ordered pair of a value and its Hibernate type.
 *
 * @see Type
 * @author Gavin King
 */
public final class TypedValue implements Serializable {
	private final Type type;
	private final Object value;
	// "transient" is important here -- NaturalIdCacheKey needs to be Serializable
	private transient ValueHolder<Integer> hashcode;

	public TypedValue(final Type type, final Object value) {
		this.type = type;
		this.value = value;
		this.hashcode = hashCode(type, value);
	}

	public Object getValue() {
		return value;
	}

	public Type getType() {
		return type;
	}
	@Override
	public String toString() {
		return value==null ? "null" : value.toString();
	}
	@Override
	public int hashCode() {
		return hashcode.getValue();
	}
	@Override
	public boolean equals(@Nullable Object other) {
		if ( this == other ) {
			return true;
		}
		if ( other == null || getClass() != other.getClass() ) {
			return false;
		}
		final TypedValue that = (TypedValue) other;
		return type.getReturnedClass() == that.type.getReturnedClass()
			&& type.isEqual( that.value, value );
	}

	@Serial
	private void readObject(ObjectInputStream ois)
			throws ClassNotFoundException, IOException {
		ois.defaultReadObject();
		this.hashcode = hashCode(type, value);
	}

	private static ValueHolder<Integer> hashCode(Type type, Object value) {
		return new ValueHolder<>( () -> value == null ? 0 : type.getHashCode( value ) );
	}
}
