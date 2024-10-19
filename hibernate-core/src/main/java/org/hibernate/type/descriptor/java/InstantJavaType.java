/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type.descriptor.java;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import jakarta.persistence.TemporalType;

import org.hibernate.dialect.Dialect;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.JdbcTypeIndicators;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * Java type descriptor for the Java {@link Instant} type.
 *
 * @see org.hibernate.cfg.AvailableSettings#PREFERRED_INSTANT_JDBC_TYPE
 *
 * @author Steve Ebersole
 */
public class InstantJavaType extends AbstractTemporalJavaType<Instant>
		implements VersionJavaType<Instant> {
	/**
	 * Singleton access
	 */
	public static final InstantJavaType INSTANCE = new InstantJavaType();

	public InstantJavaType() {
		super( Instant.class, ImmutableMutabilityPlan.instance() );
	}

	@Override
	public TemporalType getPrecision() {
		return TemporalType.TIMESTAMP;
	}

	@Override @SuppressWarnings("unchecked")
	protected <X> TemporalJavaType<X> forDatePrecision(TypeConfiguration typeConfiguration) {
		return (TemporalJavaType<X>) this;
	}

	@Override @SuppressWarnings("unchecked")
	protected <X> TemporalJavaType<X> forTimestampPrecision(TypeConfiguration typeConfiguration) {
		return (TemporalJavaType<X>) this;
	}

	@Override @SuppressWarnings("unchecked")
	protected <X> TemporalJavaType<X> forTimePrecision(TypeConfiguration typeConfiguration) {
		return (TemporalJavaType<X>) this;
	}

	@Override
	public JdbcType getRecommendedJdbcType(JdbcTypeIndicators context) {
		return context.getJdbcType( context.getPreferredSqlTypeCodeForInstant() );
	}

	@Override
	public boolean useObjectEqualsHashCode() {
		return true;
	}

	@Override
	public String toString(Instant value) {
		return DateTimeFormatter.ISO_INSTANT.format( value );
	}

	@Override
	public Instant fromString(CharSequence string) {
		return Instant.from( DateTimeFormatter.ISO_INSTANT.parse( string ) );
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

		if ( OffsetDateTime.class.isAssignableFrom( type ) ) {
			return (X) instant.atOffset( ZoneOffset.UTC );
		}

		if ( Calendar.class.isAssignableFrom( type ) ) {
			return (X) GregorianCalendar.from( instant.atZone( ZoneOffset.UTC ) );
		}

		if ( Timestamp.class.isAssignableFrom( type ) ) {
			/*
			 * This works around two bugs:
			 * - HHH-13266 (JDK-8061577): around and before 1900,
			 * the number of milliseconds since the epoch does not mean the same thing
			 * for java.util and java.time, so conversion must be done using the year, month, day, hour, etc.
			 * - HHH-13379 (JDK-4312621): after 1908 (approximately),
			 * Daylight Saving Time introduces ambiguity in the year/month/day/hour/etc representation once a year
			 * (on DST end), so conversion must be done using the number of milliseconds since the epoch.
			 * - around 1905, both methods are equally valid, so we don't really care which one is used.
			 */
			ZonedDateTime zonedDateTime = instant.atZone( ZoneId.systemDefault() );
			if ( zonedDateTime.getYear() < 1905 ) {
				return (X) Timestamp.valueOf( zonedDateTime.toLocalDateTime() );
			}
			else {
				return (X) Timestamp.from( instant );
			}
		}

		if ( java.sql.Date.class.isAssignableFrom( type ) ) {
			return (X) new java.sql.Date( instant.toEpochMilli() );
		}

		if ( java.sql.Time.class.isAssignableFrom( type ) ) {
			return (X) millisToSqlTime( instant.toEpochMilli() );
		}

		if ( Date.class.isAssignableFrom( type ) ) {
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

		if ( value instanceof Instant ) {
			return (Instant) value;
		}

		if ( value instanceof OffsetDateTime ) {
			return ( (OffsetDateTime) value ).toInstant();
		}

		if ( value instanceof Timestamp ) {
			final Timestamp ts = (Timestamp) value;
			/*
			 * This works around two bugs:
			 * - HHH-13266 (JDK-8061577): around and before 1900,
			 * the number of milliseconds since the epoch does not mean the same thing
			 * for java.util and java.time, so conversion must be done using the year, month, day, hour, etc.
			 * - HHH-13379 (JDK-4312621): after 1908 (approximately),
			 * Daylight Saving Time introduces ambiguity in the year/month/day/hour/etc representation once a year
			 * (on DST end), so conversion must be done using the number of milliseconds since the epoch.
			 * - around 1905, both methods are equally valid, so we don't really care which one is used.
			 */
			if ( ts.getYear() < 5 ) { // Timestamp year 0 is 1900
				return ts.toLocalDateTime().atZone( ZoneId.systemDefault() ).toInstant();
			}
			else {
				return ts.toInstant();
			}
		}

		if ( value instanceof Long ) {
			return Instant.ofEpochMilli( (Long) value );
		}

		if ( value instanceof Calendar ) {
			final Calendar calendar = (Calendar) value;
			return ZonedDateTime.ofInstant( calendar.toInstant(), calendar.getTimeZone().toZoneId() ).toInstant();
		}

		if ( value instanceof Date ) {
			return ( (Date) value ).toInstant();
		}

		throw unknownWrap( value.getClass() );
	}

	@Override
	public int getDefaultSqlPrecision(Dialect dialect, JdbcType jdbcType) {
		return dialect.getDefaultTimestampPrecision();
	}

	@Override
	public Instant seed(Long length, Integer precision, Integer scale, SharedSessionContractImplementor session) {
		return Instant.now( ClockHelper.forPrecision( precision, session ) );
	}

	@Override
	public Instant next(
			Instant current,
			Long length,
			Integer precision,
			Integer scale,
			SharedSessionContractImplementor session) {
		return Instant.now( ClockHelper.forPrecision( precision, session ) );
	}

}
