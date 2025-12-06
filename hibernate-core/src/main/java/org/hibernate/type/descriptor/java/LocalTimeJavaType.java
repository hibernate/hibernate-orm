/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type.descriptor.java;

import java.sql.Time;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoField;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import jakarta.persistence.TemporalType;

import org.hibernate.dialect.Dialect;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.JdbcTypeIndicators;
import org.hibernate.type.spi.TypeConfiguration;

import static org.hibernate.type.descriptor.DateTimeUtils.roundToPrecision;

/**
 * Java type descriptor for the {@link LocalTime} type.
 *
 * @author Steve Ebersole
 */
public class LocalTimeJavaType extends AbstractTemporalJavaType<LocalTime> {
	/**
	 * Singleton access
	 */
	public static final LocalTimeJavaType INSTANCE = new LocalTimeJavaType();

	public LocalTimeJavaType() {
		super( LocalTime.class, ImmutableMutabilityPlan.instance() );
	}

	@Override
	public boolean isInstance(Object value) {
		return value instanceof LocalTime;
	}

	@Override
	public TemporalType getPrecision() {
		return TemporalType.TIME;
	}

	@Override
	public JdbcType getRecommendedJdbcType(JdbcTypeIndicators context) {
		return context.isPreferJavaTimeJdbcTypesEnabled()
				? context.getJdbcType( SqlTypes.LOCAL_TIME )
				: context.getJdbcType( Types.TIME );
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
	public String toString(LocalTime value) {
		return DateTimeFormatter.ISO_LOCAL_TIME.format( value );
	}

	@Override
	public LocalTime fromString(CharSequence string) {
		return LocalTime.from( DateTimeFormatter.ISO_LOCAL_TIME.parse( string ) );
	}

	@Override
	@SuppressWarnings("unchecked")
	public <X> X unwrap(LocalTime value, Class<X> type, WrapperOptions options) {
		if ( value == null ) {
			return null;
		}

		if ( LocalTime.class.isAssignableFrom( type ) ) {
			return (X) value;
		}

		if ( Time.class.isAssignableFrom( type ) ) {
			final var time = Time.valueOf( value );
			final int nanos = value.getNano();
			return nanos == 0
					? (X) time
					// Preserve milliseconds, which java.sql.Time supports
					: (X) new Time( time.getTime() + roundToPrecision( nanos, 3 ) / 1000000 );
		}

		// Oracle documentation says to set the Date to January 1, 1970 when convert from
		// a LocalTime to a Calendar. IMO the same should hold true for converting to all
		// the legacy Date/Time types.

		final var zonedDateTime =
				value.atDate( LocalDate.of( 1970, 1, 1 ) )
						.atZone( ZoneId.systemDefault() );

		if ( Calendar.class.isAssignableFrom( type ) ) {
			return (X) GregorianCalendar.from( zonedDateTime );
		}

		final var instant = zonedDateTime.toInstant();

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

		if (value instanceof LocalTime localTime) {
			return localTime;
		}

		if (value instanceof Time time) {
			final var localTime = time.toLocalTime();
			long millis = time.getTime() % 1000;
			if ( millis == 0 ) {
				return localTime;
			}
			if ( millis < 0 ) {
				// The milliseconds for a Time could be negative,
				// which usually means the time is in a different time zone
				millis += 1_000L;
			}
			return localTime.with( ChronoField.NANO_OF_SECOND, millis * 1_000_000L );
		}

		if (value instanceof Timestamp timestamp) {
			return LocalDateTime.ofInstant( timestamp.toInstant(), ZoneId.systemDefault() ).toLocalTime();
		}

		if (value instanceof Long longValue) {
			final var instant = Instant.ofEpochMilli( longValue );
			return LocalDateTime.ofInstant( instant, ZoneId.systemDefault() ).toLocalTime();
		}

		if (value instanceof Calendar calendar) {
			return LocalDateTime.ofInstant( calendar.toInstant(), calendar.getTimeZone().toZoneId() ).toLocalTime();
		}

		if (value instanceof Date timestamp ) {
			final var instant = Instant.ofEpochMilli( timestamp.getTime() );
			return LocalDateTime.ofInstant( instant, ZoneId.systemDefault() ).toLocalTime();
		}

		throw unknownWrap( value.getClass() );
	}

	@Override
	public boolean isWider(JavaType<?> javaType) {
		return switch ( javaType.getTypeName() ) {
			case "java.sql.Time" -> true;
			default -> false;
		};
	}

	@Override
	public int getDefaultSqlPrecision(Dialect dialect, JdbcType jdbcType) {
		// times represent repeating events - they
		// almost never come equipped with seconds,
		// let alone fractional seconds!
		return 0;
	}

}
