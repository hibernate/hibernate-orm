/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2015, Red Hat Inc. or third-party contributors as
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

import java.sql.Time;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import org.hibernate.type.LocalTimeType;
import org.hibernate.type.descriptor.WrapperOptions;

/**
 * Java type descriptor for the LocalDateTime type.
 *
 * @author Steve Ebersole
 */
public class LocalTimeJavaDescriptor extends AbstractTypeDescriptor<LocalTime> {
	/**
	 * Singleton access
	 */
	public static final LocalTimeJavaDescriptor INSTANCE = new LocalTimeJavaDescriptor();

	@SuppressWarnings("unchecked")
	public LocalTimeJavaDescriptor() {
		super( LocalTime.class, ImmutableMutabilityPlan.INSTANCE );
	}

	@Override
	public String toString(LocalTime value) {
		return LocalTimeType.FORMATTER.format( value );
	}

	@Override
	public LocalTime fromString(String string) {
		return (LocalTime) LocalTimeType.FORMATTER.parse( string );
	}

	@Override
	@SuppressWarnings("unchecked")
	public <X> X unwrap(LocalTime value, Class<X> type, WrapperOptions options) {
		if ( value == null ) {
			return null;
		}

		if ( LocalDate.class.isAssignableFrom( type ) ) {
			return (X) value;
		}

		if ( Time.class.isAssignableFrom( type ) ) {
			return (X) Time.valueOf( value );
		}

		// Oracle documentation says to set the Date to January 1, 1970 when convert from
		// a LocalTime to a Calendar.  IMO the same should hold true for converting to all
		// the legacy Date/Time types...


		final ZonedDateTime zonedDateTime = value.atDate( LocalDate.of( 1970, 1, 1 ) ).atZone( ZoneId.systemDefault() );

		if ( Calendar.class.isAssignableFrom( type ) ) {
			return (X) GregorianCalendar.from( zonedDateTime );
		}

		final Instant instant = zonedDateTime.toInstant();

		if ( Timestamp.class.isAssignableFrom( type ) ) {
			return (X) Timestamp.from( instant );
		}

		if ( Date.class.equals( type ) ) {
			return (X) Date.from( instant );
		}

		if ( Long.class.isAssignableFrom( type ) ) {
			return (X) Long.valueOf( instant.toEpochMilli() );
		}

		throw unknownUnwrap( type );
	}

	@Override
	public <X> LocalTime wrap(X value, WrapperOptions options) {
		if ( value == null ) {
			return null;
		}

		if ( LocalTime.class.isInstance( value ) ) {
			return (LocalTime) value;
		}

		if ( Timestamp.class.isInstance( value ) ) {
			final Timestamp ts = (Timestamp) value;
			return LocalDateTime.ofInstant( ts.toInstant(), ZoneId.systemDefault() ).toLocalTime();
		}

		if ( Long.class.isInstance( value ) ) {
			final Instant instant = Instant.ofEpochMilli( (Long) value );
			return LocalDateTime.ofInstant( instant, ZoneId.systemDefault() ).toLocalTime();
		}

		if ( Calendar.class.isInstance( value ) ) {
			final Calendar calendar = (Calendar) value;
			return LocalDateTime.ofInstant( calendar.toInstant(), calendar.getTimeZone().toZoneId() ).toLocalTime();
		}

		if ( Date.class.isInstance( value ) ) {
			final Date ts = (Date) value;
			final Instant instant = Instant.ofEpochMilli( ts.getTime() );
			return LocalDateTime.ofInstant( instant, ZoneId.systemDefault() ).toLocalTime();
		}

		throw unknownWrap( value.getClass() );
	}
}
