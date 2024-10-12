/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type.descriptor.java;

import java.sql.Timestamp;
import java.sql.Types;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import jakarta.persistence.TemporalType;

import org.hibernate.dialect.Dialect;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.JdbcTypeIndicators;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * Java type descriptor for the {@link LocalDateTime} type.
 *
 * @author Steve Ebersole
 */
public class LocalDateTimeJavaType extends AbstractTemporalJavaType<LocalDateTime>
		implements VersionJavaType<LocalDateTime> {
	/**
	 * Singleton access
	 */
	public static final LocalDateTimeJavaType INSTANCE = new LocalDateTimeJavaType();

	public LocalDateTimeJavaType() {
		super( LocalDateTime.class, ImmutableMutabilityPlan.instance() );
	}

	@Override
	public TemporalType getPrecision() {
		return TemporalType.TIMESTAMP;
	}

	@Override
	public JdbcType getRecommendedJdbcType(JdbcTypeIndicators context) {
		if ( context.isPreferJavaTimeJdbcTypesEnabled() ) {
			return context.getJdbcType( SqlTypes.LOCAL_DATE_TIME );
		}
		return context.getJdbcType( Types.TIMESTAMP );
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
	public String toString(LocalDateTime value) {
		return DateTimeFormatter.ISO_DATE_TIME.format( value );
	}

	@Override
	public LocalDateTime fromString(CharSequence string) {
		return LocalDateTime.from( DateTimeFormatter.ISO_DATE_TIME.parse( string ) );
	}

	@Override
	@SuppressWarnings("unchecked")
	public <X> X unwrap(LocalDateTime value, Class<X> type, WrapperOptions options) {
		if ( value == null ) {
			return null;
		}

		if ( LocalDateTime.class.isAssignableFrom( type ) ) {
			return (X) value;
		}

		if ( Timestamp.class.isAssignableFrom( type ) ) {
			/*
			 * Workaround for HHH-13266 (JDK-8061577).
			 * We used to do Timestamp.from( value.atZone( ZoneId.systemDefault() ).toInstant() ),
			 * but on top of being more complex than the line below, it won't always work.
			 * Timestamp.from() assumes the number of milliseconds since the epoch
			 * means the same thing in Timestamp and Instant, but it doesn't, in particular before 1900.
			 */
			return (X) Timestamp.valueOf( value );
		}

		if ( java.sql.Date.class.isAssignableFrom( type ) ) {
			Instant instant = value.atZone( ZoneId.systemDefault() ).toInstant();
			return (X) new java.sql.Date( instant.toEpochMilli() );
		}

		if ( java.sql.Time.class.isAssignableFrom( type ) ) {
			Instant instant = value.atZone( ZoneId.systemDefault() ).toInstant();
			return (X) millisToSqlTime( instant.toEpochMilli() );
		}

		if ( Date.class.isAssignableFrom( type ) ) {
			Instant instant = value.atZone( ZoneId.systemDefault() ).toInstant();
			return (X) Date.from( instant );
		}

		if ( Calendar.class.isAssignableFrom( type ) ) {
			return (X) GregorianCalendar.from( value.atZone( ZoneId.systemDefault() ) );
		}

		if ( Long.class.isAssignableFrom( type ) ) {
			Instant instant = value.atZone( ZoneId.systemDefault() ).toInstant();
			return (X) Long.valueOf( instant.toEpochMilli() );
		}

		throw unknownUnwrap( type );
	}

	@Override
	public <X> LocalDateTime wrap(X value, WrapperOptions options) {
		if ( value == null ) {
			return null;
		}

		if (value instanceof LocalDateTime) {
			return (LocalDateTime) value;
		}

		if (value instanceof Timestamp) {
			final Timestamp ts = (Timestamp) value;
			/*
			 * Workaround for HHH-13266 (JDK-8061577).
			 * We used to do LocalDateTime.ofInstant( ts.toInstant(), ZoneId.systemDefault() ),
			 * but on top of being more complex than the line below, it won't always work.
			 * ts.toInstant() assumes the number of milliseconds since the epoch
			 * means the same thing in Timestamp and Instant, but it doesn't, in particular before 1900.
			 */
			return ts.toLocalDateTime();
		}

		if (value instanceof Long) {
			final Instant instant = Instant.ofEpochMilli( (Long) value );
			return LocalDateTime.ofInstant( instant, ZoneId.systemDefault() );
		}

		if (value instanceof Calendar) {
			final Calendar calendar = (Calendar) value;
			return LocalDateTime.ofInstant( calendar.toInstant(), calendar.getTimeZone().toZoneId() );
		}

		if (value instanceof Date) {
			final Date ts = (Date) value;
			final Instant instant = ts.toInstant();
			return LocalDateTime.ofInstant( instant, ZoneId.systemDefault() );
		}

		throw unknownWrap( value.getClass() );
	}

	@Override
	public boolean isWider(JavaType<?> javaType) {
		switch ( javaType.getTypeName() ) {
			case "java.sql.Date":
			case "java.sql.Timestamp":
			case "java.util.Date":
			case "java.util.Calendar":
				return true;
			default:
				return false;
		}
	}

	@Override
	public int getDefaultSqlPrecision(Dialect dialect, JdbcType jdbcType) {
		return dialect.getDefaultTimestampPrecision();
	}

	@Override
	public LocalDateTime seed(Long length, Integer precision, Integer scale, SharedSessionContractImplementor session) {
		return LocalDateTime.now( ClockHelper.forPrecision( precision, session ) );
	}

	@Override
	public LocalDateTime next(
			LocalDateTime current,
			Long length,
			Integer precision,
			Integer scale,
			SharedSessionContractImplementor session) {
		return LocalDateTime.now( ClockHelper.forPrecision( precision, session ) );
	}

}
