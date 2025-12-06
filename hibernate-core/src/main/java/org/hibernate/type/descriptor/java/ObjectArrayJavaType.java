/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type.descriptor.java;

import org.hibernate.type.descriptor.WrapperOptions;

/**
 * Descriptor for {@code Object[]} handling, usually used for tuples.
 *
 * @author Christian Beikov
 */
public class ObjectArrayJavaType extends AbstractClassJavaType<Object[]> {

	private final JavaType[] components;

	public ObjectArrayJavaType(JavaType[] components) {
		super(
				Object[].class,
				ImmutableMutabilityPlan.instance(),
				new ComponentArrayComparator( components )
		);
		this.components = components;
	}

	@Override
	public boolean isInstance(Object value) {
		return value instanceof Object[];
	}

	@Override
	public Object[] cast(Object value) {
		return (Object[]) value;
	}

	@Override
	public String toString(Object[] value) {
		final StringBuilder sb = new StringBuilder();
		sb.append( '(' );
		append( sb, components, value, 0 );
		for ( int i = 1; i < components.length; i++ ) {
			sb.append( ", " );
			append( sb, components, value, i );
		}
		sb.append( ')' );
		return sb.toString();
	}

	private void append(StringBuilder builder, JavaType[] components, Object[] value, int i) {
		final Object element = value[i];
		if (element == null ) {
			builder.append( "null" );
		}
		else {
			builder.append( components[i].toString( element ) );
		}
	}

	@Override
	public boolean areEqual(Object[] one, Object[] another) {
		if ( one == another ) {
			return true;
		}
		if ( one != null && another != null && one.length == another.length ) {
			for ( int i = 0; i < components.length; i++ ) {
				if ( !components[i].areEqual( one[i], another[i] ) ) {
					return false;
				}
			}
			return true;
		}
		return false;
	}

	@Override
	public int extractHashCode(Object[] objects) {
		int hashCode = 1;
		for ( int i = 0; i < objects.length; i++ ) {
			hashCode = 31 * hashCode + components[i].extractHashCode( objects[i] );
		}
		return hashCode;
	}

	@Override
	public <X> X unwrap(Object[] value, Class<X> type, WrapperOptions options) {
		if ( value == null ) {
			return null;
		}
		if ( Object[].class.isAssignableFrom( type ) ) {
			return type.cast( value );
		}
		throw unknownUnwrap( type );
	}

	@Override
	public <X> Object[] wrap(X value, WrapperOptions options) {
		if ( value == null ) {
			return null;
		}
		if (value instanceof Object[] objects) {
			return objects;
		}
		throw unknownWrap( value.getClass() );
	}
}
