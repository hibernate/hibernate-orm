/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.spi;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;

import org.hibernate.internal.util.ValueHolder;
import org.hibernate.type.Type;
import org.hibernate.type.descriptor.java.spi.JavaTypeDescriptor;

/**
 * An ordered pair of a value and its Hibernate type.
 *
 * @see Type
 * @author Gavin King
 */
public final class TypedValue implements Serializable {
	private final JavaTypeDescriptor javaTypeDescriptor;
	private final Object value;
	// "transient" is important here -- NaturalIdCacheKey needs to be Serializable
	private transient ValueHolder<Integer> hashcode;

	public TypedValue(final JavaTypeDescriptor javaTypeDescriptor, final Object value) {
		this.javaTypeDescriptor = javaTypeDescriptor;
		this.value = value;
		initTransients();
	}

	public Object getValue() {
		return value;
	}

	public JavaTypeDescriptor getJavaTypeDescriptor() {
		 return javaTypeDescriptor;
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
	@SuppressWarnings("unchecked")
	public boolean equals(Object other) {
		if ( this == other ) {
			return true;
		}
		if ( other == null || getClass() != other.getClass() ) {
			return false;
		}
		final TypedValue that = (TypedValue) other;
		return javaTypeDescriptor.getJavaType() == that.javaTypeDescriptor.getJavaType()
				&& javaTypeDescriptor.areEqual( that.value, value );
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
				return value == null ? 0 : javaTypeDescriptor.extractHashCode( value );
			}
		} );
	}
}
