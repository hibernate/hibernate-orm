/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.type.descriptor.java;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import org.hibernate.HibernateException;
import org.hibernate.type.descriptor.WrapperOptions;

/**
 * Descriptor for {@link Date} handling.
 *
 * @author Steve Ebersole
 */
public class DateTypeDescriptor extends AbstractTypeDescriptor<Date> {
	public static final DateTypeDescriptor INSTANCE = new DateTypeDescriptor();
	public static final String DATE_FORMAT = "dd MMMM yyyy";

	public static class DateMutabilityPlan extends MutableMutabilityPlan<Date> {
		public static final DateMutabilityPlan INSTANCE = new DateMutabilityPlan();
		@Override
		public Date deepCopyNotNull(Date value) {
			return new Date( value.getTime() );
		}
	}

	public DateTypeDescriptor() {
		super( Date.class, DateMutabilityPlan.INSTANCE );
	}
	@Override
	public String toString(Date value) {
		return new SimpleDateFormat( DATE_FORMAT ).format( value );
	}
	@Override
	public Date fromString(String string) {
		try {
			return new SimpleDateFormat(DATE_FORMAT).parse( string );
		}
		catch ( ParseException pe) {
			throw new HibernateException( "could not parse date string" + string, pe );
		}
	}

	@Override
	public boolean areEqual(Date one, Date another) {
		if ( one == another) {
			return true;
		}
		return !( one == null || another == null ) && one.getTime() == another.getTime();

	}

	@Override
	public int extractHashCode(Date value) {
		Calendar calendar = java.util.Calendar.getInstance();
		calendar.setTime( value );
		return CalendarTypeDescriptor.INSTANCE.extractHashCode( calendar );
	}

	@SuppressWarnings({ "unchecked" })
	@Override
	public <X> X unwrap(Date value, Class<X> type, WrapperOptions options) {
		if ( value == null ) {
			return null;
		}
		if ( java.sql.Date.class.isAssignableFrom( type ) ) {
			final java.sql.Date rtn = java.sql.Date.class.isInstance( value )
					? ( java.sql.Date ) value
					: new java.sql.Date( value.getTime() );
			return (X) rtn;
		}
		if ( java.sql.Time.class.isAssignableFrom( type ) ) {
			final java.sql.Time rtn = java.sql.Time.class.isInstance( value )
					? ( java.sql.Time ) value
					: new java.sql.Time( value.getTime() );
			return (X) rtn;
		}
		if ( java.sql.Timestamp.class.isAssignableFrom( type ) ) {
			final java.sql.Timestamp rtn = java.sql.Timestamp.class.isInstance( value )
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
		if ( Date.class.isInstance( value ) ) {
			return (Date) value;
		}

		if ( Long.class.isInstance( value ) ) {
			return new Date( (Long) value );
		}

		if ( Calendar.class.isInstance( value ) ) {
			return new Date( ( (Calendar) value ).getTimeInMillis() );
		}

		throw unknownWrap( value.getClass() );
	}
}
