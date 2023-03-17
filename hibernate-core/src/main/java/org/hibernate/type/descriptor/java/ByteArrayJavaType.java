/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.descriptor.java;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.sql.Blob;
import java.sql.SQLException;
import java.util.Arrays;

import org.hibernate.HibernateException;
import org.hibernate.engine.jdbc.BinaryStream;
import org.hibernate.engine.jdbc.internal.BinaryStreamImpl;
import org.hibernate.internal.util.SerializationHelper;
import org.hibernate.type.descriptor.WrapperOptions;

/**
 * Descriptor for {@code Byte[]} handling.
 *
 * @author Steve Ebersole
 */
public class ByteArrayJavaType extends AbstractClassJavaType<Byte[]> {
	public static final ByteArrayJavaType INSTANCE = new ByteArrayJavaType();
	private static final Byte[] EMPTY_BYTE_ARRAY = new Byte[0];

	@SuppressWarnings("unchecked")
	public ByteArrayJavaType() {
		super( Byte[].class, ArrayMutabilityPlan.INSTANCE, IncomparableComparator.INSTANCE );
	}
	@Override
	public boolean areEqual(Byte[] one, Byte[] another) {
		return one == another
				|| ( one != null && another != null && Arrays.equals(one, another) );
	}
	@Override
	public int extractHashCode(Byte[] bytes) {
		int hashCode = 1;
		for ( byte aByte : bytes ) {
			hashCode = 31 * hashCode + aByte;
		}
		return hashCode;
	}

	@Override
	public String toString(Byte[] bytes) {
		final StringBuilder buf = new StringBuilder();
		for ( Byte aByte : bytes ) {
			final String hexStr = Integer.toHexString( Byte.toUnsignedInt(aByte) );
			if ( hexStr.length() == 1 ) {
				buf.append( '0' );
			}
			buf.append( hexStr );
		}
		return buf.toString();
	}
	@Override
	public Byte[] fromString(CharSequence string) {
		if ( string == null ) {
			return null;
		}
		if ( string.length() % 2 != 0 ) {
			throw new IllegalArgumentException( "The string is not a valid string representation of a binary content." );
		}
		Byte[] bytes = new Byte[string.length() / 2];
		for ( int i = 0; i < bytes.length; i++ ) {
			final String hexStr = string.subSequence( i * 2, (i + 1) * 2 ).toString();
			bytes[i] = (byte) Integer.parseInt( hexStr, 16 );
		}
		return bytes;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <X> X unwrap(Byte[] value, Class<X> type, WrapperOptions options) {
		if ( value == null ) {
			return null;
		}
		if ( Byte[].class.isAssignableFrom( type ) ) {
			return (X) value;
		}
		if ( byte[].class.isAssignableFrom( type ) ) {
			return (X) unwrapBytes( value );
		}
		if ( InputStream.class.isAssignableFrom( type ) ) {
			return (X) new ByteArrayInputStream( unwrapBytes( value ) );
		}
		if ( BinaryStream.class.isAssignableFrom( type ) ) {
			return (X) new BinaryStreamImpl( unwrapBytes( value ) );
		}
		if ( Blob.class.isAssignableFrom( type ) ) {
			return (X) options.getLobCreator().createBlob( unwrapBytes( value ) );
		}

		throw unknownUnwrap( type );
	}
	@Override
	public <X> Byte[] wrap(X value, WrapperOptions options) {
		if ( value == null ) {
			return null;
		}
		if (value instanceof Byte[]) {
			return (Byte[]) value;
		}
		if (value instanceof byte[]) {
			return wrapBytes( (byte[]) value );
		}
		if (value instanceof InputStream) {
			return wrapBytes( DataHelper.extractBytes( (InputStream) value ) );
		}
		if ( value instanceof Blob || DataHelper.isNClob( value.getClass() ) ) {
			try {
				return wrapBytes( DataHelper.extractBytes( ( (Blob) value ).getBinaryStream() ) );
			}
			catch ( SQLException e ) {
				throw new HibernateException( "Unable to access lob stream", e );
			}
		}
		if ( value instanceof java.sql.Array ) {
			try {
				//noinspection unchecked
				value = (X) ( (java.sql.Array) value ).getArray();
				if ( value instanceof Byte[] ) {
					return (Byte[]) value;
				}
				else if ( value instanceof Object[] ) {
					final Object[] array = (Object[]) value;
					if ( array.length == 0 ) {
						return EMPTY_BYTE_ARRAY;
					}
					final Byte[] bytes = new Byte[array.length];
					for ( int i = 0; i < array.length; i++ ) {
						bytes[i] = ByteJavaType.INSTANCE.wrap( array[i], options );
					}
					return bytes;
				}
			}
			catch ( SQLException ex ) {
				// This basically shouldn't happen unless you've lost connection to the database.
				throw new HibernateException( ex );
			}
		}

		throw unknownWrap( value.getClass() );
	}

	private Byte[] wrapBytes(byte[] bytes) {
		if ( bytes == null ) {
			return null;
		}
		// Since a Byte[] can contain nulls but a byte[] can't, we have to serialize/deserialize the content
		return (Byte[]) SerializationHelper.deserialize( bytes );
	}

	private byte[] unwrapBytes(Byte[] bytes) {
		if ( bytes == null ) {
			return null;
		}

		// Since a Byte[] can contain nulls but a byte[] can't, we have to serialize/deserialize the content
		return SerializationHelper.serialize( bytes );
	}
}
