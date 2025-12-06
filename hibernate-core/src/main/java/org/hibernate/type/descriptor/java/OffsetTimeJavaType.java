/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type.descriptor.java;

import java.sql.Time;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoField;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import org.hibernate.dialect.Dialect;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.JdbcTypeIndicators;
import org.hibernate.type.spi.TypeConfiguration;

import jakarta.persistence.TemporalType;

import static org.hibernate.type.descriptor.DateTimeUtils.roundToPrecision;

/**
 * Java type descriptor for the {@link OffsetTime} type.
 *
 * @author Steve Ebersole
 */
public class OffsetTimeJavaType extends AbstractTemporalJavaType<OffsetTime> {
	/**
	 * Singleton access
	 */
	public static final OffsetTimeJavaType INSTANCE = new OffsetTimeJavaType();

	public OffsetTimeJavaType() {
		super( OffsetTime.class, ImmutableMutabilityPlan.instance() );
	}

	@Override
	public boolean isInstance(Object value) {
		return value instanceof OffsetTime;
	}

	@Override
	public TemporalType getPrecision() {
		return TemporalType.TIME;
	}

	@Override
	public JdbcType getRecommendedJdbcType(JdbcTypeIndicators stdIndicators) {
		return stdIndicators.isPreferJavaTimeJdbcTypesEnabled()
				? stdIndicators.getJdbcType( SqlTypes.OFFSET_TIME )
				: stdIndicators.getJdbcType( stdIndicators.getDefaultZonedTimeSqlType() );
	}

	@Override
	protected <X> TemporalJavaType<X> forTimePrecision(TypeConfiguration typeConfiguration) {
		//noinspection unchecked
		return (TemporalJavaType<X>) this;
	}

	@Override
	public boolean useObjectEqualsHashCode() {
		return true;
	}

	@Override
	public String toString(OffsetTime value) {
		return DateTimeFormatter.ISO_OFFSET_TIME.format( value );
	}

	@Override
	public OffsetTime fromString(CharSequence string) {
		return OffsetTime.from( DateTimeFormatter.ISO_OFFSET_TIME.parse( string ) );
	}

	@Override
	@SuppressWarnings("unchecked")
	public <X> X unwrap(OffsetTime offsetTime, Class<X> type, WrapperOptions options) {
		if ( offsetTime == null ) {
			return null;
		}

		// for java.time types, we assume that the JDBC timezone, if any, is ignored
		// (since PS.setObject() doesn't support passing a timezone)

		if ( OffsetTime.class.isAssignableFrom( type ) ) {
			return (X) offsetTime;
		}

		if ( LocalTime.class.isAssignableFrom( type ) ) {
			return (X) offsetTime.withOffsetSameInstant( getCurrentSystemOffset() ).toLocalTime();
		}

		if ( OffsetDateTime.class.isAssignableFrom( type ) ) {
			return (X) offsetTime.atDate( LocalDate.EPOCH );
		}

		// for legacy types, we assume that the JDBC timezone is passed to JDBC
		// (since PS.setTime() and friends do accept a timezone passed as a Calendar)

		final var jdbcOffsetTime = offsetTime.withOffsetSameInstant( getCurrentJdbcOffset(options) );

		if ( Time.class.isAssignableFrom( type ) ) {
			final var time = Time.valueOf( jdbcOffsetTime.toLocalTime() );
			final int nanos = jdbcOffsetTime.getNano();
			return nanos == 0
					? (X) time
					// Preserve milliseconds, which java.sql.Time supports
					: (X) new Time( time.getTime() + roundToPrecision( nanos, 3 ) / 1000000 );
		}

		final var jdbcOffsetDateTime = jdbcOffsetTime.atDate( LocalDate.EPOCH );

		if ( Timestamp.class.isAssignableFrom( type ) ) {
			// Workaround for HHH-13266 (JDK-8061577).
			// Ideally, we'd want to use Timestamp.from( jdbcOffsetDateTime.toInstant() ),
			// but this won't always work since Timestamp.from() assumes the number of
			// milliseconds since the epoch means the same thing in Timestamp and Instant,
			// but it doesn't, in particular before 1900.
			return (X) Timestamp.valueOf( jdbcOffsetDateTime.toLocalDateTime() );
		}

		if ( Calendar.class.isAssignableFrom( type ) ) {
			return (X) GregorianCalendar.from( jdbcOffsetDateTime.toZonedDateTime() );
		}

		// for instants, we assume that the JDBC timezone, if any, is ignored

		final Instant instant = offsetTime.atDate( LocalDate.EPOCH ).toInstant();

		if ( Long.class.isAssignableFrom( type ) ) {
			return (X) Long.valueOf( instant.toEpochMilli() );
		}

		if ( Date.class.isAssignableFrom( type ) ) {
			return (X) Date.from( instant );
		}

		throw unknownUnwrap( type );
	}

	@Override
	public <X> OffsetTime wrap(X value, WrapperOptions options) {
		if ( value == null ) {
			return null;
		}

		// for java.time types, we assume that the JDBC timezone, if any, is ignored
		// (since PS.setObject() doesn't support passing a timezone)

		if (value instanceof OffsetTime offsetTime) {
			return offsetTime;
		}

		if (value instanceof LocalTime localTime) {
			return localTime.atOffset( getCurrentSystemOffset() );
		}

		if ( value instanceof OffsetDateTime offsetDateTime) {
			return offsetDateTime.toOffsetTime();
		}

		// Also, in order to fix HHH-13357, and to be consistent with the conversion to Time (see above),
		// we set the offset to the current offset of the JVM (OffsetDateTime.now().getOffset()). This is
		// different from setting the *zone* to the current *zone* of the JVM (ZoneId.systemDefault()),
		// since a zone has a varying offset over time; thus the zone might have a different offset for the
		// given timezone than it has for the current date/time. For example, if the timestamp represents
		// 1970-01-01TXX:YY, and the JVM is set to use Europe/Paris as a timezone, and the current time is
		// 2019-04-16-08:53, then applying the JVM timezone to the timestamp would result in the offset
		// +01:00, but applying the JVM offset would result in the offset +02:00, since DST is in effect at
		// 2019-04-16-08:53.
		//
		// Of course, none of this would be a problem if we just stored the offset in the database, but I
		// guess there are historical reasons that explain why we don't.

		// for legacy types, we assume that the JDBC timezone is passed to JDBC
		// (since PS.setTime() and friends do accept a timezone passed as a Calendar)

		if (value instanceof Time time) {
			final var offsetTime =
					time.toLocalTime().atOffset( getCurrentJdbcOffset( options) )
							.withOffsetSameInstant( getCurrentSystemOffset() );
			long millis = time.getTime() % 1000;
			if ( millis == 0 ) {
				return offsetTime;
			}
			if ( millis < 0 ) {
				// The milliseconds for a Time could be negative,
				// which usually means the time is in a different time zone
				millis += 1_000L;
			}
			return offsetTime.with( ChronoField.NANO_OF_SECOND, millis * 1_000_000L );
		}

		if (value instanceof Timestamp timestamp) {
			// Workaround for HHH-13266 (JDK-8061577).
			// Ideally, we'd want to use OffsetDateTime.ofInstant( ts.toInstant(), ... ),
			// but this won't always work since ts.toInstant() assumes the number of
			// milliseconds since the epoch means the same thing in Timestamp and Instant,
			// but it doesn't, in particular before 1900.
			return timestamp.toLocalDateTime().toLocalTime().atOffset( getCurrentJdbcOffset(options) )
					.withOffsetSameInstant( getCurrentSystemOffset() );
		}

		if (value instanceof Date date) {
			return OffsetTime.ofInstant( date.toInstant(), getCurrentSystemOffset() );
		}

		// for instants, we assume that the JDBC timezone, if any, is ignored

		if (value instanceof Long millis) {
			return OffsetTime.ofInstant( Instant.ofEpochMilli(millis), getCurrentSystemOffset() );
		}

		if (value instanceof Calendar calendar) {
			return OffsetTime.ofInstant( calendar.toInstant(), calendar.getTimeZone().toZoneId() );
		}

		throw unknownWrap( value.getClass() );
	}

	private static ZoneOffset getCurrentJdbcOffset(WrapperOptions options) {
		final var jdbcTimeZone = options.getJdbcTimeZone();
		return jdbcTimeZone != null
				? OffsetDateTime.now().atZoneSameInstant( jdbcTimeZone.toZoneId() ).getOffset()
				: getCurrentSystemOffset();
	}

	private static ZoneOffset getCurrentSystemOffset() {
		return OffsetDateTime.now().getOffset();
	}

	@Override
	public int getDefaultSqlPrecision(Dialect dialect, JdbcType jdbcType) {
		// times represent repeating events - they
		// almost never come equipped with seconds,
		// let alone fractional seconds!
		return 0;
	}

}
