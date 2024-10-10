/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
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
import java.time.temporal.TemporalAccessor;
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
public class JdbcTimeJavaType extends AbstractTemporalJavaType<Date> {
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
	public boolean isInstance(Object value) {
		// this check holds true for java.sql.Time as well
		return value instanceof Date
			&& !( value instanceof java.sql.Date );
	}

	@Override
	public int extractHashCode(Date value) {
		final Calendar calendar = Calendar.getInstance();
		calendar.setTime( value );
		int hashCode = 1;
		hashCode = 31 * hashCode + calendar.get( Calendar.HOUR_OF_DAY );
		hashCode = 31 * hashCode + calendar.get( Calendar.MINUTE );
		hashCode = 31 * hashCode + calendar.get( Calendar.SECOND );
		hashCode = 31 * hashCode + calendar.get( Calendar.MILLISECOND );
		return hashCode;
	}

	@Override
	public boolean areEqual(Date one, Date another) {
		if ( one == another ) {
			return true;
		}

		if ( one == null || another == null ) {
			return false;
		}

		if ( one.getTime() == another.getTime() ) {
			return true;
		}

		final Calendar calendar1 = Calendar.getInstance();
		final Calendar calendar2 = Calendar.getInstance();
		calendar1.setTime( one );
		calendar2.setTime( another );

		return calendar1.get( Calendar.HOUR_OF_DAY ) == calendar2.get( Calendar.HOUR_OF_DAY )
			&& calendar1.get( Calendar.MINUTE ) == calendar2.get( Calendar.MINUTE )
			&& calendar1.get( Calendar.SECOND ) == calendar2.get( Calendar.SECOND )
			&& calendar1.get( Calendar.MILLISECOND ) == calendar2.get( Calendar.MILLISECOND );
	}

	@Override
	public Date coerce(Object value, CoercionContext coercionContext) {
		return wrap( value, null );
	}

	@SuppressWarnings("unchecked")
	@Override
	public Object unwrap(Date value, Class type, WrapperOptions options) {
		if ( value == null ) {
			return null;
		}

		if ( LocalTime.class.isAssignableFrom( type ) ) {
			final Time time = value instanceof java.sql.Time
					? ( (java.sql.Time) value )
					: millisToSqlTime( value.getTime() );
			final LocalTime localTime = time.toLocalTime();
			long millis = time.getTime() % 1000;
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

		if ( Time.class.isAssignableFrom( type ) ) {
			return millisToSqlTime( value.getTime() );
		}

		if ( java.sql.Timestamp.class.isAssignableFrom( type ) ) {
			return new java.sql.Timestamp( value.getTime() );
		}

		if ( java.sql.Date.class.isAssignableFrom( type ) ) {
			throw new IllegalArgumentException( "Illegal attempt to treat `java.sql.Time` as `java.sql.Date`" );
		}

		if ( Date.class.isAssignableFrom( type ) ) {
			return value;
		}

		if ( Long.class.isAssignableFrom( type ) ) {
			return value.getTime();
		}

		if ( String.class.isAssignableFrom( type ) ) {
			return toString( value );
		}

		if ( Calendar.class.isAssignableFrom( type ) ) {
			final GregorianCalendar cal = new GregorianCalendar();
			cal.setTimeInMillis( value.getTime() );
			return cal;
		}

		throw unknownUnwrap( type );
	}

	@Override
	public Date wrap(Object value, WrapperOptions options) {
		if ( value == null ) {
			return null;
		}

		if ( value instanceof Date ) {
			return millisToSqlTime( ( (Date) value ).getTime() );
		}

		if ( value instanceof LocalTime ) {
			final LocalTime localTime = (LocalTime) value;
			final Time time = Time.valueOf( localTime );
			if ( localTime.getNano() == 0 ) {
				return time;
			}
			// Preserve milliseconds, which java.sql.Time supports
			return new Time( time.getTime() + DateTimeUtils.roundToPrecision( localTime.getNano(), 3 ) / 1000000 );
		}

		if ( value instanceof Long ) {
			return new Time( (Long) value );
		}

		if ( value instanceof Calendar ) {
			return millisToSqlTime( ( (Calendar) value ).getTimeInMillis() );
		}

		throw unknownWrap( value.getClass() );
	}

	@Override
	public String toString(Date value) {
		if ( value instanceof java.sql.Time time ) {
			return LITERAL_FORMATTER.format( time.toLocalTime() );
		}
		else {
			return LITERAL_FORMATTER.format( LocalTime.ofInstant( value.toInstant(), ZoneOffset.systemDefault() ) );
		}
	}

	@Override
	public Date fromString(CharSequence string) {
		try {
			final TemporalAccessor accessor = LITERAL_FORMATTER.parse( string );
			final LocalTime localTime = LocalTime.from( accessor );
			final Time time = Time.valueOf( localTime );
			time.setTime( time.getTime() + localTime.getNano() / 1_000_000 );
			return time;
		}
		catch ( DateTimeParseException pe) {
			throw new HibernateException( "could not parse time string " + string, pe );
		}
	}

	@Override
	public Date fromEncodedString(CharSequence charSequence, int start, int end) {
		try {
			final TemporalAccessor accessor = ENCODED_FORMATTER.parse( subSequence( charSequence, start, end ) );
			return java.sql.Time.valueOf( accessor.query( LocalTime::from ) );
		}
		catch ( DateTimeParseException pe) {
			throw new HibernateException( "could not parse time string " + charSequence, pe );
		}
	}

	@Override
	public void appendEncodedString(SqlAppender sb, Date value) {
		if ( value instanceof java.sql.Time time ) {
			LITERAL_FORMATTER.formatTo( time.toLocalTime(), sb );
		}
		else {
			LITERAL_FORMATTER.formatTo( LocalTime.ofInstant( value.toInstant(), ZoneOffset.systemDefault() ), sb );
		}
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


	public static class TimeMutabilityPlan extends MutableMutabilityPlan<Date> {
		public static final TimeMutabilityPlan INSTANCE = new TimeMutabilityPlan();

		@Override
		public Date deepCopyNotNull(Date value) {
			return new Time( value.getTime() );
		}
	}
}
