/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.descriptor.java;

import java.io.Serializable;
import java.util.UUID;

import org.hibernate.dialect.Dialect;
import org.hibernate.internal.util.BytesHelper;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.JdbcTypeIndicators;

/**
 * Descriptor for {@link UUID} handling.
 *
 * @see org.hibernate.cfg.AvailableSettings#PREFERRED_UUID_JDBC_TYPE
 *
 * @author Steve Ebersole
 */
public class UUIDJavaType extends AbstractClassJavaType<UUID> {
	public static final UUIDJavaType INSTANCE = new UUIDJavaType();

	public UUIDJavaType() {
		super( UUID.class );
	}

	@Override
	public JdbcType getRecommendedJdbcType(JdbcTypeIndicators context) {
		return context.getJdbcType( context.getPreferredSqlTypeCodeForUuid() );
	}

	public String toString(UUID value) {
		return ToStringTransformer.INSTANCE.transform( value );
	}

	public UUID fromString(CharSequence string) {
		return ToStringTransformer.INSTANCE.parse( string.toString() );
	}

	@Override
	public long getDefaultSqlLength(Dialect dialect, JdbcType jdbcType) {
		if ( jdbcType.isString() ) {
			return 36L;
		}
		else if ( jdbcType.isBinary() ) {
			return 16L;
		}
		return super.getDefaultSqlLength( dialect, jdbcType );
	}

	@SuppressWarnings("unchecked")
	public <X> X unwrap(UUID value, Class<X> type, WrapperOptions options) {
		if ( value == null ) {
			return null;
		}
		if ( UUID.class.isAssignableFrom( type ) ) {
			return (X) PassThroughTransformer.INSTANCE.transform( value );
		}
		if ( String.class.isAssignableFrom( type ) ) {
			return (X) ToStringTransformer.INSTANCE.transform( value );
		}
		if ( byte[].class.isAssignableFrom( type ) ) {
			return (X) ToBytesTransformer.INSTANCE.transform( value );
		}
		throw unknownUnwrap( type );
	}

	public <X> UUID wrap(X value, WrapperOptions options) {
		if ( value == null ) {
			return null;
		}
		if (value instanceof UUID) {
			return PassThroughTransformer.INSTANCE.parse( value );
		}
		if (value instanceof String) {
			return ToStringTransformer.INSTANCE.parse( value );
		}
		if (value instanceof byte[]) {
			return ToBytesTransformer.INSTANCE.parse( value );
		}
		throw unknownWrap( value.getClass() );
	}

	public interface ValueTransformer {
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

	public static class NoDashesStringTransformer implements ValueTransformer {
		public static final NoDashesStringTransformer INSTANCE = new NoDashesStringTransformer();

		public String transform(UUID uuid) {
			final String stringForm = ToStringTransformer.INSTANCE.transform( uuid );
			return stringForm.substring( 0, 8 )
					+ stringForm.substring( 9, 13 )
					+ stringForm.substring( 14, 18 )
					+ stringForm.substring( 19, 23 )
					+ stringForm.substring( 24 );
		}

		public UUID parse(Object value) {
			final String stringValue = (String) value;
			final String uuidString = stringValue.substring( 0, 8 )
					+ "-"
					+ stringValue.substring( 8, 12 )
					+ "-"
					+ stringValue.substring( 12, 16 )
					+ "-"
					+ stringValue.substring( 16, 20 )
					+ "-"
					+ stringValue.substring( 20 );
			return UUID.fromString( uuidString );
		}
	}

	public static class ToBytesTransformer implements ValueTransformer {
		public static final ToBytesTransformer INSTANCE = new ToBytesTransformer();

		public byte[] transform(UUID uuid) {
			byte[] bytes = new byte[16];
			BytesHelper.fromLong( uuid.getMostSignificantBits(), bytes, 0);
			BytesHelper.fromLong( uuid.getLeastSignificantBits(), bytes, 8 );
			return bytes;
		}

		public UUID parse(Object value) {
			byte[] bytea = (byte[]) value;
			return new UUID( BytesHelper.asLong( bytea, 0 ), BytesHelper.asLong( bytea, 8 ) );
		}
	}

}
