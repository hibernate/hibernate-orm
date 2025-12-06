/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type.descriptor.java;

import java.sql.Types;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import jakarta.persistence.TemporalType;

import org.hibernate.dialect.Dialect;
import org.hibernate.internal.util.compare.CalendarComparator;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.JdbcTypeIndicators;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * Descriptor for {@link Calendar} handling, but just for the date (month, day, year) portion.
 *
 * @author Steve Ebersole
 */
public class CalendarDateJavaType extends AbstractTemporalJavaType<Calendar> {
	public static final CalendarDateJavaType INSTANCE = new CalendarDateJavaType();

	protected CalendarDateJavaType() {
		super( Calendar.class, CalendarJavaType.CalendarMutabilityPlan.INSTANCE, CalendarComparator.INSTANCE );
	}

	@Override
	public TemporalType getPrecision() {
		return TemporalType.DATE;
	}

	@Override
	public JdbcType getRecommendedJdbcType(JdbcTypeIndicators context) {
		return context.getJdbcType( Types.DATE );
	}

	@Override  @SuppressWarnings("unchecked")
	protected <X> TemporalJavaType<X> forDatePrecision(TypeConfiguration typeConfiguration) {
		return (TemporalJavaType<X>) this;
	}

	@Override  @SuppressWarnings("unchecked")
	protected <X> TemporalJavaType<X> forTimestampPrecision(TypeConfiguration typeConfiguration) {
		return (TemporalJavaType<X>) CalendarJavaType.INSTANCE;
	}

	@Override  @SuppressWarnings("unchecked")
	protected <X> TemporalJavaType<X> forTimePrecision(TypeConfiguration typeConfiguration) {
		return (TemporalJavaType<X>) CalendarTimeJavaType.INSTANCE;
	}

	public String toString(Calendar value) {
		return JdbcDateJavaType.INSTANCE.toString( value.getTime() );
	}

	public Calendar fromString(CharSequence string) {
		final var result = new GregorianCalendar();
		result.setTime( JdbcDateJavaType.INSTANCE.fromString( string.toString() ) );
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

		return one.get(Calendar.DAY_OF_MONTH) == another.get(Calendar.DAY_OF_MONTH)
			&& one.get(Calendar.MONTH) == another.get(Calendar.MONTH)
			&& one.get(Calendar.YEAR) == another.get(Calendar.YEAR);
	}

	@Override
	public int extractHashCode(Calendar value) {
		int hashCode = 1;
		hashCode = 31 * hashCode + value.get(Calendar.DAY_OF_MONTH);
		hashCode = 31 * hashCode + value.get(Calendar.MONTH);
		hashCode = 31 * hashCode + value.get(Calendar.YEAR);
		return hashCode;
	}

	public <X> X unwrap(Calendar value, Class<X> type, WrapperOptions options) {
		if ( value == null ) {
			return null;
		}
		if ( Calendar.class.isAssignableFrom( type ) ) {
			return type.cast( value );
		}
		if ( java.sql.Date.class.isAssignableFrom( type ) ) {
			return type.cast( new java.sql.Date( value.getTimeInMillis() ) );
		}
		if ( java.sql.Time.class.isAssignableFrom( type ) ) {
			return type.cast( new java.sql.Time( value.getTimeInMillis() % 86_400_000 ) );
		}
		if ( java.sql.Timestamp.class.isAssignableFrom( type ) ) {
			return type.cast( new java.sql.Timestamp( value.getTimeInMillis() ) );
		}
		if ( Date.class.isAssignableFrom( type ) ) {
			return type.cast( new  Date( value.getTimeInMillis() ) );
		}
		throw unknownUnwrap( type );
	}

	public <X> Calendar wrap(X value, WrapperOptions options) {
		if ( value == null ) {
			return null;
		}
		else if (value instanceof Calendar calendar) {
			return calendar;
		}
		else if ( value instanceof Date date ) {
			final Calendar cal = new GregorianCalendar();
			cal.setTime( date );
			return cal;
		}
		else {
			throw unknownWrap( value.getClass() );
		}
	}

	@Override
	public boolean isWider(JavaType<?> javaType) {
		return switch ( javaType.getTypeName() ) {
			case "java.sql.Date" -> true;
			default -> false;
		};
	}

	@Override
	public int getDefaultSqlPrecision(Dialect dialect, JdbcType jdbcType) {
		return 0;
	}
}
