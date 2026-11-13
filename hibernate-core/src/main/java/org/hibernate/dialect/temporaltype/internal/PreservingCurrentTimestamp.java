package org.hibernate.dialect.temporaltype.internal;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;

import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.temporaltype.spi.CurrentTimestampSelection;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.JdbcType;

/// SQL result encoding for the stock H2, PostgreSQL and Oracle preserving profiles.
/// JDBC execution belongs to the supplied descriptor, not this expression selector.
///
/// @author Steve Ebersole
public final class PreservingCurrentTimestamp {
	private PreservingCurrentTimestamp() {}

	public static String expression(Dialect dialect, JavaType<?> javaType, JdbcType jdbcType) {
		// Custom result encodings require an explicit provider selection contribution.
		if ( !java.util.Set.of(
				"org.hibernate.type.descriptor.jdbc.LocalDateTimeJdbcType",
				"org.hibernate.type.descriptor.jdbc.InstantJdbcType",
				"org.hibernate.type.descriptor.jdbc.OffsetDateTimeJdbcType",
				"org.hibernate.type.descriptor.jdbc.TimestampUtcAsPlainJdbcTimestampJdbcType",
				"org.hibernate.type.descriptor.jdbc.TimestampUtcAsOffsetDateTimeJdbcType",
				"org.hibernate.dialect.type.internal.PostgreSQLTimestampUtcAsJdbcTimestampJdbcType",
				"org.hibernate.dialect.type.internal.OracleOffsetDateTimeJdbcType",
				"org.hibernate.dialect.type.internal.OracleZonedDateTimeJdbcType"
		).contains( jdbcType.getClass().getName() ) ) {
			return null;
		}
		try {
			org.hibernate.type.internal.TemporalMappingValidator.validate(
					dialect, javaType, jdbcType, null, "database current temporal value" );
		}
		catch (org.hibernate.MappingException unsupported) {
			return null;
		}
		final var type = javaType.getJavaTypeClass();
		if ( type == LocalDateTime.class ) {
			return "localtimestamp";
		}
		if ( type == OffsetDateTime.class || type == ZonedDateTime.class ) {
			return "current_timestamp";
		}
		if ( type != Instant.class ) {
			return null;
		}
		if ( jdbcType.getDdlTypeCode() != SqlTypes.TIMESTAMP ) {
			return "current_timestamp";
		}
		if ( dialect instanceof org.hibernate.dialect.H2Dialect ) {
			// A cast of a zoned timestamp to plain timestamp reapplies the session zone.
			return "cast(formatdatetime(current_timestamp,'yyyy-MM-dd HH:mm:ss.SSSSSSSSS','en','UTC') as timestamp(9))";
		}
		if ( dialect instanceof org.hibernate.dialect.PostgreSQLDialect ) {
			return "(current_timestamp at time zone 'UTC')";
		}
		return "sys_extract_utc(current_timestamp)";
	}

	public static CurrentTimestampSelection selection(Dialect dialect, JavaType<?> javaType, JdbcType jdbcType) {
		final String expression = expression( dialect, javaType, jdbcType );
		return expression == null ? null : CurrentTimestampSelection.prepared(
				"select " + expression + (dialect instanceof org.hibernate.dialect.OracleDialect ? " from dual" : "") );
	}
}
