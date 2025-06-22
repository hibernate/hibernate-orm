/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type.descriptor.java;

import java.sql.Timestamp;
import java.sql.Types;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAccessor;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import jakarta.persistence.TemporalType;

import org.hibernate.HibernateException;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.JdbcTypeIndicators;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * Descriptor for {@link Date} handling.
 *
 * @author Steve Ebersole
 */
public class DateJavaType extends AbstractTemporalJavaType<Date> implements VersionJavaType<Date> {
	public static final DateJavaType INSTANCE = new DateJavaType();

	public static class DateMutabilityPlan extends MutableMutabilityPlan<Date> {
		public static final DateMutabilityPlan INSTANCE = new DateMutabilityPlan();
		@Override
		public Date deepCopyNotNull(Date value) {
			return new Date( value.getTime() );
		}
	}

	public DateJavaType() {
		super( Date.class, DateMutabilityPlan.INSTANCE );
	}

	@Override
	public boolean isInstance(Object value) {
		return value instanceof Date;
	}

	@Override
	public TemporalType getPrecision() {
		return TemporalType.TIMESTAMP;
	}

	@Override
	public int getDefaultSqlPrecision(Dialect dialect, JdbcType jdbcType) {
		// this "Date" is really a timestamp
		return dialect.getDefaultTimestampPrecision();
	}

	@Override
	public JdbcType getRecommendedJdbcType(JdbcTypeIndicators context) {
		return context.getJdbcType( Types.TIMESTAMP );
	}

	@Override @SuppressWarnings("unchecked")
	protected <X> TemporalJavaType<X> forDatePrecision(TypeConfiguration typeConfiguration) {
		return (TemporalJavaType<X>) JdbcDateJavaType.INSTANCE;
	}

	@Override @SuppressWarnings("unchecked")
	protected <X> TemporalJavaType<X> forTimestampPrecision(TypeConfiguration typeConfiguration) {
		return (TemporalJavaType<X>) JdbcTimestampJavaType.INSTANCE;
	}

	@Override @SuppressWarnings("unchecked")
	protected <X> TemporalJavaType<X> forTimePrecision(TypeConfiguration typeConfiguration) {
		return (TemporalJavaType<X>) JdbcTimeJavaType.INSTANCE;
	}

	@Override
	public String toString(Date value) {
		return JdbcTimestampJavaType.LITERAL_FORMATTER.format( value.toInstant() );
	}

	@Override
	public Date fromString(CharSequence string) {
		try {
			final TemporalAccessor accessor = JdbcTimestampJavaType.LITERAL_FORMATTER.parse( string );
			return new Date(
					accessor.getLong( ChronoField.INSTANT_SECONDS ) * 1000L
							+ accessor.get( ChronoField.NANO_OF_SECOND ) / 1_000_000
			);
		}
		catch ( DateTimeParseException pe) {
			throw new HibernateException( "could not parse timestamp string" + string, pe );
		}
	}

	@Override
	public boolean areEqual(Date one, Date another) {
		if ( one == another) {
			return true;
		}
		return !( one == null || another == null ) && one.getTime() == another.getTime();

	}

	@Override
	public int extractHashCode(Date value) {
		Calendar calendar = Calendar.getInstance();
		calendar.setTime( value );
		return CalendarJavaType.INSTANCE.extractHashCode( calendar );
	}

	@SuppressWarnings("unchecked")
	@Override
	public <X> X unwrap(Date value, Class<X> type, WrapperOptions options) {
		if ( value == null ) {
			return null;
		}
		if ( java.sql.Date.class.isAssignableFrom( type ) ) {
			final java.sql.Date rtn = value instanceof java.sql.Date
					? ( java.sql.Date ) value
					: new java.sql.Date( value.getTime() );
			return (X) rtn;
		}
		if ( java.sql.Time.class.isAssignableFrom( type ) ) {
			final java.sql.Time rtn = value instanceof java.sql.Time
					? ( java.sql.Time ) value
					: new java.sql.Time( value.getTime() % 86_400_000 );
			return (X) rtn;
		}
		if ( java.sql.Timestamp.class.isAssignableFrom( type ) ) {
			final java.sql.Timestamp rtn = value instanceof Timestamp
					? ( java.sql.Timestamp ) value
					: new java.sql.Timestamp( value.getTime() );
			return (X) rtn;
		}
		if ( Date.class.isAssignableFrom( type ) ) {
			return (X) value;
		}
		if ( Calendar.class.isAssignableFrom( type ) ) {
			final GregorianCalendar cal = new GregorianCalendar();
			cal.setTimeInMillis( value.getTime() );
			return (X) cal;
		}
		if ( Long.class.isAssignableFrom( type ) ) {
			return (X) Long.valueOf( value.getTime() );
		}
		throw unknownUnwrap( type );
	}
	@Override
	public <X> Date wrap(X value, WrapperOptions options) {
		if ( value == null ) {
			return null;
		}
		if (value instanceof Date date) {
			return date;
		}

		if (value instanceof Long longValue) {
			return new Date( longValue );
		}

		if (value instanceof Calendar calendar) {
			return new Date( calendar.getTimeInMillis() );
		}

		throw unknownWrap( value.getClass() );
	}

	@Override
	public boolean isWider(JavaType<?> javaType) {
		return switch ( javaType.getTypeName() ) {
			case "java.sql.Date", "java.sql.Timestamp", "java.util.Calendar" -> true;
			default -> false;
		};
	}

	@Override
	public Date next(
			Date current,
			Long length,
			Integer precision,
			Integer scale,
			SharedSessionContractImplementor session) {
		return seed( length, precision, scale, session );
	}

	@Override
	public Date seed(
			Long length,
			Integer precision, Integer scale, SharedSessionContractImplementor session) {
		return Timestamp.from( ClockHelper.forPrecision( precision, session ).instant() );
	}
}
