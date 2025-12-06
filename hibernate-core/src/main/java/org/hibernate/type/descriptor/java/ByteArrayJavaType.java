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
import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.jdbc.AdjustableJdbcType;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.JdbcTypeIndicators;

import static java.lang.Byte.toUnsignedInt;
import static java.lang.Integer.toHexString;

/**
 * Descriptor for {@code Byte[]} handling, which disallows {@code null} elements.
 * This {@link JavaType} is useful if the domain model uses {@code Byte[]} and wants to map to {@link SqlTypes#VARBINARY}.
 *
 * @author Steve Ebersole
 */
public class ByteArrayJavaType extends AbstractClassJavaType<Byte[]> {
	public static final ByteArrayJavaType INSTANCE = new ByteArrayJavaType();

	@SuppressWarnings("unchecked")
	public ByteArrayJavaType() {
		super( Byte[].class, ImmutableObjectArrayMutabilityPlan.get(), IncomparableComparator.INSTANCE );
	}

	@Override
	public boolean isInstance(Object value) {
		return value instanceof byte[];
	}

	@Override
	public Byte[] cast(Object value) {
		return (Byte[]) value;
	}

	@Override
	public boolean areEqual(Byte[] one, Byte[] another) {
		return one == another
			|| one != null && another != null && Arrays.equals(one, another);
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
	public JdbcType getRecommendedJdbcType(JdbcTypeIndicators indicators) {
		// match legacy behavior
		final var descriptor = indicators.getJdbcType( indicators.resolveJdbcTypeCode( SqlTypes.VARBINARY ) );
		return descriptor instanceof AdjustableJdbcType adjustableJdbcType
				? adjustableJdbcType.resolveIndicatedType( indicators, this )
				: descriptor;
	}

	@Override
	public String toString(Byte[] bytes) {
		final var string = new StringBuilder();
		for ( Byte aByte : bytes ) {
			final String hexStr = toHexString( toUnsignedInt( aByte ) );
			if ( hexStr.length() == 1 ) {
				string.append( '0' );
			}
			string.append( hexStr );
		}
		return string.toString();
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

	@Override
	public <X> X unwrap(Byte[] value, Class<X> type, WrapperOptions options) {
		if ( value == null ) {
			return null;
		}
		if ( Byte[].class.isAssignableFrom( type ) ) {
			return type.cast( value );
		}
		if ( byte[].class.isAssignableFrom( type ) ) {
			return type.cast( unwrapBytes( value ) );
		}
		if ( InputStream.class.isAssignableFrom( type ) ) {
			return type.cast( new ByteArrayInputStream( unwrapBytes( value ) ) );
		}
		if ( BinaryStream.class.isAssignableFrom( type ) ) {
			return type.cast( new ArrayBackedBinaryStream( unwrapBytes( value ) ) );
		}
		if ( Blob.class.isAssignableFrom( type ) ) {
			return type.cast( options.getLobCreator().createBlob( unwrapBytes( value ) ) );
		}

		throw unknownUnwrap( type );
	}
	@Override
	public <X> Byte[] wrap(X value, WrapperOptions options) {
		if ( value == null ) {
			return null;
		}
		if (value instanceof Byte[] bytes) {
			return bytes;
		}
		if (value instanceof byte[] bytes) {
			return wrapBytes( bytes );
		}
		if (value instanceof InputStream inputStream) {
			return wrapBytes( DataHelper.extractBytes( inputStream ) );
		}
		if ( value instanceof Blob blob ) {
			try {
				return wrapBytes( DataHelper.extractBytes( blob.getBinaryStream() ) );
			}
			catch ( SQLException e ) {
				throw new HibernateException( "Unable to access lob stream", e );
			}
		}

		throw unknownWrap( value.getClass() );
	}

	private Byte[] wrapBytes(byte[] bytes) {
		if ( bytes == null ) {
			return null;
		}
		final Byte[] result = new Byte[bytes.length];
		for ( int i = 0; i < bytes.length; i++ ) {
			result[i] = bytes[i];
		}
		return result;
	}

	private byte[] unwrapBytes(Byte[] bytes) {
		if ( bytes == null ) {
			return null;
		}
		final byte[] result = new byte[bytes.length];
		for ( int i = 0; i < bytes.length; i++ ) {
			result[i] = bytes[i];
		}
		return result;
	}
}
