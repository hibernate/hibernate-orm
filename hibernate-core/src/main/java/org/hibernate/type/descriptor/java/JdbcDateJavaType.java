/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type.descriptor.java;

import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAccessor;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import org.hibernate.HibernateException;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.JdbcTypeIndicators;
import org.hibernate.type.spi.TypeConfiguration;

import jakarta.persistence.TemporalType;

import static org.hibernate.internal.util.CharSequenceHelper.subSequence;

/**
 * Descriptor for {@link java.sql.Date} handling.
 *
 * @implSpec Unlike most {@link JavaType} implementations, can handle 2 different "domain
 * representations" (most map just a single type): general {@link Date} values in addition
 * to {@link java.sql.Date} values.  This capability is shared with
 * {@link JdbcTimeJavaType} and {@link JdbcTimestampJavaType}.
 */
public class JdbcDateJavaType extends AbstractTemporalJavaType<Date> {
	public static final JdbcDateJavaType INSTANCE = new JdbcDateJavaType();

	/**
	 * Intended for use in reading HQL literals and writing SQL literals
	 *
	 * @see DateTimeFormatter#ISO_LOCAL_DATE
	 */
	public static final DateTimeFormatter LITERAL_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;

	private static final DateTimeFormatter ENCODED_FORMATTER = new DateTimeFormatterBuilder()
			.append( DateTimeFormatter.ISO_DATE )
			.optionalStart()
			.appendLiteral( 'T' )
			.append( DateTimeFormatter.ISO_LOCAL_TIME )
			.toFormatter();

	public JdbcDateJavaType() {
		super( java.sql.Date.class, DateMutabilityPlan.INSTANCE );
	}

	@Override
	public TemporalType getPrecision() {
		return TemporalType.DATE;
	}

	@Override
	public boolean isInstance(Object value) {
		// this check holds true for java.sql.Date as well
		return value instanceof Date
			&& !( value instanceof java.sql.Time );
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

		return calendar1.get( Calendar.MONTH ) == calendar2.get( Calendar.MONTH )
			&& calendar1.get( Calendar.DAY_OF_MONTH ) == calendar2.get( Calendar.DAY_OF_MONTH )
			&& calendar1.get( Calendar.YEAR ) == calendar2.get( Calendar.YEAR );
	}

	@Override
	public int extractHashCode(Date value) {
		final Calendar calendar = Calendar.getInstance();
		calendar.setTime( value );
		int hashCode = 1;
		hashCode = 31 * hashCode + calendar.get( Calendar.MONTH );
		hashCode = 31 * hashCode + calendar.get( Calendar.DAY_OF_MONTH );
		hashCode = 31 * hashCode + calendar.get( Calendar.YEAR );
		return hashCode;
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

		if ( LocalDate.class.isAssignableFrom( type ) ) {
			return unwrapLocalDate( value );
		}

		if ( java.sql.Date.class.isAssignableFrom( type ) ) {
			return unwrapSqlDate( value );
		}

		if ( java.sql.Timestamp.class.isAssignableFrom( type ) ) {
			return new java.sql.Timestamp( unwrapDateEpoch( value ) );
		}

		if ( java.sql.Time.class.isAssignableFrom( type ) ) {
			throw new IllegalArgumentException( "Illegal attempt to treat `java.sql.Date` as `java.sql.Time`" );
		}

		if ( java.util.Date.class.isAssignableFrom( type ) ) {
			return value;
		}

		if ( Long.class.isAssignableFrom( type ) ) {
			return unwrapDateEpoch( value );
		}

		if ( String.class.isAssignableFrom( type ) ) {
			return toString( value );
		}

		if ( Calendar.class.isAssignableFrom( type ) ) {
			final GregorianCalendar cal = new GregorianCalendar();
			cal.setTimeInMillis( unwrapDateEpoch( value ) );
			return cal;
		}

		throw unknownUnwrap( type );
	}

	private LocalDate unwrapLocalDate(Date value) {
		return value instanceof java.sql.Date date
				? date.toLocalDate()
				: new java.sql.Date( unwrapDateEpoch( value ) ).toLocalDate();
	}

	private java.sql.Date unwrapSqlDate(Date value) {
		if ( value instanceof java.sql.Date date ) {
			final long dateEpoch = toDateEpoch( date.getTime() );
			return dateEpoch == date.getTime() ? date : new java.sql.Date( dateEpoch );
		}
		return new java.sql.Date( unwrapDateEpoch( value ) );

	}

	private static long unwrapDateEpoch(Date value) {
		return toDateEpoch( value.getTime() );
	}

	private static long toDateEpoch(long value) {
		Calendar calendar = Calendar.getInstance();
		calendar.setTimeInMillis( value );
		calendar.set(Calendar.HOUR_OF_DAY, 0);
		calendar.clear(Calendar.MINUTE);
		calendar.clear(Calendar.SECOND);
		calendar.clear(Calendar.MILLISECOND);
		return calendar.getTimeInMillis();
	}

	@Override
	public Date wrap(Object value, WrapperOptions options) {
		if ( value == null ) {
			return null;
		}

		if ( value instanceof java.sql.Date ) {
			return (java.sql.Date) value;
		}

		if ( value instanceof Long ) {
			return new java.sql.Date( toDateEpoch( (Long) value ) );
		}

		if ( value instanceof Calendar ) {
			return new java.sql.Date( toDateEpoch( ( (Calendar) value ).getTimeInMillis() ) );
		}

		if ( value instanceof Date ) {
			return unwrapSqlDate( (Date) value );
		}

		if ( value instanceof LocalDate ) {
			return java.sql.Date.valueOf( (LocalDate) value );
		}

		throw unknownWrap( value.getClass() );
	}

	@Override
	public String toString(Date value) {
		if ( value instanceof java.sql.Date ) {
			return LITERAL_FORMATTER.format( ( (java.sql.Date) value ).toLocalDate() );
		}
		else {
			return LITERAL_FORMATTER.format( LocalDate.ofInstant( value.toInstant(), ZoneOffset.systemDefault() ) );
		}
	}

	@Override
	public Date fromString(CharSequence string) {
		try {
			final TemporalAccessor accessor = LITERAL_FORMATTER.parse( string );
			return java.sql.Date.valueOf( accessor.query( LocalDate::from ) );
		}
		catch ( DateTimeParseException pe) {
			throw new HibernateException( "could not parse date string " + string, pe );
		}
	}

	@Override
	public Date fromEncodedString(CharSequence charSequence, int start, int end) {
		try {
			final TemporalAccessor accessor = ENCODED_FORMATTER.parse( subSequence( charSequence, start, end ) );
			return java.sql.Date.valueOf( accessor.query( LocalDate::from ) );
		}
		catch ( DateTimeParseException pe) {
			throw new HibernateException( "could not parse time string " + charSequence, pe );
		}
	}

	@Override
	public void appendEncodedString(SqlAppender sb, Date value) {
		if ( value instanceof java.sql.Date ) {
			LITERAL_FORMATTER.formatTo( ( (java.sql.Date) value ).toLocalDate(), sb );
		}
		else {
			LITERAL_FORMATTER.formatTo( LocalTime.ofInstant( value.toInstant(), ZoneOffset.systemDefault() ), sb );
		}
	}

	@Override
	public JdbcType getRecommendedJdbcType(JdbcTypeIndicators context) {
		return context.getJdbcType( Types.DATE );
	}

	@Override
	protected <X> TemporalJavaType<X> forDatePrecision(TypeConfiguration typeConfiguration) {
		//noinspection unchecked
		return (TemporalJavaType<X>) this;
	}

	public static class DateMutabilityPlan extends MutableMutabilityPlan<Date> {
		public static final DateMutabilityPlan INSTANCE = new DateMutabilityPlan();

		@Override
		public Date deepCopyNotNull(Date value) {
			if ( value instanceof java.sql.Date ) {
				return value;
			}

			return new java.sql.Date( value.getTime() );
		}
	}
}
