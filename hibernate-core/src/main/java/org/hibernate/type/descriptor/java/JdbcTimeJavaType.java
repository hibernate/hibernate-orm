/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.descriptor.java;

import java.sql.Time;
import java.sql.Types;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import org.hibernate.HibernateException;
import org.hibernate.dialect.Dialect;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.JdbcTypeIndicators;
import org.hibernate.type.spi.TypeConfiguration;

import jakarta.persistence.TemporalType;

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

	public static final String TIME_FORMAT = "HH:mm:ss.SSS";

	public static final DateTimeFormatter LITERAL_FORMATTER = DateTimeFormatter.ISO_LOCAL_TIME;

	/**
	 * Alias for {@link DateTimeFormatter#ISO_LOCAL_TIME}.
	 *
	 * Intended for use with logging
	 *
	 * @see #LITERAL_FORMATTER
	 */
	@SuppressWarnings("unused")
	public static final DateTimeFormatter LOGGABLE_FORMATTER = DateTimeFormatter.ISO_LOCAL_TIME;

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
			return value instanceof java.sql.Time
					? ( (java.sql.Time) value ).toLocalTime()
					: new java.sql.Time( value.getTime() ).toLocalTime();
		}

		if ( Time.class.isAssignableFrom( type ) ) {
			return value instanceof Time
					? value
					: new Time( value.getTime() );
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

		if ( java.sql.Timestamp.class.isAssignableFrom( type ) ) {
			return new java.sql.Timestamp( value.getTime() );
		}

		if ( java.sql.Date.class.isAssignableFrom( type ) ) {
			throw new IllegalArgumentException( "Illegal attempt to treat `java.sql.Time` as `java.sql.Date`" );
		}

		throw unknownUnwrap( type );
	}

	@Override
	public Date wrap(Object value, WrapperOptions options) {
		if ( value == null ) {
			return null;
		}

		if ( value instanceof LocalTime ) {
			return Time.valueOf( (LocalTime) value );
		}

		if ( value instanceof Date ) {
			return (Date) value;
		}

		if ( value instanceof Long ) {
			return new Time( (Long) value );
		}

		if ( value instanceof Calendar ) {
			return new Time( ( (Calendar) value ).getTimeInMillis() );
		}

		throw unknownWrap( value.getClass() );
	}

	@Override
	public String toString(Date value) {
		return new SimpleDateFormat( TIME_FORMAT ).format( value );
	}

	@Override
	public Date fromString(CharSequence string) {
		try {
			return new Time( new SimpleDateFormat( TIME_FORMAT ).parse( string.toString() ).getTime() );
		}
		catch ( ParseException pe ) {
			throw new HibernateException( "could not parse time string" + string, pe );
		}
	}

	@Override
	public JdbcType getRecommendedJdbcType(JdbcTypeIndicators context) {
		return context.getTypeConfiguration().getJdbcTypeRegistry().getDescriptor( Types.TIME );
	}

	@Override
	public int getDefaultSqlPrecision(Dialect dialect, JdbcType jdbcType) {
		//seconds (currently ignored since Dialects don't parameterize time type by precision)
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
