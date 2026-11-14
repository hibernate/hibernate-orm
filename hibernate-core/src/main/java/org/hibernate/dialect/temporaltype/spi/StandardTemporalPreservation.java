package org.hibernate.dialect.temporaltype.spi;

import org.hibernate.Incubating;
import java.time.*;

import org.hibernate.type.descriptor.java.*;
import org.hibernate.type.descriptor.jdbc.*;

import static org.hibernate.dialect.temporaltype.spi.TemporalPreservation.Level.*;

/// Conservative facts for exact built-in descriptor implementations. Platform
/// assessments may supplement these facts without recognizing custom subclasses.
///
/// @since 8.1
/// @author Steve Ebersole
@Incubating(since = "8.1")
public final class StandardTemporalPreservation {
	private StandardTemporalPreservation() {
	}

	/// Shared facts for established UTC instant carriers, with adjustment owned by the mapping.
	public static TemporalPreservation instantOnly(JavaType<?> javaType) {
		if ( javaType.getClass() == InstantJavaType.class ) {
			return new TemporalPreservation( IRRELEVANT, IRRELEVANT, SUPPORTED, IRRELEVANT, IRRELEVANT );
		}
		if ( javaType.getClass() == OffsetDateTimeJavaType.class ) {
			return new TemporalPreservation( UNSUPPORTED, UNSUPPORTED, SUPPORTED, UNSUPPORTED, IRRELEVANT );
		}
		if ( javaType.getClass() == ZonedDateTimeJavaType.class ) {
			return new TemporalPreservation( UNSUPPORTED, UNSUPPORTED, SUPPORTED, UNSUPPORTED, UNSUPPORTED );
		}
		return TemporalPreservation.UNKNOWN;
	}

	public static TemporalPreservation assess(
			JavaType<?> javaType, JdbcType jdbcType, org.hibernate.dialect.type.spi.DirectJavaTimeJdbcSupport directAccess) {
		final Class<?> java = javaType.getClass();
		if ( jdbcType.getClass() == org.hibernate.type.descriptor.jdbc.internal.UtcTimestampJdbcType.class ) {
			final var delegate = ((org.hibernate.type.descriptor.jdbc.internal.UtcTimestampJdbcType) jdbcType).getDelegate().getClass();
			if ( delegate == TimestampUtcAsPlainJdbcTimestampJdbcType.class || delegate == TimestampUtcAsOffsetDateTimeJdbcType.class ) {
				if ( java == OffsetDateTimeJavaType.class ) {
					return new TemporalPreservation( UNSUPPORTED, UNSUPPORTED, SUPPORTED, UNSUPPORTED, IRRELEVANT );
				}
				if ( java == ZonedDateTimeJavaType.class ) {
					return new TemporalPreservation( UNSUPPORTED, UNSUPPORTED, SUPPORTED, UNSUPPORTED, UNSUPPORTED );
				}
			}
		}

		if ( java == OffsetTimeJavaType.class
				&& jdbcType instanceof org.hibernate.type.descriptor.jdbc.internal.UtcTimeJdbcType utc
				&& utc.getDelegate().getClass() == org.hibernate.dialect.type.internal.PostgreSQLUtcTimeJdbcType.class ) {
			return new TemporalPreservation( IRRELEVANT, SUPPORTED, IRRELEVANT, UNSUPPORTED, IRRELEVANT, SUPPORTED );
		}
		final Class<?> jdbc = jdbcType.getClass();
		if ( java == LocalDateJavaType.class && jdbc == LocalDateJdbcType.class && directAccess.supports( LocalDate.class ) ) {
			return new TemporalPreservation( SUPPORTED, IRRELEVANT, IRRELEVANT, IRRELEVANT, IRRELEVANT );
		}
		if ( java == LocalTimeJavaType.class && jdbc == LocalTimeJdbcType.class && directAccess.supports( LocalTime.class ) ) {
			return new TemporalPreservation( IRRELEVANT, SUPPORTED, IRRELEVANT, IRRELEVANT, IRRELEVANT );
		}
		if ( java == LocalDateTimeJavaType.class && jdbc == LocalDateTimeJdbcType.class && directAccess.supports( LocalDateTime.class ) ) {
			return new TemporalPreservation( SUPPORTED, SUPPORTED, IRRELEVANT, IRRELEVANT, IRRELEVANT );
		}
		if ( java == InstantJavaType.class
				&& (jdbc == TimestampUtcAsPlainJdbcTimestampJdbcType.class || jdbc == TimestampUtcAsOffsetDateTimeJdbcType.class) ) {
			return new TemporalPreservation( IRRELEVANT, IRRELEVANT, SUPPORTED, IRRELEVANT, IRRELEVANT );
		}
		if ( java == ZoneOffsetJavaType.class && jdbc == IntegerJdbcType.class ) {
			return new TemporalPreservation( IRRELEVANT, IRRELEVANT, IRRELEVANT, SUPPORTED, IRRELEVANT );
		}
		final Class<?> domain = javaType.getJavaTypeClass();
		if ( domain == LocalDate.class ) {
			return new TemporalPreservation( UNKNOWN, IRRELEVANT, IRRELEVANT, IRRELEVANT, IRRELEVANT );
		}
		if ( domain == LocalTime.class ) {
			return new TemporalPreservation( IRRELEVANT, UNKNOWN, IRRELEVANT, IRRELEVANT, IRRELEVANT );
		}
		if ( domain == LocalDateTime.class ) {
			return new TemporalPreservation( UNKNOWN, UNKNOWN, IRRELEVANT, IRRELEVANT, IRRELEVANT );
		}
		if ( domain == Instant.class ) {
			return new TemporalPreservation( IRRELEVANT, IRRELEVANT, UNKNOWN, IRRELEVANT, IRRELEVANT );
		}
		if ( domain == OffsetTime.class ) {
			return new TemporalPreservation( IRRELEVANT, UNKNOWN, IRRELEVANT, UNKNOWN, IRRELEVANT );
		}
		if ( domain == OffsetDateTime.class ) {
			return new TemporalPreservation( UNKNOWN, UNKNOWN, UNKNOWN, UNKNOWN, IRRELEVANT );
		}
		return TemporalPreservation.UNKNOWN;
	}
}
