/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type.descriptor;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalAccessor;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import org.hibernate.Internal;
import org.hibernate.dialect.Dialect;
import org.hibernate.sql.ast.spi.SqlAppender;

import static java.time.format.DateTimeFormatter.ISO_LOCAL_DATE;
import static java.time.format.DateTimeFormatter.ISO_LOCAL_TIME;

/**
 * Utilities for dealing with date/times
 *
 * @author Steve Ebersole
 * @author Gavin King
 */
@Internal
public final class DateTimeUtils {
	private DateTimeUtils() {
	}

	public static final String FORMAT_STRING_DATE = "yyyy-MM-dd";
	public static final String FORMAT_STRING_TIME_WITH_OFFSET = "HH:mm:ssXXX";
	public static final String FORMAT_STRING_TIME = "HH:mm:ss";
	public static final String FORMAT_STRING_TIMESTAMP = "yyyy-MM-dd HH:mm:ss";
	public static final String FORMAT_STRING_TIMESTAMP_WITH_MILLIS = FORMAT_STRING_TIMESTAMP + ".SSS";
	public static final String FORMAT_STRING_TIMESTAMP_WITH_MICROS = FORMAT_STRING_TIMESTAMP + ".SSSSSS";
	public static final String FORMAT_STRING_TIMESTAMP_WITH_NANOS = FORMAT_STRING_TIMESTAMP + ".SSSSSSSSS";
	public static final String FORMAT_STRING_TIMESTAMP_WITH_MILLIS_AND_OFFSET = FORMAT_STRING_TIMESTAMP_WITH_MILLIS + "XXX";
	public static final String FORMAT_STRING_TIMESTAMP_WITH_MICROS_AND_OFFSET = FORMAT_STRING_TIMESTAMP_WITH_MICROS + "XXX";
	public static final String FORMAT_STRING_TIMESTAMP_WITH_NANOS_AND_OFFSET = FORMAT_STRING_TIMESTAMP_WITH_NANOS + "XXX";
	public static final String FORMAT_STRING_TIMESTAMP_WITH_MICROS_AND_OFFSET_NOZ = FORMAT_STRING_TIMESTAMP_WITH_MICROS + "xxx";
	public static final String FORMAT_STRING_TIMESTAMP_WITH_NANOS_AND_OFFSET_NOZ = FORMAT_STRING_TIMESTAMP_WITH_NANOS + "xxx";

	public static final DateTimeFormatter DATE_TIME_FORMATTER_DATE = DateTimeFormatter.ofPattern( FORMAT_STRING_DATE, Locale.ENGLISH );
	public static final DateTimeFormatter DATE_TIME_FORMATTER_TIME_WITH_OFFSET = DateTimeFormatter.ofPattern( FORMAT_STRING_TIME_WITH_OFFSET, Locale.ENGLISH );
	public static final DateTimeFormatter DATE_TIME_FORMATTER_TIME = DateTimeFormatter.ofPattern( FORMAT_STRING_TIME, Locale.ENGLISH );
	public static final DateTimeFormatter DATE_TIME_FORMATTER_TIMESTAMP_WITH_MILLIS = DateTimeFormatter.ofPattern(
			FORMAT_STRING_TIMESTAMP_WITH_MILLIS,
			Locale.ENGLISH
	);
	public static final DateTimeFormatter DATE_TIME_FORMATTER_TIMESTAMP_WITH_MICROS = DateTimeFormatter.ofPattern(
			FORMAT_STRING_TIMESTAMP_WITH_MICROS,
			Locale.ENGLISH
	);
	public static final DateTimeFormatter DATE_TIME_FORMATTER_TIMESTAMP_WITH_NANOS = DateTimeFormatter.ofPattern(
			FORMAT_STRING_TIMESTAMP_WITH_NANOS,
			Locale.ENGLISH
	);
	public static final DateTimeFormatter DATE_TIME_FORMATTER_TIMESTAMP_WITH_MILLIS_AND_OFFSET = DateTimeFormatter.ofPattern(
			FORMAT_STRING_TIMESTAMP_WITH_MILLIS_AND_OFFSET,
			Locale.ENGLISH
	);
	public static final DateTimeFormatter DATE_TIME_FORMATTER_TIMESTAMP_WITH_MICROS_AND_OFFSET = DateTimeFormatter.ofPattern(
			FORMAT_STRING_TIMESTAMP_WITH_MICROS_AND_OFFSET,
			Locale.ENGLISH
	);
	public static final DateTimeFormatter DATE_TIME_FORMATTER_TIMESTAMP_WITH_MICROS_AND_OFFSET_NOZ = DateTimeFormatter.ofPattern(
			FORMAT_STRING_TIMESTAMP_WITH_MICROS_AND_OFFSET_NOZ,
			Locale.ENGLISH
	);
	public static final DateTimeFormatter DATE_TIME_FORMATTER_TIMESTAMP_WITH_NANOS_AND_OFFSET = DateTimeFormatter.ofPattern(
			FORMAT_STRING_TIMESTAMP_WITH_NANOS_AND_OFFSET,
			Locale.ENGLISH
	);
	public static final DateTimeFormatter DATE_TIME_FORMATTER_TIMESTAMP_WITH_NANOS_AND_OFFSET_NOZ = DateTimeFormatter.ofPattern(
			FORMAT_STRING_TIMESTAMP_WITH_NANOS_AND_OFFSET_NOZ,
			Locale.ENGLISH
	);

	public static final String JDBC_ESCAPE_START_DATE = "{d '";
	public static final String JDBC_ESCAPE_START_TIME = "{t '";
	public static final String JDBC_ESCAPE_START_TIMESTAMP = "{ts '";
	public static final String JDBC_ESCAPE_END = "'}";

	/**
	 * Pattern used for parsing literal datetimes in HQL.
	 *
	 * Recognizes timestamps consisting of a date and time separated
	 * by either T or a space, and with an optional offset or time
	 * zone ID. Ideally we should accept both ISO and SQL standard
	 * zoned timestamp formats here.
	 */
	public static final DateTimeFormatter DATE_TIME = new DateTimeFormatterBuilder()
			.parseCaseInsensitive()
			.append( ISO_LOCAL_DATE )
			.optionalStart().appendLiteral( ' ' ).optionalEnd()
			.optionalStart().appendLiteral( 'T' ).optionalEnd()
			.append( ISO_LOCAL_TIME )
			.optionalStart().appendLiteral( ' ' ).optionalEnd()
			.optionalStart().appendZoneOrOffsetId().optionalEnd()
			.toFormatter();

	private static final ThreadLocal<SimpleDateFormat> LOCAL_DATE_FORMAT =
			ThreadLocal.withInitial( () -> new SimpleDateFormat( FORMAT_STRING_DATE, Locale.ENGLISH ) );
	private static final ThreadLocal<SimpleDateFormat> LOCAL_TIME_FORMAT =
			ThreadLocal.withInitial( () -> new SimpleDateFormat( FORMAT_STRING_TIME, Locale.ENGLISH ) );
	private static final ThreadLocal<SimpleDateFormat> TIME_WITH_OFFSET_FORMAT =
			ThreadLocal.withInitial( () -> new SimpleDateFormat( FORMAT_STRING_TIME_WITH_OFFSET, Locale.ENGLISH ) );
	private static final ThreadLocal<SimpleDateFormat> TIMESTAMP_WITH_MILLIS_FORMAT =
			ThreadLocal.withInitial( () -> new SimpleDateFormat( FORMAT_STRING_TIMESTAMP_WITH_MILLIS, Locale.ENGLISH ) );

	/**
	 * Pattern used for parsing literal offset datetimes in HQL.
	 *
	 * Recognizes timestamps consisting of a date and time separated
	 * by either T or a space, and with a required offset. Ideally we
	 * should accept both ISO and SQL standard timestamp formats here.
	 */
	public static final DateTimeFormatter OFFSET_DATE_TIME = new DateTimeFormatterBuilder()
			.parseCaseInsensitive()
			.append( ISO_LOCAL_DATE )
			.optionalStart().appendLiteral( ' ' ).optionalEnd()
			.optionalStart().appendLiteral( 'T' ).optionalEnd()
			.append( ISO_LOCAL_TIME )
			.optionalStart().appendLiteral( ' ' ).optionalEnd()
			.appendOffset("+HH:mm", "+00")
			.toFormatter();

	public static void appendAsTimestampWithNanos(
			SqlAppender appender,
			TemporalAccessor temporalAccessor,
			boolean supportsOffset,
			TimeZone jdbcTimeZone) {
		appendAsTimestamp(
				appender,
				temporalAccessor,
				supportsOffset,
				jdbcTimeZone,
				DATE_TIME_FORMATTER_TIMESTAMP_WITH_NANOS,
				DATE_TIME_FORMATTER_TIMESTAMP_WITH_NANOS_AND_OFFSET
		);
	}

	public static void appendAsTimestampWithNanos(
			SqlAppender appender,
			TemporalAccessor temporalAccessor,
			boolean supportsOffset,
			TimeZone jdbcTimeZone,
			boolean allowZforZeroOffset) {
		appendAsTimestamp(
				appender,
				temporalAccessor,
				supportsOffset,
				jdbcTimeZone,
				DATE_TIME_FORMATTER_TIMESTAMP_WITH_MICROS,
				allowZforZeroOffset
						? DATE_TIME_FORMATTER_TIMESTAMP_WITH_NANOS_AND_OFFSET
						: DATE_TIME_FORMATTER_TIMESTAMP_WITH_NANOS_AND_OFFSET_NOZ
		);
	}

	public static void appendAsTimestampWithMicros(
			SqlAppender appender,
			TemporalAccessor temporalAccessor,
			boolean supportsOffset,
			TimeZone jdbcTimeZone) {
		appendAsTimestamp(
				appender,
				temporalAccessor,
				supportsOffset,
				jdbcTimeZone,
				DATE_TIME_FORMATTER_TIMESTAMP_WITH_MICROS,
				DATE_TIME_FORMATTER_TIMESTAMP_WITH_MICROS_AND_OFFSET
		);
	}

	public static void appendAsTimestampWithMicros(
			SqlAppender appender,
			TemporalAccessor temporalAccessor,
			boolean supportsOffset,
			TimeZone jdbcTimeZone,
			boolean allowZforZeroOffset) {
		appendAsTimestamp(
				appender,
				temporalAccessor,
				supportsOffset,
				jdbcTimeZone,
				DATE_TIME_FORMATTER_TIMESTAMP_WITH_MICROS,
				allowZforZeroOffset
						? DATE_TIME_FORMATTER_TIMESTAMP_WITH_MICROS_AND_OFFSET
						: DATE_TIME_FORMATTER_TIMESTAMP_WITH_MICROS_AND_OFFSET_NOZ
		);
	}

	public static void appendAsTimestampWithMillis(
			SqlAppender appender,
			TemporalAccessor temporalAccessor,
			boolean supportsOffset,
			TimeZone jdbcTimeZone) {
		appendAsTimestamp(
				appender,
				temporalAccessor,
				supportsOffset,
				jdbcTimeZone,
				DATE_TIME_FORMATTER_TIMESTAMP_WITH_MILLIS,
				DATE_TIME_FORMATTER_TIMESTAMP_WITH_MILLIS_AND_OFFSET
		);
	}

	private static void appendAsTimestamp(
			SqlAppender appender,
			TemporalAccessor temporalAccessor,
			boolean supportsOffset,
			TimeZone jdbcTimeZone,
			DateTimeFormatter format,
			DateTimeFormatter formatWithOffset) {
		if ( temporalAccessor.isSupported( ChronoField.OFFSET_SECONDS ) ) {
			if ( supportsOffset ) {
				formatWithOffset.formatTo( temporalAccessor, appender );
			}
			else {
				format.formatTo(
						LocalDateTime.ofInstant(
								Instant.from( temporalAccessor ),
								jdbcTimeZone.toZoneId()
						),
						appender
				);
			}
		}
		else if ( temporalAccessor instanceof Instant instant ) {
			if ( supportsOffset ) {
				formatWithOffset.formatTo(
						instant.atZone( jdbcTimeZone.toZoneId() ),
						appender
				);
			}
			else {
				format.formatTo(
						LocalDateTime.ofInstant( instant, jdbcTimeZone.toZoneId() ),
						appender
				);
			}
		}
		else {
			format.formatTo( temporalAccessor, appender );
		}
	}

	public static void appendAsDate(SqlAppender appender, TemporalAccessor temporalAccessor) {
		DATE_TIME_FORMATTER_DATE.formatTo( temporalAccessor, appender );
	}

	public static void appendAsTime(
			SqlAppender appender,
			TemporalAccessor temporalAccessor,
			boolean supportsOffset,
			TimeZone jdbcTimeZone) {
		if ( temporalAccessor.isSupported( ChronoField.OFFSET_SECONDS ) ) {
			if ( supportsOffset ) {
				DATE_TIME_FORMATTER_TIME_WITH_OFFSET.formatTo( temporalAccessor, appender );
			}
			else {
				DATE_TIME_FORMATTER_TIME.formatTo( LocalTime.from( temporalAccessor ), appender );
			}
		}
		else {
			DATE_TIME_FORMATTER_TIME.formatTo( temporalAccessor, appender );
		}
	}

	public static void appendAsLocalTime(SqlAppender appender, TemporalAccessor temporalAccessor) {
		DATE_TIME_FORMATTER_TIME.formatTo( temporalAccessor, appender );
	}

	public static void appendAsTimestampWithMillis(SqlAppender appender, java.util.Date date, TimeZone jdbcTimeZone) {
		final SimpleDateFormat simpleDateFormat = TIMESTAMP_WITH_MILLIS_FORMAT.get();
		final TimeZone originalTimeZone = simpleDateFormat.getTimeZone();
		try {
			simpleDateFormat.setTimeZone( jdbcTimeZone );
			appender.appendSql( simpleDateFormat.format( date ) );
		}
		finally {
			simpleDateFormat.setTimeZone( originalTimeZone );
		}
	}

	public static void appendAsTimestampWithMicros(SqlAppender appender, Date date, TimeZone jdbcTimeZone) {
		if ( date instanceof Timestamp ) {
			// java.sql.Timestamp supports nano sec
			appendAsTimestamp(
					appender,
					date.toInstant().atZone( jdbcTimeZone.toZoneId() ),
					false,
					jdbcTimeZone,
					DATE_TIME_FORMATTER_TIMESTAMP_WITH_MICROS,
					DATE_TIME_FORMATTER_TIMESTAMP_WITH_MICROS_AND_OFFSET
			);
		}
		else {
			// java.util.Date supports only milli sec
			final SimpleDateFormat simpleDateFormat = TIMESTAMP_WITH_MILLIS_FORMAT.get();
			final TimeZone originalTimeZone = simpleDateFormat.getTimeZone();
			try {
				simpleDateFormat.setTimeZone( jdbcTimeZone );
				appender.appendSql( simpleDateFormat.format( date ) );
			}
			finally {
				simpleDateFormat.setTimeZone( originalTimeZone );
			}
		}
	}

	public static void appendAsTimestampWithNanos(SqlAppender appender, Date date, TimeZone jdbcTimeZone) {
		if ( date instanceof Timestamp ) {
			// java.sql.Timestamp supports nano sec
			appendAsTimestamp(
					appender,
					date.toInstant().atZone( jdbcTimeZone.toZoneId() ),
					false,
					jdbcTimeZone,
					DATE_TIME_FORMATTER_TIMESTAMP_WITH_NANOS,
					DATE_TIME_FORMATTER_TIMESTAMP_WITH_NANOS_AND_OFFSET
			);
		}
		else {
			// java.util.Date supports only milli sec
			final SimpleDateFormat simpleDateFormat = TIMESTAMP_WITH_MILLIS_FORMAT.get();
			final TimeZone originalTimeZone = simpleDateFormat.getTimeZone();
			try {
				simpleDateFormat.setTimeZone( jdbcTimeZone );
				appender.appendSql( simpleDateFormat.format( date ) );
			}
			finally {
				simpleDateFormat.setTimeZone( originalTimeZone );
			}
		}
	}

	public static void appendAsDate(SqlAppender appender, Date date) {
		appender.appendSql( LOCAL_DATE_FORMAT.get().format( date ) );
	}

	/**
	 * @deprecated Use {@link #appendAsLocalTime(SqlAppender, Date)} instead
	 */
	@Deprecated
	public static void appendAsTime(SqlAppender appender, java.util.Date date) {
		appendAsLocalTime( appender, date );
	}

	public static void appendAsTime(SqlAppender appender, java.util.Date date, TimeZone jdbcTimeZone) {
		final SimpleDateFormat simpleDateFormat = TIME_WITH_OFFSET_FORMAT.get();
		final TimeZone originalTimeZone = simpleDateFormat.getTimeZone();
		try {
			simpleDateFormat.setTimeZone( jdbcTimeZone );
			appender.appendSql( simpleDateFormat.format( date ) );
		}
		finally {
			simpleDateFormat.setTimeZone( originalTimeZone );
		}
	}

	public static void appendAsLocalTime(SqlAppender appender, Date date) {
		appender.appendSql( LOCAL_TIME_FORMAT.get().format( date ) );
	}

	public static void appendAsTimestampWithMillis(
			SqlAppender appender,
			java.util.Calendar calendar,
			TimeZone jdbcTimeZone) {
		final SimpleDateFormat simpleDateFormat = TIMESTAMP_WITH_MILLIS_FORMAT.get();
		final TimeZone originalTimeZone = simpleDateFormat.getTimeZone();
		try {
			simpleDateFormat.setTimeZone( jdbcTimeZone );
			appender.appendSql( simpleDateFormat.format( calendar.getTime() ) );
		}
		finally {
			simpleDateFormat.setTimeZone( originalTimeZone );
		}
	}

	public static void appendAsDate(SqlAppender appender, java.util.Calendar calendar) {
		final SimpleDateFormat simpleDateFormat = LOCAL_DATE_FORMAT.get();
		final TimeZone originalTimeZone = simpleDateFormat.getTimeZone();
		try {
			simpleDateFormat.setTimeZone( calendar.getTimeZone() );
			appender.appendSql( simpleDateFormat.format( calendar.getTime() ) );
		}
		finally {
			simpleDateFormat.setTimeZone( originalTimeZone );
		}
	}

	/**
	 * @deprecated Use {@link #appendAsLocalTime(SqlAppender, Calendar)} instead
	 */
	@Deprecated
	public static void appendAsTime(SqlAppender appender, java.util.Calendar calendar) {
		appendAsLocalTime( appender, calendar );
	}

	public static void appendAsTime(SqlAppender appender, java.util.Calendar calendar, TimeZone jdbcTimeZone) {
		final SimpleDateFormat simpleDateFormat = TIME_WITH_OFFSET_FORMAT.get();
		final TimeZone originalTimeZone = simpleDateFormat.getTimeZone();
		try {
			simpleDateFormat.setTimeZone( jdbcTimeZone );
			appender.appendSql( simpleDateFormat.format( calendar.getTime() ) );
		}
		finally {
			simpleDateFormat.setTimeZone( originalTimeZone );
		}
	}

	public static void appendAsLocalTime(SqlAppender appender, Calendar calendar) {
		appender.appendSql( LOCAL_TIME_FORMAT.get().format( calendar.getTime() ) );
	}

	/**
	 * Do the same conversion that databases do when they encounter a timestamp with a higher precision
	 * than what is supported by a column, which is to round the excess fractions.
	 */
	public static <T extends Temporal> T adjustToDefaultPrecision(T temporal, Dialect d) {
		return adjustToPrecision( temporal, d.getDefaultTimestampPrecision(), d );
	}

	public static <T extends Temporal> T adjustToPrecision(T temporal, int precision, Dialect dialect) {
		return dialect.doesRoundTemporalOnOverflow()
				? roundToSecondPrecision( temporal, precision )
				: truncateToPrecision( temporal, precision );
	}

	public static <T extends Temporal> T truncateToPrecision(T temporal, int precision) {
		if ( precision >= 9 || !temporal.isSupported( ChronoField.NANO_OF_SECOND ) ) {
			return temporal;
		}
		final long factor = pow10( 9 - precision );
		//noinspection unchecked
		return (T) temporal.with(
				ChronoField.NANO_OF_SECOND,
				temporal.get( ChronoField.NANO_OF_SECOND ) / factor * factor
		);
	}

	/**
	 * Do the same conversion that databases do when they encounter a timestamp with a higher precision
	 * than what is supported by a column, which is to round the excess fractions.
	 *
	 * @deprecated Use {@link #adjustToDefaultPrecision(Temporal, Dialect)} instead
	 */
	@Deprecated(forRemoval = true, since = "6.6.1")
	public static <T extends Temporal> T roundToDefaultPrecision(T temporal, Dialect d) {
		final int defaultTimestampPrecision = d.getDefaultTimestampPrecision();
		if ( defaultTimestampPrecision >= 9 || !temporal.isSupported( ChronoField.NANO_OF_SECOND ) ) {
			return temporal;
		}
		//noinspection unchecked
		return (T) temporal.with(
				ChronoField.NANO_OF_SECOND,
				roundToPrecision( temporal.get( ChronoField.NANO_OF_SECOND ), defaultTimestampPrecision )
		);
	}

	public static <T extends Temporal> T roundToSecondPrecision(T temporal, int precision) {
		if ( precision >= 9 || !temporal.isSupported( ChronoField.NANO_OF_SECOND ) ) {
			return temporal;
		}
		if ( precision == 0 ) {
			//noinspection unchecked
			return temporal.get( ChronoField.NANO_OF_SECOND ) >= 500_000_000L
					? (T) temporal.plus( 1, ChronoUnit.SECONDS ).with( ChronoField.NANO_OF_SECOND, 0L )
					: (T) temporal.with( ChronoField.NANO_OF_SECOND, 0L );
		}
		final long nanos = roundToPrecision( temporal.get( ChronoField.NANO_OF_SECOND ), precision );
		if ( nanos == 1000000000L ) {
			//noinspection unchecked
			return (T) temporal.plus( 1L, ChronoUnit.SECONDS ).with( ChronoField.NANO_OF_SECOND, 0L );
		}
		//noinspection unchecked
		return (T) temporal.with( ChronoField.NANO_OF_SECOND, nanos );
	}

	public static long roundToPrecision(int nano, int precision) {
		assert precision > 0 : "Can't round fractional seconds to less-than 0";
		if ( precision >= 9 ) {
			return nano;
		}
		final int precisionMask = pow10( 9 - precision );
		final int nanosToRound = nano % precisionMask;
		return nano - nanosToRound + ( nanosToRound >= ( precisionMask >> 1 ) ? precisionMask : 0 );
	}

	private static int pow10(int exponent) {
		return switch ( exponent ) {
			case 0 -> 1;
			case 1 -> 10;
			case 2 -> 100;
			case 3 -> 1_000;
			case 4 -> 10_000;
			case 5 -> 100_000;
			case 6 -> 1_000_000;
			case 7 -> 10_000_000;
			case 8 -> 100_000_000;
			default -> (int) Math.pow( 10, exponent );
		};
	}
}
