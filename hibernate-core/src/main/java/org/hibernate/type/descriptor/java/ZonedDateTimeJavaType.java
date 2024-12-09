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
	public TemporalType getPrecision() {
		return TemporalType.TIMESTAMP;
	}

	@Override
	public JdbcType getRecommendedJdbcType(JdbcTypeIndicators stdIndicators) {
		if ( stdIndicators.isPreferJavaTimeJdbcTypesEnabled() ) {
			return stdIndicators.getJdbcType( SqlTypes.ZONED_DATE_TIME );
		}
		return stdIndicators.getJdbcType( stdIndicators.getDefaultZonedTimestampSqlType() );
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
	@SuppressWarnings("unchecked")
	public <X> X unwrap(ZonedDateTime zonedDateTime, Class<X> type, WrapperOptions options) {
		if ( zonedDateTime == null ) {
			return null;
		}

		if ( ZonedDateTime.class.isAssignableFrom( type ) ) {
			return (X) zonedDateTime;
		}

		if ( OffsetDateTime.class.isAssignableFrom( type ) ) {
			return (X) OffsetDateTime.of( zonedDateTime.toLocalDateTime(), zonedDateTime.getOffset() );
		}

		if ( Instant.class.isAssignableFrom( type ) ) {
			return (X) zonedDateTime.toInstant();
		}

		if ( Calendar.class.isAssignableFrom( type ) ) {
			return (X) GregorianCalendar.from( zonedDateTime );
		}

		if ( Timestamp.class.isAssignableFrom( type ) ) {
			final ZonedDateTime dateTime =
					zonedDateTime.withZoneSameInstant( options.getJdbcZoneId() );  // convert to the JDBC timezone
			return (X) Timestamp.valueOf( dateTime.toLocalDateTime() );
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
			return timestamp.toLocalDateTime()
					.atZone( options.getJdbcZoneId() ) // the Timestamp is in the JDBC timezone
					.withZoneSameInstant( ZoneId.systemDefault() ); // convert back to the VM timezone
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
