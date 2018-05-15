/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.descriptor.java.internal;

import java.io.Serializable;
import java.util.Comparator;
import java.util.TimeZone;
import java.util.UUID;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.util.BytesHelper;
import org.hibernate.type.descriptor.java.spi.AbstractBasicJavaDescriptor;
import org.hibernate.type.descriptor.spi.JdbcRecommendedSqlTypeMappingContext;
import org.hibernate.type.descriptor.sql.spi.SqlTypeDescriptor;

/**
 * Descriptor for {@link TimeZone} handling.
 *
 * @author Steve Ebersole
 */
public class UUIDJavaDescriptor extends AbstractBasicJavaDescriptor<UUID> {
	public static final UUIDJavaDescriptor INSTANCE = new UUIDJavaDescriptor();

	public static class UUIDComparator implements Comparator<UUID> {
		public static final UUIDComparator INSTANCE = new UUIDComparator();

		public int compare(UUID o1, UUID o2) {
			return o1.compareTo( o2 );
		}
	}

	public UUIDJavaDescriptor() {
		super( UUID.class );
	}

	public String toString(UUID value) {
		return value.toString();
	}

	public UUID fromString(String string) {
		return UUID.fromString( string );
	}

	@Override
	public SqlTypeDescriptor getJdbcRecommendedSqlType(JdbcRecommendedSqlTypeMappingContext context) {
		return StringJavaDescriptor.INSTANCE.getJdbcRecommendedSqlType( context );
	}

	@Override
	public Comparator<UUID> getComparator() {
		return UUIDComparator.INSTANCE;
	}

	@SuppressWarnings({ "unchecked" })
	public <X> X unwrap(UUID value, Class<X> type, SharedSessionContractImplementor session) {
		if ( value == null ) {
			return null;
		}
		if ( String.class.isAssignableFrom( type ) ) {
			return (X) toString( value );
		}
		throw unknownUnwrap( type );
	}

	public <X> UUID wrap(X value, SharedSessionContractImplementor session) {
		if ( value == null ) {
			return null;
		}
		if ( String.class.isInstance( value ) ) {
			return fromString( (String) value );
		}
		throw unknownWrap( value.getClass() );
	}

	public static interface ValueTransformer {
		Serializable transform(UUID uuid);
		UUID parse(Object value);
	}

	public static class PassThroughTransformer implements ValueTransformer {
		public static final PassThroughTransformer INSTANCE = new PassThroughTransformer();

		public UUID transform(UUID uuid) {
			return uuid;
		}

		public UUID parse(Object value) {
			return (UUID)value;
		}
	}

	public static class ToStringTransformer implements ValueTransformer {
		public static final ToStringTransformer INSTANCE = new ToStringTransformer();

		public String transform(UUID uuid) {
			return uuid.toString();
		}

		public UUID parse(Object value) {
			return UUID.fromString( (String) value );
		}
	}

	public static class ToBytesTransformer implements ValueTransformer {
		public static final ToBytesTransformer INSTANCE = new ToBytesTransformer();

		public byte[] transform(UUID uuid) {
			byte[] bytes = new byte[16];
			System.arraycopy( BytesHelper.fromLong( uuid.getMostSignificantBits() ), 0, bytes, 0, 8 );
			System.arraycopy( BytesHelper.fromLong( uuid.getLeastSignificantBits() ), 0, bytes, 8, 8 );
			return bytes;
		}

		public UUID parse(Object value) {
			byte[] msb = new byte[8];
			byte[] lsb = new byte[8];
			System.arraycopy( value, 0, msb, 0, 8 );
			System.arraycopy( value, 8, lsb, 0, 8 );
			return new UUID( BytesHelper.asLong( msb ), BytesHelper.asLong( lsb ) );
		}
	}
}
