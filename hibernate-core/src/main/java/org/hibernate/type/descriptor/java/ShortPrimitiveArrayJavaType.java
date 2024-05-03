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
import java.util.Collection;
import java.util.List;

import org.hibernate.HibernateException;
import org.hibernate.SharedSessionContract;
import org.hibernate.engine.jdbc.BinaryStream;
import org.hibernate.engine.jdbc.internal.BinaryStreamImpl;
import org.hibernate.internal.util.SerializationHelper;
import org.hibernate.type.descriptor.WrapperOptions;

/**
 * Descriptor for {@code short[]} handling.
 *
 * @author Christian Beikov
 */
public class ShortPrimitiveArrayJavaType extends AbstractArrayJavaType<short[], Short> {

	public static final ShortPrimitiveArrayJavaType INSTANCE = new ShortPrimitiveArrayJavaType();

	private ShortPrimitiveArrayJavaType() {
		this( ShortJavaType.INSTANCE );
	}

	protected ShortPrimitiveArrayJavaType(JavaType<Short> baseDescriptor) {
		super( short[].class, baseDescriptor, new ArrayMutabilityPlan() );
	}

	@Override
	public String extractLoggableRepresentation(short[] value) {
		return value == null ? super.extractLoggableRepresentation( null ) : Arrays.toString( value );
	}

	@Override
	public boolean areEqual(short[] one, short[] another) {
		return Arrays.equals( one, another );
	}

	@Override
	public int extractHashCode(short[] value) {
		return Arrays.hashCode( value );
	}

	@Override
	public String toString(short[] value) {
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
	public short[] fromString(CharSequence charSequence) {
		if ( charSequence == null ) {
			return null;
		}
		final List<Short> list = new ArrayList<>();
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
				list.add( Short.parseShort( charSequence.subSequence( elementStart, i ).toString(), 10 ) );
				elementStart = i + 1;
			}
		}
		final short[] result = new short[list.size()];
		for ( int i = 0; i < result.length; i ++ ) {
			result[ i ] = list.get( i );
		}
		return result;
	}

	@Override
	public <X> X unwrap(short[] value, Class<X> type, WrapperOptions options) {
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
	public <X> short[] wrap(X value, WrapperOptions options) {
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

		if ( value instanceof short[] ) {
			return (short[]) value;
		}
		else if ( value instanceof byte[] ) {
			// When the value is a byte[], this is a deserialization request
			return (short[]) SerializationHelper.deserialize( (byte[]) value );
		}
		else if ( value instanceof BinaryStream ) {
			// When the value is a BinaryStream, this is a deserialization request
			return (short[]) SerializationHelper.deserialize( ( (BinaryStream) value ).getBytes() );
		}
		else if ( value.getClass().isArray() ) {
			final short[] wrapped = new short[Array.getLength( value )];
			for ( int i = 0; i < wrapped.length; i++ ) {
				wrapped[i] = getElementJavaType().wrap( Array.get( value, i ), options );
			}
			return wrapped;
		}
		else if ( value instanceof Short ) {
			// Support binding a single element as parameter value
			return new short[]{ (short) value };
		}
		else if ( value instanceof Collection<?> ) {
			final Collection<?> collection = (Collection<?>) value;
			final short[] wrapped = new short[collection.size()];
			int i = 0;
			for ( Object e : collection ) {
				wrapped[i++] = getElementJavaType().wrap( e, options );
			}
			return wrapped;
		}

		throw unknownWrap( value.getClass() );
	}

	private static class ArrayMutabilityPlan implements MutabilityPlan<short[]> {

		@Override
		public boolean isMutable() {
			return true;
		}

		@Override
		public short[] deepCopy(short[] value) {
			return value == null ? null : value.clone();
		}

		@Override
		public Serializable disassemble(short[] value, SharedSessionContract session) {
			return deepCopy( value );
		}

		@Override
		public short[] assemble(Serializable cached, SharedSessionContract session) {
			return deepCopy( (short[]) cached );
		}

	}
}
