/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.type.descriptor.java;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.sql.Blob;
import java.sql.SQLException;

import org.hibernate.HibernateException;
import org.hibernate.engine.jdbc.BinaryStream;
import org.hibernate.engine.jdbc.internal.BinaryStreamImpl;
import org.hibernate.type.descriptor.WrapperOptions;

/**
 * Descriptor for {@code Byte[]} handling.
 *
 * @author Steve Ebersole
 */
public class ByteArrayTypeDescriptor extends AbstractTypeDescriptor<Byte[]> {
	public static final ByteArrayTypeDescriptor INSTANCE = new ByteArrayTypeDescriptor();

	@SuppressWarnings({ "unchecked" })
	public ByteArrayTypeDescriptor() {
		super( Byte[].class, ArrayMutabilityPlan.INSTANCE );
	}
	@Override
	public String toString(Byte[] bytes) {
		final StringBuilder buf = new StringBuilder();
		for ( Byte aByte : bytes ) {
			final String hexStr = Integer.toHexString( aByte - Byte.MIN_VALUE );
			if ( hexStr.length() == 1 ) {
				buf.append( '0' );
			}
			buf.append( hexStr );
		}
		return buf.toString();
	}
	@Override
	public Byte[] fromString(String string) {
		if ( string == null ) {
			return null;
		}
		if ( string.length() % 2 != 0 ) {
			throw new IllegalArgumentException( "The string is not a valid string representation of a binary content." );
		}
		Byte[] bytes = new Byte[string.length() / 2];
		for ( int i = 0; i < bytes.length; i++ ) {
			final String hexStr = string.substring( i * 2, (i + 1) * 2 );
			bytes[i] = (byte) ( Integer.parseInt( hexStr, 16 ) + Byte.MIN_VALUE );
		}
		return bytes;
	}

	@SuppressWarnings({ "unchecked" })
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
		if ( Byte[].class.isInstance( value ) ) {
			return (Byte[]) value;
		}
		if ( byte[].class.isInstance( value ) ) {
			return wrapBytes( (byte[]) value );
		}
		if ( InputStream.class.isInstance( value ) ) {
			return wrapBytes( DataHelper.extractBytes( (InputStream) value ) );
		}
		if ( Blob.class.isInstance( value ) || DataHelper.isNClob( value.getClass() ) ) {
			try {
				return wrapBytes( DataHelper.extractBytes( ( (Blob) value ).getBinaryStream() ) );
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
