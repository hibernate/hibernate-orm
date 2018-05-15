/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.descriptor.java.internal;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.sql.Blob;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Arrays;
import java.util.Comparator;

import org.hibernate.HibernateException;
import org.hibernate.engine.jdbc.BinaryStream;
import org.hibernate.engine.jdbc.internal.BinaryStreamImpl;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.type.descriptor.java.spi.AbstractBasicJavaDescriptor;
import org.hibernate.type.descriptor.java.spi.ArrayMutabilityPlan;
import org.hibernate.type.descriptor.spi.IncomparableComparator;
import org.hibernate.type.descriptor.spi.JdbcRecommendedSqlTypeMappingContext;
import org.hibernate.type.descriptor.sql.spi.SqlTypeDescriptor;

/**
 * Descriptor for {@code Byte[]} handling.
 *
 * @author Steve Ebersole
 */
public class ByteArrayJavaDescriptor extends AbstractBasicJavaDescriptor<Byte[]> {
	public static final ByteArrayJavaDescriptor INSTANCE = new ByteArrayJavaDescriptor();

	@SuppressWarnings({ "unchecked" })
	public ByteArrayJavaDescriptor() {
		super( Byte[].class, ArrayMutabilityPlan.INSTANCE );
	}

	@Override
	public SqlTypeDescriptor getJdbcRecommendedSqlType(JdbcRecommendedSqlTypeMappingContext context) {
		final int jdbcCode;
		if ( context.isLob() ) {
			jdbcCode = Types.BLOB;
		}
		else {
			jdbcCode = Types.LONGVARBINARY;
		}

		return context.getTypeConfiguration().getSqlTypeDescriptorRegistry().getDescriptor( jdbcCode );
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
	public boolean areEqual(Byte[] one, Byte[] another) {
		return one == another
				|| ( one != null && another != null && Arrays.equals(one, another) );
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
	public <X> X unwrap(Byte[] value, Class<X> type, SharedSessionContractImplementor session) {
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
			return (X) session.getLobCreator().createBlob( unwrapBytes( value ) );
		}

		throw unknownUnwrap( type );
	}

	@Override
	public <X> Byte[] wrap(X value, SharedSessionContractImplementor session) {
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
			return wrapBytes( LobStreamDataHelper.extractBytes( (InputStream) value ) );
		}
		if ( Blob.class.isInstance( value ) || LobStreamDataHelper.isNClob( value.getClass() ) ) {
			try {
				return wrapBytes( LobStreamDataHelper.extractBytes( ( (Blob) value ).getBinaryStream() ) );
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

	@Override
	public Comparator<Byte[]> getComparator() {
		return IncomparableComparator.INSTANCE;
	}
}
