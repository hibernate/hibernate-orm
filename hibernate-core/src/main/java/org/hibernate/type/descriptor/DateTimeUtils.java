/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.type.descriptor;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.Year;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.chrono.ChronoZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalAccessor;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.function.Function;

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
		else if ( temporalAccessor instanceof Instant ) {
			if ( supportsOffset ) {
				formatWithOffset.formatTo(
						( (Instant) temporalAccessor ).atZone( jdbcTimeZone.toZoneId() ),
						appender
				);
			}
			else {
				format.formatTo(
						LocalDateTime.ofInstant(
								(Instant) temporalAccessor,
								jdbcTimeZone.toZoneId()
						),
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

	/**
	 * Calendar has no microseconds.
	 *
	 * @deprecated use {@link #appendAsTimestampWithMillis(SqlAppender, Calendar, TimeZone)} instead
	 */
	@Deprecated(forRemoval = true)
	public static void appendAsTimestampWithMicros(SqlAppender appender, Calendar calendar, TimeZone jdbcTimeZone) {
		// it is possible to use micro sec resolution with java.util.Date
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

	public static long roundToPrecision(int nano, int precision) {
		final int precisionMask = pow10( 9 - precision );
		final int nanosToRound = nano % precisionMask;
		return nano - nanosToRound + ( nanosToRound >= ( precisionMask >> 1 ) ? precisionMask : 0 );
	}

	/**
	 * The mapping of supported temporal types to a factory which can convert them to a legacy {@code Calendar}.
	 * Keep sorted alphabetically by class name.
	 */
	private static final Map<Class<? extends Temporal>, Function<Calendar, Temporal>> CALENDAR_TO_TEMPORAL = Map.of(
		Instant.class, Calendar::toInstant,
		LocalDate.class, cal -> LocalDate.of(
			cal.get( Calendar.YEAR ),
			cal.get( Calendar.MONTH ) + 1,
			cal.get( Calendar.DAY_OF_MONTH ) ),
		LocalDateTime.class, cal -> LocalDateTime.of(
			cal.get( Calendar.YEAR ),
			cal.get( Calendar.MONTH ) + 1,
			cal.get( Calendar.DAY_OF_MONTH ),
			cal.get( Calendar.HOUR_OF_DAY ),
			cal.get( Calendar.MINUTE ),
			cal.get( Calendar.SECOND ),
			cal.get( Calendar.MILLISECOND ) * 1_000_000 ),
		OffsetDateTime.class, cal -> OffsetDateTime.of(
			cal.get( Calendar.YEAR ),
			cal.get( Calendar.MONTH ) + 1,
			cal.get( Calendar.DAY_OF_MONTH ),
			cal.get( Calendar.HOUR_OF_DAY ),
			cal.get( Calendar.MINUTE ),
			cal.get( Calendar.SECOND ),
			cal.get( Calendar.MILLISECOND ) * 1_000_000,
			ZoneOffset.ofTotalSeconds( cal.get( Calendar.ZONE_OFFSET ) / 1_000 )
		),
		Year.class, cal -> Year.of(
			cal.get( Calendar.YEAR )
		),
		YearMonth.class, cal -> YearMonth.of(
			cal.get( Calendar.YEAR ),
			cal.get( Calendar.MONTH ) + 1
		),
		ZonedDateTime.class, cal ->
			cal instanceof GregorianCalendar ?
				((GregorianCalendar) cal).toZonedDateTime() :
				ZonedDateTime.of(
					cal.get( Calendar.YEAR ),
					cal.get( Calendar.MONTH ) + 1,
					cal.get( Calendar.DAY_OF_MONTH ),
					cal.get( Calendar.HOUR_OF_DAY ),
					cal.get( Calendar.MINUTE ),
					cal.get( Calendar.SECOND ),
					cal.get( Calendar.MILLISECOND ) * 1_000_000,
					cal.getTimeZone().toZoneId()
				)
	);

	/**
	 * Convert a legacy {@link Calendar} to a temporal value on a best-effort basis, if possible.
	 * Only ISO dates and date-times are supported.
	 * Handling dates and times correctly is a tricky business, so we'll centralize the logic here to avoid problems.
	 *
	 * @param cal the calendar to convert
	 * @param temporalClass the class of the target temporal type (must not be {@code null})
	 * @return the temporal object, or {@code null} if the
	 * @param <T> the target temporal type
	 */
	public static <T extends Temporal> T calendarToTemporal( Calendar cal, Class<T> temporalClass ) {
		if ( cal == null ) {
			return null;
		}
		Function<Calendar, Temporal> func = CALENDAR_TO_TEMPORAL.get( temporalClass );
		if (func == null) {
			return null;
		}
		return temporalClass.cast( func.apply( cal ) );
	}

	private static final Set<Class<? extends Temporal>> CALENDAR_TEMPORAL_TYPES = CALENDAR_TO_TEMPORAL.keySet();

	/**
	 * Convert a temporal value to a legacy {@link Calendar} on a best-effort basis, if possible.
	 * The result presently always uses the {@code "gregorian"} {@linkplain Calendar.Builder#setCalendarType(String) calendar type}.
	 * Only ISO dates and date-times are supported.
	 * Handling dates and times correctly is a tricky business, so we'll centralize the logic here to avoid problems.
	 *
	 * @param temporal the value to convert
	 * @return the converted value, or {@code null} if the value cannot be converted
	 */
	public static Calendar temporalToCalendar( Temporal temporal ) {
		if ( temporal == null ) {
			return null;
		}
		if (! CALENDAR_TEMPORAL_TYPES.contains( temporal.getClass() ) ) {
			// we don't support conversion of non-ISO or non-chronological dates and times to legacy calendars
			return null;
		}
		if ( temporal instanceof ZonedDateTime ) {
			// special case: there's a dedicated factory method
			return GregorianCalendar.from( (ZonedDateTime) temporal );
		}
		Calendar.Builder cb = new Calendar.Builder().setCalendarType( "gregorian" );
		return copyToCalendarBuilder( cb, temporal ).build();
	}

	/**
	 * Copy all the known fields of the given {@code temporal} to the calendar builder.
	 *
	 * @param cb the calendar builder (must not be {@code null})
	 * @param temporal the temporal (must not be {@code null})
	 * @return the calendar builder (not {@code null})
	 */
	private static Calendar.Builder copyToCalendarBuilder( Calendar.Builder cb, Temporal temporal ) {
		// copy over time zone information, if any
		if ( temporal instanceof ChronoZonedDateTime<?> ) {
			ChronoZonedDateTime<?> zoned = (ChronoZonedDateTime<?>) temporal;
			cb.setTimeZone( TimeZone.getTimeZone( zoned.getZone() ) );
		}
		else if ( temporal.isSupported( ChronoField.OFFSET_SECONDS ) ) {
			cb.set( Calendar.ZONE_OFFSET, temporal.get( ChronoField.OFFSET_SECONDS ) * 1_000 );
		}
		// now copy all date/time information
		if ( temporal.isSupported( ChronoField.INSTANT_SECONDS ) ) {
			// copy as an instant instead
			long instant = temporal.get( ChronoField.INSTANT_SECONDS ) * 1_000L;
			if ( temporal.isSupported( ChronoField.NANO_OF_SECOND ) ) {
				instant += temporal.get( ChronoField.NANO_OF_SECOND ) / 1_000_000;
			}
			else if (temporal.isSupported( ChronoField.MICRO_OF_SECOND ) ) {
				instant += temporal.get( ChronoField.MICRO_OF_SECOND ) / 1_000;
			}
			else if (temporal.isSupported( ChronoField.MILLI_OF_SECOND ) ) {
				instant += temporal.get( ChronoField.MILLI_OF_SECOND );
			}
			cb.setInstant( instant );
			return cb;
		}
		else if ( temporal.isSupported( ChronoField.YEAR )) {
			cb.set( Calendar.YEAR, temporal.get( ChronoField.YEAR ) );
			if ( temporal.isSupported( ChronoField.MONTH_OF_YEAR ) ) {
				// note: not supporting odd months like Calendar#UNDECIMBER
				cb.set( Calendar.MONTH, temporal.get( ChronoField.MONTH_OF_YEAR ) - 1 );
				if ( temporal.isSupported( ChronoField.DAY_OF_MONTH ) ) {
					cb.set( Calendar.DAY_OF_MONTH, temporal.get( ChronoField.DAY_OF_MONTH ) );
					return copyTimeOfDayToCalendarBuilder( cb, temporal );
				}
				else {
					// just a year + month
					return cb;
				}
			}
			else if ( temporal.isSupported( ChronoField.DAY_OF_YEAR ) ) {
				cb.set( Calendar.DAY_OF_YEAR, temporal.get( ChronoField.DAY_OF_YEAR ) );
				return copyTimeOfDayToCalendarBuilder( cb, temporal );
			}
			else {
				// just a year
				return cb;
			}
		}
		else {
			// no date information whatsoever
			return cb;
		}
	}

	/**
	 * Copy all the known time-of-day fields of the given {@code temporal} to the calendar builder.
	 *
	 * @param cb the calendar builder (must not be {@code null})
	 * @param temporal the temporal (must not be {@code null})
	 * @return the calendar builder (not {@code null})
	 */
	private static Calendar.Builder copyTimeOfDayToCalendarBuilder(final Calendar.Builder cb, final Temporal temporal) {
		if ( temporal.isSupported( ChronoField.HOUR_OF_DAY ) ) {
			cb.set( Calendar.HOUR_OF_DAY, temporal.get( ChronoField.HOUR_OF_DAY ) );
			return copyTimeOfHourToCalendarBuilder( cb, temporal );
		}
		else if ( temporal.isSupported( ChronoField.HOUR_OF_AMPM ) && temporal.isSupported( ChronoField.AMPM_OF_DAY )) {
			cb.set( Calendar.AM_PM, temporal.get( ChronoField.AMPM_OF_DAY ) );
			cb.set( Calendar.HOUR, temporal.get( ChronoField.HOUR_OF_AMPM ) );
			return copyTimeOfHourToCalendarBuilder( cb, temporal );
		}
		else {
			// just a date with no TOD info
			return cb;
		}
	}

	/**
	 * Copy all the known time-of-hour fields of the given {@code temporal} to the calendar builder.
	 *
	 * @param cb the calendar builder (must not be {@code null})
	 * @param temporal the temporal (must not be {@code null})
	 * @return the calendar builder (not {@code null})
	 */
	private static Calendar.Builder copyTimeOfHourToCalendarBuilder(final Calendar.Builder cb, final Temporal temporal) {
		if ( temporal.isSupported( ChronoField.MINUTE_OF_HOUR ) ) {
			cb.set( Calendar.MINUTE, temporal.get( ChronoField.MINUTE_OF_HOUR ) );
			if ( temporal.isSupported( ChronoField.SECOND_OF_MINUTE ) ) {
				cb.set( Calendar.SECOND, temporal.get( ChronoField.SECOND_OF_MINUTE ) );
				if ( temporal.isSupported( ChronoField.NANO_OF_SECOND ) ) {
					// round downwards always
					cb.set( Calendar.MILLISECOND, temporal.get( ChronoField.NANO_OF_SECOND ) / 1_000_000 );
				}
				else if ( temporal.isSupported( ChronoField.MICRO_OF_SECOND ) ) {
					// round downwards always
					cb.set( Calendar.MILLISECOND, temporal.get( ChronoField.MICRO_OF_SECOND ) / 1_000 );
				}
				else if ( temporal.isSupported( ChronoField.MILLI_OF_SECOND ) ) {
					cb.set( Calendar.MILLISECOND, temporal.get( ChronoField.MILLI_OF_SECOND ) );
				}
			}
		}
		return cb;
	}

	private static int pow10(int exponent) {
		switch ( exponent ) {
			case 0:
				return 1;
			case 1:
				return 10;
			case 2:
				return 100;
			case 3:
				return 1000;
			case 4:
				return 10000;
			case 5:
				return 100000;
			case 6:
				return 1000000;
			case 7:
				return 10000000;
			case 8:
				return 100000000;
			default:
				return (int) Math.pow( 10, exponent );
		}
	}
}
