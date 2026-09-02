/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.literal.spi;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAccessor;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import jakarta.persistence.TemporalType;

import org.hibernate.Incubating;
import org.hibernate.SPI;
import org.hibernate.sql.spi.SqlAppender;
import org.hibernate.type.descriptor.DateTimeUtils;

import static java.util.Objects.requireNonNull;
import static org.hibernate.SPI.Role.USE;

/// Appends the standard datetime body used while composing a SQL literal.
///
/// Call these operations from an implementation of
/// [LiteralSupport#appendDateTimeLiteral] and append the database-specific
/// introducer, quotes, escape delimiters, cast, or other surrounding syntax
/// separately. Each operation appends immediately; do not retain the appender
/// or supplied value.
///
/// @author Steve Ebersole
/// @since 8.0
/// @see LiteralSupport#appendDateTimeLiteral(SqlAppender, TemporalAccessor, TemporalType, TimeZone)
/// @see LiteralSupport#appendDateTimeLiteral(SqlAppender, Date, TemporalType, TimeZone)
/// @see LiteralSupport#appendDateTimeLiteral(SqlAppender, Calendar, TemporalType, TimeZone)
@Incubating
@SPI(USE)
public final class StandardDateTimeLiteralRendering {
	private static final DateTimeFormatter TIME_WITH_NUMERIC_OFFSET =
			DateTimeFormatter.ofPattern( "HH:mm:ssxxx", Locale.ENGLISH );
	private static final DateTimeFormatter TIMESTAMP_WITH_MILLIS_AND_NUMERIC_OFFSET =
			DateTimeFormatter.ofPattern( "yyyy-MM-dd HH:mm:ss.SSSxxx", Locale.ENGLISH );
	private static final DateTimeFormatter TIMESTAMP_WITH_MICROS_AND_NUMERIC_OFFSET =
			DateTimeFormatter.ofPattern( "yyyy-MM-dd HH:mm:ss.SSSSSSxxx", Locale.ENGLISH );
	private static final DateTimeFormatter TIMESTAMP_WITH_NANOS_AND_NUMERIC_OFFSET =
			DateTimeFormatter.ofPattern( "yyyy-MM-dd HH:mm:ss.SSSSSSSSSxxx", Locale.ENGLISH );
	private static final DateTimeFormatter TIMESTAMP_WITH_NANOS =
			DateTimeFormatter.ofPattern( "yyyy-MM-dd HH:mm:ss.SSSSSSSSS", Locale.ENGLISH );

	private StandardDateTimeLiteralRendering() {
	}

	/// Append a date from a `java.time` value without surrounding literal syntax.
	public static void appendAsDate(SqlAppender appender, TemporalAccessor temporalAccessor) {
		DateTimeUtils.appendAsDate( required( appender ), requireNonNull( temporalAccessor, "temporalAccessor" ) );
	}

	/// Append a date from a legacy date value without surrounding literal syntax.
	public static void appendAsDate(SqlAppender appender, Date date) {
		DateTimeUtils.appendAsDate( required( appender ), requireNonNull( date, "date" ) );
	}

	/// Append a date from a calendar without surrounding literal syntax.
	public static void appendAsDate(SqlAppender appender, Calendar calendar) {
		DateTimeUtils.appendAsDate( required( appender ), requireNonNull( calendar, "calendar" ) );
	}

	/// Append a local time from a `java.time` value without an offset.
	public static void appendAsLocalTime(SqlAppender appender, TemporalAccessor temporalAccessor) {
		DateTimeUtils.appendAsLocalTime( required( appender ), requireNonNull( temporalAccessor, "temporalAccessor" ) );
	}

	/// Append a local time from a legacy date value without an offset.
	public static void appendAsLocalTime(SqlAppender appender, Date date) {
		DateTimeUtils.appendAsLocalTime( required( appender ), requireNonNull( date, "date" ) );
	}

	/// Append a local time from a calendar without an offset.
	public static void appendAsLocalTime(SqlAppender appender, Calendar calendar) {
		DateTimeUtils.appendAsLocalTime( required( appender ), requireNonNull( calendar, "calendar" ) );
	}

	/// Append a time, retaining an available offset only when the database supports it.
	public static void appendAsTime(
			SqlAppender appender,
			TemporalAccessor temporalAccessor,
			boolean supportsOffset,
			TimeZone jdbcTimeZone) {
		appendAsTime(
				appender,
				temporalAccessor,
				supportsOffset,
				jdbcTimeZone,
				ZeroOffsetLiteralStyle.UTC_DESIGNATOR
		);
	}

	/// Append a time using the requested representation for a rendered zero offset.
	public static void appendAsTime(
			SqlAppender appender,
			TemporalAccessor temporalAccessor,
			boolean supportsOffset,
			TimeZone jdbcTimeZone,
			ZeroOffsetLiteralStyle zeroOffsetStyle) {
		required( appender );
		requireNonNull( temporalAccessor, "temporalAccessor" );
		requireNonNull( jdbcTimeZone, "jdbcTimeZone" );
		requireNonNull( zeroOffsetStyle, "zeroOffsetStyle" );
		if ( supportsOffset
				&& zeroOffsetStyle == ZeroOffsetLiteralStyle.NUMERIC_OFFSET
				&& temporalAccessor.isSupported( ChronoField.OFFSET_SECONDS ) ) {
			TIME_WITH_NUMERIC_OFFSET.formatTo( temporalAccessor, appender );
		}
		else {
			DateTimeUtils.appendAsTime( appender, temporalAccessor, supportsOffset, jdbcTimeZone );
		}
	}

	/// Append an offset-bearing time from a legacy date using the JDBC timezone.
	public static void appendAsTime(SqlAppender appender, Date date, TimeZone jdbcTimeZone) {
		DateTimeUtils.appendAsTime(
				required( appender ),
				requireNonNull( date, "date" ),
				requireNonNull( jdbcTimeZone, "jdbcTimeZone" )
		);
	}

	/// Append an offset-bearing time from a calendar using the JDBC timezone.
	public static void appendAsTime(SqlAppender appender, Calendar calendar, TimeZone jdbcTimeZone) {
		DateTimeUtils.appendAsTime(
				required( appender ),
				requireNonNull( calendar, "calendar" ),
				requireNonNull( jdbcTimeZone, "jdbcTimeZone" )
		);
	}

	/// Append a timestamp with three fractional digits.
	public static void appendAsTimestampWithMillis(
			SqlAppender appender,
			TemporalAccessor temporalAccessor,
			boolean supportsOffset,
			TimeZone jdbcTimeZone) {
		appendAsTimestampWithMillis(
				appender,
				temporalAccessor,
				supportsOffset,
				jdbcTimeZone,
				ZeroOffsetLiteralStyle.UTC_DESIGNATOR
		);
	}

	/// Append a timestamp with three fractional digits and the requested zero-offset representation.
	public static void appendAsTimestampWithMillis(
			SqlAppender appender,
			TemporalAccessor temporalAccessor,
			boolean supportsOffset,
			TimeZone jdbcTimeZone,
			ZeroOffsetLiteralStyle zeroOffsetStyle) {
		appendAsTimestamp(
				appender,
				temporalAccessor,
				supportsOffset,
				jdbcTimeZone,
				zeroOffsetStyle,
				DateTimeUtils.DATE_TIME_FORMATTER_TIMESTAMP_WITH_MILLIS,
				TIMESTAMP_WITH_MILLIS_AND_NUMERIC_OFFSET,
				DateTimeUtils::appendAsTimestampWithMillis
		);
	}

	/// Append a timestamp with millisecond precision from a legacy date value.
	public static void appendAsTimestampWithMillis(SqlAppender appender, Date date, TimeZone jdbcTimeZone) {
		DateTimeUtils.appendAsTimestampWithMillis(
				required( appender ),
				requireNonNull( date, "date" ),
				requireNonNull( jdbcTimeZone, "jdbcTimeZone" )
		);
	}

	/// Append a timestamp with millisecond precision from a calendar.
	public static void appendAsTimestampWithMillis(SqlAppender appender, Calendar calendar, TimeZone jdbcTimeZone) {
		DateTimeUtils.appendAsTimestampWithMillis(
				required( appender ),
				requireNonNull( calendar, "calendar" ),
				requireNonNull( jdbcTimeZone, "jdbcTimeZone" )
		);
	}

	/// Append a timestamp with six fractional digits.
	public static void appendAsTimestampWithMicros(
			SqlAppender appender,
			TemporalAccessor temporalAccessor,
			boolean supportsOffset,
			TimeZone jdbcTimeZone) {
		appendAsTimestampWithMicros(
				appender,
				temporalAccessor,
				supportsOffset,
				jdbcTimeZone,
				ZeroOffsetLiteralStyle.UTC_DESIGNATOR
		);
	}

	/// Append a timestamp with six fractional digits and the requested zero-offset representation.
	public static void appendAsTimestampWithMicros(
			SqlAppender appender,
			TemporalAccessor temporalAccessor,
			boolean supportsOffset,
			TimeZone jdbcTimeZone,
			ZeroOffsetLiteralStyle zeroOffsetStyle) {
		appendAsTimestamp(
				appender,
				temporalAccessor,
				supportsOffset,
				jdbcTimeZone,
				zeroOffsetStyle,
				DateTimeUtils.DATE_TIME_FORMATTER_TIMESTAMP_WITH_MICROS,
				TIMESTAMP_WITH_MICROS_AND_NUMERIC_OFFSET,
				DateTimeUtils::appendAsTimestampWithMicros
		);
	}

	/// Append a timestamp with microsecond precision from a legacy date value.
	public static void appendAsTimestampWithMicros(SqlAppender appender, Date date, TimeZone jdbcTimeZone) {
		DateTimeUtils.appendAsTimestampWithMicros(
				required( appender ),
				requireNonNull( date, "date" ),
				requireNonNull( jdbcTimeZone, "jdbcTimeZone" )
		);
	}

	/// Append a timestamp with nine fractional digits.
	public static void appendAsTimestampWithNanos(
			SqlAppender appender,
			TemporalAccessor temporalAccessor,
			boolean supportsOffset,
			TimeZone jdbcTimeZone) {
		appendAsTimestampWithNanos(
				appender,
				temporalAccessor,
				supportsOffset,
				jdbcTimeZone,
				ZeroOffsetLiteralStyle.UTC_DESIGNATOR
		);
	}

	/// Append a timestamp with nine fractional digits and the requested zero-offset representation.
	public static void appendAsTimestampWithNanos(
			SqlAppender appender,
			TemporalAccessor temporalAccessor,
			boolean supportsOffset,
			TimeZone jdbcTimeZone,
			ZeroOffsetLiteralStyle zeroOffsetStyle) {
		appendAsTimestamp(
				appender,
				temporalAccessor,
				supportsOffset,
				jdbcTimeZone,
				zeroOffsetStyle,
				TIMESTAMP_WITH_NANOS,
				TIMESTAMP_WITH_NANOS_AND_NUMERIC_OFFSET,
				DateTimeUtils::appendAsTimestampWithNanos
		);
	}

	/// Append a timestamp with nanosecond precision from a legacy date value.
	public static void appendAsTimestampWithNanos(SqlAppender appender, Date date, TimeZone jdbcTimeZone) {
		DateTimeUtils.appendAsTimestampWithNanos(
				required( appender ),
				requireNonNull( date, "date" ),
				requireNonNull( jdbcTimeZone, "jdbcTimeZone" )
		);
	}

	private static void appendAsTimestamp(
			SqlAppender appender,
			TemporalAccessor temporalAccessor,
			boolean supportsOffset,
			TimeZone jdbcTimeZone,
			ZeroOffsetLiteralStyle zeroOffsetStyle,
			DateTimeFormatter localFormatter,
			DateTimeFormatter numericOffsetFormatter,
			StandardTimestampRenderer standardRenderer) {
		required( appender );
		requireNonNull( temporalAccessor, "temporalAccessor" );
		requireNonNull( jdbcTimeZone, "jdbcTimeZone" );
		requireNonNull( zeroOffsetStyle, "zeroOffsetStyle" );
		if ( zeroOffsetStyle == ZeroOffsetLiteralStyle.UTC_DESIGNATOR ) {
			standardRenderer.append( appender, temporalAccessor, supportsOffset, jdbcTimeZone );
		}
		else {
			appendAsTimestamp(
					appender,
					temporalAccessor,
					supportsOffset,
					jdbcTimeZone,
					localFormatter,
					numericOffsetFormatter
			);
		}
	}

	private static void appendAsTimestamp(
			SqlAppender appender,
			TemporalAccessor temporalAccessor,
			boolean supportsOffset,
			TimeZone jdbcTimeZone,
			DateTimeFormatter localFormatter,
			DateTimeFormatter offsetFormatter) {
		if ( temporalAccessor.isSupported( ChronoField.OFFSET_SECONDS ) ) {
			if ( supportsOffset ) {
				offsetFormatter.formatTo( temporalAccessor, appender );
			}
			else {
				localFormatter.formatTo(
						LocalDateTime.ofInstant( Instant.from( temporalAccessor ), jdbcTimeZone.toZoneId() ),
						appender
				);
			}
		}
		else if ( temporalAccessor instanceof Instant instant ) {
			if ( supportsOffset ) {
				offsetFormatter.formatTo( instant.atZone( jdbcTimeZone.toZoneId() ), appender );
			}
			else {
				localFormatter.formatTo( LocalDateTime.ofInstant( instant, jdbcTimeZone.toZoneId() ), appender );
			}
		}
		else {
			localFormatter.formatTo( temporalAccessor, appender );
		}
	}

	private static SqlAppender required(SqlAppender appender) {
		return requireNonNull( appender, "appender" );
	}

	@FunctionalInterface
	private interface StandardTimestampRenderer {
		void append(
				SqlAppender appender,
				TemporalAccessor temporalAccessor,
				boolean supportsOffset,
				TimeZone jdbcTimeZone);
	}
}
