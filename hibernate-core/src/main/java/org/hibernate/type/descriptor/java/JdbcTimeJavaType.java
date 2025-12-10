/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type.descriptor.java;

import java.sql.Time;
import java.sql.Types;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoField;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import org.hibernate.HibernateException;
import org.hibernate.dialect.Dialect;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.type.descriptor.DateTimeUtils;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.JdbcTypeIndicators;
import org.hibernate.type.spi.TypeConfiguration;

import jakarta.persistence.TemporalType;

import static org.hibernate.internal.util.CharSequenceHelper.subSequence;

/**
 * Descriptor for {@link Time} handling.
 *
 * @implSpec Unlike most {@link JavaType} implementations, can handle 2 different "domain
 * representations" (most map just a single type): general {@link Date} values in addition
 * to {@link Time} values.  This capability is shared with
 * {@link JdbcDateJavaType} and {@link JdbcTimestampJavaType}.
 */
public class JdbcTimeJavaType extends AbstractTemporalJavaType<Time> {
	public static final JdbcTimeJavaType INSTANCE = new JdbcTimeJavaType();

	public static final DateTimeFormatter LITERAL_FORMATTER = DateTimeFormatter.ISO_LOCAL_TIME;

	private static final DateTimeFormatter ENCODED_FORMATTER = new DateTimeFormatterBuilder()
			.optionalStart()
			.append( DateTimeFormatter.ISO_DATE )
			.appendLiteral( 'T' )
			.optionalEnd()
			.append( DateTimeFormatter.ISO_LOCAL_TIME )
			.toFormatter();


	public JdbcTimeJavaType() {
		super( Time.class, TimeMutabilityPlan.INSTANCE );
	}

	@Override
	public TemporalType getPrecision() {
		return TemporalType.TIME;
	}

	@Override
	public Class<Time> getJavaType() {
		return java.sql.Time.class;
	}

	@Override
	public boolean isInstance(Object value) {
		// this check holds true for java.sql.Time as well
		return value instanceof Date
			&& !( value instanceof java.sql.Date );
	}

	@Override
	public Time cast(Object value) {
		return (Time) value;
	}

	@Override
	public int extractHashCode(Time value) {
		final var calendar = Calendar.getInstance();
		calendar.setTime( value );
		int hashCode = 1;
		hashCode = 31 * hashCode + calendar.get( Calendar.HOUR_OF_DAY );
		hashCode = 31 * hashCode + calendar.get( Calendar.MINUTE );
		hashCode = 31 * hashCode + calendar.get( Calendar.SECOND );
		hashCode = 31 * hashCode + calendar.get( Calendar.MILLISECOND );
		return hashCode;
	}

	@Override
	public boolean areEqual(Time one, Time another) {
		if ( one == another ) {
			return true;
		}

		if ( one == null || another == null ) {
			return false;
		}

		if ( one.getTime() == another.getTime() ) {
			return true;
		}

		final var calendar1 = Calendar.getInstance();
		final var calendar2 = Calendar.getInstance();
		calendar1.setTime( one );
		calendar2.setTime( another );

		return calendar1.get( Calendar.HOUR_OF_DAY ) == calendar2.get( Calendar.HOUR_OF_DAY )
			&& calendar1.get( Calendar.MINUTE ) == calendar2.get( Calendar.MINUTE )
			&& calendar1.get( Calendar.SECOND ) == calendar2.get( Calendar.SECOND )
			&& calendar1.get( Calendar.MILLISECOND ) == calendar2.get( Calendar.MILLISECOND );
	}

	@Override
	public Time coerce(Object value) {
		return wrap( value, null );
	}

	@Override
	public <X> X unwrap(Time value, Class<X> type, WrapperOptions options) {
		if ( value == null ) {
			return null;
		}

		if ( LocalTime.class.isAssignableFrom( type ) ) {
			return type.cast( unwrapLocalTime( value ) );
		}

		if ( Time.class.isAssignableFrom( type ) ) {
			return type.cast( value );
		}

		if ( Date.class.isAssignableFrom( type ) ) {
			// SHOULD THROW!
//			throw new IllegalArgumentException( "Illegal attempt to treat 'java.sql.Time' as 'java.sql.Date'" );
			return type.cast( value );
		}

		if ( Long.class.isAssignableFrom( type ) ) {
			return type.cast( value.getTime() );
		}

		if ( String.class.isAssignableFrom( type ) ) {
			return type.cast( toString( value ) );
		}

		if ( Calendar.class.isAssignableFrom( type ) ) {
			final var gregorianCalendar = new GregorianCalendar();
			gregorianCalendar.setTimeInMillis( value.getTime() );
			return type.cast( gregorianCalendar );
		}

		if ( java.sql.Timestamp.class.isAssignableFrom( type ) ) {
			return type.cast( new java.sql.Timestamp( value.getTime() ) );
		}

		if ( java.sql.Date.class.isAssignableFrom( type ) ) {
			throw new IllegalArgumentException( "Illegal attempt to treat `java.sql.Time` as `java.sql.Date`" );
		}

		throw unknownUnwrap( type );
	}

	private static LocalTime unwrapLocalTime(Time value) {
		final var localTime = value.toLocalTime();
		long millis = value.getTime() % 1000;
		if ( millis == 0 ) {
			return localTime;
		}
		if ( millis < 0 ) {
			// The milliseconds for a Time could be negative,
			// which usually means the time is in a different time zone
			millis += 1_000L;
		}
		return localTime.with( ChronoField.NANO_OF_SECOND, millis * 1_000_000L );
	}

	@Override
	public Time wrap(Object value, WrapperOptions options) {
		if ( value == null ) {
			return null;
		}

		if ( value instanceof Time time ) {
			return time;
		}

		if ( value instanceof Date date ) {
			return wrapSqlTime( date );
		}

		if ( value instanceof LocalTime localTime ) {
			final var time = Time.valueOf( localTime );
			if ( localTime.getNano() == 0 ) {
				return time;
			}
			// Preserve milliseconds, which java.sql.Time supports
			return new Time( time.getTime() + DateTimeUtils.roundToPrecision( localTime.getNano(), 3 ) / 1000000 );
		}

		if ( value instanceof Long longValue ) {
			return new Time( longValue );
		}

		if ( value instanceof Calendar calendar ) {
			return new Time( calendar.getTimeInMillis() % 86_400_000 );
		}

		throw unknownWrap( value.getClass() );
	}

	static Time wrapSqlTime(Date date) {
		return new Time( date.getTime() % 86_400_000 );
	}

	private static LocalTime fromDate(Date value) {
		return value instanceof Time time
				? time.toLocalTime()
				: LocalTime.ofInstant( value.toInstant(), ZoneOffset.systemDefault() );
	}

	@Override
	public String toString(Time value) {
		return LITERAL_FORMATTER.format( fromDate( value ) );
	}

	@Override
	public Time fromString(CharSequence string) {
		try {
			final var temporalAccessor = LITERAL_FORMATTER.parse( string );
			final var localTime = LocalTime.from( temporalAccessor );
			final var time = Time.valueOf( localTime );
			time.setTime( time.getTime() + localTime.getNano() / 1_000_000 );
			return time;
		}
		catch ( DateTimeParseException pe) {
			throw new HibernateException( "could not parse time string " + string, pe );
		}
	}

	@Override
	public Time fromEncodedString(CharSequence charSequence, int start, int end) {
		try {
			final var temporalAccessor = ENCODED_FORMATTER.parse( subSequence( charSequence, start, end ) );
			return Time.valueOf( temporalAccessor.query( LocalTime::from ) );
		}
		catch ( DateTimeParseException pe) {
			throw new HibernateException( "could not parse time string " + subSequence( charSequence, start, end ), pe );
		}
	}

	@Override
	public void appendEncodedString(SqlAppender sb, Time value) {
		LITERAL_FORMATTER.formatTo( fromDate( value ), sb );
	}

	@Override
	public JdbcType getRecommendedJdbcType(JdbcTypeIndicators context) {
		return context.getJdbcType( Types.TIME );
	}

	@Override
	public int getDefaultSqlPrecision(Dialect dialect, JdbcType jdbcType) {
		// times represent repeating events - they
		// almost never come equipped with seconds,
		// let alone fractional seconds!
		return 0;
	}

	@Override
	protected <X> TemporalJavaType<X> forTimePrecision(TypeConfiguration typeConfiguration) {
		//noinspection unchecked
		return (TemporalJavaType<X>) this;
	}

	public static class TimeMutabilityPlan extends MutableMutabilityPlan<Time> {
		public static final TimeMutabilityPlan INSTANCE = new TimeMutabilityPlan();

		@Override
		public Time deepCopyNotNull(Time value) {
			return new Time( value.getTime() );
		}
	}
}
