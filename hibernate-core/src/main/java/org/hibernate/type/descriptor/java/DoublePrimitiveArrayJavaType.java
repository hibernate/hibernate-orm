/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.descriptor.java;

import java.io.Serializable;
import java.lang.reflect.Array;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.hibernate.HibernateException;
import org.hibernate.SharedSessionContract;
import org.hibernate.engine.jdbc.BinaryStream;
import org.hibernate.engine.jdbc.internal.BinaryStreamImpl;
import org.hibernate.internal.util.SerializationHelper;
import org.hibernate.type.descriptor.WrapperOptions;

/**
 * Descriptor for {@code double[]} handling.
 *
 * @author Christian Beikov
 */
public class DoublePrimitiveArrayJavaType extends AbstractArrayJavaType<double[], Double> {

	public static final DoublePrimitiveArrayJavaType INSTANCE = new DoublePrimitiveArrayJavaType();

	private DoublePrimitiveArrayJavaType() {
		this( DoubleJavaType.INSTANCE );
	}

	protected DoublePrimitiveArrayJavaType(JavaType<Double> baseDescriptor) {
		super( double[].class, baseDescriptor, new ArrayMutabilityPlan() );
	}

	@Override
	public String extractLoggableRepresentation(double[] value) {
		return value == null ? super.extractLoggableRepresentation( null ) : Arrays.toString( value );
	}

	@Override
	public boolean areEqual(double[] one, double[] another) {
		return Arrays.equals( one, another );
	}

	@Override
	public int extractHashCode(double[] value) {
		return Arrays.hashCode( value );
	}

	@Override
	public String toString(double[] value) {
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
	public double[] fromString(CharSequence charSequence) {
		if ( charSequence == null ) {
			return null;
		}
		final List<Double> list = new ArrayList<>();
		final char lastChar = charSequence.charAt( charSequence.length() - 1 );
		final char firstChar = charSequence.charAt( 0 );
		if ( firstChar != '{' || lastChar != '}' ) {
			throw new IllegalArgumentException( "Cannot parse given string into array of strings. First and last character must be { and }" );
		}
		final int len = charSequence.length();
		int elementStart = 1;
		for ( int i = elementStart; i < len; i ++ ) {
			final char c = charSequence.charAt( i );
			if ( c == ',' ) {
				list.add( Double.parseDouble( charSequence.subSequence( elementStart, i ).toString() ) );
				elementStart = i + 1;
			}
		}
		final double[] result = new double[list.size()];
		for ( int i = 0; i < result.length; i ++ ) {
			result[ i ] = list.get( i );
		}
		return result;
	}

	@Override
	public <X> X unwrap(double[] value, Class<X> type, WrapperOptions options) {
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
			return (X) new BinaryStreamImpl( SerializationHelper.serialize( value ) );
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
	public <X> double[] wrap(X value, WrapperOptions options) {
		if ( value == null ) {
			return null;
		}

		if ( value instanceof java.sql.Array ) {
			try {
				//noinspection unchecked
				value = (X) ( (java.sql.Array) value ).getArray();
			}
			catch ( SQLException ex ) {
				// This basically shouldn't happen unless you've lost connection to the database.
				throw new HibernateException( ex );
			}
		}

		if ( value instanceof double[] ) {
			return (double[]) value;
		}
		else if ( value instanceof byte[] ) {
			// When the value is a byte[], this is a deserialization request
			return (double[]) SerializationHelper.deserialize( (byte[]) value );
		}
		else if ( value instanceof BinaryStream ) {
			// When the value is a BinaryStream, this is a deserialization request
			return (double[]) SerializationHelper.deserialize( ( (BinaryStream) value ).getBytes() );
		}
		else if ( value.getClass().isArray() ) {
			final double[] wrapped = new double[Array.getLength( value )];
			for ( int i = 0; i < wrapped.length; i++ ) {
				wrapped[i] = getElementJavaType().wrap( Array.get( value, i ), options );
			}
			return wrapped;
		}
		else if ( value instanceof Double ) {
			// Support binding a single element as parameter value
			return new double[]{ (double) value };
		}

		throw unknownWrap( value.getClass() );
	}

	private static class ArrayMutabilityPlan implements MutabilityPlan<double[]> {

		@Override
		public boolean isMutable() {
			return true;
		}

		@Override
		public double[] deepCopy(double[] value) {
			return value == null ? null : value.clone();
		}

		@Override
		public Serializable disassemble(double[] value, SharedSessionContract session) {
			return deepCopy( value );
		}

		@Override
		public double[] assemble(Serializable cached, SharedSessionContract session) {
			return deepCopy( (double[]) cached );
		}

	}
}
