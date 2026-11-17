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
import org.hibernate.type.descriptor.java.OffsetDateTimeJavaType;
import org.hibernate.type.descriptor.java.OffsetTimeJavaType;
import org.hibernate.type.descriptor.java.ZonedDateTimeJavaType;
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
	/// The first day of the range which MySQL and MariaDB document as
	/// supported for their DATE and DATETIME types. Earlier dates have been
	/// seen to be stored and returned correctly, and are documented as
	/// working without any guarantee.
	public static final LocalDate MYSQL_SUPPORTED_START = LocalDate.of( 1000, 1, 1 );
	/// The first day from which a value requested from a JDBC driver as a
	/// [java.sql.Timestamp] can be trusted when the database uses the proleptic
	/// Gregorian calendar. Such a driver reads a stored date before 1582-10-15
	/// as the date ten days later, and decides that by the date as seen in the
	/// time zone of the database session, which may be most of a day away from
	/// the date in UTC. A value read as the first day of 1583 or later cannot
	/// be one of those, whatever the session time zone.
	public static final LocalDate TIMESTAMP_READ_SAFE_START = LocalDate.of( 1583, 1, 1 );
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

	/// Report for an [java.time.OffsetDateTime] or [java.time.ZonedDateTime]
	/// stored through exactly the given JDBC type class in a column which
	/// itself denotes an instant, preserved from the start of the given first
	/// day in UTC through the year 9999. The identity of a region-based zone
	/// is reported as lost, since no assessed column keeps it. Answers
	/// [TemporalPreservation#unknown()] for anything else.
	///
	/// @param offset whether the offset is preserved as well as the instant
	public static TemporalPreservation offsetOrZoned(
			JavaType<?> javaType,
			JdbcType jdbcType,
			Class<? extends JdbcType> jdbcTypeClass,
			TemporalPreservation.Level offset,
			LocalDate firstDay) {
		return offsetOrZoned( javaType, jdbcType, jdbcTypeClass, offset, InstantBasis.NATIVE, firstDay );
	}

	/// As [#offsetOrZoned(JavaType, JdbcType, Class, TemporalPreservation.Level, LocalDate)],
	/// for a column which denotes an instant in the given way.
	public static TemporalPreservation offsetOrZoned(
			JavaType<?> javaType,
			JdbcType jdbcType,
			Class<? extends JdbcType> jdbcTypeClass,
			TemporalPreservation.Level offset,
			InstantBasis basis,
			LocalDate firstDay) {
		final boolean zoned = javaType.getClass() == ZonedDateTimeJavaType.class;
		if ( jdbcType.getClass() != jdbcTypeClass
				|| !zoned && javaType.getClass() != OffsetDateTimeJavaType.class ) {
			return TemporalPreservation.unknown();
		}
		return new TemporalPreservation(
				TemporalPreservation.Level.UNKNOWN,
				TemporalPreservation.Level.UNKNOWN,
				TemporalPreservation.Level.SUPPORTED,
				offset,
				zoned ? TemporalPreservation.Level.UNSUPPORTED : TemporalPreservation.Level.UNKNOWN,
				basis,
				firstDay.atStartOfDay().toInstant( ZoneOffset.UTC ),
				lastMoment().toInstant( ZoneOffset.UTC )
		);
	}

	/// Report for an [java.time.OffsetTime] passed to and requested from the
	/// JDBC driver directly, through exactly the given JDBC type class. The
	/// time of day, as expressed at UTC, is preserved for every value.
	/// Answers [TemporalPreservation#unknown()] for anything else.
	///
	/// @param offset whether the offset is preserved as well
	public static TemporalPreservation offsetTime(
			JavaType<?> javaType,
			JdbcType jdbcType,
			Class<? extends JdbcType> jdbcTypeClass,
			TemporalPreservation.Level offset) {
		return isPair( javaType, jdbcType, OffsetTimeJavaType.class, jdbcTypeClass )
				? new TemporalPreservation(
						TemporalPreservation.Level.UNKNOWN,
						TemporalPreservation.Level.SUPPORTED,
						TemporalPreservation.Level.UNKNOWN,
						offset,
						TemporalPreservation.Level.UNKNOWN,
						null,
						null,
						null
				)
				: TemporalPreservation.unknown();
	}

	private static LocalDateTime lastMoment() {
		return LocalDateTime.of( FOUR_DIGIT_YEAR_END, LocalTime.MAX );
	}
}
