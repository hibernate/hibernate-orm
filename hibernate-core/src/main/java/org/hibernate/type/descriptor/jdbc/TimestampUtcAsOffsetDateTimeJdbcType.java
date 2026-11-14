package org.hibernate.type.descriptor.jdbc;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.ValueBinder;
import org.hibernate.type.descriptor.ValueExtractor;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.internal.JdbcLiteralFormatterTemporal;
import org.hibernate.type.spi.TypeConfiguration;

import jakarta.persistence.TemporalType;

/**
 * Descriptor for {@link SqlTypes#TIMESTAMP_UTC TIMESTAMP_UTC} handling.
 *
 * @author Christian Beikov
 */
public class TimestampUtcAsOffsetDateTimeJdbcType implements JdbcType {

	public static final TimestampUtcAsOffsetDateTimeJdbcType INSTANCE = new TimestampUtcAsOffsetDateTimeJdbcType();

	public TimestampUtcAsOffsetDateTimeJdbcType() {
	}

	@Override
	public int getJdbcTypeCode() {
		return Types.TIMESTAMP_WITH_TIMEZONE;
	}

	@Override
	public int getDefaultSqlTypeCode() {
		return SqlTypes.TIMESTAMP_UTC;
	}

	@Override
	public String getFriendlyName() {
		return "TIMESTAMP_UTC";
	}

	@Override
	public String toString() {
		return "TimestampUtcDescriptor";
	}

	@Override
	public JavaType<?> getRecommendedJavaType(
			Integer length,
			Integer scale,
			TypeConfiguration typeConfiguration) {
		return typeConfiguration.getJavaTypeRegistry().resolveDescriptor( Instant.class );
	}

	@Override
	public Class<?> getPreferredJavaTypeClass(WrapperOptions options) {
		return OffsetDateTime.class;
	}

	@Override
	public <T> JdbcLiteralFormatter<T> getJdbcLiteralFormatter(JavaType<T> javaType) {
		return new JdbcLiteralFormatterTemporal<>( javaType, TemporalType.TIMESTAMP );
	}

	@Override
	public <X> ValueBinder<X> getBinder(final JavaType<X> javaType) {
		return new BasicBinder<>( javaType, this ) {
			@Override
			public Object getBindValue(X value, WrapperOptions options) throws SQLException {
				if ( !org.hibernate.type.descriptor.jdbc.internal.TemporalJdbcValueRanges.isNewHandling( options ) ) return super.getBindValue( value, options );
				if ( value == null ) return null;
				if ( value instanceof Instant ) org.hibernate.type.descriptor.jdbc.internal.TemporalJdbcValueRanges.check( value, options );
				final var dateTime = javaType.unwrap( value, OffsetDateTime.class, options );
				org.hibernate.type.descriptor.jdbc.internal.TemporalJdbcValueRanges.check( dateTime.toInstant(), options );
				return dateTime.withOffsetSameInstant( ZoneOffset.UTC );
			}
			@Override
			protected void doBind(
					PreparedStatement st,
					X value,
					int index,
					WrapperOptions wrapperOptions) throws SQLException {
				if ( value instanceof java.time.Instant ) org.hibernate.type.descriptor.jdbc.internal.TemporalJdbcValueRanges.check( value, wrapperOptions );
				final OffsetDateTime dateTime = javaType.unwrap( value, OffsetDateTime.class, wrapperOptions );
				org.hibernate.type.descriptor.jdbc.internal.TemporalJdbcValueRanges.check( dateTime.toInstant(), wrapperOptions );
				// supposed to be supported in JDBC 4.2
				st.setObject( index, dateTime.withOffsetSameInstant( ZoneOffset.UTC ), Types.TIMESTAMP_WITH_TIMEZONE );
			}

			@Override
			protected void doBind(
					CallableStatement st,
					X value,
					String name,
					WrapperOptions wrapperOptions)
					throws SQLException {
				if ( value instanceof java.time.Instant ) org.hibernate.type.descriptor.jdbc.internal.TemporalJdbcValueRanges.check( value, wrapperOptions );
				final OffsetDateTime dateTime = javaType.unwrap( value, OffsetDateTime.class, wrapperOptions );
				org.hibernate.type.descriptor.jdbc.internal.TemporalJdbcValueRanges.check( dateTime.toInstant(), wrapperOptions );
				// supposed to be supported in JDBC 4.2
				st.setObject( name, dateTime.withOffsetSameInstant( ZoneOffset.UTC ), Types.TIMESTAMP_WITH_TIMEZONE );
			}
		};
	}

	@Override
	public <X> ValueExtractor<X> getExtractor(final JavaType<X> javaType) {
		return new BasicExtractor<>( javaType, this ) {
			@Override
			protected X doExtract(ResultSet rs, int position, WrapperOptions wrapperOptions) throws SQLException {
				final var value = rs.getObject( position, OffsetDateTime.class );
				org.hibernate.type.descriptor.jdbc.internal.TemporalJdbcValueRanges.check( value, wrapperOptions );
				return javaType.wrap( value, wrapperOptions );
			}

			@Override
			protected X doExtract(CallableStatement statement, int position, WrapperOptions wrapperOptions) throws SQLException {
				final var value = statement.getObject( position, OffsetDateTime.class );
				org.hibernate.type.descriptor.jdbc.internal.TemporalJdbcValueRanges.check( value, wrapperOptions );
				return javaType.wrap( value, wrapperOptions );
			}

			@Override
			protected X doExtract(CallableStatement statement, String name, WrapperOptions wrapperOptions) throws SQLException {
				final var value = statement.getObject( name, OffsetDateTime.class );
				org.hibernate.type.descriptor.jdbc.internal.TemporalJdbcValueRanges.check( value, wrapperOptions );
				return javaType.wrap( value, wrapperOptions );
			}
		};
	}
}
