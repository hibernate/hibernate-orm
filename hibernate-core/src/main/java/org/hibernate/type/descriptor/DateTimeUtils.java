/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.type.descriptor;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalQueries;
import java.util.Locale;
import java.util.TimeZone;

import static java.time.format.DateTimeFormatter.ISO_LOCAL_DATE;
import static java.time.format.DateTimeFormatter.ISO_LOCAL_TIME;

/**
 * Utilities for dealing with date/times
 *
 * @author Steve Ebersole
 * @author Gavin King
 */
@SuppressWarnings({"WeakerAccess", "unused"})
public final class DateTimeUtils {
	private DateTimeUtils() {
	}

	public static final String FORMAT_STRING_DATE = "yyyy-MM-dd";
	public static final String FORMAT_STRING_TIME = "HH:mm:ss";
	public static final String FORMAT_STRING_TIMESTAMP = "yyyy-MM-dd HH:mm:ss.SSSSSS";
	public static final String FORMAT_STRING_TIMESTAMP_WITH_OFFSET = "yyyy-MM-dd HH:mm:ss.SSSSSSxxx";

	public static final DateTimeFormatter DATE_TIME_FORMATTER_DATE = DateTimeFormatter.ofPattern( FORMAT_STRING_DATE, Locale.ENGLISH );
	public static final DateTimeFormatter DATE_TIME_FORMATTER_TIME = DateTimeFormatter.ofPattern( FORMAT_STRING_TIME, Locale.ENGLISH );
	public static final DateTimeFormatter DATE_TIME_FORMATTER_TIMESTAMP = DateTimeFormatter.ofPattern( FORMAT_STRING_TIMESTAMP, Locale.ENGLISH );
	public static final DateTimeFormatter DATE_TIME_FORMATTER_TIMESTAMP_WITH_OFFSET = DateTimeFormatter.ofPattern( FORMAT_STRING_TIMESTAMP_WITH_OFFSET, Locale.ENGLISH );

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

	/**
	 * Pattern used for parsing literal dates in HQL.
	 */
	public static final DateTimeFormatter DATE = ISO_LOCAL_DATE;

	/**
	 * Pattern used for parsing literal times in HQL.
	 */
	public static final DateTimeFormatter TIME = ISO_LOCAL_TIME;

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

	public static String formatAsTimestamp(TemporalAccessor temporalAccessor) {
		return temporalAccessor.isSupported(ChronoField.OFFSET_SECONDS)
				? DATE_TIME_FORMATTER_TIMESTAMP_WITH_OFFSET.format( temporalAccessor )
				: DATE_TIME_FORMATTER_TIMESTAMP.format( temporalAccessor );
	}

	public static String formatAsDate(TemporalAccessor temporalAccessor) {
		return DATE_TIME_FORMATTER_DATE.format( temporalAccessor );
	}

	public static String formatAsTime(TemporalAccessor temporalAccessor) {
		return DATE_TIME_FORMATTER_TIME.format( temporalAccessor );
	}

	public static String formatAsTimestamp(java.util.Date date) {
		return simpleDateFormatTimestamp().format( date );
	}

	public static String wrapAsJdbcDateLiteral(String literal) {
		return JDBC_ESCAPE_START_DATE + literal + JDBC_ESCAPE_END;
	}

	public static String wrapAsJdbcTimeLiteral(String literal) {
		return JDBC_ESCAPE_START_TIME + literal + JDBC_ESCAPE_END;
	}

	public static String wrapAsJdbcTimestampLiteral(String literal) {
		return JDBC_ESCAPE_START_TIMESTAMP + literal + JDBC_ESCAPE_END;
	}

	public static String wrapAsAnsiDateLiteral(String literal) {
		return "date '" + literal + "'";
	}

	public static String wrapAsAnsiTimeLiteral(String literal) {
		return "time '" + literal + "'";
	}

	public static String wrapAsAnsiTimestampLiteral(String literal) {
		return "timestamp '" + literal + "'";
	}

	public static SimpleDateFormat simpleDateFormatTimestamp() {
		return new SimpleDateFormat( FORMAT_STRING_TIMESTAMP, Locale.ENGLISH );
	}

	public static String formatAsDate(java.util.Date date) {
		return simpleDateFormatDate().format( date );
	}

	public static SimpleDateFormat simpleDateFormatDate() {
		return new SimpleDateFormat( FORMAT_STRING_DATE, Locale.ENGLISH );
	}

	public static String formatAsTime(java.util.Date date) {
		return simpleDateFormatTime().format( date );
	}

	public static SimpleDateFormat simpleDateFormatTime() {
		return new SimpleDateFormat( FORMAT_STRING_TIME, Locale.ENGLISH );
	}

	public static String formatAsTimestamp(java.util.Calendar calendar) {
		return simpleDateFormatTimestamp( calendar.getTimeZone() ).format( calendar.getTime() );
	}

	public static SimpleDateFormat simpleDateFormatTimestamp(TimeZone timeZone) {
		final SimpleDateFormat formatter = new SimpleDateFormat( FORMAT_STRING_TIMESTAMP, Locale.ENGLISH );
		formatter.setTimeZone( timeZone );
		return formatter;
	}

	public static String formatAsDate(java.util.Calendar calendar) {
		return simpleDateFormatDate( calendar.getTimeZone() ).format( calendar.getTime() );
	}

	public static SimpleDateFormat simpleDateFormatDate(TimeZone timeZone) {
		final SimpleDateFormat formatter = new SimpleDateFormat( FORMAT_STRING_DATE, Locale.ENGLISH );
		formatter.setTimeZone( timeZone );
		return formatter;
	}

	public static String formatAsTime(java.util.Calendar calendar) {
		return simpleDateFormatTime( calendar.getTimeZone() ).format( calendar.getTime() );
	}

	public static SimpleDateFormat simpleDateFormatTime(TimeZone timeZone) {
		final SimpleDateFormat formatter = new SimpleDateFormat( FORMAT_STRING_TIME, Locale.ENGLISH );
		formatter.setTimeZone( timeZone );
		return formatter;
	}

	public static void main(String... args) {
		final ZoneId localTzId = ZoneId.systemDefault();
		System.out.printf( "Local tz : %s\n", localTzId );

		final String[] values = new String[] {
				"1999-12-31 12:59:59.3",
				"1999-12-31 12:59:59 +02:00",
				"1999-12-31 12:59:59 UTC",
				"1999-12-31 12:59:59 UTC+02:00",
				"1999-12-31 12:59:59 " + localTzId.getId()
		};

		for ( String value : values ) {
			final TemporalAccessor parsed = DATE_TIME.parseBest(
					value,
					OffsetDateTime::from,
					ZonedDateTime::from,
					LocalDateTime::from
			);

			System.out.println( value + " -> " + parsed + " (" + parsed.getClass().getName() + ")" );

			final ZonedDateTime zdt;

			if ( parsed instanceof LocalDateTime ) {
				// here, "localTzId" would come from the "jdbc timezone" setting?
				zdt = ( (LocalDateTime) parsed ).atZone( localTzId );
				System.out.println( "    - LocalDateTime adjusted to ZonedDateTime : " + parsed );
			}
			else if ( parsed instanceof OffsetDateTime ) {
				zdt = ( (OffsetDateTime) parsed ).toZonedDateTime();
				System.out.println( "    - OffsetDateTime adjusted to ZonedDateTime : " + parsed );
			}
			else {
				zdt = (ZonedDateTime) parsed;
			}

			System.out.println( "    - ZoneId = " + zdt.getZone().getId() );
			System.out.println( "    - offset = " + zdt.getOffset() );
			System.out.println( "    - normalized = " + zdt.getZone().normalized() );

			final ZonedDateTime adjusted = zdt.withZoneSameInstant( localTzId );
			System.out.println( "    - adjusted = " + adjusted.toLocalDateTime() + " (zone-id:" + adjusted.getZone() + ")" );
		}
	}

	public static TemporalAccessor transform(String text) {
		return DATE_TIME.parse(
				text,
				(temporal) -> {
					// see if there is an offset or tz
					final ZoneId zoneOrOffset = temporal.query( TemporalQueries.zone() );
					if ( zoneOrOffset != null ) {
						final ZonedDateTime zdt = ZonedDateTime.from( temporal );
						// EDIT: call normalized() to convert a ZoneId
						// with constant offset, e.g., UTC+02:00, to ZoneOffset
						if ( zoneOrOffset.normalized() instanceof ZoneOffset ) {
							return zdt.toOffsetDateTime();
						}
						else {
							return zdt;
						}
					}

					// otherwise it's a LocalDateTime
					return LocalDateTime.from( temporal );
				}
		);
	}

	public static OffsetDateTime usingOffset(String text) {
		// does not work
		final TemporalAccessor parsed = DATE_TIME.parse( text );
		return OffsetDateTime.from( parsed );
	}
}
