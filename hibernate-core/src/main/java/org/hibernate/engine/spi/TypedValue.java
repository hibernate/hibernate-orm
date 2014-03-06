/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008-2011, Red Hat Inc. or third-party contributors as
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
package org.hibernate.engine.spi;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;

import org.hibernate.EntityMode;
import org.hibernate.internal.util.ValueHolder;
import org.hibernate.type.Type;

/**
 * An ordered pair of a value and its Hibernate type.
 * 
 * @see org.hibernate.type.Type
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
		initTransients();
	}

	/**
	 * @deprecated explicit entity mode support is deprecated
	 */
	@Deprecated
	public TypedValue(Type type, Object value, EntityMode entityMode) {
		this(type, value);
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
	public boolean equals(Object other) {
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

	private void readObject(ObjectInputStream ois)
			throws ClassNotFoundException, IOException {
		ois.defaultReadObject();
		initTransients();
	}

	private void initTransients() {
		this.hashcode = new ValueHolder<Integer>( new ValueHolder.DeferredInitializer<Integer>() {
			@Override
			public Integer initialize() {
				return value == null ? 0 : type.getHashCode( value );
			}
		} );
	}
}
