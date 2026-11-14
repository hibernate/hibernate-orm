package org.hibernate.dialect.temporaltype.internal;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;

import org.hibernate.dialect.Dialect;
import org.hibernate.annotations.TemporalPreservationPolicy.Type;
import org.hibernate.type.internal.TemporalPolicySettings.Selection;
import org.hibernate.type.internal.TemporalMappingValidator;
import org.hibernate.MappingException;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.dialect.OracleDialect;
import java.util.Set;
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
		return expression( dialect, javaType, jdbcType, Type.DOMAIN_VALUE );
	}

	public static String expression(Dialect dialect, JavaType<?> javaType, JdbcType jdbcType, Type policy) {
		// Custom result encodings require an explicit provider selection contribution.
		if ( !Set.of(
				"org.hibernate.type.descriptor.jdbc.LocalDateTimeJdbcType",
				"org.hibernate.type.descriptor.jdbc.InstantJdbcType",
				"org.hibernate.type.descriptor.jdbc.OffsetDateTimeJdbcType",
				"org.hibernate.type.descriptor.jdbc.TimestampUtcAsPlainJdbcTimestampJdbcType",
				"org.hibernate.type.descriptor.jdbc.TimestampUtcAsOffsetDateTimeJdbcType",
				"org.hibernate.dialect.type.internal.PostgreSQLTimestampUtcAsJdbcTimestampJdbcType",
				"org.hibernate.dialect.type.internal.PostgreSQLOffsetDateTimeJdbcType",
				"org.hibernate.dialect.type.internal.OracleOffsetDateTimeJdbcType",
				"org.hibernate.dialect.type.internal.OracleZonedDateTimeJdbcType"
		).contains( jdbcType.getClass().getName() ) ) {
			return null;
		}
		try {
			TemporalMappingValidator.validate(
					dialect, javaType, jdbcType, null, "database current temporal value", false,
					new Selection( policy, "version mapping", false ) );
		}
		catch (MappingException unsupported) {
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
		if ( dialect instanceof H2Dialect ) {
			// A cast of a zoned timestamp to plain timestamp reapplies the session zone.
			return "cast(formatdatetime(current_timestamp,'yyyy-MM-dd HH:mm:ss.SSSSSSSSS','en','UTC') as timestamp(9))";
		}
		if ( dialect instanceof PostgreSQLDialect ) {
			return "(current_timestamp at time zone 'UTC')";
		}
		return "sys_extract_utc(current_timestamp)";
	}

	public static CurrentTimestampSelection selection(Dialect dialect, JavaType<?> javaType, JdbcType jdbcType) {
		return selection( dialect, javaType, jdbcType, Type.DOMAIN_VALUE );
	}

	public static CurrentTimestampSelection selection(Dialect dialect, JavaType<?> javaType, JdbcType jdbcType, Type policy) {
		final String expression = expression( dialect, javaType, jdbcType, policy );
		return expression == null ? null : CurrentTimestampSelection.prepared(
				"select " + expression + (dialect instanceof OracleDialect ? " from dual" : "") );
	}
}
