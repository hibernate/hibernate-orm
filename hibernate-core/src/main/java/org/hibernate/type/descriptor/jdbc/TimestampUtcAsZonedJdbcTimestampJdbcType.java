package org.hibernate.type.descriptor.jdbc;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.Instant;

import org.hibernate.type.descriptor.ValueBinder;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaType;

/// Direct-enabled java.sql fallback for a zoned timestamp column. Drivers must
/// support explicit zoned Timestamp binding; a Dialect must establish that support.
/// The legacy UTC-Calendar descriptor remains unchanged.
///
/// @author Steve Ebersole
/// @since 8.0
public class TimestampUtcAsZonedJdbcTimestampJdbcType extends TimestampUtcAsJdbcTimestampJdbcType {
	public static final TimestampUtcAsZonedJdbcTimestampJdbcType INSTANCE = new TimestampUtcAsZonedJdbcTimestampJdbcType();

	@Override
	public <T> org.hibernate.type.descriptor.jdbc.JdbcLiteralFormatter<T> getJdbcLiteralFormatter(
			org.hibernate.type.descriptor.java.JavaType<T> javaType) {
		return (appender, value, dialect, options) -> dialect.appendDateTimeLiteral(
				appender, javaType.unwrap( value, java.time.Instant.class, options ).atOffset( java.time.ZoneOffset.UTC ),
				jakarta.persistence.TemporalType.TIMESTAMP, java.util.TimeZone.getTimeZone( "UTC" ) );
	}

	@Override
	public int getJdbcTypeCode() {
		return Types.TIMESTAMP_WITH_TIMEZONE;
	}

	@Override
	public int getDdlTypeCode() {
		return Types.TIMESTAMP_WITH_TIMEZONE;
	}

	protected void bindTimestamp(PreparedStatement statement, Timestamp value, int index) throws SQLException {
		statement.setObject( index, value, Types.TIMESTAMP_WITH_TIMEZONE );
	}

	protected void bindTimestamp(CallableStatement statement, Timestamp value, String name) throws SQLException {
		statement.setObject( name, value, Types.TIMESTAMP_WITH_TIMEZONE );
	}

	@Override
	public <T> ValueBinder<T> getBinder(JavaType<T> javaType) {
		return new BasicBinder<>( javaType, this ) {
			@Override
			public Object getBindValue(T value, WrapperOptions options) throws SQLException {
				if ( value != null ) org.hibernate.type.descriptor.jdbc.internal.TemporalJdbcValueRanges.checkTimestamp( javaType.unwrap( value, Instant.class, options ), options );
				return super.getBindValue( value, options );
			}
			@Override
			protected void doBind(PreparedStatement statement, T value, int index, WrapperOptions options) throws SQLException {
				final var instant = javaType.unwrap( value, Instant.class, options );
				org.hibernate.type.descriptor.jdbc.internal.TemporalJdbcValueRanges.checkTimestamp( instant, options );
				bindTimestamp( statement, Timestamp.from( instant ), index );
			}

			@Override
			protected void doBind(CallableStatement statement, T value, String name, WrapperOptions options) throws SQLException {
				final var instant = javaType.unwrap( value, Instant.class, options );
				org.hibernate.type.descriptor.jdbc.internal.TemporalJdbcValueRanges.checkTimestamp( instant, options );
				bindTimestamp( statement, Timestamp.from( instant ), name );
			}
		};
	}
	@Override
	public <T> org.hibernate.type.descriptor.ValueExtractor<T> getExtractor(JavaType<T> javaType) {
		return org.hibernate.type.descriptor.jdbc.internal.TemporalJdbcValueRanges.instantExtractor( javaType, super.getExtractor( javaType ) );
	}

}
