/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type.descriptor.java;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import jakarta.persistence.TemporalType;

import org.hibernate.dialect.Dialect;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.util.ZonedDateTimeComparator;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.JdbcTypeIndicators;
import org.hibernate.type.spi.TypeConfiguration;

import static java.time.format.DateTimeFormatter.ISO_ZONED_DATE_TIME;

/**
 * Java type descriptor for the {@link ZonedDateTime} type.
 *
 * @author Steve Ebersole
 */
public class ZonedDateTimeJavaType extends AbstractTemporalJavaType<ZonedDateTime> implements VersionJavaType<ZonedDateTime> {
	/**
	 * Singleton access
	 */
	public static final ZonedDateTimeJavaType INSTANCE = new ZonedDateTimeJavaType();

	public ZonedDateTimeJavaType() {
		super( ZonedDateTime.class, ImmutableMutabilityPlan.instance(), ZonedDateTimeComparator.INSTANCE );
	}

	@Override
	public boolean isInstance(Object value) {
		return value instanceof ZonedDateTime;
	}

	@Override
	public ZonedDateTime cast(Object value) {
		return (ZonedDateTime) value;
	}

	@Override
	public TemporalType getPrecision() {
		return TemporalType.TIMESTAMP;
	}

	@Override
	public JdbcType getRecommendedJdbcType(JdbcTypeIndicators stdIndicators) {
		return stdIndicators.isPreferJavaTimeJdbcTypesEnabled()
				? stdIndicators.getJdbcType( SqlTypes.ZONED_DATE_TIME )
				: stdIndicators.getJdbcType( stdIndicators.getDefaultZonedTimestampSqlType() );
	}

	@Override @SuppressWarnings("unchecked")
	protected <X> TemporalJavaType<X> forTimestampPrecision(TypeConfiguration typeConfiguration) {
		return (TemporalJavaType<X>) this;
	}

	@Override
	public boolean useObjectEqualsHashCode() {
		return true;
	}

	@Override
	public String toString(ZonedDateTime value) {
		return ISO_ZONED_DATE_TIME.format( value );
	}

	@Override
	public ZonedDateTime fromString(CharSequence string) {
		return ZonedDateTime.from( ISO_ZONED_DATE_TIME.parse( string ) );
	}

	@Override
	public <X> X unwrap(ZonedDateTime zonedDateTime, Class<X> type, WrapperOptions options) {
		if ( zonedDateTime == null ) {
			return null;
		}

		if ( ZonedDateTime.class.isAssignableFrom( type ) ) {
			return type.cast( zonedDateTime );
		}

		if ( OffsetDateTime.class.isAssignableFrom( type ) ) {
			return type.cast( OffsetDateTime.of( zonedDateTime.toLocalDateTime(), zonedDateTime.getOffset() ) );
		}

		if ( Instant.class.isAssignableFrom( type ) ) {
			return type.cast( zonedDateTime.toInstant() );
		}

		if ( Calendar.class.isAssignableFrom( type ) ) {
			return type.cast( GregorianCalendar.from( zonedDateTime ) );
		}

		if ( Timestamp.class.isAssignableFrom( type ) ) {
			// This works around two bugs:
			// - HHH-13266 (JDK-8061577): around and before 1900,
			//   the number of milliseconds since the epoch does not mean the same thing
			//   for java.util and java.time, so conversion must be done using the year,
			//   month, day, hour, etc.
			// - HHH-13379 (JDK-4312621): after 1908 (approximately),
			//   Daylight Saving Time introduces ambiguity in the year/month/day/hour/etc
			//   representation once a year (on DST end), so conversion must be done using
			//   the number of milliseconds since the epoch.
			// - around 1905, both methods are equally valid, so we don't really care which
			//   one is used.
			if ( zonedDateTime.getYear() < 1905 ) {
				return type.cast( Timestamp.valueOf(
						zonedDateTime.withZoneSameInstant( ZoneId.systemDefault() )
								.toLocalDateTime()
				) );
			}
			else {
				return type.cast( Timestamp.from( zonedDateTime.toInstant() ) );
			}
		}

		if ( java.sql.Date.class.isAssignableFrom( type ) ) {
			return type.cast( java.sql.Date.from( zonedDateTime.toInstant() ) );
		}

		if ( java.sql.Time.class.isAssignableFrom( type ) ) {
			return type.cast( java.sql.Time.from( zonedDateTime.toInstant() ) );
		}

		if ( Date.class.isAssignableFrom( type ) ) {
			return type.cast( Date.from( zonedDateTime.toInstant() ) );
		}

		if ( Long.class.isAssignableFrom( type ) ) {
			return type.cast( zonedDateTime.toInstant().toEpochMilli() );
		}

		throw unknownUnwrap( type );
	}

	@Override
	public <X> ZonedDateTime wrap(X value, WrapperOptions options) {
		if ( value == null ) {
			return null;
		}

		if (value instanceof ZonedDateTime zonedDateTime) {
			return zonedDateTime;
		}

		if (value instanceof OffsetDateTime offsetDateTime) {
			return offsetDateTime.toZonedDateTime();
		}

		if (value instanceof Instant instant) {
			return instant.atZone( ZoneOffset.UTC );
		}

		if (value instanceof Timestamp timestamp) {
			// This works around two bugs:
			// - HHH-13266 (JDK-8061577): around and before 1900,
			//   the number of milliseconds since the epoch does not mean the same thing
			//   for java.util and java.time, so conversion must be done using the year,
			//   month, day, hour, etc.
			// - HHH-13379 (JDK-4312621): after 1908 (approximately),
			//   Daylight Saving Time introduces ambiguity in the year/month/day/hour/etc
			//   representation once a year (on DST end), so conversion must be done using
			//   the number of milliseconds since the epoch.
			// - around 1905, both methods are equally valid, so we don't really care which
			//   one is used.
			return timestamp.getYear() < 5 // Timestamp year 0 is 1900
					? timestamp.toLocalDateTime().atZone( ZoneId.systemDefault() )
					: timestamp.toInstant().atZone( ZoneId.systemDefault() );
		}

		if (value instanceof Date date) {
			return ZonedDateTime.ofInstant( date.toInstant(), ZoneId.systemDefault() );
		}

		if (value instanceof Long longValue) {
			return ZonedDateTime.ofInstant( Instant.ofEpochMilli( longValue ), ZoneId.systemDefault() );
		}

		if (value instanceof Calendar calendar) {
			return ZonedDateTime.ofInstant( calendar.toInstant(), calendar.getTimeZone().toZoneId() );
		}

		throw unknownWrap( value.getClass() );
	}

	@Override
	public int getDefaultSqlPrecision(Dialect dialect, JdbcType jdbcType) {
		return dialect.getDefaultTimestampPrecision();
	}

	@Override
	public ZonedDateTime seed(Long length, Integer precision, Integer scale, SharedSessionContractImplementor session) {
		return ZonedDateTime.now( ClockHelper.forPrecision( precision, session ) );
	}

	@Override
	public ZonedDateTime next(
			ZonedDateTime current,
			Long length,
			Integer precision,
			Integer scale,
			SharedSessionContractImplementor session) {
		return ZonedDateTime.now( ClockHelper.forPrecision( precision, session ) );
	}

}
