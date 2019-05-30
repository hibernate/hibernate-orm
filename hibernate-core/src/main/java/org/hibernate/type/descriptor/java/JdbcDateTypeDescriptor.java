/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.descriptor.java;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import org.hibernate.HibernateException;
import org.hibernate.type.descriptor.WrapperOptions;

/**
 * Descriptor for {@link java.sql.Date} handling.
 *
 * @author Steve Ebersole
 */
public class JdbcDateTypeDescriptor extends AbstractTypeDescriptor<Date> {
	public static final JdbcDateTypeDescriptor INSTANCE = new JdbcDateTypeDescriptor();

	@SuppressWarnings("WeakerAccess")
	public static final String DATE_FORMAT = "dd MMMM yyyy";

	/**
	 * Intended for use in reading HQL literals and writing SQL literals
	 *
	 * @see #DATE_FORMAT
	 */
	@SuppressWarnings("unused")
	public static final DateTimeFormatter LITERAL_FORMATTER = DateTimeFormatter.ofPattern( DATE_FORMAT );

	/**
	 * Alias for {@link java.time.format.DateTimeFormatter#ISO_LOCAL_DATE}.
	 *
	 * Intended for use with logging
	 *
	 * @see #LITERAL_FORMATTER
	 */
	@SuppressWarnings({"unused", "WeakerAccess"})
	public static final DateTimeFormatter LOGGABLE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;

	public static class DateMutabilityPlan extends MutableMutabilityPlan<Date> {
		public static final DateMutabilityPlan INSTANCE = new DateMutabilityPlan();
		@Override
		public Date deepCopyNotNull(Date value) {
			return value instanceof java.sql.Date
					? new java.sql.Date( value.getTime() )
					: new Date( value.getTime() );
		}
	}

	@SuppressWarnings("WeakerAccess")
	public JdbcDateTypeDescriptor() {
		super( Date.class, DateMutabilityPlan.INSTANCE );
	}

	@Override
	public String toString(Date value) {
		return LOGGABLE_FORMATTER.format( value.toInstant() );
	}

	@Override
	public Date fromString(String string) {
		try {
			return new Date( new SimpleDateFormat(DATE_FORMAT).parse( string ).getTime() );
		}
		catch ( ParseException pe) {
			throw new HibernateException( "could not parse date string" + string, pe );
		}
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

		Calendar calendar1 = Calendar.getInstance();
		Calendar calendar2 = Calendar.getInstance();
		calendar1.setTime( one );
		calendar2.setTime( another );

		return calendar1.get( Calendar.MONTH ) == calendar2.get( Calendar.MONTH )
				&& calendar1.get( Calendar.DAY_OF_MONTH ) == calendar2.get( Calendar.DAY_OF_MONTH )
				&& calendar1.get( Calendar.YEAR ) == calendar2.get( Calendar.YEAR );
	}

	@Override
	public int extractHashCode(Date value) {
		Calendar calendar = Calendar.getInstance();
		calendar.setTime( value );
		int hashCode = 1;
		hashCode = 31 * hashCode + calendar.get( Calendar.MONTH );
		hashCode = 31 * hashCode + calendar.get( Calendar.DAY_OF_MONTH );
		hashCode = 31 * hashCode + calendar.get( Calendar.YEAR );
		return hashCode;
	}

	@SuppressWarnings({ "unchecked" })
	@Override
	public <X> X unwrap(Date value, Class<X> type, WrapperOptions options) {
		if ( value == null ) {
			return null;
		}
		if ( java.sql.Date.class.isAssignableFrom( type ) ) {
			final java.sql.Date rtn = value instanceof java.sql.Date
					? ( java.sql.Date ) value
					: new java.sql.Date( value.getTime() );
			return (X) rtn;
		}
		if ( java.sql.Time.class.isAssignableFrom( type ) ) {
			final java.sql.Time rtn = value instanceof java.sql.Time
					? ( java.sql.Time ) value
					: new java.sql.Time( value.getTime() );
			return (X) rtn;
		}
		if ( java.sql.Timestamp.class.isAssignableFrom( type ) ) {
			final java.sql.Timestamp rtn = value instanceof java.sql.Timestamp
					? ( java.sql.Timestamp ) value
					: new java.sql.Timestamp( value.getTime() );
			return (X) rtn;
		}
		if ( Date.class.isAssignableFrom( type ) ) {
			return (X) value;
		}
		if ( Calendar.class.isAssignableFrom( type ) ) {
			final GregorianCalendar cal = new GregorianCalendar();
			cal.setTimeInMillis( value.getTime() );
			return (X) cal;
		}
		if ( Long.class.isAssignableFrom( type ) ) {
			return (X) Long.valueOf( value.getTime() );
		}
		throw unknownUnwrap( type );
	}

	@Override
	public <X> Date wrap(X value, WrapperOptions options) {
		if ( value == null ) {
			return null;
		}
		if ( value instanceof java.sql.Date ) {
			return (Date) value;
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

		throw unknownWrap( value.getClass() );
	}
}
