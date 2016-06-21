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
import java.util.Comparator;

import org.hibernate.HibernateException;
import org.hibernate.engine.jdbc.BinaryStream;
import org.hibernate.engine.jdbc.internal.BinaryStreamImpl;
import org.hibernate.type.descriptor.WrapperOptions;

/**
 * Descriptor for {@code byte[]} handling.
 *
 * @author Steve Ebersole
 */
public class PrimitiveByteArrayTypeDescriptor extends AbstractTypeDescriptor<byte[]> {
	public static final PrimitiveByteArrayTypeDescriptor INSTANCE = new PrimitiveByteArrayTypeDescriptor();

	@SuppressWarnings({ "unchecked" })
	public PrimitiveByteArrayTypeDescriptor() {
		super( byte[].class, ArrayMutabilityPlan.INSTANCE );
	}

	@Override
	public boolean areEqual(byte[] one, byte[] another) {
		return one == another 
				|| ( one != null && another != null && Arrays.equals( one, another ) );
	}

	@Override
	public int extractHashCode(byte[] bytes) {
		int hashCode = 1;
		for ( byte aByte : bytes ) {
			hashCode = 31 * hashCode + aByte;
		}
		return hashCode;
	}

	public String toString(byte[] bytes) {
		final StringBuilder buf = new StringBuilder( bytes.length * 2 );
		for ( byte aByte : bytes ) {
			final String hexStr = Integer.toHexString( aByte - Byte.MIN_VALUE );
			if ( hexStr.length() == 1 ) {
				buf.append( '0' );
			}
			buf.append( hexStr );
		}
		return buf.toString();
	}

	@Override
	public String extractLoggableRepresentation(byte[] value) {
		return (value == null) ? super.extractLoggableRepresentation( null ) : Arrays.toString( value );
	}

	public byte[] fromString(String string) {
		if ( string == null ) {
			return null;
		}
		if ( string.length() % 2 != 0 ) {
			throw new IllegalArgumentException( "The string is not a valid string representation of a binary content." );
		}
		byte[] bytes = new byte[string.length() / 2];
		for ( int i = 0; i < bytes.length; i++ ) {
			final String hexStr = string.substring( i * 2, (i + 1) * 2 );
			bytes[i] = (byte) (Integer.parseInt(hexStr, 16) + Byte.MIN_VALUE);
		}
		return bytes;
	}

	@Override
	@SuppressWarnings({ "unchecked" })
	public Comparator<byte[]> getComparator() {
		return IncomparableComparator.INSTANCE;
	}

	@SuppressWarnings({ "unchecked" })
	public <X> X unwrap(byte[] value, Class<X> type, WrapperOptions options) {
		if ( value == null ) {
			return null;
		}
		if ( byte[].class.isAssignableFrom( type ) ) {
			return (X) value;
		}
		if ( InputStream.class.isAssignableFrom( type ) ) {
			return (X) new ByteArrayInputStream( value );
		}
		if ( BinaryStream.class.isAssignableFrom( type ) ) {
			return (X) new BinaryStreamImpl( value );
		}
		if ( Blob.class.isAssignableFrom( type ) ) {
			return (X) options.getLobCreator().createBlob( value );
		}

		throw unknownUnwrap( type );
	}

	public <X> byte[] wrap(X value, WrapperOptions options) {
		if ( value == null ) {
			return null;
		}
		if ( byte[].class.isInstance( value ) ) {
			return (byte[]) value;
		}
		if ( InputStream.class.isInstance( value ) ) {
			return DataHelper.extractBytes( (InputStream) value );
		}
		if ( Blob.class.isInstance( value ) || DataHelper.isNClob( value.getClass() ) ) {
			try {
				return DataHelper.extractBytes( ( (Blob) value ).getBinaryStream() );
			}
			catch ( SQLException e ) {
				throw new HibernateException( "Unable to access lob stream", e );
			}
		}

		throw unknownWrap( value.getClass() );
	}
}
