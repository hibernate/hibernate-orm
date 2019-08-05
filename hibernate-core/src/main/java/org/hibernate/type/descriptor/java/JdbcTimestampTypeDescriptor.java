/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.descriptor.java;

import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import org.hibernate.HibernateException;
import org.hibernate.type.descriptor.WrapperOptions;

/**
 * Descriptor for {@link Timestamp} handling.
 *
 * @author Steve Ebersole
 */
public class JdbcTimestampTypeDescriptor extends AbstractTypeDescriptor<Date> {
	public static final JdbcTimestampTypeDescriptor INSTANCE = new JdbcTimestampTypeDescriptor();

	@SuppressWarnings("WeakerAccess")
	public static final String TIMESTAMP_FORMAT = "yyyy-MM-dd HH:mm:ss.SSS";

	/**
	 * Intended for use in reading HQL literals and writing SQL literals
	 *
	 * @see #TIMESTAMP_FORMAT
	 */
	@SuppressWarnings("unused")
	public static final DateTimeFormatter LITERAL_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

	/**
	 * Alias for {@link java.time.format.DateTimeFormatter#ISO_LOCAL_DATE_TIME}.
	 *
	 * Intended for use with logging
	 *
	 * @see #LITERAL_FORMATTER
	 */
	@SuppressWarnings({"unused", "WeakerAccess"})
	public static final DateTimeFormatter LOGGABLE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

	public static class TimestampMutabilityPlan extends MutableMutabilityPlan<Date> {
		public static final TimestampMutabilityPlan INSTANCE = new TimestampMutabilityPlan();
		@Override
		public Date deepCopyNotNull(Date value) {
			if ( value instanceof Timestamp ) {
				Timestamp orig = (Timestamp) value;
				Timestamp ts = new Timestamp( orig.getTime() );
				ts.setNanos( orig.getNanos() );
				return ts;
			}
			else {
				return new Date( value.getTime() );
			}
		}
	}

	@SuppressWarnings("WeakerAccess")
	public JdbcTimestampTypeDescriptor() {
		super( Date.class, TimestampMutabilityPlan.INSTANCE );
	}

	@Override
	public String toString(Date value) {
		return LOGGABLE_FORMATTER.format( value.toInstant() );
	}

	@Override
	public Date fromString(String string) {
		try {
			return new Timestamp( new SimpleDateFormat( TIMESTAMP_FORMAT ).parse( string ).getTime() );
		}
		catch ( ParseException pe) {
			throw new HibernateException( "could not parse timestamp string" + string, pe );
		}
	}

	@Override
	public boolean areEqual(Date one, Date another) {
		if ( one == another ) {
			return true;
		}
		if ( one == null || another == null) {
			return false;
		}

		long t1 = one.getTime();
		long t2 = another.getTime();

		boolean oneIsTimestamp = one instanceof Timestamp;
		boolean anotherIsTimestamp = another instanceof Timestamp;

		int n1 = oneIsTimestamp ? ( (Timestamp) one ).getNanos() : 0;
		int n2 = anotherIsTimestamp ? ( (Timestamp) another ).getNanos() : 0;

		if ( t1 != t2 ) {
			return false;
		}

		if ( oneIsTimestamp && anotherIsTimestamp ) {
			// both are Timestamps
			int nn1 = n1 % 1000000;
			int nn2 = n2 % 1000000;
			return nn1 == nn2;
		}
		else {
			// at least one is a plain old Date
			return true;
		}
	}

	@Override
	public int extractHashCode(Date value) {
		return Long.valueOf( value.getTime() / 1000 ).hashCode();
	}

	@SuppressWarnings({ "unchecked" })
	@Override
	public <X> X unwrap(Date value, Class<X> type, WrapperOptions options) {
		if ( value == null ) {
			return null;
		}
		if ( Timestamp.class.isAssignableFrom( type ) ) {
			final Timestamp rtn = value instanceof Timestamp
					? ( Timestamp ) value
					: new Timestamp( value.getTime() );
			return (X) rtn;
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
		if ( value instanceof Timestamp ) {
			return (Timestamp) value;
		}

		if ( value instanceof Long ) {
			return new Timestamp( (Long) value );
		}

		if ( value instanceof Calendar ) {
			return new Timestamp( ( (Calendar) value ).getTimeInMillis() );
		}

		if ( value instanceof Date ) {
			return new Timestamp( ( (Date) value ).getTime() );
		}

		throw unknownWrap( value.getClass() );
	}
}
