/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type.descriptor.java;

import java.sql.Types;
import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.GregorianCalendar;

import jakarta.persistence.TemporalType;

import org.hibernate.dialect.Dialect;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.util.compare.CalendarComparator;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.JdbcTypeIndicators;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * Descriptor for {@link Calendar} handling.
 *
 * @author Steve Ebersole
 */
public class CalendarJavaType extends AbstractTemporalJavaType<Calendar> implements VersionJavaType<Calendar> {
	public static final CalendarJavaType INSTANCE = new CalendarJavaType();

	public static class CalendarMutabilityPlan extends MutableMutabilityPlan<Calendar> {
		public static final CalendarMutabilityPlan INSTANCE = new CalendarMutabilityPlan();

		public Calendar deepCopyNotNull(Calendar value) {
			return (Calendar) value.clone();
		}
	}

	protected CalendarJavaType() {
		super( Calendar.class, CalendarMutabilityPlan.INSTANCE, CalendarComparator.INSTANCE );
	}

	@Override
	public TemporalType getPrecision() {
		return TemporalType.TIMESTAMP;
	}

	@Override
	public JdbcType getRecommendedJdbcType(JdbcTypeIndicators context) {
		return context.getJdbcType( Types.TIMESTAMP );
	}

	@Override @SuppressWarnings("unchecked")
	protected <X> TemporalJavaType<X> forTimestampPrecision(TypeConfiguration typeConfiguration) {
		return (TemporalJavaType<X>) this;
	}

	@Override @SuppressWarnings("unchecked")
	protected <X> TemporalJavaType<X> forDatePrecision(TypeConfiguration typeConfiguration) {
		return (TemporalJavaType<X>) CalendarDateJavaType.INSTANCE;
	}

	@Override @SuppressWarnings("unchecked")
	protected <X> TemporalJavaType<X> forTimePrecision(TypeConfiguration typeConfiguration) {
		return (TemporalJavaType<X>) CalendarTimeJavaType.INSTANCE;
	}

	public String toString(Calendar value) {
		return DateJavaType.INSTANCE.toString( value.getTime() );
	}

	public Calendar fromString(CharSequence string) {
		Calendar result = new GregorianCalendar();
		result.setTime( DateJavaType.INSTANCE.fromString( string.toString() ) );
		return result;
	}

	@Override
	public boolean areEqual(Calendar one, Calendar another) {
		if ( one == another ) {
			return true;
		}
		if ( one == null || another == null ) {
			return false;
		}

		return one.get(Calendar.MILLISECOND) == another.get(Calendar.MILLISECOND)
			&& one.get(Calendar.SECOND) == another.get(Calendar.SECOND)
			&& one.get(Calendar.MINUTE) == another.get(Calendar.MINUTE)
			&& one.get(Calendar.HOUR_OF_DAY) == another.get(Calendar.HOUR_OF_DAY)
			&& one.get(Calendar.DAY_OF_MONTH) == another.get(Calendar.DAY_OF_MONTH)
			&& one.get(Calendar.MONTH) == another.get(Calendar.MONTH)
			&& one.get(Calendar.YEAR) == another.get(Calendar.YEAR);
	}

	@Override
	public int extractHashCode(Calendar value) {
		int hashCode = 1;
		hashCode = 31 * hashCode + value.get(Calendar.MILLISECOND);
		hashCode = 31 * hashCode + value.get(Calendar.SECOND);
		hashCode = 31 * hashCode + value.get(Calendar.MINUTE);
		hashCode = 31 * hashCode + value.get(Calendar.HOUR_OF_DAY);
		hashCode = 31 * hashCode + value.get(Calendar.DAY_OF_MONTH);
		hashCode = 31 * hashCode + value.get(Calendar.MONTH);
		hashCode = 31 * hashCode + value.get(Calendar.YEAR);
		return hashCode;
	}

	@SuppressWarnings("unchecked")
	public <X> X unwrap(Calendar value, Class<X> type, WrapperOptions options) {
		if ( value == null ) {
			return null;
		}
		if ( Calendar.class.isAssignableFrom( type ) ) {
			return (X) value;
		}
		if ( java.sql.Date.class.isAssignableFrom( type ) ) {
			return (X) new java.sql.Date( value.getTimeInMillis() );
		}
		if ( java.sql.Time.class.isAssignableFrom( type ) ) {
			return (X) millisToSqlTime( value.getTimeInMillis() );
		}
		if ( java.sql.Timestamp.class.isAssignableFrom( type ) ) {
			return (X) new java.sql.Timestamp( value.getTimeInMillis() );
		}
		if ( java.util.Date.class.isAssignableFrom( type ) ) {
			return (X) new  java.util.Date( value.getTimeInMillis() );
		}
		throw unknownUnwrap( type );
	}

	public <X> Calendar wrap(X value, WrapperOptions options) {
		if ( value == null ) {
			return null;
		}
		if (value instanceof Calendar) {
			return (Calendar) value;
		}

		if ( !(value instanceof java.util.Date)) {
			throw unknownWrap( value.getClass() );
		}

		Calendar cal = new GregorianCalendar();
		cal.setTime( (java.util.Date) value );
		return cal;
	}

	@Override
	public boolean isWider(JavaType<?> javaType) {
		switch ( javaType.getTypeName() ) {
			case "java.util.Date":
			case "java.sql.Date":
			case "java.sql.Timestamp":
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
	public Calendar next(
			Calendar current,
			Long length,
			Integer precision,
			Integer scale,
			SharedSessionContractImplementor session) {
		return seed( length, precision, scale, session );
	}

	@Override
	public Calendar seed(Long length, Integer precision, Integer scale, SharedSessionContractImplementor session) {
		return GregorianCalendar.from( ZonedDateTime.now( ClockHelper.forPrecision( precision, session, 3 ) ) );
	}
}
