/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type.descriptor.java;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.sql.Blob;
import java.sql.SQLException;
import java.util.Arrays;

import org.hibernate.HibernateException;
import org.hibernate.engine.jdbc.BinaryStream;
import org.hibernate.engine.jdbc.internal.ArrayBackedBinaryStream;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.util.compare.RowVersionComparator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.spi.StringBuilderSqlAppender;
import org.hibernate.type.descriptor.WrapperOptions;

/**
 * Descriptor for {@code byte[]} handling.
 *
 * @author Steve Ebersole
 */
public class PrimitiveByteArrayJavaType extends AbstractClassJavaType<byte[]>
		implements VersionJavaType<byte[]> {
	public static final PrimitiveByteArrayJavaType INSTANCE = new PrimitiveByteArrayJavaType();

	public PrimitiveByteArrayJavaType() {
		super( byte[].class, new ArrayMutabilityPlan(), RowVersionComparator.INSTANCE );
	}

	@Override
	public boolean isInstance(Object value) {
		return value instanceof byte[];
	}

	@Override
	public byte[] cast(Object value) {
		return (byte[]) value;
	}

	@Override
	public boolean areEqual(byte[] one, byte[] another) {
		return one == another
			|| one != null && another != null && Arrays.equals( one, another );
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
		final StringBuilder sb = new StringBuilder( bytes.length * 2 );
		appendString( new StringBuilderSqlAppender( sb ), bytes );
		return sb.toString();
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

	public void appendString(StringBuilder appender, byte[] bytes) {
		for ( byte aByte : bytes ) {
			final String hexStr = Integer.toHexString( Byte.toUnsignedInt(aByte) );
			if ( hexStr.length() == 1 ) {
				appender.append( '0' );
			}
			appender.append( hexStr );
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

	public <X> X unwrap(byte[] value, Class<X> type, WrapperOptions options) {
		if ( value == null ) {
			return null;
		}
		if ( byte[].class.isAssignableFrom( type ) ) {
			return type.cast( value );
		}
		if ( InputStream.class.isAssignableFrom( type ) ) {
			return type.cast( new ByteArrayInputStream( value ) );
		}
		if ( BinaryStream.class.isAssignableFrom( type ) ) {
			return type.cast( new ArrayBackedBinaryStream( value ) );
		}
		if ( Blob.class.isAssignableFrom( type ) ) {
			return type.cast( options.getLobCreator().createBlob( value ) );
		}
		if ( type.isAssignableFrom( Byte[].class ) ) {
			final Byte[] array = new Byte[value.length];
			for ( int i = 0; i < value.length; i++ ) {
				array[i] = value[i];
			}
			return type.cast( array );
		}

		throw unknownUnwrap( type );
	}

	public <X> byte[] wrap(X value, WrapperOptions options) {
		if ( value == null ) {
			return null;
		}
		if (value instanceof byte[] bytes) {
			return bytes;
		}
		if (value instanceof InputStream inputStream) {
			return DataHelper.extractBytes( inputStream );
		}
		if ( value instanceof Blob blob ) {
			try {
				return DataHelper.extractBytes( blob.getBinaryStream() );
			}
			catch ( SQLException e ) {
				throw new HibernateException( "Unable to access lob stream", e );
			}
		}
		else if ( value instanceof Byte byteValue ) {
			// Support binding a single element as parameter value
			return new byte[]{ byteValue };
		}
		else if ( value instanceof Byte[] array ) {
			final byte[] bytes = new byte[array.length];
			for ( int i = 0; i < array.length; i++ ) {
				bytes[i] = array[i];
			}
			return bytes;
		}

		throw unknownWrap( value.getClass() );
	}

	@Override
	public byte[] seed(
			Long length, Integer precision, Integer scale, SharedSessionContractImplementor session) {
		// Note : simply returns null for seed() and next() as the only known
		// 		application of binary types for versioning is for use with the
		// 		TIMESTAMP datatype supported by Sybase and SQL Server, which
		// 		are completely db-generated values...
		return null;
	}

	@Override
	public byte[] next(
			byte[] current,
			Long length,
			Integer precision,
			Integer scale, SharedSessionContractImplementor session) {
		return current;
	}

	private static class ArrayMutabilityPlan extends MutableMutabilityPlan<byte[]> {
		@Override
		protected byte[] deepCopyNotNull(byte[] value) {
			return value.clone();
		}
	}
}
