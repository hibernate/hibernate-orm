package org.hibernate.dialect.type.internal;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;

import org.hibernate.Internal;
import org.hibernate.dialect.type.spi.TemporalPreservation;
import org.hibernate.dialect.type.spi.TemporalPreservation.InstantBasis;
import org.hibernate.type.descriptor.java.InstantJavaType;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.java.LocalDateJavaType;
import org.hibernate.type.descriptor.java.LocalDateTimeJavaType;
import org.hibernate.type.descriptor.java.LocalTimeJavaType;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.LocalDateJdbcType;
import org.hibernate.type.descriptor.jdbc.LocalDateTimeJdbcType;
import org.hibernate.type.descriptor.jdbc.LocalTimeJdbcType;

/// Building blocks for the [TemporalPreservation] reports of the stock
/// Dialects. Every report is matched on the exact classes of the Java type
/// and the JDBC type.
///
/// The ranges are deliberately narrower than what the databases can store.
/// Dates before the common era are excluded because Hibernate renders their
/// SQL literals incorrectly, and years beyond 9999 because the literal
/// format is no longer unambiguous. Dates before the Gregorian calendar took
/// effect are excluded where the database or the JDBC driver does not use
/// the proleptic Gregorian calendar, which is the calendar of Java Time.
///
/// @since 8.1
/// @author Steve Ebersole
@Internal
public final class StandardTemporalPreservations {
	/// The first day of the common era.
	public static final LocalDate COMMON_ERA_START = LocalDate.of( 1, 1, 1 );
	/// The first day of the Gregorian calendar.
	public static final LocalDate GREGORIAN_START = LocalDate.of( 1582, 10, 15 );
	/// The first day from which a value requested from a JDBC driver as a
	/// [java.sql.Timestamp] can be trusted when the database uses the proleptic
	/// Gregorian calendar. Such a driver reads the ten days up to 1582-10-14
	/// as the ten days from 1582-10-15, so only a value read as this day or
	/// later is known not to be one of those.
	public static final LocalDate TIMESTAMP_READ_SAFE_START = LocalDate.of( 1582, 10, 25 );
	/// The last day with a four-digit year.
	public static final LocalDate FOUR_DIGIT_YEAR_END = LocalDate.of( 9999, 12, 31 );

	private StandardTemporalPreservations() {
	}

	/// Whether the descriptors are exactly of the given classes.
	public static boolean isPair(
			JavaType<?> javaType,
			JdbcType jdbcType,
			Class<?> javaTypeClass,
			Class<?> jdbcTypeClass) {
		return javaType.getClass() == javaTypeClass && jdbcType.getClass() == jdbcTypeClass;
	}

	/// Report for the Java Time classes without zone or offset which are
	/// passed to and requested from the JDBC driver directly, with dates
	/// preserved from the given first day through the year 9999. Answers
	/// [TemporalPreservation#unknown()] for anything else.
	public static TemporalPreservation directLocal(JavaType<?> javaType, JdbcType jdbcType, LocalDate firstDay) {
		if ( isPair( javaType, jdbcType, LocalDateJavaType.class, LocalDateJdbcType.class ) ) {
			return TemporalPreservation.ofDate( firstDay, FOUR_DIGIT_YEAR_END );
		}
		else if ( isPair( javaType, jdbcType, LocalDateTimeJavaType.class, LocalDateTimeJdbcType.class ) ) {
			return TemporalPreservation.ofDateTime( firstDay.atStartOfDay(), lastMoment() );
		}
		else {
			return TemporalPreservation.unknown();
		}
	}

	/// Report for a [LocalTime] passed to and requested from the JDBC driver
	/// directly. Answers [TemporalPreservation#unknown()] for anything else.
	public static TemporalPreservation directLocalTime(JavaType<?> javaType, JdbcType jdbcType) {
		return isPair( javaType, jdbcType, LocalTimeJavaType.class, LocalTimeJdbcType.class )
				? TemporalPreservation.ofTime()
				: TemporalPreservation.unknown();
	}

	/// Report for an [Instant] stored through exactly the given JDBC type
	/// class, preserved from the start of the given first day in UTC through
	/// the year 9999. Answers [TemporalPreservation#unknown()] for anything else.
	public static TemporalPreservation instant(
			JavaType<?> javaType,
			JdbcType jdbcType,
			Class<? extends JdbcType> jdbcTypeClass,
			InstantBasis basis,
			LocalDate firstDay) {
		return isPair( javaType, jdbcType, InstantJavaType.class, jdbcTypeClass )
				? TemporalPreservation.ofInstant(
						basis,
						firstDay.atStartOfDay().toInstant( ZoneOffset.UTC ),
						lastMoment().toInstant( ZoneOffset.UTC )
				)
				: TemporalPreservation.unknown();
	}

	private static LocalDateTime lastMoment() {
		return LocalDateTime.of( FOUR_DIGIT_YEAR_END, LocalTime.MAX );
	}
}
