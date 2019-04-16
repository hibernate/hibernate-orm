/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.descriptor.java;

import java.sql.Time;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import org.hibernate.type.OffsetTimeType;
import org.hibernate.type.descriptor.WrapperOptions;

/**
 * Java type descriptor for the LocalDateTime type.
 *
 * @author Steve Ebersole
 */
public class OffsetTimeJavaDescriptor extends AbstractTypeDescriptor<OffsetTime> {
	/**
	 * Singleton access
	 */
	public static final OffsetTimeJavaDescriptor INSTANCE = new OffsetTimeJavaDescriptor();

	@SuppressWarnings("unchecked")
	public OffsetTimeJavaDescriptor() {
		super( OffsetTime.class, ImmutableMutabilityPlan.INSTANCE );
	}

	@Override
	public String toString(OffsetTime value) {
		return OffsetTimeType.FORMATTER.format( value );
	}

	@Override
	public OffsetTime fromString(String string) {
		return OffsetTime.from( OffsetTimeType.FORMATTER.parse( string ) );
	}

	@Override
	@SuppressWarnings("unchecked")
	public <X> X unwrap(OffsetTime offsetTime, Class<X> type, WrapperOptions options) {
		if ( offsetTime == null ) {
			return null;
		}

		if ( OffsetTime.class.isAssignableFrom( type ) ) {
			return (X) offsetTime;
		}

		if ( java.sql.Time.class.isAssignableFrom( type ) ) {
			return (X) java.sql.Time.valueOf( offsetTime.toLocalTime() );
		}

		final ZonedDateTime zonedDateTime = offsetTime.atDate( LocalDate.of( 1970, 1, 1 ) ).toZonedDateTime();

		if ( Timestamp.class.isAssignableFrom( type ) ) {
			/*
			 * Workaround for HHH-13266 (JDK-8061577).
			 * Ideally we'd want to use Timestamp.from( offsetDateTime.toInstant() ), but this won't always work.
			 * Timestamp.from() assumes the number of milliseconds since the epoch
			 * means the same thing in Timestamp and Instant, but it doesn't, in particular before 1900.
			 */
			return (X) Timestamp.valueOf( zonedDateTime.toLocalDateTime() );
		}

		if ( Calendar.class.isAssignableFrom( type ) ) {
			return (X) GregorianCalendar.from( zonedDateTime );
		}

		final Instant instant = zonedDateTime.toInstant();

		if ( Long.class.isAssignableFrom( type ) ) {
			return (X) Long.valueOf( instant.toEpochMilli() );
		}

		if ( java.util.Date.class.isAssignableFrom( type ) ) {
			return (X) java.util.Date.from( instant );
		}

		throw unknownUnwrap( type );
	}

	@Override
	public <X> OffsetTime wrap(X value, WrapperOptions options) {
		if ( value == null ) {
			return null;
		}

		if ( OffsetTime.class.isInstance( value ) ) {
			return (OffsetTime) value;
		}

		/*
		 * Also, in order to fix HHH-13357, and to be consistent with the conversion to Time (see above),
		 * we set the offset to the current offset of the JVM (OffsetDateTime.now().getOffset()).
		 * This is different from setting the *zone* to the current *zone* of the JVM (ZoneId.systemDefault()),
		 * since a zone has a varying offset over time,
		 * thus the zone might have a different offset for the given timezone than it has for the current date/time.
		 * For example, if the timestamp represents 1970-01-01TXX:YY,
		 * and the JVM is set to use Europe/Paris as a timezone, and the current time is 2019-04-16-08:53,
		 * then applying the JVM timezone to the timestamp would result in the offset +01:00,
		 * but applying the JVM offset would result in the offset +02:00, since DST is in effect at 2019-04-16-08:53.
		 *
		 * Of course none of this would be a problem if we just stored the offset in the database,
		 * but I guess there are historical reasons that explain why we don't.
		 */
		ZoneOffset offset = OffsetDateTime.now().getOffset();

		if ( Time.class.isInstance( value ) ) {
			return ( (Time) value ).toLocalTime().atOffset( offset );
		}

		if ( Timestamp.class.isInstance( value ) ) {
			final Timestamp ts = (Timestamp) value;
			/*
			 * Workaround for HHH-13266 (JDK-8061577).
			 * Ideally we'd want to use OffsetDateTime.ofInstant( ts.toInstant(), ... ), but this won't always work.
			 * ts.toInstant() assumes the number of milliseconds since the epoch
			 * means the same thing in Timestamp and Instant, but it doesn't, in particular before 1900.
			 */
			return ts.toLocalDateTime().toLocalTime().atOffset( offset );
		}

		if ( Date.class.isInstance( value ) ) {
			final Date date = (Date) value;
			return OffsetTime.ofInstant( date.toInstant(), offset );
		}

		if ( Long.class.isInstance( value ) ) {
			return OffsetTime.ofInstant( Instant.ofEpochMilli( (Long) value ), offset );
		}

		if ( Calendar.class.isInstance( value ) ) {
			final Calendar calendar = (Calendar) value;
			return OffsetTime.ofInstant( calendar.toInstant(), calendar.getTimeZone().toZoneId() );
		}

		throw unknownWrap( value.getClass() );
	}
}
