/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type.descriptor.java;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.OffsetDateTime;
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
			// NOTE: do not use Timestamp.from(Instant) because that returns a Timestamp in UTC
			final ZonedDateTime dateTime =
					instant.atZone( options.getJdbcZoneId() ); // convert to the JDBC timezone
			return (X) Timestamp.valueOf( dateTime.toLocalDateTime() );
		}

		if ( java.sql.Date.class.isAssignableFrom( type ) ) {
			return (X) new java.sql.Date( instant.toEpochMilli() );
		}

		if ( java.sql.Time.class.isAssignableFrom( type ) ) {
			return (X) new java.sql.Time( instant.toEpochMilli() % 86_400_000 );
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

		if ( value instanceof Instant instant ) {
			return instant;
		}

		if ( value instanceof OffsetDateTime offsetDateTime ) {
			return offsetDateTime.toInstant();
		}

		if ( value instanceof Timestamp timestamp ) {
			// NOTE: do not use Timestamp.getInstant() because that assumes the Timestamp is in UTC
			return timestamp.toLocalDateTime()
					.atZone( options.getJdbcZoneId() ) // the Timestamp is in the JDBC timezone
					.toInstant(); // return the instant (always in UTC)
		}

		if ( value instanceof Long longValue ) {
			return Instant.ofEpochMilli( longValue );
		}

		if ( value instanceof Calendar calendar ) {
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
