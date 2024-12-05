/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
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
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoField;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import org.hibernate.dialect.Dialect;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.DateTimeUtils;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.JdbcTypeIndicators;
import org.hibernate.type.spi.TypeConfiguration;

import jakarta.persistence.TemporalType;

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
	public TemporalType getPrecision() {
		return TemporalType.TIME;
	}

	@Override
	public JdbcType getRecommendedJdbcType(JdbcTypeIndicators stdIndicators) {
		if ( stdIndicators.isPreferJavaTimeJdbcTypesEnabled() ) {
			return stdIndicators.getJdbcType( SqlTypes.OFFSET_TIME );
		}
		return stdIndicators.getJdbcType( stdIndicators.getDefaultZonedTimeSqlType() );
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
			return (X) offsetTime.withOffsetSameInstant( options.getSystemZoneOffset() ).toLocalTime();
		}

		if ( OffsetDateTime.class.isAssignableFrom( type ) ) {
			return (X) offsetDateTimeAtEpoch( offsetTime );
		}

		// for legacy types, we assume that the JDBC timezone is passed to JDBC
		// (since PS.setTime() and friends do accept a timezone passed as a Calendar)


		if ( Time.class.isAssignableFrom( type ) ) {
			// Use ZoneOffset rather than ZoneId in the conversion,
			// since the offset for a zone varies over time, but
			// a Time does not have an attached Date.
			final OffsetTime jdbcOffsetTime =
					offsetTime.withOffsetSameInstant( options.getJdbcZoneOffset() ); // convert to the JDBC timezone
			final Time time = Time.valueOf( jdbcOffsetTime.toLocalTime() );
			// Time.valueOf() throws away milliseconds
			return (X) withMillis( jdbcOffsetTime, time );
		}

		if ( Timestamp.class.isAssignableFrom( type ) ) {
			final OffsetTime jdbcOffsetTime =
					offsetTime.withOffsetSameInstant( options.getJdbcZoneOffset() ); // convert to the JDBC timezone
			return (X) Timestamp.valueOf( offsetDateTimeAtEpoch( jdbcOffsetTime ).toLocalDateTime() );
		}

		if ( Calendar.class.isAssignableFrom( type ) ) {
			return (X) GregorianCalendar.from( offsetDateTimeAtEpoch( offsetTime ).toZonedDateTime() );
		}

		// for instants, we assume that the JDBC timezone, if any, is ignored

		if ( Long.class.isAssignableFrom( type ) ) {
			return (X) Long.valueOf( instantAtEpoch( offsetTime ).toEpochMilli() );
		}

		if ( Date.class.isAssignableFrom( type ) ) {
			return (X) Date.from( instantAtEpoch( offsetTime ) );
		}

		throw unknownUnwrap( type );
	}

	private static Time withMillis(OffsetTime jdbcOffsetTime, Time time) {
		final int nanos = jdbcOffsetTime.getNano();
		if ( nanos == 0 ) {
			return time;
		}
		else {
			// Preserve milliseconds, which java.sql.Time supports
			final long millis = DateTimeUtils.roundToPrecision( nanos, 3 ) / 1_000_000;
			return new Time( time.getTime() + millis );
		}
	}

	private static OffsetDateTime offsetDateTimeAtEpoch(OffsetTime jdbcOffsetTime) {
		return jdbcOffsetTime.atDate( LocalDate.EPOCH );
	}

	private static Instant instantAtEpoch(OffsetTime offsetTime) {
		return offsetDateTimeAtEpoch( offsetTime ).toInstant();
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
			return localTime.atOffset( options.getSystemZoneOffset() );
		}

		if ( value instanceof OffsetDateTime offsetDateTime) {
			return offsetDateTime.toOffsetTime();
		}

		if (value instanceof Time time) {
			// Use ZoneOffset rather than ZoneId in the conversion,
			// since the offset for a zone varies over time, but
			// a Time does not have an attached Date.
			final OffsetTime offsetTime =
					time.toLocalTime().atOffset( options.getJdbcZoneOffset() ) // the Timestamp is in the current JDBC timezone offset
							.withOffsetSameInstant( options.getSystemZoneOffset() ); // convert back to the VM timezone
			// Time.toLocalTime() strips off nanos
			return withNanos( time, offsetTime );
		}

		if (value instanceof Timestamp timestamp) {
			return timestamp.toLocalDateTime()
					.atZone( options.getJdbcZoneId() ) // the Timestamp is in the JDBC timezone
					.withZoneSameInstant( ZoneId.systemDefault() ) // convert back to the VM timezone
					.toOffsetDateTime().toOffsetTime(); // return the time part
		}

		if (value instanceof Date date) {
			return OffsetTime.ofInstant( date.toInstant(), options.getSystemZoneOffset() );
		}

		// for instants, we assume that the JDBC timezone, if any, is ignored

		if (value instanceof Long millis) {
			return OffsetTime.ofInstant( Instant.ofEpochMilli(millis), options.getSystemZoneOffset() );
		}

		if (value instanceof Calendar calendar) {
			return OffsetTime.ofInstant( calendar.toInstant(), calendar.getTimeZone().toZoneId() );
		}

		throw unknownWrap( value.getClass() );
	}

	private static OffsetTime withNanos(Time time, OffsetTime offsetTime) {
		final long millis = time.getTime() % 1000;
		final long nanos;
		if ( millis == 0 ) {
			return offsetTime;
		}
		else if ( millis < 0 ) {
			// The milliseconds for a Time could be negative,
			// which usually means the time is in a different time zone
			nanos = (millis + 1_000L) * 1_000_000L;
		}
		else {
			nanos = millis * 1_000_000L;
		}
		return offsetTime.with( ChronoField.NANO_OF_SECOND, nanos );
	}

	@Override
	public int getDefaultSqlPrecision(Dialect dialect, JdbcType jdbcType) {
		// times represent repeating events - they
		// almost never come equipped with seconds,
		// let alone fractional seconds!
		return 0;
	}

}
