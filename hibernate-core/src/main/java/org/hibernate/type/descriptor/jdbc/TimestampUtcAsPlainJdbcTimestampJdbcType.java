package org.hibernate.type.descriptor.jdbc;

import java.sql.Types;

/// Direct-enabled java.sql fallback using UTC clock fields in a zone-less column.
/// Explicit DDL identity prevents TIMESTAMP_UTC's dialect default from choosing
/// a zoned column for the UTC-Calendar binding inherited here.
///
/// @author Steve Ebersole
/// @since 8.1
public final class TimestampUtcAsPlainJdbcTimestampJdbcType extends TimestampUtcAsJdbcTimestampJdbcType {
	public static final TimestampUtcAsPlainJdbcTimestampJdbcType INSTANCE = new TimestampUtcAsPlainJdbcTimestampJdbcType();

	@Override
	public <T> org.hibernate.type.descriptor.jdbc.JdbcLiteralFormatter<T> getJdbcLiteralFormatter(
			org.hibernate.type.descriptor.java.JavaType<T> javaType) {
		return (appender, value, dialect, options) -> dialect.appendDateTimeLiteral(
				appender, javaType.unwrap( value, java.time.Instant.class, options ).atOffset( java.time.ZoneOffset.UTC ).toLocalDateTime(),
				jakarta.persistence.TemporalType.TIMESTAMP, java.util.TimeZone.getTimeZone( "UTC" ) );
	}

	@Override
	public <T> org.hibernate.type.descriptor.ValueBinder<T> getBinder(org.hibernate.type.descriptor.java.JavaType<T> javaType) {
		return org.hibernate.type.descriptor.jdbc.internal.TemporalJdbcValueRanges.instantBinder( javaType, super.getBinder( javaType ) );
	}

	@Override
	public <T> org.hibernate.type.descriptor.ValueExtractor<T> getExtractor(org.hibernate.type.descriptor.java.JavaType<T> javaType) {
		return org.hibernate.type.descriptor.jdbc.internal.TemporalJdbcValueRanges.instantExtractor( javaType, super.getExtractor( javaType ) );
	}

	@Override
	public int getDdlTypeCode() {
		return Types.TIMESTAMP;
	}
}
