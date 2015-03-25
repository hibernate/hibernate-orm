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
import org.hibernate.type.descriptor.WrapperOptions;

/**
 * Java type descriptor for the LocalDateTime type.
 *
 * @author Steve Ebersole
 */
public class InstantJavaDescriptor extends AbstractTypeDescriptor<Instant> {
	/**
	 * Singleton access
	 */
	public static final InstantJavaDescriptor INSTANCE = new InstantJavaDescriptor();

	@SuppressWarnings("unchecked")
	public InstantJavaDescriptor() {
		super( Instant.class, ImmutableMutabilityPlan.INSTANCE );
	}

	@Override
	public String toString(Instant value) {
		return InstantType.FORMATTER.format( ZonedDateTime.ofInstant( value, ZoneId.of( "UTC" ) ) );
	}

	@Override
	public Instant fromString(String string) {
		return (Instant) InstantType.FORMATTER.parse( string );
	}

	@Override
	@SuppressWarnings("unchecked")
	public <X> X unwrap(Instant instant, Class<X> type, WrapperOptions options) {
		if ( instant == null ) {
			return null;
		}

		if ( Instant.class.isAssignableFrom( type ) ) {
			return (X) instant;
		}

		if ( Calendar.class.isAssignableFrom( type ) ) {
			final ZoneId zoneId = ZoneId.ofOffset( "UTC", ZoneOffset.UTC );
			return (X) GregorianCalendar.from( instant.atZone( zoneId ) );
		}

		if ( java.sql.Timestamp.class.isAssignableFrom( type ) ) {
			return (X) Timestamp.from( instant );
		}

		if ( java.sql.Date.class.isAssignableFrom( type ) ) {
			return (X) java.sql.Date.from( instant );
		}

		if ( java.sql.Time.class.isAssignableFrom( type ) ) {
			return (X) java.sql.Time.from( instant );
		}

		if ( java.util.Date.class.isAssignableFrom( type ) ) {
			return (X) Date.from( instant );
		}

		if ( Long.class.isAssignableFrom( type ) ) {
			return (X) Long.valueOf( instant.toEpochMilli() );
		}

		throw unknownUnwrap( type );
	}

	@Override
	public <X> Instant wrap(X value, WrapperOptions options) {
		if ( value == null ) {
			return null;
		}

		if ( Instant.class.isInstance( value ) ) {
			return (Instant) value;
		}

		if ( Timestamp.class.isInstance( value ) ) {
			final Timestamp ts = (Timestamp) value;
			return ts.toInstant();
		}

		if ( Long.class.isInstance( value ) ) {
			return Instant.ofEpochMilli( (Long) value );
		}

		if ( Calendar.class.isInstance( value ) ) {
			final Calendar calendar = (Calendar) value;
			return ZonedDateTime.ofInstant( calendar.toInstant(), calendar.getTimeZone().toZoneId() ).toInstant();
		}

		if ( java.util.Date.class.isInstance( value ) ) {
			return ( (java.util.Date) value ).toInstant();
		}

		throw unknownWrap( value.getClass() );
	}
}
