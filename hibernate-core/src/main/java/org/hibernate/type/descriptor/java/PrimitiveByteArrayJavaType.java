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
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.util.compare.RowVersionComparator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.type.descriptor.WrapperOptions;

/**
 * Descriptor for {@code byte[]} handling.
 *
 * @author Steve Ebersole
 */
public class PrimitiveByteArrayJavaType extends AbstractClassJavaType<byte[]>
		implements VersionJavaType<byte[]> {
	public static final PrimitiveByteArrayJavaType INSTANCE = new PrimitiveByteArrayJavaType();

	@SuppressWarnings("unchecked")
	public PrimitiveByteArrayJavaType() {
		super( byte[].class, ArrayMutabilityPlan.INSTANCE, RowVersionComparator.INSTANCE );
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
		appendString( buf::append, bytes );
		return buf.toString();
	}

	public void appendString(SqlAppender appender, byte[] bytes) {
		for ( byte aByte : bytes ) {
			final String hexStr = Integer.toHexString( Byte.toUnsignedInt(aByte) );
			if ( hexStr.length() == 1 ) {
				appender.appendSql( '0' );
			}
			appender.appendSql( hexStr );
		}
	}

	@Override
	public String extractLoggableRepresentation(byte[] value) {
		return value == null ? super.extractLoggableRepresentation( null ) : Arrays.toString( value );
	}

	@Override
	public byte[] fromString(CharSequence string) {
		if ( string == null ) {
			return null;
		}
		if ( string.length() % 2 != 0 ) {
			throw new IllegalArgumentException( "The string is not a valid string representation of a binary content." );
		}
		byte[] bytes = new byte[string.length() / 2];
		for ( int i = 0; i < bytes.length; i++ ) {
			final String hexStr = string.subSequence( i * 2, (i + 1) * 2 ).toString();
			bytes[i] = (byte) Integer.parseInt( hexStr, 16 );
		}
		return bytes;
	}

	@SuppressWarnings("unchecked")
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
		if (value instanceof byte[]) {
			return (byte[]) value;
		}
		if (value instanceof InputStream) {
			return DataHelper.extractBytes( (InputStream) value );
		}
		if ( value instanceof Blob || DataHelper.isNClob( value.getClass() ) ) {
			try {
				return DataHelper.extractBytes( ( (Blob) value ).getBinaryStream() );
			}
			catch ( SQLException e ) {
				throw new HibernateException( "Unable to access lob stream", e );
			}
		}

		throw unknownWrap( value.getClass() );
	}

	@Override
	public byte[] seed(SharedSessionContractImplementor session) {
		// Note : simply returns null for seed() and next() as the only known
		// 		application of binary types for versioning is for use with the
		// 		TIMESTAMP datatype supported by Sybase and SQL Server, which
		// 		are completely db-generated values...
		return null;
	}

	@Override
	public byte[] next(byte[] current, SharedSessionContractImplementor session) {
		return current;
	}
}
