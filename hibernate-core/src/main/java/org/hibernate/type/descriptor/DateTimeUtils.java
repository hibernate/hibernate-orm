/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type.descriptor;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.chrono.IsoEra;
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
import org.hibernate.sql.ast.spi.StringBuilderSqlAppender;

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
	public static final String FORMAT_STRING_DATE_WITH_ERA = "uuuu-MM-dd";
	public static final String FORMAT_STRING_DATE_WITH_ERA_SUFFIX = "yyyy-MM-dd G";
	public static final String FORMAT_STRING_DATE_WITH_ERA_PREFIX = "G yyyy-MM-dd";
	public static final String FORMAT_STRING_TIME_WITH_OFFSET = "HH:mm:ssXXX";
	public static final String FORMAT_STRING_TIME = "HH:mm:ss";
	public static final String FORMAT_STRING_TIMESTAMP = "yyyy-MM-dd HH:mm:ss";
	private static final String FORMAT_STRING_TIMESTAMP_WITH_ERA = "uuuu-MM-dd HH:mm:ss";
	public static final String FORMAT_STRING_TIMESTAMP_WITH_MILLIS = FORMAT_STRING_TIMESTAMP + ".SSS";
	public static final String FORMAT_STRING_TIMESTAMP_WITH_MILLIS_AND_ERA = FORMAT_STRING_TIMESTAMP_WITH_ERA + ".SSS";
	public static final String FORMAT_STRING_TIMESTAMP_WITH_MILLIS_AND_ERA_SUFFIX = FORMAT_STRING_TIMESTAMP_WITH_MILLIS + " G";
	public static final String FORMAT_STRING_TIMESTAMP_WITH_MILLIS_AND_ERA_PREFIX = "G " + FORMAT_STRING_TIMESTAMP_WITH_MILLIS;
	public static final String FORMAT_STRING_TIMESTAMP_WITH_MICROS = FORMAT_STRING_TIMESTAMP + ".SSSSSS";
	public static final String FORMAT_STRING_TIMESTAMP_WITH_MICROS_AND_ERA = FORMAT_STRING_TIMESTAMP_WITH_ERA + ".SSSSSS";
	public static final String FORMAT_STRING_TIMESTAMP_WITH_MICROS_AND_ERA_SUFFIX = FORMAT_STRING_TIMESTAMP_WITH_MICROS + " G";
	public static final String FORMAT_STRING_TIMESTAMP_WITH_MICROS_PADDED = FORMAT_STRING_TIMESTAMP + ".SSS000";
	public static final String FORMAT_STRING_TIMESTAMP_WITH_MICROS_AND_ERA_SUFFIX_PADDED = FORMAT_STRING_TIMESTAMP_WITH_MICROS_PADDED + " G";
	public static final String FORMAT_STRING_TIMESTAMP_WITH_NANOS = FORMAT_STRING_TIMESTAMP + ".SSSSSSSSS";
	public static final String FORMAT_STRING_TIMESTAMP_WITH_NANOS_AND_ERA = FORMAT_STRING_TIMESTAMP_WITH_ERA + ".SSSSSSSSS";
	public static final String FORMAT_STRING_TIMESTAMP_WITH_NANOS_AND_ERA_PREFIX = "G " + FORMAT_STRING_TIMESTAMP_WITH_NANOS;
	public static final String FORMAT_STRING_TIMESTAMP_WITH_NANOS_PADDED = FORMAT_STRING_TIMESTAMP + ".SSS000000";
	public static final String FORMAT_STRING_TIMESTAMP_WITH_NANOS_AND_ERA_PREFIX_PADDED = "G " + FORMAT_STRING_TIMESTAMP_WITH_NANOS_PADDED;
	public static final String FORMAT_STRING_TIMESTAMP_WITH_NANOS_IN_BC = "-" + FORMAT_STRING_TIMESTAMP_WITH_NANOS;
	public static final String FORMAT_STRING_TIMESTAMP_WITH_MILLIS_AND_OFFSET = FORMAT_STRING_TIMESTAMP_WITH_MILLIS + "XXX";
	public static final String FORMAT_STRING_TIMESTAMP_WITH_MILLIS_AND_OFFSET_AND_ERA = FORMAT_STRING_TIMESTAMP_WITH_MILLIS_AND_ERA + "XXX";
	public static final String FORMAT_STRING_TIMESTAMP_WITH_MILLIS_AND_OFFSET_AND_ERA_SUFFIX = FORMAT_STRING_TIMESTAMP_WITH_MILLIS_AND_OFFSET + " G";
	public static final String FORMAT_STRING_TIMESTAMP_WITH_MICROS_AND_OFFSET = FORMAT_STRING_TIMESTAMP_WITH_MICROS + "XXX";
	public static final String FORMAT_STRING_TIMESTAMP_WITH_MICROS_AND_OFFSET_AND_ERA = FORMAT_STRING_TIMESTAMP_WITH_MICROS_AND_ERA + "XXX";
	public static final String FORMAT_STRING_TIMESTAMP_WITH_MICROS_AND_OFFSET_AND_ERA_SUFFIX = FORMAT_STRING_TIMESTAMP_WITH_MICROS_AND_OFFSET + " G";
	public static final String FORMAT_STRING_TIMESTAMP_WITH_NANOS_AND_OFFSET = FORMAT_STRING_TIMESTAMP_WITH_NANOS + "XXX";
	public static final String FORMAT_STRING_TIMESTAMP_WITH_NANOS_AND_OFFSET_AND_ERA = FORMAT_STRING_TIMESTAMP_WITH_NANOS_AND_ERA + "XXX";
	public static final String FORMAT_STRING_TIMESTAMP_WITH_NANOS_AND_OFFSET_AND_ERA_PREFIX = "G " + FORMAT_STRING_TIMESTAMP_WITH_NANOS_AND_OFFSET;
	public static final String FORMAT_STRING_TIMESTAMP_WITH_NANOS_AND_OFFSET_IN_BC = "-" + FORMAT_STRING_TIMESTAMP_WITH_NANOS_AND_OFFSET;
	public static final String FORMAT_STRING_TIMESTAMP_WITH_MICROS_AND_OFFSET_NOZ = FORMAT_STRING_TIMESTAMP_WITH_MICROS + "xxx";
	public static final String FORMAT_STRING_TIMESTAMP_WITH_MICROS_AND_OFFSET_NOZ_AND_ERA = FORMAT_STRING_TIMESTAMP_WITH_MICROS_AND_ERA + "xxx";
	public static final String FORMAT_STRING_TIMESTAMP_WITH_MICROS_AND_OFFSET_NOZ_AND_ERA_SUFFIX = FORMAT_STRING_TIMESTAMP_WITH_MICROS_AND_OFFSET_NOZ + " G";
	public static final String FORMAT_STRING_TIMESTAMP_WITH_NANOS_AND_OFFSET_NOZ = FORMAT_STRING_TIMESTAMP_WITH_NANOS + "xxx";
	public static final String FORMAT_STRING_TIMESTAMP_WITH_NANOS_AND_OFFSET_NOZ_AND_ERA = FORMAT_STRING_TIMESTAMP_WITH_NANOS_AND_ERA + "xxx";
	public static final String FORMAT_STRING_TIMESTAMP_WITH_NANOS_AND_OFFSET_NOZ_IN_BC = "-" + FORMAT_STRING_TIMESTAMP_WITH_NANOS_AND_OFFSET_NOZ;

	public static final DateTimeFormatter DATE_TIME_FORMATTER_DATE = DateTimeFormatter.ofPattern( FORMAT_STRING_DATE, Locale.ENGLISH );
	public static final DateTimeFormatter DATE_TIME_FORMATTER_DATE_WITH_ERA = DateTimeFormatter.ofPattern( FORMAT_STRING_DATE_WITH_ERA, Locale.ENGLISH );
	public static final DateTimeFormatter DATE_TIME_FORMATTER_DATE_WITH_ERA_SUFFIX = DateTimeFormatter.ofPattern( FORMAT_STRING_DATE_WITH_ERA_SUFFIX, Locale.ENGLISH );
	public static final DateTimeFormatter DATE_TIME_FORMATTER_DATE_WITH_ERA_PREFIX = DateTimeFormatter.ofPattern( FORMAT_STRING_DATE_WITH_ERA_PREFIX, Locale.ENGLISH );
	public static final DateTimeFormatter DATE_TIME_FORMATTER_TIME_WITH_OFFSET = DateTimeFormatter.ofPattern( FORMAT_STRING_TIME_WITH_OFFSET, Locale.ENGLISH );
	public static final DateTimeFormatter DATE_TIME_FORMATTER_TIME = DateTimeFormatter.ofPattern( FORMAT_STRING_TIME, Locale.ENGLISH );
	public static final DateTimeFormatter DATE_TIME_FORMATTER_TIMESTAMP_WITH_MILLIS = DateTimeFormatter.ofPattern(
			FORMAT_STRING_TIMESTAMP_WITH_MILLIS_AND_ERA,
			Locale.ENGLISH
	);
	public static final DateTimeFormatter DATE_TIME_FORMATTER_TIMESTAMP_WITH_MILLIS_AND_ERA_SUFFIX = DateTimeFormatter.ofPattern(
			FORMAT_STRING_TIMESTAMP_WITH_MILLIS_AND_ERA_SUFFIX,
			Locale.ENGLISH
	);
	public static final DateTimeFormatter DATE_TIME_FORMATTER_TIMESTAMP_WITH_MICROS = DateTimeFormatter.ofPattern(
			FORMAT_STRING_TIMESTAMP_WITH_MICROS_AND_ERA,
			Locale.ENGLISH
	);
	public static final DateTimeFormatter DATE_TIME_FORMATTER_TIMESTAMP_WITH_MICROS_AND_ERA_SUFFIX = DateTimeFormatter.ofPattern(
			FORMAT_STRING_TIMESTAMP_WITH_MICROS_AND_ERA_SUFFIX,
			Locale.ENGLISH
	);
	public static final DateTimeFormatter DATE_TIME_FORMATTER_TIMESTAMP_WITH_NANOS = DateTimeFormatter.ofPattern(
			FORMAT_STRING_TIMESTAMP_WITH_NANOS_AND_ERA,
			Locale.ENGLISH
	);
	public static final DateTimeFormatter DATE_TIME_FORMATTER_TIMESTAMP_WITH_NANOS_AND_ERA_PREFIX = DateTimeFormatter.ofPattern(
			FORMAT_STRING_TIMESTAMP_WITH_NANOS_AND_ERA_PREFIX,
			Locale.ENGLISH
	);
	public static final DateTimeFormatter DATE_TIME_FORMATTER_TIMESTAMP_WITH_NANOS_IN_BC = DateTimeFormatter.ofPattern(
			FORMAT_STRING_TIMESTAMP_WITH_NANOS_IN_BC,
			Locale.ENGLISH
	);
	public static final DateTimeFormatter DATE_TIME_FORMATTER_TIMESTAMP_WITH_MILLIS_AND_OFFSET = DateTimeFormatter.ofPattern(
			FORMAT_STRING_TIMESTAMP_WITH_MILLIS_AND_OFFSET_AND_ERA,
			Locale.ENGLISH
	);
	public static final DateTimeFormatter DATE_TIME_FORMATTER_TIMESTAMP_WITH_MILLIS_AND_OFFSET_AND_ERA_SUFFIX = DateTimeFormatter.ofPattern(
			FORMAT_STRING_TIMESTAMP_WITH_MILLIS_AND_OFFSET_AND_ERA_SUFFIX,
			Locale.ENGLISH
	);
	public static final DateTimeFormatter DATE_TIME_FORMATTER_TIMESTAMP_WITH_MICROS_AND_OFFSET = DateTimeFormatter.ofPattern(
			FORMAT_STRING_TIMESTAMP_WITH_MICROS_AND_OFFSET_AND_ERA,
			Locale.ENGLISH
	);
	public static final DateTimeFormatter DATE_TIME_FORMATTER_TIMESTAMP_WITH_MICROS_AND_OFFSET_AND_ERA_SUFFIX = DateTimeFormatter.ofPattern(
			FORMAT_STRING_TIMESTAMP_WITH_MICROS_AND_OFFSET_AND_ERA_SUFFIX,
			Locale.ENGLISH
	);
	public static final DateTimeFormatter DATE_TIME_FORMATTER_TIMESTAMP_WITH_MICROS_AND_OFFSET_NOZ = DateTimeFormatter.ofPattern(
			FORMAT_STRING_TIMESTAMP_WITH_MICROS_AND_OFFSET_NOZ_AND_ERA,
			Locale.ENGLISH
	);
	public static final DateTimeFormatter DATE_TIME_FORMATTER_TIMESTAMP_WITH_MICROS_AND_OFFSET_NOZ_AND_ERA_SUFFIX = DateTimeFormatter.ofPattern(
			FORMAT_STRING_TIMESTAMP_WITH_MICROS_AND_OFFSET_NOZ_AND_ERA_SUFFIX,
			Locale.ENGLISH
	);
	public static final DateTimeFormatter DATE_TIME_FORMATTER_TIMESTAMP_WITH_NANOS_AND_OFFSET = DateTimeFormatter.ofPattern(
			FORMAT_STRING_TIMESTAMP_WITH_NANOS_AND_OFFSET_AND_ERA,
			Locale.ENGLISH
	);
	public static final DateTimeFormatter DATE_TIME_FORMATTER_TIMESTAMP_WITH_NANOS_AND_OFFSET_AND_ERA_PREFIX = DateTimeFormatter.ofPattern(
			FORMAT_STRING_TIMESTAMP_WITH_NANOS_AND_OFFSET_AND_ERA_PREFIX,
			Locale.ENGLISH
	);
	public static final DateTimeFormatter DATE_TIME_FORMATTER_TIMESTAMP_WITH_NANOS_AND_OFFSET_IN_BC = DateTimeFormatter.ofPattern(
			FORMAT_STRING_TIMESTAMP_WITH_NANOS_AND_OFFSET_IN_BC,
			Locale.ENGLISH
	);
	public static final DateTimeFormatter DATE_TIME_FORMATTER_TIMESTAMP_WITH_NANOS_AND_OFFSET_NOZ = DateTimeFormatter.ofPattern(
			FORMAT_STRING_TIMESTAMP_WITH_NANOS_AND_OFFSET_NOZ_AND_ERA,
			Locale.ENGLISH
	);
	public static final DateTimeFormatter DATE_TIME_FORMATTER_TIMESTAMP_WITH_NANOS_AND_OFFSET_NOZ_IN_BC = DateTimeFormatter.ofPattern(
			FORMAT_STRING_TIMESTAMP_WITH_NANOS_AND_OFFSET_NOZ_IN_BC,
			Locale.ENGLISH
	);

	public static final String JDBC_ESCAPE_START_DATE = "{d '";
	public static final String JDBC_ESCAPE_START_TIME = "{t '";
	public static final String JDBC_ESCAPE_START_TIMESTAMP = "{ts '";
	public static final String JDBC_ESCAPE_END = "'}";

	public static final long YEAR_ONE_EPOCH_MILLIS = new java.sql.Timestamp( -1899, 0, 1, 0, 0, 0, 0 ).getTime();
	/**
	 * The millisecond value since the epoch at which the gregorian Calendar starts.
	 * Starting with this value, conversions between java.time and java.util.Date is safe,
	 * but before that, year, month and day based conversion is necessary.
	 */
	public static final long GREGORIAN_START_EPOCH_MILLIS = new java.sql.Timestamp( 1582-1900, 9, 5, 0, 0, 0, 0 ).getTime();

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
	private static final ThreadLocal<SimpleDateFormat> LOCAL_DATE_WITH_ERA_SUFFIX_FORMAT =
			ThreadLocal.withInitial( () -> new SimpleDateFormat( FORMAT_STRING_DATE_WITH_ERA_SUFFIX, Locale.ENGLISH ) );
	private static final ThreadLocal<SimpleDateFormat> LOCAL_DATE_WITH_ERA_PREFIX_FORMAT =
			ThreadLocal.withInitial( () -> new SimpleDateFormat( FORMAT_STRING_DATE_WITH_ERA_PREFIX, Locale.ENGLISH ) );
	private static final ThreadLocal<SimpleDateFormat> LOCAL_TIME_FORMAT =
			ThreadLocal.withInitial( () -> new SimpleDateFormat( FORMAT_STRING_TIME, Locale.ENGLISH ) );
	private static final ThreadLocal<SimpleDateFormat> TIME_WITH_OFFSET_FORMAT =
			ThreadLocal.withInitial( () -> new SimpleDateFormat( FORMAT_STRING_TIME_WITH_OFFSET, Locale.ENGLISH ) );
	private static final ThreadLocal<SimpleDateFormat> TIMESTAMP_WITH_MILLIS_FORMAT =
			ThreadLocal.withInitial( () -> new SimpleDateFormat( FORMAT_STRING_TIMESTAMP_WITH_MILLIS, Locale.ENGLISH ) );
	private static final ThreadLocal<SimpleDateFormat> TIMESTAMP_WITH_MILLIS_AND_ERA_SUFFIX_FORMAT =
			ThreadLocal.withInitial( () -> new SimpleDateFormat( FORMAT_STRING_TIMESTAMP_WITH_MILLIS_AND_ERA_SUFFIX, Locale.ENGLISH ) );
	private static final ThreadLocal<SimpleDateFormat> TIMESTAMP_WITH_MILLIS_AND_ERA_PREFIX_FORMAT =
			ThreadLocal.withInitial( () -> new SimpleDateFormat( FORMAT_STRING_TIMESTAMP_WITH_MILLIS_AND_ERA_PREFIX, Locale.ENGLISH ) );
	private static final ThreadLocal<SimpleDateFormat> TIMESTAMP_WITH_MICROS_FORMAT =
			ThreadLocal.withInitial( () -> new SimpleDateFormat( FORMAT_STRING_TIMESTAMP_WITH_MICROS_PADDED, Locale.ENGLISH ) );
	private static final ThreadLocal<SimpleDateFormat> TIMESTAMP_WITH_MICROS_AND_ERA_SUFFIX_FORMAT =
			ThreadLocal.withInitial( () -> new SimpleDateFormat( FORMAT_STRING_TIMESTAMP_WITH_MICROS_AND_ERA_SUFFIX_PADDED, Locale.ENGLISH ) );
	private static final ThreadLocal<SimpleDateFormat> TIMESTAMP_WITH_NANOS_FORMAT =
			ThreadLocal.withInitial( () -> new SimpleDateFormat( FORMAT_STRING_TIMESTAMP_WITH_NANOS_PADDED, Locale.ENGLISH ) );
	private static final ThreadLocal<SimpleDateFormat> TIMESTAMP_WITH_NANOS_AND_ERA_PREFIX_FORMAT =
			ThreadLocal.withInitial( () -> new SimpleDateFormat( FORMAT_STRING_TIMESTAMP_WITH_NANOS_AND_ERA_PREFIX_PADDED, Locale.ENGLISH ) );

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

	public static void appendAsTimestampWithNanosAndEraPrefix(
			SqlAppender appender,
			TemporalAccessor temporalAccessor,
			boolean supportsOffset,
			TimeZone jdbcTimeZone) {
		if ( isBcEra( temporalAccessor ) ) {
			appendAsTimestamp(
					appender,
					temporalAccessor,
					supportsOffset,
					jdbcTimeZone,
					DATE_TIME_FORMATTER_TIMESTAMP_WITH_NANOS_AND_ERA_PREFIX,
					DATE_TIME_FORMATTER_TIMESTAMP_WITH_NANOS_AND_OFFSET_AND_ERA_PREFIX
			);
		}
		else {
			appendAsTimestamp(
					appender,
					temporalAccessor,
					supportsOffset,
					jdbcTimeZone,
					DATE_TIME_FORMATTER_TIMESTAMP_WITH_NANOS,
					DATE_TIME_FORMATTER_TIMESTAMP_WITH_NANOS_AND_OFFSET
			);
		}
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
				DATE_TIME_FORMATTER_TIMESTAMP_WITH_NANOS,
				allowZforZeroOffset
						? DATE_TIME_FORMATTER_TIMESTAMP_WITH_NANOS_AND_OFFSET
						: DATE_TIME_FORMATTER_TIMESTAMP_WITH_NANOS_AND_OFFSET_NOZ
		);
	}

	public static void appendAsTimestampWithNanosWithoutYear0(
			SqlAppender appender,
			TemporalAccessor temporalAccessor,
			boolean supportsOffset,
			TimeZone jdbcTimeZone,
			boolean allowZforZeroOffset) {
		if ( isBcEra( temporalAccessor ) ) {
			appendAsTimestamp(
					appender,
					temporalAccessor,
					supportsOffset,
					jdbcTimeZone,
					DATE_TIME_FORMATTER_TIMESTAMP_WITH_NANOS_IN_BC,
					allowZforZeroOffset
							? DATE_TIME_FORMATTER_TIMESTAMP_WITH_NANOS_AND_OFFSET_IN_BC
							: DATE_TIME_FORMATTER_TIMESTAMP_WITH_NANOS_AND_OFFSET_NOZ_IN_BC
			);
		}
		else {
			appendAsTimestampWithNanos( appender, temporalAccessor, supportsOffset, jdbcTimeZone, allowZforZeroOffset );
		}
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

	public static void appendAsTimestampWithMicrosAndEraSuffix(
			SqlAppender appender,
			TemporalAccessor temporalAccessor,
			boolean supportsOffset,
			TimeZone jdbcTimeZone) {
		if ( isBcEra( temporalAccessor ) ) {
			appendAsTimestamp(
					appender,
					temporalAccessor,
					supportsOffset,
					jdbcTimeZone,
					DATE_TIME_FORMATTER_TIMESTAMP_WITH_MICROS_AND_ERA_SUFFIX,
					DATE_TIME_FORMATTER_TIMESTAMP_WITH_MICROS_AND_OFFSET_AND_ERA_SUFFIX
			);
		}
		else {
			appendAsTimestamp(
					appender,
					temporalAccessor,
					supportsOffset,
					jdbcTimeZone,
					DATE_TIME_FORMATTER_TIMESTAMP_WITH_MICROS,
					DATE_TIME_FORMATTER_TIMESTAMP_WITH_MICROS_AND_OFFSET
			);
		}
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

	public static void appendAsTimestampWithMicrosAndEraSuffix(
			SqlAppender appender,
			TemporalAccessor temporalAccessor,
			boolean supportsOffset,
			TimeZone jdbcTimeZone,
			boolean allowZforZeroOffset) {
		if ( isBcEra( temporalAccessor ) ) {
			appendAsTimestamp(
					appender,
					temporalAccessor,
					supportsOffset,
					jdbcTimeZone,
					DATE_TIME_FORMATTER_TIMESTAMP_WITH_MICROS_AND_ERA_SUFFIX,
					allowZforZeroOffset
							? DATE_TIME_FORMATTER_TIMESTAMP_WITH_MICROS_AND_OFFSET_AND_ERA_SUFFIX
							: DATE_TIME_FORMATTER_TIMESTAMP_WITH_MICROS_AND_OFFSET_NOZ_AND_ERA_SUFFIX
			);
		}
		else {
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

	public static void appendAsTimestampWithMillisAndEraSuffix(
			SqlAppender appender,
			TemporalAccessor temporalAccessor,
			boolean supportsOffset,
			TimeZone jdbcTimeZone) {
		if ( isBcEra( temporalAccessor ) ) {
			appendAsTimestamp(
					appender,
					temporalAccessor,
					supportsOffset,
					jdbcTimeZone,
					DATE_TIME_FORMATTER_TIMESTAMP_WITH_MILLIS_AND_ERA_SUFFIX,
					DATE_TIME_FORMATTER_TIMESTAMP_WITH_MILLIS_AND_OFFSET_AND_ERA_SUFFIX
			);
		}
		else {
			appendAsTimestamp(
					appender,
					temporalAccessor,
					supportsOffset,
					jdbcTimeZone,
					DATE_TIME_FORMATTER_TIMESTAMP_WITH_MILLIS,
					DATE_TIME_FORMATTER_TIMESTAMP_WITH_MILLIS_AND_OFFSET
			);
		}
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
		DATE_TIME_FORMATTER_DATE_WITH_ERA.formatTo( temporalAccessor, appender );
	}

	public static void appendAsDateWithoutYear0(SqlAppender appender, TemporalAccessor temporalAccessor) {
		if ( isBcEra( temporalAccessor ) ) {
			appender.appendSql( '-' );;
		}
		DATE_TIME_FORMATTER_DATE.formatTo( temporalAccessor, appender );
	}

	public static void appendAsDateWithEraPrefix(SqlAppender appender, TemporalAccessor temporalAccessor) {
		if ( isBcEra( temporalAccessor ) ) {
			DATE_TIME_FORMATTER_DATE_WITH_ERA_PREFIX.formatTo( temporalAccessor, appender );
		}
		else {
			DATE_TIME_FORMATTER_DATE.formatTo( temporalAccessor, appender );
		}
	}

	public static void appendAsDateWithEraSuffix(SqlAppender appender, TemporalAccessor temporalAccessor) {
		if ( isBcEra( temporalAccessor ) ) {
			DATE_TIME_FORMATTER_DATE_WITH_ERA_SUFFIX.formatTo( temporalAccessor, appender );
		}
		else {
			DATE_TIME_FORMATTER_DATE.formatTo( temporalAccessor, appender );
		}
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
		appendDateWithFormat( appender, date, jdbcTimeZone, TIMESTAMP_WITH_MILLIS_FORMAT.get() );
	}

	public static void appendAsTimestampWithMillisAndEraSuffix(SqlAppender appender, java.util.Date date, TimeZone jdbcTimeZone) {
		if ( date.getTime() < YEAR_ONE_EPOCH_MILLIS ) {
			appendDateWithFormatOnly( appender, date, jdbcTimeZone, TIMESTAMP_WITH_MILLIS_AND_ERA_SUFFIX_FORMAT.get() );
		}
		else {
			appendDateWithFormatOnly( appender, date, jdbcTimeZone, TIMESTAMP_WITH_MILLIS_FORMAT.get() );
		}
	}

	public static void appendAsTimestampWithMicros(SqlAppender appender, Date date, TimeZone jdbcTimeZone) {
		if ( date instanceof Timestamp timestamp ) {
			// java.sql.Timestamp supports nano sec
			appendAsTimestamp(
					appender,
					toLocalDateTime( timestamp ).atZone( jdbcTimeZone.toZoneId() ),
					false,
					jdbcTimeZone,
					DATE_TIME_FORMATTER_TIMESTAMP_WITH_MICROS,
					DATE_TIME_FORMATTER_TIMESTAMP_WITH_MICROS_AND_OFFSET
			);
		}
		else {
			appendDateWithFormat( appender, date, jdbcTimeZone, TIMESTAMP_WITH_MICROS_FORMAT.get() );
		}
	}

	public static void appendAsTimestampWithMicrosAndEraSuffix(SqlAppender appender, Date date, TimeZone jdbcTimeZone) {
		if ( date instanceof Timestamp timestamp ) {
			// java.sql.Timestamp supports nano sec
			final ZonedDateTime zonedDateTime = toLocalDateTime( timestamp ).atZone( jdbcTimeZone.toZoneId() );
			if ( date.getTime() < YEAR_ONE_EPOCH_MILLIS ) {
				appendAsTimestamp(
						appender,
						zonedDateTime,
						false,
						jdbcTimeZone,
						DATE_TIME_FORMATTER_TIMESTAMP_WITH_MICROS_AND_ERA_SUFFIX,
						DATE_TIME_FORMATTER_TIMESTAMP_WITH_MICROS_AND_OFFSET_AND_ERA_SUFFIX
				);
			}
			else {
				appendAsTimestamp(
						appender,
						zonedDateTime,
						false,
						jdbcTimeZone,
						DATE_TIME_FORMATTER_TIMESTAMP_WITH_MICROS,
						DATE_TIME_FORMATTER_TIMESTAMP_WITH_MICROS_AND_OFFSET
				);
			}
		}
		else if ( date.getTime() < YEAR_ONE_EPOCH_MILLIS ) {
			appendDateWithFormatOnly( appender, date, jdbcTimeZone, TIMESTAMP_WITH_MICROS_AND_ERA_SUFFIX_FORMAT.get() );
		}
		else {
			appendDateWithFormatOnly( appender, date, jdbcTimeZone, TIMESTAMP_WITH_MICROS_FORMAT.get() );
		}
	}

	public static void appendAsTimestampWithNanos(SqlAppender appender, Date date, TimeZone jdbcTimeZone) {
		if ( date instanceof Timestamp timestamp ) {
			// java.sql.Timestamp supports nano sec
			appendAsTimestamp(
					appender,
					toLocalDateTime( timestamp ).atZone( jdbcTimeZone.toZoneId() ),
					false,
					jdbcTimeZone,
					DATE_TIME_FORMATTER_TIMESTAMP_WITH_NANOS,
					DATE_TIME_FORMATTER_TIMESTAMP_WITH_NANOS_AND_OFFSET
			);
		}
		else {
			appendDateWithFormat( appender, date, jdbcTimeZone, TIMESTAMP_WITH_NANOS_FORMAT.get() );
		}
	}

	public static void appendAsTimestampWithNanosAndEraPrefix(SqlAppender appender, Date date, TimeZone jdbcTimeZone) {
		if ( date instanceof Timestamp timestamp ) {
			// java.sql.Timestamp supports nano sec
			final ZonedDateTime zonedDateTime = toLocalDateTime( timestamp ).atZone( jdbcTimeZone.toZoneId() );
			if ( date.getTime() < YEAR_ONE_EPOCH_MILLIS ) {
				appendAsTimestamp(
						appender,
						zonedDateTime,
						false,
						jdbcTimeZone,
						DATE_TIME_FORMATTER_TIMESTAMP_WITH_NANOS_AND_ERA_PREFIX,
						DATE_TIME_FORMATTER_TIMESTAMP_WITH_NANOS_AND_OFFSET_AND_ERA_PREFIX
				);
			}
			else {
				appendAsTimestamp(
						appender,
						zonedDateTime,
						false,
						jdbcTimeZone,
						DATE_TIME_FORMATTER_TIMESTAMP_WITH_NANOS,
						DATE_TIME_FORMATTER_TIMESTAMP_WITH_NANOS_AND_OFFSET
				);
			}
		}
		else if ( date.getTime() < YEAR_ONE_EPOCH_MILLIS ) {
			appendDateWithFormatOnly( appender, date, jdbcTimeZone, TIMESTAMP_WITH_NANOS_AND_ERA_PREFIX_FORMAT.get() );
		}
		else {
			appendDateWithFormatOnly( appender, date, jdbcTimeZone, TIMESTAMP_WITH_NANOS_FORMAT.get() );
		}
	}

	public static void appendAsTimestampWithNanosWithoutYear0(SqlAppender appender, Date date, TimeZone jdbcTimeZone) {
		if ( date instanceof Timestamp timestamp ) {
			// java.sql.Timestamp supports nano sec
			final ZonedDateTime zonedDateTime = toLocalDateTime( timestamp ).atZone( jdbcTimeZone.toZoneId() );
			if ( date.getTime() < YEAR_ONE_EPOCH_MILLIS ) {
				appendAsTimestamp(
						appender,
						zonedDateTime,
						false,
						jdbcTimeZone,
						DATE_TIME_FORMATTER_TIMESTAMP_WITH_NANOS_IN_BC,
						DATE_TIME_FORMATTER_TIMESTAMP_WITH_NANOS_AND_OFFSET_IN_BC
				);
			}
			else {
				appendAsTimestamp(
						appender,
						zonedDateTime,
						false,
						jdbcTimeZone,
						DATE_TIME_FORMATTER_TIMESTAMP_WITH_NANOS,
						DATE_TIME_FORMATTER_TIMESTAMP_WITH_NANOS_AND_OFFSET
				);
			}
		}
		else {
			if ( date.getTime() < YEAR_ONE_EPOCH_MILLIS ) {
				appender.appendSql( '-' );
			}
			appendDateWithFormatOnly( appender, date, jdbcTimeZone, TIMESTAMP_WITH_NANOS_FORMAT.get() );
		}
	}

	private static void appendDateWithFormat(SqlAppender appender, java.util.Date date, TimeZone jdbcTimeZone, SimpleDateFormat simpleDateFormat) {
		if ( date.getTime() < YEAR_ONE_EPOCH_MILLIS ) {
			appender.appendSql( '-' );
			date = addOneYear( date );
		}
		appendDateWithFormatOnly( appender, date, jdbcTimeZone, simpleDateFormat );
	}

	private static void appendDateWithFormatOnly(SqlAppender appender, java.util.Date date, TimeZone jdbcTimeZone, SimpleDateFormat simpleDateFormat) {
		final TimeZone originalTimeZone = simpleDateFormat.getTimeZone();
		try {
			simpleDateFormat.setTimeZone( jdbcTimeZone );
			appender.appendSql( simpleDateFormat.format( date ) );
		}
		finally {
			simpleDateFormat.setTimeZone( originalTimeZone );
		}
	}

	public static String dateToString(Date date) {
		final StringBuilderSqlAppender appender = new StringBuilderSqlAppender();
		appendAsDate( appender, date );
		return appender.toString();
	}

	public static void appendAsDate(SqlAppender appender, Date date) {
		if ( date.getTime() < YEAR_ONE_EPOCH_MILLIS ) {
			appender.appendSql( '-' );
			appender.appendSql( LOCAL_DATE_FORMAT.get().format( addOneYear( date ) ) );
		}
		else {
			appender.appendSql( LOCAL_DATE_FORMAT.get().format( date ) );
		}
	}

	public static void appendAsDateWithEraPrefix(SqlAppender appender, Date date) {
		if ( date.getTime() < YEAR_ONE_EPOCH_MILLIS ) {
			appender.appendSql( LOCAL_DATE_WITH_ERA_PREFIX_FORMAT.get().format( date ) );
		}
		else {
			appender.appendSql( LOCAL_DATE_FORMAT.get().format( date ) );
		}
	}

	public static void appendAsDateWithEraSuffix(SqlAppender appender, Date date) {
		if ( date.getTime() < YEAR_ONE_EPOCH_MILLIS ) {
			appender.appendSql( LOCAL_DATE_WITH_ERA_SUFFIX_FORMAT.get().format( date ) );
		}
		else {
			appender.appendSql( LOCAL_DATE_FORMAT.get().format( date ) );
		}
	}

	public static void appendAsDateWithoutYear0(SqlAppender appender, Date date) {
		if ( date.getTime() < YEAR_ONE_EPOCH_MILLIS ) {
			appender.appendSql( '-' );
		}
		appender.appendSql( LOCAL_DATE_FORMAT.get().format( date ) );
	}

	public static String timestampToString(Date date) {
		final StringBuilderSqlAppender appender = new StringBuilderSqlAppender();
		appendAsTimestampWithNanos( appender, date, TimeZone.getTimeZone( "UTC" ) );
		return appender.toString();
	}

	private static Date addOneYear(Date date) {
		final Calendar calendar = Calendar.getInstance();
		calendar.setTime( date );
		calendar.add( Calendar.YEAR, 1 );
		if ( date instanceof Timestamp ) {
			return new Timestamp( calendar.getTime().getTime() );
		}
		else if ( date instanceof java.sql.Date ) {
			return new java.sql.Date( calendar.getTime().getTime() );
		}
		else {
			assert date.getClass() == java.util.Date.class;
			return new Date( calendar.getTime().getTime() );
		}
	}

	/**
	 * @deprecated Use {@link #appendAsLocalTime(SqlAppender, Date)} instead
	 */
	@Deprecated
	public static void appendAsTime(SqlAppender appender, java.util.Date date) {
		appendAsLocalTime( appender, date );
	}

	public static void appendAsTime(SqlAppender appender, java.util.Date date, TimeZone jdbcTimeZone) {
		appendDateWithFormatOnly(  appender, date, jdbcTimeZone, TIME_WITH_OFFSET_FORMAT.get() );
	}

	public static void appendAsLocalTime(SqlAppender appender, Date date) {
		appender.appendSql( LOCAL_TIME_FORMAT.get().format( date ) );
	}

	public static void appendAsTimestampWithMillis(
			SqlAppender appender,
			java.util.Calendar calendar,
			TimeZone jdbcTimeZone) {
		appendDateWithFormat( appender, calendar.getTime(), jdbcTimeZone, TIMESTAMP_WITH_MILLIS_FORMAT.get() );
	}

	public static void appendAsTimestampWithMillisAndEraPrefix(
			SqlAppender appender,
			java.util.Calendar calendar,
			TimeZone jdbcTimeZone) {
		final Date date = calendar.getTime();
		if ( date.getTime() < YEAR_ONE_EPOCH_MILLIS ) {
			appendDateWithFormatOnly( appender, date, jdbcTimeZone, TIMESTAMP_WITH_MILLIS_AND_ERA_PREFIX_FORMAT.get() );
		}
		else {
			appendDateWithFormatOnly( appender, date, jdbcTimeZone, TIMESTAMP_WITH_MILLIS_FORMAT.get() );
		}
	}

	public static void appendAsTimestampWithMillisAndEraSuffix(
			SqlAppender appender,
			java.util.Calendar calendar,
			TimeZone jdbcTimeZone) {
		final Date date = calendar.getTime();
		if ( date.getTime() < YEAR_ONE_EPOCH_MILLIS ) {
			appendDateWithFormatOnly( appender, date, jdbcTimeZone, TIMESTAMP_WITH_MILLIS_AND_ERA_SUFFIX_FORMAT.get() );
		}
		else {
			appendDateWithFormatOnly( appender, date, jdbcTimeZone, TIMESTAMP_WITH_MILLIS_FORMAT.get() );
		}
	}

	public static void appendAsTimestampWithMillisWithoutYear0(
			SqlAppender appender,
			java.util.Calendar calendar,
			TimeZone jdbcTimeZone) {
		final Date date = calendar.getTime();
		if ( date.getTime() < YEAR_ONE_EPOCH_MILLIS ) {
			appender.appendSql( '-' );
		}
		appendDateWithFormatOnly( appender, date, jdbcTimeZone, TIMESTAMP_WITH_MILLIS_FORMAT.get() );
	}

	public static void appendAsDate(SqlAppender appender, java.util.Calendar calendar) {
		appendDateWithFormat( appender, calendar.getTime(), calendar.getTimeZone(), LOCAL_DATE_FORMAT.get() );
	}

	public static void appendAsDateWithEraPrefix(SqlAppender appender, java.util.Calendar calendar) {
		final Date date = calendar.getTime();
		if ( date.getTime() < YEAR_ONE_EPOCH_MILLIS ) {
			appendDateWithFormatOnly( appender, date, calendar.getTimeZone(), LOCAL_DATE_WITH_ERA_PREFIX_FORMAT.get() );
		}
		else {
			appendDateWithFormatOnly( appender, date, calendar.getTimeZone(), LOCAL_DATE_FORMAT.get() );
		}
	}

	public static void appendAsDateWithEraSuffix(SqlAppender appender, java.util.Calendar calendar) {
		final Date date = calendar.getTime();
		if ( date.getTime() < YEAR_ONE_EPOCH_MILLIS ) {
			appendDateWithFormatOnly( appender, date, calendar.getTimeZone(), LOCAL_DATE_WITH_ERA_SUFFIX_FORMAT.get() );
		}
		else {
			appendDateWithFormatOnly( appender, date, calendar.getTimeZone(), LOCAL_DATE_FORMAT.get() );
		}
	}

	public static void appendAsDateWithoutYear0(SqlAppender appender, java.util.Calendar calendar) {
		final Date date = calendar.getTime();
		if ( date.getTime() < YEAR_ONE_EPOCH_MILLIS ) {
			appender.appendSql( '-' );
		}
		appendDateWithFormatOnly( appender, date, calendar.getTimeZone(), LOCAL_DATE_FORMAT.get() );
	}

	/**
	 * @deprecated Use {@link #appendAsLocalTime(SqlAppender, Calendar)} instead
	 */
	@Deprecated
	public static void appendAsTime(SqlAppender appender, java.util.Calendar calendar) {
		appendAsLocalTime( appender, calendar );
	}

	public static void appendAsTime(SqlAppender appender, java.util.Calendar calendar, TimeZone jdbcTimeZone) {
		appendDateWithFormatOnly( appender, calendar.getTime(), jdbcTimeZone, TIME_WITH_OFFSET_FORMAT.get() );
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

	public static Timestamp toTimestamp(Instant instant) {
		/*
		 * This works around two bugs:
		 * - HHH-13266 (JDK-8061577): around and before 1900,
		 * the number of milliseconds since the epoch does not mean the same thing
		 * for java.util and java.time, so conversion must be done using the year, month, day, hour, etc.
		 * - HHH-13379 (JDK-4312621): after 1908 (approximately),
		 * Daylight Saving Time introduces ambiguity in the year/month/day/hour/etc representation once a year
		 * (on DST end), so conversion must be done using the number of milliseconds since the epoch.
		 * - around 1905, both methods are equally valid, so we don't really care which one is used.
		 */
		final ZonedDateTime zonedDateTime = instant.atZone( ZoneId.systemDefault() );
		if ( zonedDateTime.getYear() < 1905 ) {
			return Timestamp.valueOf( zonedDateTime.toLocalDateTime() );
		}
		else {
			return Timestamp.from( instant );
		}
	}

	public static java.sql.Date toSqlDate(Instant instant) {
		/*
		 * This works around two bugs:
		 * - HHH-13266 (JDK-8061577): around and before 1900,
		 * the number of milliseconds since the epoch does not mean the same thing
		 * for java.util and java.time, so conversion must be done using the year, month, day, hour, etc.
		 * - HHH-13379 (JDK-4312621): after 1908 (approximately),
		 * Daylight Saving Time introduces ambiguity in the year/month/day/hour/etc representation once a year
		 * (on DST end), so conversion must be done using the number of milliseconds since the epoch.
		 * - around 1905, both methods are equally valid, so we don't really care which one is used.
		 */
		final ZonedDateTime zonedDateTime = instant.atZone( ZoneId.systemDefault() );
		if ( zonedDateTime.getYear() < 1905 ) {
			return java.sql.Date.valueOf( zonedDateTime.toLocalDate() );
		}
		else {
			return new java.sql.Date( instant.toEpochMilli() );
		}
	}

	public static Instant toInstant(Timestamp timestamp) {
		/*
		 * This works around two bugs:
		 * - HHH-13266 (JDK-8061577): around and before 1900,
		 * the number of milliseconds since the epoch does not mean the same thing
		 * for java.util and java.time, so conversion must be done using the year, month, day, hour, etc.
		 * - HHH-13379 (JDK-4312621): after 1908 (approximately),
		 * Daylight Saving Time introduces ambiguity in the year/month/day/hour/etc representation once a year
		 * (on DST end), so conversion must be done using the number of milliseconds since the epoch.
		 * - around 1905, both methods are equally valid, so we don't really care which one is used.
		 */
		if ( timestamp.getYear() < 5 ) { // Timestamp year 0 is 1900
			return toLocalDateTime( timestamp ).atZone( ZoneId.systemDefault() ).toInstant();
		}
		else {
			return timestamp.toInstant();
		}
	}

	public static Instant toInstant(Date date) {
		if ( date instanceof Timestamp timestamp ) {
			return toInstant( timestamp );
		}
		else {
			return date.toInstant();
		}
	}

	public static LocalDate toLocalDate(java.sql.Date sqlDate) {
		final LocalDate localDate = sqlDate.toLocalDate();
		// Workaround the JDK-8269590 bug in java.sql.Date.toLocalDate(), which will use the positive BCE year
		// instead of a negative year when constructing LocalDate
		if ( sqlDate.getTime() < DateTimeUtils.YEAR_ONE_EPOCH_MILLIS && localDate.getYear() > 0 ) {
			return localDate.withYear( -localDate.getYear() + 1 );
		}
		else {
			return localDate;
		}
	}

	public static LocalDateTime toLocalDateTime(Timestamp timestamp) {
		final LocalDateTime localDateTime = timestamp.toLocalDateTime();
		// Workaround the JDK-8269590 bug in java.sql.Date.toLocalDate(), which will use the positive BCE year
		// instead of a negative year when constructing LocalDate
		if ( timestamp.getTime() < DateTimeUtils.YEAR_ONE_EPOCH_MILLIS && localDateTime.getYear() > 0 ) {
			return localDateTime.withYear( -localDateTime.getYear() + 1 );
		}
		else {
			return localDateTime;
		}
	}

	public static boolean isBcEra(TemporalAccessor temporalAccessor) {
		if ( temporalAccessor.isSupported( ChronoField.ERA ) ) {
			return temporalAccessor.get( ChronoField.ERA ) == IsoEra.BCE.getValue();
		}
		else if ( temporalAccessor instanceof Instant instant ) {
			return instant.toEpochMilli() < YEAR_ONE_EPOCH_MILLIS;
		}
		else if ( temporalAccessor.isSupported( ChronoField.EPOCH_DAY ) ) {
			return temporalAccessor.get( ChronoField.EPOCH_DAY ) < YEAR_ONE_EPOCH_MILLIS / 1000;
		}
		else {
			return false;
		}
	}

	public static boolean isBcEra(Date date) {
		return date.getTime() < YEAR_ONE_EPOCH_MILLIS;
	}

	public static boolean isBcEra(Calendar calendar) {
		return calendar.getTime().getTime() < YEAR_ONE_EPOCH_MILLIS;
	}
}
