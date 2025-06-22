/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type.descriptor.java;

import java.lang.reflect.Array;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.hibernate.HibernateException;
import org.hibernate.engine.jdbc.BinaryStream;
import org.hibernate.engine.jdbc.internal.ArrayBackedBinaryStream;
import org.hibernate.internal.build.AllowReflection;
import org.hibernate.internal.util.SerializationHelper;
import org.hibernate.type.descriptor.WrapperOptions;

/**
 * Descriptor for {@code float[]} handling.
 *
 * @author Christian Beikov
 */
@AllowReflection // Needed for arbitrary array wrapping/unwrapping
public class FloatPrimitiveArrayJavaType extends AbstractArrayJavaType<float[], Float> {

	public static final FloatPrimitiveArrayJavaType INSTANCE = new FloatPrimitiveArrayJavaType();

	private FloatPrimitiveArrayJavaType() {
		this( FloatJavaType.INSTANCE );
	}

	protected FloatPrimitiveArrayJavaType(JavaType<Float> baseDescriptor) {
		super( float[].class, baseDescriptor, new ArrayMutabilityPlan() );
	}

	@Override
	public boolean isInstance(Object value) {
		return value instanceof float[];
	}

	@Override
	public String extractLoggableRepresentation(float[] value) {
		return value == null ? super.extractLoggableRepresentation( null ) : Arrays.toString( value );
	}

	@Override
	public boolean areEqual(float[] one, float[] another) {
		return Arrays.equals( one, another );
	}

	@Override
	public int extractHashCode(float[] value) {
		return Arrays.hashCode( value );
	}

	@Override
	public String toString(float[] value) {
		if ( value == null ) {
			return null;
		}
		final StringBuilder sb = new StringBuilder();
		sb.append( '{' );
		sb.append( value[0] );
		for ( int i = 1; i < value.length; i++ ) {
			sb.append( value[i] );
			sb.append( ',' );
		}
		sb.append( '}' );
		return sb.toString();
	}

	@Override
	public float[] fromString(CharSequence charSequence) {
		if ( charSequence == null ) {
			return null;
		}
		final List<Float> list = new ArrayList<>();
		final char lastChar = charSequence.charAt( charSequence.length() - 1 );
		final char firstChar = charSequence.charAt( 0 );
		if ( firstChar != '{' || lastChar != '}' ) {
			throw new IllegalArgumentException( "Cannot parse given string into array of Floats. First and last character must be { and }" );
		}
		final int len = charSequence.length();
		int elementStart = 1;
		for ( int i = elementStart; i < len; i ++ ) {
			final char c = charSequence.charAt( i );
			if ( c == ',' ) {
				list.add( Float.parseFloat( charSequence.subSequence( elementStart, i ).toString() ) );
				elementStart = i + 1;
			}
		}
		final float[] result = new float[list.size()];
		for ( int i = 0; i < result.length; i ++ ) {
			result[ i ] = list.get( i );
		}
		return result;
	}

	@Override
	public <X> X unwrap(float[] value, Class<X> type, WrapperOptions options) {
		if ( value == null ) {
			return null;
		}

		if ( type.isInstance( value ) ) {
			return (X) value;
		}
		else if ( Object[].class.isAssignableFrom( type ) ) {
			final Class<?> preferredJavaTypeClass = type.getComponentType();
			final Object[] unwrapped = (Object[]) Array.newInstance( preferredJavaTypeClass, value.length );
			for ( int i = 0; i < value.length; i++ ) {
				unwrapped[i] = getElementJavaType().unwrap( value[i], preferredJavaTypeClass, options );
			}
			return (X) unwrapped;
		}
		else if ( type == byte[].class ) {
			// byte[] can only be requested if the value should be serialized
			return (X) SerializationHelper.serialize( value );
		}
		else if ( type == BinaryStream.class ) {
			// BinaryStream can only be requested if the value should be serialized
			//noinspection unchecked
			return (X) new ArrayBackedBinaryStream( SerializationHelper.serialize( value ) );
		}
		else if ( type.isArray() ) {
			final Class<?> preferredJavaTypeClass = type.getComponentType();
			final Object unwrapped = Array.newInstance( preferredJavaTypeClass, value.length );
			for ( int i = 0; i < value.length; i++ ) {
				Array.set( unwrapped, i, getElementJavaType().unwrap( value[i], preferredJavaTypeClass, options ) );
			}
			return (X) unwrapped;
		}

		throw unknownUnwrap( type );
	}

	@Override
	public <X> float[] wrap(X value, WrapperOptions options) {
		if ( value == null ) {
			return null;
		}

		if ( value instanceof java.sql.Array array ) {
			try {
				//noinspection unchecked
				value = (X) array.getArray();
			}
			catch ( SQLException ex ) {
				// This basically shouldn't happen unless you've lost connection to the database.
				throw new HibernateException( ex );
			}
		}

		if ( value instanceof float[] floats ) {
			return floats;
		}
		else if ( value instanceof byte[] bytes ) {
			// When the value is a byte[], this is a deserialization request
			return (float[]) SerializationHelper.deserialize( bytes );
		}
		else if ( value instanceof BinaryStream binaryStream ) {
			// When the value is a BinaryStream, this is a deserialization request
			return (float[]) SerializationHelper.deserialize( binaryStream.getBytes() );
		}
		else if ( value.getClass().isArray() ) {
			final float[] wrapped = new float[Array.getLength( value )];
			for ( int i = 0; i < wrapped.length; i++ ) {
				wrapped[i] = getElementJavaType().wrap( Array.get( value, i ), options );
			}
			return wrapped;
		}
		else if ( value instanceof Float floatValue) {
			// Support binding a single element as parameter value
			return new float[]{ floatValue };
		}
		else if ( value instanceof Collection<?> collection ) {
			final float[] wrapped = new float[collection.size()];
			int i = 0;
			for ( Object e : collection ) {
				wrapped[i++] = getElementJavaType().wrap( e, options );
			}
			return wrapped;
		}

		throw unknownWrap( value.getClass() );
	}

	private static class ArrayMutabilityPlan extends MutableMutabilityPlan<float[]> {
		@Override
		protected float[] deepCopyNotNull(float[] value) {
			return value.clone();
		}
	}
}
