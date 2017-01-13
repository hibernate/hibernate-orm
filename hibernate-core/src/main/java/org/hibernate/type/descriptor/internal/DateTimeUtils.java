/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.descriptor.internal;

import java.text.SimpleDateFormat;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import javax.persistence.TemporalType;

/**
 * @author Steve Ebersole
 */
public final class DateTimeUtils {
	private DateTimeUtils() {
	}

	public static final String FORMAT_STRING_TIMESTAMP = "yyyy-MM-dd HH:mm:ss.S";
	public static final String FORMAT_STRING_DATE = "yyyy-MM-dd";
	public static final String FORMAT_STRING_TIME = "HH:mm:ss";

	public static final DateTimeFormatter DATE_TIME_FORMATTER_TIMESTAMP = DateTimeFormatter.ofPattern( FORMAT_STRING_TIMESTAMP, Locale.ENGLISH );
	public static final DateTimeFormatter DATE_TIME_FORMATTER_DATE = DateTimeFormatter.ofPattern( FORMAT_STRING_DATE, Locale.ENGLISH );
	public static final DateTimeFormatter DATE_TIME_FORMATTER_TIME = DateTimeFormatter.ofPattern( FORMAT_STRING_TIME, Locale.ENGLISH );

	public static final String JDBC_ESCAPE_START_TIMESTAMP = "{ts '";
	public static final String JDBC_ESCAPE_START_DATE = "{d '";
	public static final String JDBC_ESCAPE_START_TIME = "{t '";
	public static final String JDBC_ESCAPE_END = "'}";

	public static String formatUsingPrecision(TemporalAccessor temporalAccessor, TemporalType precision) {
		switch ( precision ) {
			case DATE: {
				return formatAsDate( temporalAccessor );
			}
			case TIME: {
				return formatAsTime( temporalAccessor );
			}
			default: {
				return formatAsTimestamp( temporalAccessor );
			}
		}
	}

	public static String formatAsTimestamp(TemporalAccessor temporalAccessor) {
		return DATE_TIME_FORMATTER_TIMESTAMP.format( temporalAccessor );
	}

	public static String formatAsDate(TemporalAccessor temporalAccessor) {
		return DATE_TIME_FORMATTER_DATE.format( temporalAccessor );
	}

	public static String formatAsTime(TemporalAccessor temporalAccessor) {
		return DATE_TIME_FORMATTER_TIME.format( temporalAccessor );
	}

	public static String formatJdbcLiteralUsingPrecision(TemporalAccessor temporalAccessor, TemporalType precision) {
		switch ( precision ) {
			case DATE: {
				return formatAsJdbcLiteralDate( temporalAccessor );
			}
			case TIME: {
				return formatAsJdbcLiteralTime( temporalAccessor );
			}
			default: {
				return formatAsJdbcLiteralTimestamp( temporalAccessor );
			}
		}
	}

	public static String formatAsJdbcLiteralDate(TemporalAccessor temporalAccessor) {
		return wrapAsJdbcDateLiteral( formatAsDate( temporalAccessor ));
	}

	public static String wrapAsJdbcDateLiteral(String literal) {
		return JDBC_ESCAPE_START_DATE + literal + JDBC_ESCAPE_END;
	}

	public static String formatAsJdbcLiteralTime(TemporalAccessor temporalAccessor) {
		return wrapAsJdbcTimeLiteral( formatAsTime( temporalAccessor ));
	}

	public static String wrapAsJdbcTimeLiteral(String literal) {
		return JDBC_ESCAPE_START_TIME + literal + JDBC_ESCAPE_END;
	}

	public static String formatAsJdbcLiteralTimestamp(TemporalAccessor temporalAccessor) {
		return wrapAsJdbcTimestampLiteral( formatAsTimestamp( temporalAccessor ));
	}

	public static String wrapAsJdbcTimestampLiteral(String literal) {
		return JDBC_ESCAPE_START_TIMESTAMP + literal + JDBC_ESCAPE_END;
	}

	public static String formatUsingPrecision(java.util.Date date, TemporalType precision) {
		switch ( precision ) {
			case DATE: {
				return formatAsDate( date );
			}
			case TIME: {
				return formatAsTime( date );
			}
			default: {
				return formatAsTimestamp( date );
			}
		}
	}


	public static String formatAsTimestamp(java.util.Date date) {
		return simpleDateFormatTimestamp().format( date );
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

	public static String formatJdbcLiteralUsingPrecision(java.util.Date date, TemporalType precision) {
		switch ( precision ) {
			case DATE: {
				return formatAsJdbcLiteralDate( date );
			}
			case TIME: {
				return formatAsJdbcLiteralTime( date );
			}
			default: {
				return formatAsJdbcLiteralTimestamp( date );
			}
		}
	}

	public static String formatAsJdbcLiteralDate(Date date) {
		return wrapAsJdbcDateLiteral( formatAsDate( date ) );
	}

	public static String formatAsJdbcLiteralTime(Date date) {
		return wrapAsJdbcTimeLiteral( formatAsTime( date ) );
	}

	public static String formatAsJdbcLiteralTimestamp(Date date) {
		return wrapAsJdbcTimestampLiteral( formatAsTimestamp( date ) );
	}

	public static String formatUsingPrecision(java.util.Calendar calendar, TemporalType precision) {
		switch ( precision ) {
			case DATE: {
				return formatAsDate( calendar );
			}
			case TIME: {
				return formatAsTime( calendar );
			}
			default: {
				return formatAsTimestamp( calendar );
			}
		}
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

	public static String formatJdbcLiteralUsingPrecision(java.util.Calendar calendar, TemporalType precision) {
		switch ( precision ) {
			case DATE: {
				return formatAsJdbcLiteralDate( calendar );
			}
			case TIME: {
				return formatAsJdbcLiteralTime( calendar );
			}
			default: {
				return formatAsJdbcLiteralTimestamp( calendar );
			}
		}
	}

	public static String formatAsJdbcLiteralTimestamp(Calendar calendar) {
		return wrapAsJdbcTimestampLiteral( formatAsTimestamp( calendar ) );
	}

	public static String formatAsJdbcLiteralDate(Calendar calendar) {
		return wrapAsJdbcDateLiteral( formatAsDate( calendar ) );
	}

	public static String formatAsJdbcLiteralTime(Calendar calendar) {
		return wrapAsJdbcTimeLiteral( formatAsTime( calendar ) );
	}
}
