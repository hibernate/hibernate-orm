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

import java.sql.Timestamp;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import org.hibernate.type.InstantType;
import org.hibernate.type.ZonedDateTimeType;
import org.hibernate.type.descriptor.WrapperOptions;

/**
 * Java type descriptor for the LocalDateTime type.
 *
 * @author Steve Ebersole
 */
public class ZonedDateTimeJavaDescriptor extends AbstractTypeDescriptor<ZonedDateTime> {
	/**
	 * Singleton access
	 */
	public static final ZonedDateTimeJavaDescriptor INSTANCE = new ZonedDateTimeJavaDescriptor();

	@SuppressWarnings("unchecked")
	public ZonedDateTimeJavaDescriptor() {
		super( ZonedDateTime.class, ImmutableMutabilityPlan.INSTANCE );
	}

	@Override
	public String toString(ZonedDateTime value) {
		return ZonedDateTimeType.FORMATTER.format( value );
	}

	@Override
	public ZonedDateTime fromString(String string) {
		return (ZonedDateTime) ZonedDateTimeType.FORMATTER.parse( string );
	}

	@Override
	@SuppressWarnings("unchecked")
	public <X> X unwrap(ZonedDateTime zonedDateTime, Class<X> type, WrapperOptions options) {
		if ( zonedDateTime == null ) {
			return null;
		}

		if ( ZonedDateTime.class.isAssignableFrom( type ) ) {
			return (X) zonedDateTime;
		}

		if ( Calendar.class.isAssignableFrom( type ) ) {
			return (X) GregorianCalendar.from( zonedDateTime );
		}

		if ( Timestamp.class.isAssignableFrom( type ) ) {
			return (X) Timestamp.from( zonedDateTime.toInstant() );
		}

		if ( java.sql.Date.class.isAssignableFrom( type ) ) {
			return (X) java.sql.Date.from( zonedDateTime.toInstant() );
		}

		if ( java.sql.Time.class.isAssignableFrom( type ) ) {
			return (X) java.sql.Time.from( zonedDateTime.toInstant() );
		}

		if ( Date.class.isAssignableFrom( type ) ) {
			return (X) Date.from( zonedDateTime.toInstant() );
		}

		if ( Long.class.isAssignableFrom( type ) ) {
			return (X) Long.valueOf( zonedDateTime.toInstant().toEpochMilli() );
		}

		throw unknownUnwrap( type );
	}

	@Override
	public <X> ZonedDateTime wrap(X value, WrapperOptions options) {
		if ( value == null ) {
			return null;
		}

		if ( ZonedDateTime.class.isInstance( value ) ) {
			return (ZonedDateTime) value;
		}

		if ( java.sql.Timestamp.class.isInstance( value ) ) {
			final Timestamp ts = (Timestamp) value;
			return ZonedDateTime.ofInstant( ts.toInstant(), ZoneId.systemDefault() );
		}

		if ( java.util.Date.class.isInstance( value ) ) {
			final java.util.Date date = (java.util.Date) value;
			return ZonedDateTime.ofInstant( date.toInstant(), ZoneId.systemDefault() );
		}

		if ( Long.class.isInstance( value ) ) {
			return ZonedDateTime.ofInstant( Instant.ofEpochMilli( (Long) value ), ZoneId.systemDefault() );
		}

		if ( Calendar.class.isInstance( value ) ) {
			final Calendar calendar = (Calendar) value;
			return ZonedDateTime.ofInstant( calendar.toInstant(), calendar.getTimeZone().toZoneId() );
		}

		throw unknownWrap( value.getClass() );
	}
}
