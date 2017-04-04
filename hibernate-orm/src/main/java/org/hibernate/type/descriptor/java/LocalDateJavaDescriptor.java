/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.descriptor.java;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import org.hibernate.type.LocalDateType;
import org.hibernate.type.descriptor.WrapperOptions;

/**
 * Java type descriptor for the LocalDateTime type.
 *
 * @author Steve Ebersole
 */
public class LocalDateJavaDescriptor extends AbstractTypeDescriptor<LocalDate> {
	/**
	 * Singleton access
	 */
	public static final LocalDateJavaDescriptor INSTANCE = new LocalDateJavaDescriptor();

	@SuppressWarnings("unchecked")
	public LocalDateJavaDescriptor() {
		super( LocalDate.class, ImmutableMutabilityPlan.INSTANCE );
	}

	@Override
	public String toString(LocalDate value) {
		return LocalDateType.FORMATTER.format( value );
	}

	@Override
	public LocalDate fromString(String string) {
		return LocalDate.from( LocalDateType.FORMATTER.parse( string ) );
	}

	@Override
	@SuppressWarnings("unchecked")
	public <X> X unwrap(LocalDate value, Class<X> type, WrapperOptions options) {
		if ( value == null ) {
			return null;
		}

		if ( LocalDate.class.isAssignableFrom( type ) ) {
			return (X) value;
		}

		if ( java.sql.Date.class.isAssignableFrom( type ) ) {
			return (X) java.sql.Date.valueOf( value );
		}

		final LocalDateTime localDateTime = value.atStartOfDay();

		if ( Timestamp.class.isAssignableFrom( type ) ) {
			return (X) Timestamp.valueOf( localDateTime );
		}

		final ZonedDateTime zonedDateTime = localDateTime.atZone( ZoneId.systemDefault() );

		if ( Calendar.class.isAssignableFrom( type ) ) {
			return (X) GregorianCalendar.from( zonedDateTime );
		}

		final Instant instant = zonedDateTime.toInstant();

		if ( Date.class.equals( type ) ) {
			return (X) Date.from( instant );
		}

		if ( Long.class.isAssignableFrom( type ) ) {
			return (X) Long.valueOf( instant.toEpochMilli() );
		}

		throw unknownUnwrap( type );
	}

	@Override
	public <X> LocalDate wrap(X value, WrapperOptions options) {
		if ( value == null ) {
			return null;
		}

		if ( LocalDate.class.isInstance( value ) ) {
			return (LocalDate) value;
		}

		if ( Timestamp.class.isInstance( value ) ) {
			final Timestamp ts = (Timestamp) value;
			return LocalDateTime.ofInstant( ts.toInstant(), ZoneId.systemDefault() ).toLocalDate();
		}

		if ( Long.class.isInstance( value ) ) {
			final Instant instant = Instant.ofEpochMilli( (Long) value );
			return LocalDateTime.ofInstant( instant, ZoneId.systemDefault() ).toLocalDate();
		}

		if ( Calendar.class.isInstance( value ) ) {
			final Calendar calendar = (Calendar) value;
			return LocalDateTime.ofInstant( calendar.toInstant(), calendar.getTimeZone().toZoneId() ).toLocalDate();
		}

		if ( Date.class.isInstance( value ) ) {
			if ( java.sql.Date.class.isInstance( value ) ) {
				return ((java.sql.Date) value).toLocalDate();
			}
			else {
				return Instant.ofEpochMilli( ((Date) value).getTime() ).atZone( ZoneId.systemDefault() ).toLocalDate();
			}
		}

		throw unknownWrap( value.getClass() );
	}

}
