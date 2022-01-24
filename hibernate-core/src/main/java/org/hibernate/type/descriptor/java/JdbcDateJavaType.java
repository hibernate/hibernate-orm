/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.descriptor.java;

import java.sql.Types;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import org.hibernate.HibernateException;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.JdbcTypeIndicators;
import org.hibernate.type.spi.TypeConfiguration;

import jakarta.persistence.TemporalType;

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

	public static final String DATE_FORMAT = "dd MMMM yyyy";

	/**
	 * Intended for use in reading HQL literals and writing SQL literals
	 *
	 * @see #DATE_FORMAT
	 */
	@SuppressWarnings("unused")
	public static final DateTimeFormatter LITERAL_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;

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

		if ( java.sql.Timestamp.class.isAssignableFrom( type ) ) {
			return new java.sql.Timestamp( value.getTime() );
		}

		if ( java.sql.Time.class.isAssignableFrom( type ) ) {
			throw new IllegalArgumentException( "Illegal attempt to treat `java.sql.Date` as `java.sql.Time`" );
		}

		throw unknownUnwrap( type );
	}

	private LocalDate unwrapLocalDate(Date value) {
		return value instanceof java.sql.Date
				? ( (java.sql.Date) value ).toLocalDate()
				: new java.sql.Date( value.getTime() ).toLocalDate();
	}

	private java.sql.Date unwrapSqlDate(Date value) {
		return value instanceof java.sql.Date
				? (java.sql.Date) value
				: new java.sql.Date( value.getTime() );

	}

	private static long unwrapDateEpoch(Date value) {
		return value.getTime();
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
			return new java.sql.Date( (Long) value );
		}

		if ( value instanceof Calendar ) {
			return new java.sql.Date( ( (Calendar) value ).getTimeInMillis() );
		}

		if ( value instanceof Date ) {
			return new java.sql.Date( ( (Date) value ).getTime() );
		}

		if ( value instanceof LocalDate ) {
			return java.sql.Date.valueOf( (LocalDate) value );
		}

		throw unknownWrap( value.getClass() );
	}

	@Override
	public String toString(Date value) {
		return new SimpleDateFormat( DATE_FORMAT ).format( value );
	}

	@Override
	public Date fromString(CharSequence string) {
		try {
			return new java.sql.Date( new SimpleDateFormat(DATE_FORMAT).parse( string.toString() ).getTime() );
		}
		catch ( ParseException pe) {
			throw new HibernateException( "could not parse date string" + string, pe );
		}
	}

	@Override
	public JdbcType getRecommendedJdbcType(JdbcTypeIndicators context) {
		return context.getTypeConfiguration().getJdbcTypeRegistry().getDescriptor( Types.DATE );
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
